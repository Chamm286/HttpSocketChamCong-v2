/*
 * FaceID Engine — camera thật + face-api.js (model local).
 *
 * Luồng:
 *   1. getUserMedia mở webcam.
 *   2. tiny_face_detector + face_landmark_68_tiny phát hiện khuôn mặt / mắt.
 *   3. Tính EAR (Eye Aspect Ratio) để phát hiện blink → liveness.
 *   4. face_recognition trích descriptor 128 chiều.
 *   5. Chụp JPEG khuôn mặt gửi server; server tự so khớp.
 *
 * Không dùng CDN runtime: model nằm trong /assets/models.
 */
(function () {
  'use strict';

  const MODEL_URL = '/assets/models';
  const DETECT_INTERVAL = 90;       // ms giữa các lần detect liveness
  const BLINK_WINDOW_MS = 9000;     // thời gian tối đa chờ blink
  const MIN_FACE_RATIO = 0.20;      // khuôn mặt tối thiểu theo chiều rộng video
  const EAR_OPEN_MIN = 0.17;
  const EAR_BLINK_RATIO = 0.66;

  let modelsPromise = null;

  function loadModels(onStatus) {
    if (modelsPromise) return modelsPromise;
    if (!window.faceapi) {
      return Promise.reject(new Error('face-api.js chua duoc tai'));
    }
    modelsPromise = (async () => {
      if (onStatus) onStatus('Đang tải model FaceID...');
      await faceapi.nets.tinyFaceDetector.loadFromUri(MODEL_URL);
      await faceapi.nets.faceLandmark68TinyNet.loadFromUri(MODEL_URL);
      await faceapi.nets.faceRecognitionNet.loadFromUri(MODEL_URL);
      return true;
    })().catch(err => {
      modelsPromise = null;
      throw new Error('Không tải được model FaceID: ' + (err.message || err));
    });
    return modelsPromise;
  }

  function dist(a, b) {
    const dx = a.x - b.x;
    const dy = a.y - b.y;
    return Math.sqrt(dx * dx + dy * dy);
  }

  function eyeAspectRatio(points) {
    if (!points || points.length < 6) return 0;
    const vertical = dist(points[1], points[5]) + dist(points[2], points[4]);
    const horizontal = dist(points[0], points[3]);
    return horizontal <= 0 ? 0 : vertical / (2 * horizontal);
  }

  function averageEar(landmarks) {
    if (!landmarks) return 0;
    try {
      const left = eyeAspectRatio(landmarks.getLeftEye());
      const right = eyeAspectRatio(landmarks.getRightEye());
      return (left + right) / 2;
    } catch (e) {
      return 0;
    }
  }

  function percentile(values, p) {
    if (!values.length) return 0;
    const sorted = values.slice().sort((a, b) => a - b);
    const idx = Math.min(sorted.length - 1, Math.max(0, Math.round((sorted.length - 1) * p)));
    return sorted[idx];
  }

  class FaceScanner {
    constructor(options) {
      this.video = options.video;
      this.overlay = options.overlay || null;
      this.onStatus = options.onStatus || (() => {});
      this.onProgress = options.onProgress || (() => {});
      this.onFace = options.onFace || (() => {});
      this.onError = options.onError || (() => {});
      this.onReady = options.onReady || (() => {});

      this.stream = null;
      this.running = false;
      this.timer = null;
      this.options = null;
      this.earHistory = [];
      this.baseline = 0;
      this.minEar = 1;
      this.blinkSeen = false;
      this.blinkFinished = false;
      this.startedAt = 0;
      this.ready = false;
      this.lastResult = null;
      this.detecting = false;
    }

    async start() {
      if (this.running) return;
      await loadModels(this.onStatus);

      this.onProgress(8);
      this.onStatus('Đang mở camera...');
      if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia) {
        throw new Error('Trình duyệt không hỗ trợ camera (getUserMedia). Hãy dùng Chrome/Edge và HTTPS hoặc localhost.');
      }

      try {
        this.stream = await navigator.mediaDevices.getUserMedia({
          audio: false,
          video: {
            facingMode: 'user',
            width: { ideal: 640 },
            height: { ideal: 480 },
            frameRate: { ideal: 24, max: 30 }
          }
        });
      } catch (err) {
        const name = err && err.name ? err.name : '';
        if (name === 'NotAllowedError') {
          throw new Error('Bạn đã từ chối quyền camera. Hãy bật quyền camera cho trang này rồi thử lại.');
        }
        if (name === 'NotFoundError') {
          throw new Error('Không tìm thấy camera trên thiết bị.');
        }
        throw new Error('Không mở được camera: ' + (err.message || err));
      }

      this.video.srcObject = this.stream;
      this.video.muted = true;
      this.video.playsInline = true;
      await new Promise((resolve, reject) => {
        const timer = setTimeout(() => reject(new Error('Camera không phát được hình ảnh')), 8000);
        const done = () => { clearTimeout(timer); resolve(); };
        if (this.video.readyState >= 2) done();
        else {
          this.video.onloadedmetadata = done;
          this.video.oncanplay = done;
        }
        this.video.play().catch(() => {});
      });
      this.video.play().catch(() => {});

      if (this.overlay) {
        this.overlay.width = this.video.videoWidth || 640;
        this.overlay.height = this.video.videoHeight || 480;
      }

      this.options = new faceapi.TinyFaceDetectorOptions({
        inputSize: 224,
        scoreThreshold: 0.45
      });

      this.running = true;
      this.startedAt = Date.now();
      this.onProgress(16);
      this.onStatus('Đưa khuôn mặt vào khung oval...');
      this.loop();
    }

    async loop() {
      if (!this.running) return;
      const now = Date.now();
      if (!this.detecting && now - (this.lastDetectAt || 0) >= DETECT_INTERVAL) {
        this.lastDetectAt = now;
        this.detecting = true;
        this.detectLiveness().catch(() => {}).finally(() => { this.detecting = false; });
      }
      this.animation = requestAnimationFrame(() => this.loop());
    }

    async detectLiveness() {
      if (!this.running || this.video.readyState < 2) return;
      let result;
      try {
        result = await faceapi
          .detectSingleFace(this.video, this.options)
          .withFaceLandmarks(true);
      } catch (e) {
        return;
      }

      this.drawOverlay(result);

      if (!result || !result.detection) {
        this.earHistory = [];
        this.onProgress(18);
        this.onStatus('Chưa thấy khuôn mặt — hãy nhìn thẳng vào camera.');
        return;
      }

      const box = result.detection.box;
      const vw = this.video.videoWidth || 640;
      const vh = this.video.videoHeight || 480;
      const faceRatio = box.width / vw;
      const cx = box.x + box.width / 2;
      const cy = box.y + box.height / 2;
      const centered = cx > vw * 0.18 && cx < vw * 0.82 && cy > vh * 0.16 && cy < vh * 0.84;

      if (faceRatio < MIN_FACE_RATIO || !centered) {
        this.earHistory = [];
        this.onProgress(24);
        this.onStatus('Đưa khuôn mặt gần hơn và căn giữa khung.');
        return;
      }

      const ear = averageEar(result.landmarks);
      if (ear <= 0) return;

      this.earHistory.push(ear);
      if (this.earHistory.length > 40) this.earHistory.shift();
      this.minEar = Math.min(this.minEar, ear);

      if (this.earHistory.length >= 8) {
        this.baseline = percentile(this.earHistory, 0.75);
      }

      const elapsed = Date.now() - this.startedAt;
      const openEnough = this.baseline >= EAR_OPEN_MIN && ear >= this.baseline * 0.82;

      if (!this.blinkSeen && openEnough) {
        this.onProgress(38);
        this.onStatus('Giữ yên... bây giờ hãy CHỚP MẮT một lần.');
      } else if (!this.blinkSeen && this.baseline > 0 && ear < this.baseline * EAR_BLINK_RATIO) {
        this.blinkSeen = true;
        this.blinkFinished = false;
        this.onProgress(62);
        this.onStatus('Đã thấy mắt nhắm — mở mắt ra...');
      } else if (this.blinkSeen && !this.blinkFinished && openEnough) {
        this.blinkFinished = true;
        this.onProgress(82);
        this.onStatus('Liveness OK — đang khóa mẫu khuôn mặt...');
        await this.finishCapture();
        return;
      }

      if (!this.blinkSeen && elapsed > BLINK_WINDOW_MS * 2) {
        this.onError('Không phát hiện liveness (blink). Vui lòng thử lại.');
        this.stop();
      } else if (!this.blinkSeen && elapsed > BLINK_WINDOW_MS) {
        this.onProgress(30);
        this.onStatus('Chưa thấy blink. Hãy chớp mắt rõ hơn hoặc tăng ánh sáng.');
      }
    }

    async finishCapture() {
      if (!this.running || this.ready) return;
      this.ready = true;
      // Chờ 1 nhịp để khuôn mặt ổn định sau blink.
      await new Promise(r => setTimeout(r, 260));
      try {
        const payload = await this.capture();
        const depth = this.baseline > 0
          ? Math.max(0, (this.baseline - this.minEar) / this.baseline)
          : 0;
        payload.livenessScore = Math.max(0.6, Math.min(1, 0.68 + depth * 1.4));
        payload.baselineEar = this.baseline;
        payload.minEar = this.minEar;
        this.lastResult = payload;
        this.onProgress(100);
        this.onStatus('FaceID hoàn tất — đang xác thực với server...');
        this.onReady(payload);
      } catch (err) {
        this.ready = false;
        this.onError(err.message || 'Không trích xuất được khuôn mặt');
      }
    }

    async capture() {
      const result = await faceapi
        .detectSingleFace(this.video, this.options)
        .withFaceLandmarks(true)
        .withFaceDescriptor();
      if (!result || !result.descriptor) {
        throw new Error('Không thấy khuôn mặt khi chụp mẫu');
      }
      if (!result.detection || result.detection.score < 0.5) {
        throw new Error('Ảnh khuôn mặt chưa đủ rõ');
      }

      const vw = this.video.videoWidth || 640;
      const vh = this.video.videoHeight || 480;
      const box = result.detection.box;
      const margin = box.width * 0.38;
      const x = Math.max(0, box.x - margin);
      const y = Math.max(0, box.y - margin * 1.15);
      const w = Math.min(vw - x, box.width + margin * 2);
      const h = Math.min(vh - y, box.height + margin * 2.2);

      const outW = 320;
      const outH = Math.max(220, Math.round(outW * (h / w)));
      const canvas = document.createElement('canvas');
      canvas.width = outW;
      canvas.height = outH;
      const ctx = canvas.getContext('2d');
      ctx.drawImage(this.video, x, y, w, h, 0, 0, outW, outH);

      return {
        descriptor: Array.from(result.descriptor),
        detectionScore: result.detection.score,
        ear: averageEar(result.landmarks),
        image: canvas.toDataURL('image/jpeg', 0.78),
        width: outW,
        height: outH
      };
    }

    drawOverlay(result) {
      if (!this.overlay) return;
      const ctx = this.overlay.getContext('2d');
      const w = this.overlay.width;
      const h = this.overlay.height;
      ctx.clearRect(0, 0, w, h);

      if (!result || !result.detection) return;
      const box = result.detection.box;
      const color = this.blinkSeen ? '#22c55e' : '#38bdf8';
      ctx.save();
      ctx.strokeStyle = color;
      ctx.lineWidth = 3;
      ctx.shadowColor = color;
      ctx.shadowBlur = 14;

      const corner = Math.max(18, box.width * 0.18);
      const x = box.x, y = box.y, r = x + box.width, b = y + box.height;
      const paths = [
        [[x, y + corner], [x, y], [x + corner, y]],
        [[r - corner, y], [r, y], [r, y + corner]],
        [[x, b - corner], [x, b], [x + corner, b]],
        [[r - corner, b], [r, b], [r, b - corner]]
      ];
      for (const p of paths) {
        ctx.beginPath();
        ctx.moveTo(p[0][0], p[0][1]);
        ctx.lineTo(p[1][0], p[1][1]);
        ctx.lineTo(p[2][0], p[2][1]);
        ctx.stroke();
      }
      ctx.restore();
    }

    stop() {
      this.running = false;
      if (this.animation) cancelAnimationFrame(this.animation);
      if (this.timer) clearTimeout(this.timer);
      if (this.stream) {
        this.stream.getTracks().forEach(t => t.stop());
        this.stream = null;
      }
      if (this.video) {
        try { this.video.pause(); } catch (e) {}
        this.video.srcObject = null;
      }
      if (this.overlay) {
        const ctx = this.overlay.getContext('2d');
        ctx.clearRect(0, 0, this.overlay.width, this.overlay.height);
      }
    }
  }

  window.FaceEngine = {
    loadModels,
    FaceScanner,
    eyeAspectRatio
  };
})();
