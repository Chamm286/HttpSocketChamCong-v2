/* Trang quét FaceID dùng cho Desktop Client (browser bridge). */
(function () {
  'use strict';

  const params = new URLSearchParams(location.search);
  const sessionId = params.get('session') || '';
  const $ = sel => document.querySelector(sel);

  const statusEl = $('#face-status');
  const barEl = $('#face-progress-bar');
  const retryBtn = $('#face-retry');
  const loadingEl = $('#face-loading');
  const actionEl = $('#scan-action');
  const metaEl = $('#scan-meta');

  let scanner = null;
  let action = 'CHECKIN';
  let starting = false;
  let finished = false;

  const setStatus = (text, tone = '') => {
    statusEl.textContent = text;
    statusEl.dataset.tone = tone;
  };
  const setProgress = p => {
    barEl.style.width = Math.max(0, Math.min(100, p)) + '%';
    const detect = $('[data-step="detect"]');
    const blink = $('[data-step="blink"]');
    const match = $('[data-step="match"]');
    if (!detect || !blink || !match) return;
    detect.classList.toggle('active', p >= 18 && p < 62);
    detect.classList.toggle('done', p >= 62);
    blink.classList.toggle('active', p >= 38 && p < 82);
    blink.classList.toggle('done', p >= 82);
    match.classList.toggle('active', p >= 82 && p < 100);
    match.classList.toggle('done', p >= 100);
  };
  const showError = message => {
    setStatus(message || 'FaceID thất bại', 'error');
    setProgress(0);
    if (loadingEl) loadingEl.hidden = true;
    if (retryBtn) retryBtn.hidden = false;
    try { scanner?.stop(); } catch (e) {}
    scanner = null;
    starting = false;
  };

  async function loadContext() {
    if (!sessionId) {
      showError('Thiếu session FaceID. Hãy mở lại từ Desktop Client.');
      return false;
    }
    metaEl.textContent = 'Session: ' + sessionId.slice(0, 8) + '…';
    try {
      const res = await fetch('/api/face/desktop/context?session=' + encodeURIComponent(sessionId), {
        cache: 'no-store'
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || 'Phiên FaceID không hợp lệ');
      action = data.action || 'CHECKIN';
      actionEl.textContent = action === 'CHECKOUT' ? 'CHECK-OUT' : 'CHECK-IN';
      return true;
    } catch (err) {
      showError(err.message || 'Không đọc được phiên FaceID');
      return false;
    }
  }

  async function verify(payload) {
    setProgress(91);
    setStatus('Đang gửi descriptor lên server để so khớp…');
    try {
      const res = await fetch('/api/face/desktop/verify', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json; charset=UTF-8' },
        body: JSON.stringify({
          session: sessionId,
          descriptor: payload.descriptor,
          livenessScore: payload.livenessScore,
          detectionScore: payload.detectionScore,
          image: payload.image
        })
      });
      const data = await res.json().catch(() => ({}));
      if (!res.ok) throw new Error(data.error || data.message || 'Xác thực khuôn mặt thất bại');

      finished = true;
      try { scanner?.stop(); } catch (e) {}
      setProgress(100);
      setStatus(
        data.enrolled
          ? 'Đã đăng ký khuôn mặt lần đầu. Desktop Client sẽ tự chấm công.'
          : `FaceID hợp lệ · độ khớp ${Math.round((data.score || 0) * 100)}% · đang gửi về Desktop.`,
        'success'
      );
      if (retryBtn) retryBtn.hidden = true;
      try { scanner?.stop(); } catch (e) {}
      setTimeout(() => {
        try { window.close(); } catch (e) {}
      }, 3200);
    } catch (err) {
      showError(err.message || 'Server từ chối khuôn mặt');
    }
  }

  async function startScan() {
    if (starting || finished) return;
    starting = true;
    if (retryBtn) retryBtn.hidden = true;
    setProgress(6);
    setStatus('Đang mở camera và tải model FaceID…');

    try {
      if (!window.FaceEngine) throw new Error('Không tải được face-engine.js');
      scanner = new FaceEngine.FaceScanner({
        video: $('#face-video'),
        overlay: $('#face-overlay'),
        onStatus: text => setStatus(text),
        onProgress: p => setProgress(p),
        onError: err => showError(err.message || String(err)),
        onReady: payload => verify(payload)
      });
      await scanner.start();
      if (loadingEl) loadingEl.hidden = true;
      setStatus('Camera sẵn sàng — nhìn thẳng và chớp mắt một lần.');
    } catch (err) {
      showError(err.message || 'Không mở được camera');
    } finally {
      starting = false;
    }
  }

  retryBtn?.addEventListener('click', () => {
    finished = false;
    startScan();
  });
  $('#close-page')?.addEventListener('click', () => {
    try { scanner?.stop(); } catch (e) {}
    window.close();
    setTimeout(() => {
      setStatus('Bạn có thể đóng tab này và quay lại Desktop Client.', '');
    }, 300);
  });

  (async () => {
    const ok = await loadContext();
    if (ok) {
      // getUserMedia cần user gesture ở một số cấu hình; nút Quét lại cũng là fallback.
      setTimeout(startScan, 120);
    }
  })();
})();
