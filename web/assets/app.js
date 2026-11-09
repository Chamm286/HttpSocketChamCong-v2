'use strict';

/* ============================================================
   HttpSocketChamCong — Web client
   Vanilla JS, gọi API JSON qua HTTP/1.1 bằng fetch().
   ============================================================ */

/* ---------------- DOM helpers ---------------- */
const $  = (sel, root = document) => root.querySelector(sel);
const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));
const pad2 = n => String(n).padStart(2, '0');

const esc = s => String(s ?? '').replace(/[&<>"']/g, c => ({
  '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;'
}[c]));

const icon = (name, cls = '') =>
  `<svg class="icon ${cls}" aria-hidden="true"><use href="#i-${name}"/></svg>`;

/* ============================================================
   NETWORK CONSOLE — hiển thị HTTP/1.1 raw request/response
   ============================================================ */
const netState = {
  entries: [],
  seq: 0,
  max: 60,
};

function netNow() {
  const d = new Date();
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
}

function netIsOpen() {
  const el = $('#net-console');
  return !!el && !el.hidden;
}

function netHeaderName(name) {
  return String(name || '').split('-')
    .map(p => p ? p.charAt(0).toUpperCase() + p.slice(1).toLowerCase() : p)
    .join('-');
}

function netStatusText(code) {
  return ({
    200: 'OK', 201: 'Created', 204: 'No Content',
    400: 'Bad Request', 401: 'Unauthorized', 403: 'Forbidden',
    404: 'Not Found', 405: 'Method Not Allowed', 409: 'Conflict',
    429: 'Too Many Requests', 500: 'Internal Server Error',
    501: 'Not Implemented', 505: 'HTTP Version Not Supported',
  })[code] || '';
}

/** Che mật khẩu/token khỏi log trình duyệt. */
function netSafeBody(path, bodyString) {
  if (bodyString === undefined || bodyString === null) return bodyString;
  const sensitive = path === '/api/login'
    || path === '/api/register'
    || path === '/api/change-password'
    || path.startsWith('/api/admin/users');
  if (!sensitive) return bodyString;
  try {
    const obj = JSON.parse(bodyString);
    ['password', 'oldPassword', 'newPassword', 'confirmPassword'].forEach(k => {
      if (k in obj) obj[k] = '<redacted>';
    });
    return JSON.stringify(obj);
  } catch {
    return '<redacted>';
  }
}

/** Không đưa mật khẩu/token của các API nhạy cảm vào Network Console. */
function netShouldTrace(path) {
  return path !== '/api/login'
    && path !== '/api/register'
    && path !== '/api/change-password'
    && !path.startsWith('/api/admin/users');
}

/** Tái dựng request thô khi response không kèm X-Request-Trace. */
function netBuildRequest(method, path, headers, bodyString) {
  const url = new URL(path, location.href);
  const h = {};
  Object.entries(headers || {}).forEach(([k, v]) => {
    if (k.toLowerCase() === 'authorization') h.Authorization = 'Bearer <redacted>';
    else h[netHeaderName(k)] = v;
  });
  h.Host = url.host;
  if (bodyString !== undefined && bodyString !== null) {
    h['Content-Type'] = h['Content-Type'] || 'application/json; charset=UTF-8';
    h['Content-Length'] = String(new TextEncoder().encode(bodyString).length);
  }
  h.Connection = 'keep-alive';
  h.Accept = h.Accept || '*/*';
  h['User-Agent'] = h['User-Agent'] || navigator.userAgent;

  const lines = [`${method} ${url.pathname}${url.search} HTTP/1.1`];
  Object.entries(h).forEach(([k, v]) => lines.push(`${k}: ${v}`));
  lines.push('');
  if (bodyString !== undefined && bodyString !== null && bodyString !== '') {
    lines.push(bodyString);
  }
  return lines.join('\r\n');
}

function netBuildResponse(res, text) {
  const statusText = res.statusText || netStatusText(res.status);
  const lines = [`HTTP/1.1 ${res.status} ${statusText}`.trim()];
  const hs = [];
  res.headers.forEach((value, key) => {
    // Header trace chỉ dùng nội bộ cho console, không lặp lại trong raw response.
    if (key.toLowerCase() === 'x-request-trace') return;
    hs.push(`${netHeaderName(key)}: ${value}`);
  });
  hs.sort((a, b) => a.localeCompare(b));
  lines.push(...hs);
  lines.push('');
  if (text) lines.push(text);
  return lines.join('\r\n');
}

function netDecodeTrace(base64) {
  if (!base64) return '';
  try {
    const bin = atob(base64);
    const bytes = new Uint8Array(bin.length);
    for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
    return new TextDecoder('utf-8').decode(bytes);
  } catch {
    return '';
  }
}

function netEntryHtml(e, index) {
  const ok = e.status >= 200 && e.status < 300;
  const open = index === 0 || e.status >= 400;
  const methodCls = e.method === 'POST' ? 'post' : e.method === 'DELETE' ? 'delete' : '';
  return `
    <details class="net-entry" ${open ? 'open' : ''}>
      <summary class="net-entry-head">
        <span class="net-method ${methodCls}">${esc(e.method)}</span>
        <span class="net-path" title="${esc(e.path)}">${esc(e.path)}</span>
        <span class="net-status ${ok ? 'ok' : 'err'}">${e.status || 'ERR'} ${esc(e.statusText || '')}</span>
        <span class="net-time">${esc(e.time)} · ${e.duration} ms</span>
      </summary>
      <div class="net-entry-body">
        <div class="net-section">
          <div class="net-label">→ Request · raw HTTP/1.1</div>
          <pre class="net-pre req">${esc(e.requestRaw)}</pre>
        </div>
        <div class="net-section">
          <div class="net-label response">← Response · raw HTTP/1.1</div>
          <pre class="net-pre res">${esc(e.responseRaw)}</pre>
        </div>
      </div>
    </details>`;
}

function netRender() {
  const list = $('#net-list');
  if (!list) return;

  const count = $('#net-count');
  if (count) count.textContent = String(netState.entries.length);

  if (!netState.entries.length) {
    list.innerHTML = '<div class="net-empty">Chưa có request.<br>Bấm CHECK-IN / CHECK-OUT để xem raw HTTP/1.1.</div>';
    return;
  }
  list.innerHTML = netState.entries.map(netEntryHtml).join('');
}

function netRecord({ method, path, requestRaw, responseRaw, status, statusText, duration }) {
  netState.entries.unshift({
    id: ++netState.seq,
    method: String(method || '?').toUpperCase(),
    path,
    requestRaw,
    responseRaw,
    status: Number(status) || 0,
    statusText: statusText || netStatusText(status),
    duration: Math.max(0, Math.round(duration || 0)),
    time: netNow(),
  });
  if (netState.entries.length > netState.max) {
    netState.entries.length = netState.max;
  }

  const dot = $('#net-console-dot');
  if (dot) dot.hidden = netIsOpen();
  netRender();
}

function netOpen() {
  const el = $('#net-console');
  if (!el) return;
  el.hidden = false;
  el.setAttribute('aria-hidden', 'false');
  const dot = $('#net-console-dot');
  if (dot) dot.hidden = true;
  netRender();
  const list = $('#net-list');
  if (list) list.scrollTop = 0;
}

function netClose() {
  const el = $('#net-console');
  if (!el) return;
  el.hidden = true;
  el.setAttribute('aria-hidden', 'true');
}

function netToggle() {
  if (netIsOpen()) netClose();
  else netOpen();
}

function netClear() {
  netState.entries = [];
  const dot = $('#net-console-dot');
  if (dot) dot.hidden = true;
  netRender();
}

/* Cho phép debug/test từ Console DevTools. */
window.NetConsole = {
  get entries() { return netState.entries; },
  open: netOpen,
  close: netClose,
  clear: netClear,
  render: netRender,
};

/* ---------------- Session state ---------------- */
const state = {
  token: sessionStorage.getItem('cc.token') || '',
  user: null,
  offsetMs: 0,
  timers: [],
  historyView: null,
  currentPage: '',
};

try {
  state.user = JSON.parse(sessionStorage.getItem('cc.user') || 'null');
} catch { state.user = null; }

function persistSession(token, user) {
  state.token = token;
  state.user = user;
  sessionStorage.setItem('cc.token', token);
  sessionStorage.setItem('cc.user', JSON.stringify(user));
}

function clearSession() {
  state.token = '';
  state.user = null;
  sessionStorage.removeItem('cc.token');
  sessionStorage.removeItem('cc.user');
}

/* ---------------- API client ---------------- */
class ApiError extends Error {
  constructor(message, status, data) {
    super(message);
    this.status = status;
    this.data = data;
  }
}

async function api(path, { method = 'GET', body } = {}) {
  const httpMethod = String(method || 'GET').toUpperCase();
  const headers = {};
  if (state.token) headers.Authorization = `Bearer ${state.token}`;
  if (body !== undefined) headers['Content-Type'] = 'application/json; charset=UTF-8';

  const bodyText = body !== undefined ? JSON.stringify(body) : undefined;
  const traceBody = netSafeBody(path, bodyText);
  const shouldTrace = netShouldTrace(path);
  const started = performance.now();

  let res;
  try {
    res = await fetch(path, {
      method: httpMethod,
      headers,
      body: bodyText,
      cache: 'no-store',
    });
  } catch (err) {
    if (shouldTrace) {
      netRecord({
        method: httpMethod,
        path,
        requestRaw: netBuildRequest(httpMethod, path, headers, traceBody),
        responseRaw: `NETWORK ERROR\r\n\r\n${err.message || err}`,
        status: 0,
        statusText: 'Network Error',
        duration: performance.now() - started,
      });
      if (path === '/api/checkin' || path === '/api/checkout') netOpen();
    }
    throw err;
  }

  const text = await res.text();
  let data = null;
  if (text) {
    try { data = JSON.parse(text); }
    catch { data = { error: text }; }
  }

  if (shouldTrace) {
    // Server gắn X-Request-Trace cho check-in/check-out: đây là request THÔ
    // do server parse được, gồm đúng header/body mà trình duyệt đã gửi.
    const trace = res.headers.get('x-request-trace');
    const requestRaw = trace
      ? netDecodeTrace(trace)
      : netBuildRequest(httpMethod, path, headers, traceBody);

    netRecord({
      method: httpMethod,
      path,
      requestRaw,
      responseRaw: netBuildResponse(res, text),
      status: res.status,
      statusText: res.statusText,
      duration: performance.now() - started,
    });
    if (path === '/api/checkin' || path === '/api/checkout') netOpen();
  }

  if (res.status === 401 && state.token && path !== '/api/login') {
    handleUnauthorized();
    throw new ApiError('Phiên đăng nhập đã hết hạn', 401, data);
  }
  if (!res.ok) {
    throw new ApiError(data?.error || `Lỗi ${res.status} ${res.statusText}`, res.status, data);
  }
  return data;
}

function handleUnauthorized() {
  clearSession();
  showLogin('Phiên đăng nhập đã hết hạn. Vui lòng đăng nhập lại.');
}

/* ---------------- Formatting ---------------- */
function serverNow() { return new Date(Date.now() + state.offsetMs); }

function setServerTime(dateISO, hhmmss) {
  if (!dateISO || !hhmmss) return;
  const [y, mo, d] = dateISO.split('-').map(Number);
  const [h, mi, s] = hhmmss.split(':').map(Number);
  if (!y || !mo || !d) return;
  const server = new Date(y, mo - 1, d, h || 0, mi || 0, s || 0);
  state.offsetMs = server.getTime() - Date.now();
}

function isoOf(date) {
  return `${date.getFullYear()}-${pad2(date.getMonth() + 1)}-${pad2(date.getDate())}`;
}
function isoToday() { return isoOf(serverNow()); }
function firstOfMonthISO() {
  const d = serverNow();
  return `${d.getFullYear()}-${pad2(d.getMonth() + 1)}-01`;
}
function nowHMSS() {
  const d = serverNow();
  return `${pad2(d.getHours())}:${pad2(d.getMinutes())}:${pad2(d.getSeconds())}`;
}
function hmss(date) {
  return `${pad2(date.getHours())}:${pad2(date.getMinutes())}:${pad2(date.getSeconds())}`;
}

function fmtDate(iso) {
  if (!iso) return '—';
  const [y, m, d] = iso.split('-');
  return `${d}/${m}/${y}`;
}
function weekdayName(iso) {
  const names = ['Chủ nhật', 'Thứ hai', 'Thứ ba', 'Thứ tư', 'Thứ năm', 'Thứ sáu', 'Thứ bảy'];
  const d = new Date(`${iso}T00:00:00`);
  return isNaN(d) ? '' : names[d.getDay()];
}
function parseMinutes(t) {
  if (!t) return null;
  const p = String(t).split(':').map(Number);
  if (p.length < 2 || p.some(isNaN)) return null;
  return p[0] * 60 + p[1] + (p[2] || 0) / 60;
}
function workedMinutes(checkIn, checkOut) {
  const a = parseMinutes(checkIn);
  const b = checkOut ? parseMinutes(checkOut) : (serverNow().getHours() * 60 + serverNow().getMinutes() + serverNow().getSeconds() / 60);
  if (a === null || b === null) return 0;
  let m = b - a;
  if (m < 0) m += 24 * 60;
  return Math.max(0, Math.round(m));
}
function fmtDuration(minutes) {
  const m = Math.max(0, Math.round(minutes || 0));
  if (m < 60) return `${m} phút`;
  const h = Math.floor(m / 60);
  const r = m % 60;
  return r ? `${h}g ${r}p` : `${h} giờ`;
}
function fmtHours(minutes) {
  return `${(Math.max(0, minutes || 0) / 60).toFixed(1)}h`;
}
function initials(name) {
  const parts = String(name || '?').trim().split(/\s+/).filter(Boolean);
  if (!parts.length) return '?';
  if (parts.length === 1) return parts[0].slice(0, 2).toUpperCase();
  return (parts[0][0] + parts[parts.length - 1][0]).toUpperCase();
}

/* ---------------- Toast ---------------- */
function toast(title, message = '', type = 'success') {
  const root = $('#toast-root');
  if (!root) return;
  const el = document.createElement('div');
  el.className = `toast ${type}`;
  const ic = type === 'error' ? 'alert' : type === 'info' ? 'info' : 'check-circle';
  el.innerHTML = `
    <span class="toast-ic">${icon(ic)}</span>
    <div>
      <strong>${esc(title)}</strong>
      ${message ? `<p>${esc(message)}</p>` : ''}
    </div>`;
  root.appendChild(el);
  setTimeout(() => {
    el.classList.add('out');
    setTimeout(() => el.remove(), 220);
  }, 3800);
}

/* ---------------- Modal ---------------- */
function openModal({
  title,
  subtitle = '',
  body = '',
  confirmText = 'Xác nhận',
  cancelText = 'Hủy',
  tone = 'primary',
  iconName = 'info',
  wide = false,
  hideCancel = false,
  onConfirm = null,
}) {
  return new Promise(resolve => {
    const root = $('#modal-root');
    const confirmClass = tone === 'danger' ? 'btn-danger'
      : tone === 'success' ? 'btn-success' : 'btn-primary';

    root.innerHTML = `
      <div class="modal-backdrop">
        <div class="modal ${wide ? 'modal-wide' : ''}" role="dialog" aria-modal="true">
          <div class="modal-head">
            <div class="modal-ic ${tone === 'primary' ? '' : tone}">${icon(iconName)}</div>
            <div>
              <h3>${esc(title)}</h3>
              ${subtitle ? `<p>${esc(subtitle)}</p>` : ''}
            </div>
          </div>
          <div class="modal-body">${body}</div>
          <div class="modal-error" role="alert"></div>
          <div class="modal-foot">
            ${hideCancel ? '' : `<button type="button" class="btn btn-ghost modal-cancel">${esc(cancelText)}</button>`}
            <button type="button" class="btn ${confirmClass} modal-confirm">${esc(confirmText)}</button>
          </div>
        </div>
      </div>`;

    const backdrop = $('.modal-backdrop', root);
    const errBox = $('.modal-error', root);
    let done = false;

    const close = value => {
      if (done) return;
      done = true;
      root.innerHTML = '';
      document.removeEventListener('keydown', onKey);
      resolve(value);
    };

    const onKey = e => {
      if (e.key === 'Escape') close(false);
      if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') {
        // Enter trong input của modal = xác nhận
        if (e.target.closest('.modal')) $('.modal-confirm', root)?.click();
      }
    };
    document.addEventListener('keydown', onKey);

    $('.modal-cancel', root)?.addEventListener('click', () => close(false));
    backdrop.addEventListener('mousedown', e => { if (e.target === backdrop) close(false); });

    $('.modal-confirm', root).addEventListener('click', async e => {
      const btn = e.currentTarget;
      btn.disabled = true;
      errBox.textContent = '';
      try {
        const result = onConfirm ? await onConfirm(root) : true;
        if (result !== false) close(true);
      } catch (err) {
        errBox.textContent = err?.message || String(err);
      } finally {
        btn.disabled = false;
      }
    });

    setTimeout(() => {
      const first = $('input, select, textarea', root);
      if (first) first.focus();
    }, 40);
  });
}

function confirmDialog({ title, message, confirmText = 'Xác nhận', tone = 'danger', iconName = 'alert' }) {
  return openModal({
    title,
    body: `<p>${message}</p>`,
    confirmText,
    tone,
    iconName,
    onConfirm: () => true,
  });
}

/* ---------------- Theme ---------------- */
function applyTheme(theme) {
  document.documentElement.dataset.theme = theme;
  try { localStorage.setItem('cc.theme', theme); } catch {}
  const btn = $('#theme-btn');
  if (btn) btn.innerHTML = icon(theme === 'dark' ? 'sun' : 'moon');
}

/* ---------------- Header clock ---------------- */
let globalClockTimer = null;

function startGlobalClock() {
  clearInterval(globalClockTimer);
  const tick = () => {
    const d = serverNow();
    const text = hmss(d);
    $$('[data-clock]').forEach(el => { el.textContent = text; });
    const top = $('#top-clock');
    if (top) top.textContent = text;
  };
  tick();
  globalClockTimer = setInterval(tick, 1000);
}

function cleanupTimers() {
  state.timers.forEach(t => clearInterval(t));
  state.timers = [];
}

/* ---------------- Sidebar ---------------- */
function openSidebar() {
  $('#sidebar')?.classList.add('open');
  $('#sidebar-backdrop')?.classList.add('show');
}
function closeSidebar() {
  $('#sidebar')?.classList.remove('open');
  $('#sidebar-backdrop')?.classList.remove('show');
}

/* ---------------- Route table ---------------- */
const ROUTES = {
  dashboard: {
    title: 'Trang chủ',
    sub: 'Tổng quan hệ thống chấm công',
    render: renderDashboard,
  },
  attendance: {
    title: 'Chấm công hôm nay',
    sub: 'Điểm danh vào / ra — giờ do server ghi nhận',
    render: renderAttendance,
  },
  history: {
    title: 'Lịch sử chấm công',
    sub: 'Tra cứu theo khoảng ngày',
    render: root => renderHistory(root, false),
  },
  stats: {
    title: 'Thống kê',
    sub: 'Số liệu chấm công của bạn',
    render: renderStats,
  },
  leaves: {
    title: 'Xin nghỉ phép',
    sub: 'Gửi đơn và theo dõi trạng thái',
    render: root => renderLeaves(root, false),
  },
  shifts: {
    title: 'Ca làm việc',
    sub: 'Đăng ký ca sáng / chiều / tối',
    render: renderShifts,
  },
  notes: {
    title: 'Ghi chú công việc',
    sub: 'Kế hoạch và nội dung trong ngày',
    render: root => renderNotes(root, false),
  },
  profile: {
    title: 'Hồ sơ & mật khẩu',
    sub: 'Thông tin tài khoản và bảo mật',
    render: renderProfile,
  },
  'admin-attendance': {
    title: 'Bảng chấm công',
    sub: 'Toàn bộ nhân viên — lọc và xuất CSV',
    admin: true,
    render: root => renderHistory(root, true),
  },
  'admin-users': {
    title: 'Quản lý nhân viên',
    sub: 'Thêm, sửa, xóa tài khoản',
    admin: true,
    render: renderAdminUsers,
  },
  'admin-leaves': {
    title: 'Duyệt nghỉ phép',
    sub: 'Xử lý đơn của nhân viên',
    admin: true,
    render: root => renderLeaves(root, true),
  },
  'admin-notes': {
    title: 'Ghi chú nhân viên',
    sub: 'Xem ghi chú công việc toàn công ty',
    admin: true,
    render: root => renderNotes(root, true),
  },
  'admin-monitor': {
    title: 'Giám sát Online / Offline',
    sub: 'Theo dõi kết nối và hoạt động của nhân viên',
    admin: true,
    render: renderAdminMonitor,
  },
};

function navigate(page) {
  if (location.hash !== `#/${page}`) location.hash = `#/${page}`;
  else renderRoute();
}

function renderRoute() {
  if (!state.user) { showLogin(); return; }

  let key = (location.hash || '').replace(/^#\/?/, '') || 'dashboard';
  let route = ROUTES[key];
  if (!route) { key = 'dashboard'; route = ROUTES.dashboard; }

  if (route.admin && state.user.role !== 'ADMIN') {
    location.hash = '#/dashboard';
    return;
  }

  cleanupTimers();
  state.currentPage = key;
  closeSidebar();

  $$('#nav .nav-item[data-page]').forEach(a => {
    a.classList.toggle('active', a.dataset.page === key);
  });

  const titleEl = $('#page-title');
  const subEl = $('#page-subtitle');
  if (titleEl) titleEl.textContent = route.title;
  if (subEl) subEl.textContent = route.sub;

  const page = $('#page');
  page.innerHTML = `
    <div class="skeleton sk-hero"></div>
    <div class="stat-grid">
      <div class="skeleton sk-row"></div>
      <div class="skeleton sk-row"></div>
      <div class="skeleton sk-row"></div>
      <div class="skeleton sk-row"></div>
    </div>
    <div class="card card-pad" style="margin-top:20px">
      <div class="sk-lines">
        <div class="skeleton sk-line short"></div>
        <div class="skeleton sk-line"></div>
        <div class="skeleton sk-line"></div>
      </div>
    </div>`;

  window.scrollTo({ top: 0, behavior: 'smooth' });

  Promise.resolve(route.render(page)).catch(err => {
    if (err?.status === 401) return;
    page.innerHTML = pageError(err);
  });
}

function pageError(err) {
  return `
    <div class="page-error card">
      ${icon('alert-triangle', 'icon-xl')}
      <strong>Không tải được dữ liệu</strong>
      <p>${esc(err?.message || 'Lỗi không xác định')}</p>
      <button type="button" class="btn btn-primary" onclick="renderRoute()">${icon('refresh')} Thử lại</button>
    </div>`;
}

/* ---------------- Auth flow ---------------- */
function showLogin(message = '') {
  cleanupTimers();
  clearInterval(globalClockTimer);
  document.body.classList.remove('is-admin');
  $('#app-view').hidden = true;
  const loginView = $('#login-view');
  loginView.hidden = false;
  $('#login-error').textContent = message;
  const hash = location.hash;
  if (hash) history.replaceState(null, '', location.pathname + location.search);
  setTimeout(() => $('#login-username')?.focus(), 60);
}

function showApp() {
  if (!state.user) { showLogin(); return; }
  $('#login-view').hidden = true;
  $('#app-view').hidden = false;

  const isAdmin = state.user.role === 'ADMIN';
  document.body.classList.toggle('is-admin', isAdmin);

  const name = state.user.fullName || state.user.username;
  const roleText = isAdmin ? 'Quản trị viên' : 'Nhân viên';
  const ini = initials(name);

  ['sidebar-avatar', 'topbar-avatar'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = ini;
  });
  ['sidebar-name', 'topbar-name'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = name;
  });
  ['sidebar-role', 'topbar-role'].forEach(id => {
    const el = document.getElementById(id);
    if (el) el.textContent = roleText;
  });

  startGlobalClock();

  if (!location.hash) location.hash = '#/dashboard';
  else renderRoute();
}

async function login(username, password) {
  const data = await api('/api/login', { method: 'POST', body: { username, password } });
  persistSession(data.token, data.user);
  showApp();
  toast('Đăng nhập thành công', `Xin chào ${data.user.fullName || data.user.username}!`);
}

async function logout() {
  try {
    if (state.token) await api('/api/logout', { method: 'POST', body: {} });
  } catch { /* bỏ qua */ }
  clearSession();
  showLogin();
  toast('Đã đăng xuất', 'Hẹn gặp lại bạn!', 'info');
}

/* ---------------- Shared render helpers ---------------- */
function userCell(row) {
  const name = row.fullName || row.username;
  return `
    <div class="cell-user">
      <div class="avatar">${esc(initials(name))}</div>
      <div>
        <strong>${esc(name)}</strong>
        <small>@${esc(row.username)}</small>
      </div>
    </div>`;
}

function rowStatus(row) {
  if (row.checkOut) return 'COMPLETED';
  if (row.checkIn) return 'WORKING';
  return 'ABSENT';
}

function rowStatusBadge(row) {
  const s = rowStatus(row);
  if (s === 'COMPLETED') return '<span class="badge success">Hoàn thành</span>';
  if (s === 'WORKING') return '<span class="badge warning">Đang làm</span>';
  return '<span class="badge neutral">Chưa vào</span>';
}

function isLate(row) {
  return row.checkIn && row.checkIn > '08:00:00';
}

function roleBadge(role) {
  return role === 'ADMIN'
    ? '<span class="badge violet">Quản trị</span>'
    : '<span class="badge info">Nhân viên</span>';
}

function leaveStatusBadge(status) {
  if (status === 'APPROVED') return '<span class="badge success">Đã duyệt</span>';
  if (status === 'REJECTED') return '<span class="badge danger">Từ chối</span>';
  return '<span class="badge warning">Chờ duyệt</span>';
}

function leaveTypeLabel(type) {
  return ({ NGHI_PHEP: 'Nghỉ phép', DI_MUON: 'Đi muộn', CONG_TAC: 'Công tác' })[type] || type || 'Nghỉ phép';
}

function leaveTypeBadge(type) {
  const cls = type === 'DI_MUON' ? 'warning' : type === 'CONG_TAC' ? 'info' : 'violet';
  return `<span class="badge ${cls}">${esc(leaveTypeLabel(type))}</span>`;
}

function shiftLabel(type) {
  return ({ SANG: 'Ca sáng', CHIEU: 'Ca chiều', TOI: 'Ca tối' })[type] || type;
}
function shiftTime(type) {
  return ({ SANG: '08:00 – 12:00', CHIEU: '13:00 – 17:00', TOI: '18:00 – 22:00' })[type] || '';
}

function emptyState(title, text, iconName = 'info') {
  return `
    <div class="empty">
      <div class="empty-ic">${icon(iconName)}</div>
      <strong>${esc(title)}</strong>
      <p>${esc(text)}</p>
    </div>`;
}

function historyTable(rows, showUser) {
  if (!rows.length) {
    return emptyState('Chưa có dữ liệu', 'Chưa có bản ghi chấm công nào trong khoảng thời gian này.', 'history');
  }
  return `
    <div class="table-wrap">
      <table class="data">
        <thead>
          <tr>
            ${showUser ? '<th>Nhân viên</th>' : ''}
            <th>Ngày</th>
            <th>Giờ vào</th>
            <th>Giờ ra</th>
            <th>Thời gian</th>
            <th>Trạng thái</th>
          </tr>
        </thead>
        <tbody>
          ${rows.map(row => {
            const done = row.checkOut ? workedMinutes(row.checkIn, row.checkOut) : null;
            return `
              <tr>
                ${showUser ? `<td>${userCell(row)}</td>` : ''}
                <td class="strong nowrap">${esc(fmtDate(row.date))}</td>
                <td class="mono nowrap">${row.checkIn ? esc(row.checkIn) : '—'}${isLate(row) ? ' <span class="badge warning" style="margin-left:6px">Muộn</span>' : ''}</td>
                <td class="mono nowrap">${row.checkOut ? esc(row.checkOut) : '—'}</td>
                <td class="nowrap">${done === null ? '<span class="muted">Đang tính…</span>' : esc(fmtDuration(done))}</td>
                <td>${rowStatusBadge(row)}</td>
              </tr>`;
          }).join('')}
        </tbody>
      </table>
    </div>`;
}

function weekChart(rows, days = 7, large = false) {
  const map = new Map(rows.map(r => [r.date, r]));
  const items = [];
  const base = serverNow();
  for (let i = days - 1; i >= 0; i--) {
    const d = new Date(base.getFullYear(), base.getMonth(), base.getDate() - i);
    const iso = isoOf(d);
    const row = map.get(iso);
    let mins = 0;
    if (row?.checkIn) mins = workedMinutes(row.checkIn, row.checkOut);
    items.push({
      iso,
      label: `${pad2(d.getDate())}/${pad2(d.getMonth() + 1)}`,
      wd: ['CN', 'T2', 'T3', 'T4', 'T5', 'T6', 'T7'][d.getDay()],
      mins,
      today: i === 0,
    });
  }
  const max = Math.max(60, ...items.map(it => it.mins));
  return `
    <div class="chart ${large ? 'chart-lg' : ''}">
      ${items.map(it => {
        const pct = it.mins ? Math.max(3, Math.round(it.mins / max * 100)) : 0;
        return `
          <div class="bar-col ${it.today ? 'today' : ''}" title="${esc(it.label)}: ${esc(fmtDuration(it.mins))}">
            <span class="bar-value">${it.mins ? esc(fmtHours(it.mins)) : ''}</span>
            <div class="bar-track"><div class="bar" style="height:${pct}%;${it.mins ? '' : 'min-height:0;opacity:0'}"></div></div>
            <span class="bar-label">${esc(it.wd)}<br>${esc(it.label.slice(0, 5))}</span>
          </div>`;
      }).join('')}
    </div>`;
}

/* ============================================================
   DASHBOARD
   ============================================================ */
async function renderDashboard(root) {
  if (state.user?.role === 'ADMIN') return renderAdminDashboard(root);
  const today = isoToday();
  const from = isoOf(new Date(serverNow().getFullYear(), serverNow().getMonth(), serverNow().getDate() - 6));

  const [att, hist] = await Promise.all([
    api('/api/attendance/today'),
    api(`/api/history?page=1&limit=7&from=${from}&to=${today}&user=${encodeURIComponent(state.user.username)}`),
  ]);

  setServerTime(att.date, att.serverTime);

  const rows = hist.data || [];
  const st = att.status;
  const statusLabel = st === 'WORKING' ? 'Đang làm việc' : st === 'COMPLETED' ? 'Đã hoàn thành' : 'Chưa chấm công';
  const statusCls = st === 'WORKING' ? 'working' : st === 'COMPLETED' ? 'done' : 'not';
  const firstName = (state.user.fullName || state.user.username).trim().split(/\s+/).pop();
  const completed = rows.filter(r => r.checkOut).length;
  const totalMins = rows.reduce((s, r) => s + (r.checkIn ? workedMinutes(r.checkIn, r.checkOut) : 0), 0);
  const lateCount = rows.filter(isLate).length;

  root.innerHTML = `
    <section class="hero">
      <div>
        <span class="eyebrow">${icon('sparkles', 'icon-sm')} ${esc(weekdayName(today))}, ${esc(fmtDate(today))}</span>
        <h2>Xin chào, ${esc(firstName)}!</h2>
        <p>${st === 'WORKING'
          ? 'Bạn đang trong ca làm việc. Đừng quên CHECK-OUT khi kết thúc ngày.'
          : st === 'COMPLETED'
            ? 'Hôm nay bạn đã chấm công đầy đủ. Chúc bạn một ngày tốt lành!'
            : 'Bạn chưa chấm công hôm nay. Bắt đầu ngày làm việc bằng một cú nhấp.'}</p>
        <div class="hero-meta">
          <span class="hero-chip">${icon('clock')} Vào: ${esc(att.checkIn || '—')}</span>
          <span class="hero-chip">${icon('timer')} Làm việc: ${esc(fmtDuration(att.workedMinutes))}</span>
          <span class="hero-chip">${icon('calendar')} ${esc(completed)}/${rows.length} ngày đủ giờ</span>
        </div>
      </div>
      <div class="hero-side">
        <span class="status-pill ${statusCls}">${esc(statusLabel)}</span>
        <div>
          <div class="hero-clock" data-clock>--:--:--</div>
          <div class="hero-clock-sub">Giờ máy chủ</div>
        </div>
        <a class="btn btn-light" href="#/attendance">${icon('clock')} Mở trang chấm công</a>
      </div>
    </section>

    <div class="stat-grid">
      <div class="stat-card green">
        <div class="stat-top"><span class="stat-ic">${icon('check-circle')}</span><span class="stat-label">Trạng thái</span></div>
        <div class="stat-value">${esc(statusLabel)}</div>
        <div class="stat-foot">Cập nhật theo thời gian thực</div>
      </div>
      <div class="stat-card blue">
        <div class="stat-top"><span class="stat-ic">${icon('login')}</span><span class="stat-label">Giờ vào</span></div>
        <div class="stat-value mono">${esc(att.checkIn || '—')}</div>
        <div class="stat-foot">${att.checkIn ? 'Server đã ghi nhận' : 'Chưa check-in hôm nay'}</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-top"><span class="stat-ic">${icon('logout')}</span><span class="stat-label">Giờ ra</span></div>
        <div class="stat-value mono">${esc(att.checkOut || '—')}</div>
        <div class="stat-foot">${att.checkOut ? 'Đã kết thúc ngày' : 'Chưa check-out'}</div>
      </div>
      <div class="stat-card violet">
        <div class="stat-top"><span class="stat-ic">${icon('timer')}</span><span class="stat-label">Thời gian làm</span></div>
        <div class="stat-value">${esc(fmtDuration(att.workedMinutes))}</div>
        <div class="stat-foot">Trong ngày hôm nay</div>
      </div>
    </div>

    <div class="dash-grid">
      <section class="card card-pad">
        <div class="card-head">
          <div>
            <div class="card-title-row">${icon('chart')}<span class="card-title">7 ngày gần nhất</span></div>
            <div class="card-sub">Tổng thời gian làm việc theo ngày</div>
          </div>
          <span class="badge info">${esc(fmtHours(totalMins))} tổng</span>
        </div>
        ${weekChart(rows, 7)}
      </section>

      <div class="stack">
        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title-row">${icon('sparkles')}<span class="card-title">Tóm tắt nhanh</span></div>
              <div class="card-sub">7 ngày gần nhất</div>
            </div>
          </div>
          <div class="metric-list">
            <div class="metric-row"><span>${icon('check-circle')} Ngày đủ giờ</span><strong>${completed}/${rows.length}</strong></div>
            <div class="metric-row"><span>${icon('timer')} Tổng thời gian</span><strong>${esc(fmtHours(totalMins))}</strong></div>
            <div class="metric-row"><span>${icon('alert-triangle')} Lần đi muộn</span><strong>${lateCount}</strong></div>
            <div class="metric-row"><span>${icon('coffee')} Ngày nghỉ / thiếu</span><strong>${Math.max(0, 7 - rows.length)}</strong></div>
          </div>
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title-row">${icon('arrow-right')}<span class="card-title">Truy cập nhanh</span></div>
            </div>
          </div>
          <div class="quick-grid">
            <a class="quick-card" href="#/attendance"><span class="quick-ic">${icon('clock')}</span><strong>Chấm công</strong><small>Vào / ra hôm nay</small></a>
            <a class="quick-card" href="#/history"><span class="quick-ic">${icon('history')}</span><strong>Lịch sử</strong><small>Tra cứu chấm công</small></a>
            <a class="quick-card" href="#/leaves"><span class="quick-ic">${icon('calendar')}</span><strong>Nghỉ phép</strong><small>Gửi đơn mới</small></a>
          </div>
        </section>
      </div>
    </div>

    <section class="card card-pad" style="margin-top:20px">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('history')}<span class="card-title">Chấm công gần đây</span></div>
          <div class="card-sub">7 ngày gần nhất</div>
        </div>
        <a class="btn btn-soft btn-sm" href="#/history">Xem tất cả ${icon('arrow-right', 'icon-sm')}</a>
      </div>
      ${historyTable(rows, false)}
    </section>`;
}

/* ============================================================
   ADMIN DASHBOARD
   ============================================================ */
async function renderAdminDashboard(root) {
  const [stats, leaves, clients, attendance] = await Promise.all([
    api('/api/dashboard/stats'),
    api('/api/admin/leaves'),
    api('/api/admin/clients'),
    api('/api/admin/all'),
  ]);

  const pending = (leaves.data || []).filter(r => r.status === 'PENDING');
  const staff = clients.data || [];
  const online = staff.filter(c => c.status === 'ONLINE').length;
  const rows = (attendance.data || []).slice(0, 10);
  const today = stats.date || isoToday();

  root.innerHTML = `
    <section class="hero admin-hero">
      <div>
        <span class="eyebrow">${icon('shield', 'icon-sm')} Quản trị hệ thống · ${esc(weekdayName(today))}, ${esc(fmtDate(today))}</span>
        <h2>Xin chào, ${esc(state.user.fullName || 'Quản trị viên')}!</h2>
        <p>Tổng quan realtime toàn công ty. Duyệt đơn, theo dõi chấm công và giám sát nhân viên online.</p>
        <div class="hero-meta">
          <span class="hero-chip">${icon('users')} ${stats.totalEmployees} nhân viên</span>
          <span class="hero-chip">${icon('check-circle')} ${stats.present} đi làm</span>
          <span class="hero-chip">${icon('alert-triangle')} ${stats.pendingLeaves} đơn chờ duyệt</span>
          <span class="hero-chip">${icon('activity')} ${online} online</span>
        </div>
      </div>
      <div class="hero-side">
        <span class="status-pill working">ADMIN</span>
        <div>
          <div class="hero-clock" data-clock>--:--:--</div>
          <div class="hero-clock-sub">Giờ máy chủ</div>
        </div>
        <a class="btn btn-light" href="#/admin-monitor">${icon('activity')} Giám sát online</a>
      </div>
    </section>

    <div class="stat-grid">
      <div class="stat-card blue">
        <div class="stat-top"><span class="stat-ic">${icon('users')}</span><span class="stat-label">Tổng nhân viên</span></div>
        <div class="stat-value">${stats.totalEmployees}</div>
        <div class="stat-foot">${stats.totalUsers} tài khoản hệ thống</div>
      </div>
      <div class="stat-card green">
        <div class="stat-top"><span class="stat-ic">${icon('check-circle')}</span><span class="stat-label">Đi làm hôm nay</span></div>
        <div class="stat-value">${stats.present}</div>
        <div class="stat-foot">${stats.attendanceRate}% tỉ lệ có mặt · ${stats.working} đang làm</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-top"><span class="stat-ic">${icon('coffee')}</span><span class="stat-label">Vắng / chưa chấm</span></div>
        <div class="stat-value">${stats.absent}</div>
        <div class="stat-foot">${stats.completed} đã hoàn thành hôm nay</div>
      </div>
      <div class="stat-card violet">
        <div class="stat-top"><span class="stat-ic">${icon('alert-triangle')}</span><span class="stat-label">Đi muộn</span></div>
        <div class="stat-value">${stats.late}</div>
        <div class="stat-foot">${stats.pendingLeaves} đơn đang chờ duyệt</div>
      </div>
    </div>

    <div class="dash-grid" style="margin-top:20px">
      <section class="card card-pad">
        <div class="card-head">
          <div>
            <div class="card-title-row">${icon('history')}<span class="card-title">Chấm công gần đây</span></div>
            <div class="card-sub">Dữ liệu mới nhất toàn công ty</div>
          </div>
          <a class="btn btn-soft btn-sm" href="#/admin-attendance">Xem tất cả ${icon('arrow-right', 'icon-sm')}</a>
        </div>
        ${historyTable(rows, true)}
      </section>

      <div class="stack">
        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title-row">${icon('calendar')}<span class="card-title">Đơn chờ duyệt</span></div>
              <div class="card-sub">${pending.length} đơn cần xử lý</div>
            </div>
            <a class="btn btn-soft btn-sm" href="#/admin-leaves">Mở trang duyệt</a>
          </div>
          ${pending.length ? pending.slice(0, 4).map(r => `
            <div class="pending-row">
              <div class="pending-main">
                ${userCell(r)}
                <div class="pending-meta">${leaveTypeBadge(r.leaveType)} <span>${esc(fmtDate(r.startDate))}</span></div>
              </div>
              <div class="table-actions">
                <button type="button" class="icon-btn success" data-admin-approve="${r.id}" title="Duyệt">${icon('check')}</button>
                <button type="button" class="icon-btn danger" data-admin-reject="${r.id}" title="Từ chối">${icon('x')}</button>
              </div>
            </div>`).join('') : emptyState('Không có đơn chờ', 'Tất cả đơn đã được xử lý.', 'check-circle')}
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title-row">${icon('activity')}<span class="card-title">Giám sát Online</span></div>
              <div class="card-sub">${online} online · ${Math.max(0, staff.length - online)} offline</div>
            </div>
            <a class="btn btn-soft btn-sm" href="#/admin-monitor">Chi tiết</a>
          </div>
          <div class="monitor-mini-list">
            ${staff.slice(0, 5).map(c => `
              <div class="monitor-mini">
                ${userCell(c)}
                <span class="monitor-status ${c.status === 'ONLINE' ? 'on' : 'off'}">
                  <span class="monitor-dot"></span>${c.status === 'ONLINE' ? 'Online' : 'Offline'}
                </span>
              </div>`).join('') || emptyState('Chưa có dữ liệu', 'Chưa có nhân viên đăng nhập.', 'users')}
          </div>
        </section>
      </div>
    </div>`;

  const resolveLeave = async (id, status) => {
    try {
      await api('/api/leave/approve', { method: 'PUT', body: { id: Number(id), status } });
      toast(status === 'APPROVED' ? 'Đã duyệt đơn' : 'Đã từ chối đơn', '', 'success');
      await renderAdminDashboard(root);
    } catch (err) {
      toast('Không cập nhật được', err.message, 'error');
    }
  };

  $$('[data-admin-approve]', root).forEach(btn =>
    btn.addEventListener('click', () => resolveLeave(btn.dataset.adminApprove, 'APPROVED')));
  $$('[data-admin-reject]', root).forEach(btn =>
    btn.addEventListener('click', () => resolveLeave(btn.dataset.adminReject, 'REJECTED')));
}

async function renderAdminMonitor(root) {
  const data = await api('/api/admin/clients');
  const rows = data.data || [];
  const online = rows.filter(c => c.status === 'ONLINE').length;

  root.innerHTML = `
    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('activity')}<span class="card-title">Giám sát nhân viên Online / Offline</span></div>
          <div class="card-sub">${rows.length} tài khoản · ${online} online · ${Math.max(0, rows.length - online)} offline</div>
        </div>
        <button type="button" class="btn btn-soft btn-sm" id="monitor-refresh">${icon('refresh')} Làm mới</button>
      </div>
      ${rows.length ? `
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Nhân viên</th><th>Phòng ban</th><th>Trạng thái</th><th>IP</th><th>Đăng nhập</th><th>Hoạt động cuối</th><th>Requests</th></tr></thead>
          <tbody>
            ${rows.map(c => `
              <tr>
                <td>${userCell(c)}</td>
                <td>${esc(c.department || '—')}</td>
                <td><span class="monitor-status ${c.status === 'ONLINE' ? 'on' : 'off'}"><span class="monitor-dot"></span>${c.status === 'ONLINE' ? 'Online' : 'Offline'}</span></td>
                <td class="mono">${esc(c.ip || '—')}${c.port ? `:${c.port}` : ''}</td>
                <td class="mono">${esc(c.loginTime || '—')}</td>
                <td class="mono">${esc(c.lastActivity || '—')}</td>
                <td>${c.requestCount || 0}</td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>` : emptyState('Chưa có dữ liệu', 'Chưa có nhân viên kết nối.', 'users')}
    </section>`;

  $('#monitor-refresh')?.addEventListener('click', () => renderAdminMonitor(root));
}

/* ============================================================
   ATTENDANCE — FaceID mô phỏng + GPS
   ============================================================ */
function getGeo() {
  const fallback = {
    lat: 21.0278,
    lng: 105.8342,
    accuracy: 0,
    source: 'demo',
    error: '',
  };
  return new Promise(resolve => {
    if (!navigator.geolocation) return resolve(fallback);
    let settled = false;
    const timer = setTimeout(() => {
      if (settled) return;
      settled = true;
      resolve(fallback);
    }, 5000);
    navigator.geolocation.getCurrentPosition(
      pos => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        resolve({
          lat: Number(pos.coords.latitude.toFixed(6)),
          lng: Number(pos.coords.longitude.toFixed(6)),
          accuracy: Math.round(pos.coords.accuracy || 0),
          source: 'gps',
          error: '',
        });
      },
      err => {
        if (settled) return;
        settled = true;
        clearTimeout(timer);
        resolve({ ...fallback, error: err?.message || 'Không lấy được GPS' });
      },
      { enableHighAccuracy: true, timeout: 4500, maximumAge: 30000 }
    );
  });
}

function faceScanMarkup(actionName) {
  return `
    <div class="face-scan">
      <div class="face-frame">
        <span class="face-corner tl"></span><span class="face-corner tr"></span>
        <span class="face-corner bl"></span><span class="face-corner br"></span>
        <div class="face-oval">${icon('user')}</div>
        <div class="scan-line"></div>
      </div>
      <div class="face-status" id="face-status">Đang quét khuôn mặt…</div>
      <div class="face-progress"><span id="face-progress-bar"></span></div>
      <div class="geo-chip" id="geo-info">${icon('info')} Đang lấy vị trí GPS tự động…</div>
    </div>
    <div class="metric-list" style="margin-top:14px">
      <div class="metric-row"><span>Hành động</span><strong>${esc(actionName)}</strong></div>
      <div class="metric-row"><span>Xác thực</span><strong>FaceID mô phỏng</strong></div>
      <div class="metric-row"><span>Ghi nhận</span><strong>Server tự ghi giờ</strong></div>
    </div>
    <div class="field" style="margin-top:14px">
      <label>Ghi chú (không bắt buộc)</label>
      <div class="input-wrap"><input id="att-note" placeholder="VD: Đi muộn do kẹt xe…" /></div>
    </div>`;
}

async function renderAttendance(root) {
  const today = isoToday();

  const [att, hist] = await Promise.all([
    api('/api/attendance/today'),
    api(`/api/history?page=1&limit=5&to=${today}`),
  ]);

  setServerTime(att.date, att.serverTime);
  const rows = hist.data || [];
  const st = att.status;

  const statusLabel = st === 'WORKING' ? 'Đang làm việc' : st === 'COMPLETED' ? 'Đã hoàn thành' : 'Chưa check-in';
  const stateName = st === 'WORKING' ? 'working' : st === 'COMPLETED' ? 'completed' : 'not';
  const hint = st === 'WORKING'
    ? 'Đã ghi nhận giờ vào. Khi kết thúc, nhấn CHECK-OUT để chốt giờ ra.'
    : st === 'COMPLETED'
      ? 'Hôm nay bạn đã chấm công đầy đủ. Hẹn gặp lại vào ngày mai!'
      : 'Nhấn CHECK-IN để bắt đầu ngày làm việc. Giờ vào sẽ do server ghi lại.';

  const actionClass = st === 'WORKING' ? 'checkout' : st === 'COMPLETED' ? 'done' : 'checkin';
  const actionText = st === 'WORKING' ? 'CHECK-OUT NGAY' : st === 'COMPLETED' ? 'ĐÃ HOÀN THÀNH HÔM NAY' : 'CHECK-IN NGAY';
  const actionIcon = st === 'WORKING' ? 'logout' : st === 'COMPLETED' ? 'check-circle' : 'login';

  root.innerHTML = `
    <section class="att-hero" data-state="${stateName}">
      <div class="att-hero-left">
        <div class="att-date">${icon('calendar', 'icon-sm')} ${esc(weekdayName(att.date))}, ${esc(fmtDate(att.date))}</div>
        <h2 class="att-title">${esc(statusLabel)}</h2>
        <p class="att-hint">${esc(hint)}</p>
      </div>
      <div class="att-hero-right">
        <div class="att-clock" data-clock>--:--:--</div>
        <div class="att-clock-label">Giờ máy chủ</div>
      </div>
    </section>

    <div class="att-info-grid">
      <div class="info-card green">
        <span class="stat-ic">${icon('login')}</span>
        <div><div class="label">Giờ vào</div><div class="value mono">${esc(att.checkIn || '—')}</div></div>
      </div>
      <div class="info-card orange">
        <span class="stat-ic">${icon('logout')}</span>
        <div><div class="label">Giờ ra</div><div class="value mono">${esc(att.checkOut || '—')}</div></div>
      </div>
      <div class="info-card blue">
        <span class="stat-ic">${icon('timer')}</span>
        <div><div class="label">Thời gian làm</div><div class="value" id="att-worked">${esc(fmtDuration(att.workedMinutes))}</div></div>
      </div>
      <div class="info-card violet">
        <span class="stat-ic">${icon('check-circle')}</span>
        <div><div class="label">Trạng thái</div><div class="value">${esc(statusLabel)}</div></div>
      </div>
    </div>

    <div class="att-layout">
      <section class="card action-card">
        <h3>Một nút duy nhất — đúng trạng thái</h3>
        <p>Lần đầu trong ngày là CHECK-IN. Khi kết thúc là CHECK-OUT. Hệ thống tự đổi nút theo trạng thái thực tế.</p>

        <div class="att-timeline">
          <div class="tl-step ${att.checkIn ? 'done' : ''}">
            <span class="tl-dot">${icon(att.checkIn ? 'check' : 'login')}</span> Check-in
          </div>
          <div class="tl-line ${att.checkIn ? 'done' : ''}"></div>
          <div class="tl-step ${att.checkOut ? 'done' : ''}">
            <span class="tl-dot">${icon(att.checkOut ? 'check' : 'logout')}</span> Check-out
          </div>
        </div>

        <button type="button" class="btn att-action-btn ${actionClass}" id="att-action"
          ${st === 'COMPLETED' ? 'disabled' : ''}>
          ${icon(actionIcon)} ${actionText}
        </button>
        <div class="att-result" id="att-result"></div>
      </section>

      <section class="rules-card">
        <h3>${icon('info')} Quy tắc chấm công</h3>
        <ul class="rules-list">
          <li><span class="tick">${icon('check')}</span> Mỗi ngày chỉ CHECK-IN một lần.</li>
          <li><span class="tick">${icon('check')}</span> Phải CHECK-IN trước khi CHECK-OUT.</li>
          <li><span class="tick">${icon('check')}</span> Giờ vào / ra do server ghi nhận, không sửa được từ client.</li>
          <li><span class="tick">${icon('check')}</span> Ca làm chuẩn: 08:00 — 17:00.</li>
        </ul>
      </section>
    </div>

    <section class="card card-pad" style="margin-top:20px">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('history')}<span class="card-title">5 ngày chấm công gần nhất</span></div>
          <div class="card-sub">Dữ liệu lấy trực tiếp từ server</div>
        </div>
        <span class="badge info">${rows.length} bản ghi</span>
      </div>
      ${historyTable(rows, false)}
    </section>`;

  // Live worked-time + clock
  const tick = () => {
    const workedEl = $('#att-worked');
    if (!workedEl) return;
    if (st === 'WORKING' && att.checkIn) {
      workedEl.textContent = fmtDuration(workedMinutes(att.checkIn, null));
    }
  };
  tick();
  state.timers.push(setInterval(tick, 1000));

  const btn = $('#att-action');
  if (btn && st !== 'COMPLETED') {
    btn.addEventListener('click', async () => {
      const isCheckIn = st !== 'WORKING';
      const actionName = isCheckIn ? 'CHECK-IN' : 'CHECK-OUT';
      const geoPromise = getGeo();
      let geo = { lat: null, lng: null };
      let faceVerified = false;

      const modalPromise = openModal({
        title: `FaceID & GPS — ${actionName}`,
        subtitle: 'Camera mô phỏng xác thực khuôn mặt và lấy GPS trước khi gửi server.',
        iconName: isCheckIn ? 'login' : 'logout',
        tone: isCheckIn ? 'success' : 'warning',
        confirmText: `Xác nhận khuôn mặt & ${actionName}`,
        wide: true,
        body: faceScanMarkup(actionName),
        onConfirm: async modal => {
          const g = await geoPromise;
          geo = g;
          if (!faceVerified) {
            await new Promise(r => setTimeout(r, 900));
            faceVerified = true;
          }
          const note = $('#att-note', modal)?.value.trim() || '';
          const res = await api(isCheckIn ? '/api/checkin' : '/api/checkout', {
            method: 'POST',
            body: {
              lat: g.lat,
              lng: g.lng,
              faceVerified: true,
              note,
            },
          });
          toast(
            isCheckIn ? 'Check-in thành công' : 'Check-out thành công',
            `FaceID OK · ${g.source === 'gps' ? 'GPS thật' : 'GPS mô phỏng'} · Server ghi nhận lúc ${res.time}`,
            'success'
          );
          renderRoute();
          return true;
        },
      });

      setTimeout(() => {
        const status = $('#face-status');
        const progress = $('#face-progress-bar');
        if (status) status.textContent = 'Đã xác thực khuôn mặt ✓';
        if (progress) progress.style.width = '100%';
        faceVerified = true;
      }, 750);

      geoPromise.then(g => {
        const info = $('#geo-info');
        if (!info) return;
        const source = g.source === 'gps' ? 'GPS thật' : 'GPS mô phỏng';
        const accuracy = g.source === 'gps' && g.accuracy ? ` · sai số ~${g.accuracy}m` : '';
        info.innerHTML = `${icon('info')} ${source}: ${g.lat}, ${g.lng}${accuracy}`;
      });

      const ok = await modalPromise;
      if (!ok) return;
    });
  }
}

/* ============================================================
   HISTORY
   ============================================================ */
async function renderHistory(root, admin) {
  if (!state.historyView) {
    state.historyView = {
      page: 1,
      limit: 20,
      from: firstOfMonthISO(),
      to: isoToday(),
      user: '',
      admin,
    };
  }
  const v = state.historyView;
  v.admin = admin;

  root.innerHTML = `
    <div class="filter-bar">
      <div class="field"><label>Từ ngày</label><input type="date" id="h-from" value="${esc(v.from)}" /></div>
      <div class="field"><label>Đến ngày</label><input type="date" id="h-to" value="${esc(v.to)}" /></div>
      ${admin ? `<div class="field grow"><label>Nhân viên</label><input type="text" id="h-user" placeholder="username (để trống = tất cả)" value="${esc(v.user)}" /></div>` : ''}
      <div class="field">
        <label>Số dòng</label>
        <select id="h-limit">
          ${[10, 20, 50, 100].map(n => `<option value="${n}" ${n === v.limit ? 'selected' : ''}>${n}</option>`).join('')}
        </select>
      </div>
      <button type="button" class="btn btn-primary" id="h-apply">${icon('search', 'icon-sm')} Lọc</button>
      <button type="button" class="btn btn-ghost" id="h-reset">${icon('refresh', 'icon-sm')} Đặt lại</button>
      <div class="spacer"></div>
      <button type="button" class="btn btn-soft" id="h-export">${icon('download', 'icon-sm')} Xuất CSV</button>
    </div>

    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('history')}<span class="card-title">${admin ? 'Bảng chấm công toàn công ty' : 'Lịch sử chấm công'}</span></div>
          <div class="card-sub" id="h-summary">Đang tải…</div>
        </div>
        <span class="badge info" id="h-count">—</span>
      </div>
      <div id="h-body">
        <div class="sk-lines">
          <div class="skeleton sk-line"></div>
          <div class="skeleton sk-line"></div>
          <div class="skeleton sk-line short"></div>
        </div>
      </div>
    </section>`;

  const body = $('#h-body');
  const summary = $('#h-summary');
  const count = $('#h-count');

  const readFilters = () => {
    v.from = $('#h-from').value || firstOfMonthISO();
    v.to = $('#h-to').value || isoToday();
    v.limit = Number($('#h-limit').value) || 20;
    if (admin) v.user = ($('#h-user').value || '').trim();
    v.page = 1;
  };

  const load = async () => {
    body.innerHTML = `<div class="sk-lines"><div class="skeleton sk-line"></div><div class="skeleton sk-line"></div><div class="skeleton sk-line short"></div></div>`;
    const params = new URLSearchParams({
      page: String(v.page),
      limit: String(v.limit),
      from: v.from,
      to: v.to,
    });
    if (admin && v.user) params.set('user', v.user);

    const data = await api(`/api/history?${params}`);
    const rows = data.data || [];
    const total = data.total || 0;
    const totalPages = data.totalPages || 1;

    summary.textContent = `${fmtDate(v.from)} → ${fmtDate(v.to)} · ${total} bản ghi`;
    count.textContent = `${rows.length} / ${total}`;

    body.innerHTML = `
      ${historyTable(rows, admin)}
      <div class="pagination">
        <span class="info">Trang ${data.page || v.page} / ${Math.max(1, totalPages)}</span>
        <div class="controls">
          <button type="button" class="page-btn" data-page="${v.page - 1}" ${v.page <= 1 ? 'disabled' : ''}>${icon('chevron-left', 'icon-sm')}</button>
          <span class="page-btn current">${v.page}</span>
          <button type="button" class="page-btn" data-page="${v.page + 1}" ${v.page >= totalPages ? 'disabled' : ''}>${icon('chevron-right', 'icon-sm')}</button>
        </div>
      </div>`;

    $$('.page-btn[data-page]', body).forEach(btn => {
      btn.addEventListener('click', () => {
        const p = Number(btn.dataset.page);
        if (!p || p < 1 || p > totalPages) return;
        v.page = p;
        load().catch(err => toast('Lỗi tải dữ liệu', err.message, 'error'));
      });
    });
  };

  $('#h-apply').addEventListener('click', () => {
    readFilters();
    load().catch(err => toast('Lỗi tải dữ liệu', err.message, 'error'));
  });

  $('#h-reset').addEventListener('click', () => {
    v.page = 1;
    v.limit = 20;
    v.from = firstOfMonthISO();
    v.to = isoToday();
    v.user = '';
    $('#h-from').value = v.from;
    $('#h-to').value = v.to;
    $('#h-limit').value = '20';
    if (admin) $('#h-user').value = '';
    load().catch(err => toast('Lỗi tải dữ liệu', err.message, 'error'));
  });

  $('#h-export').addEventListener('click', async () => {
    try {
      readFilters();
      const params = new URLSearchParams({ from: v.from, to: v.to });
      const res = await fetch(`/api/export/csv?${params}`, {
        headers: state.token ? { Authorization: `Bearer ${state.token}` } : {},
      });
      if (!res.ok) throw new Error(`Máy chủ trả về ${res.status}`);
      const blob = await res.blob();
      const url = URL.createObjectURL(blob);
      const a = document.createElement('a');
      a.href = url;
      a.download = `chamcong_${isoToday()}.csv`;
      document.body.appendChild(a);
      a.click();
      a.remove();
      setTimeout(() => URL.revokeObjectURL(url), 1000);
      toast('Đã xuất CSV', 'File đang được tải xuống.', 'info');
    } catch (err) {
      toast('Không xuất được CSV', err.message, 'error');
    }
  });

  await load();
}

/* ============================================================
   STATS
   ============================================================ */
async function renderStats(root) {
  const today = isoToday();
  const from = isoOf(new Date(serverNow().getFullYear(), serverNow().getMonth(), serverNow().getDate() - 29));

  const data = await api(`/api/history?page=1&limit=100&from=${from}&to=${today}&user=${encodeURIComponent(state.user.username)}`);
  const rows = data.data || [];

  const completedRows = rows.filter(r => r.checkOut);
  const presentRows = rows.filter(r => r.checkIn);
  const lateRows = rows.filter(isLate);
  const totalMins = completedRows.reduce((s, r) => s + workedMinutes(r.checkIn, r.checkOut), 0);
  const avgMins = completedRows.length ? totalMins / completedRows.length : 0;
  const rate = rows.length ? Math.round(completedRows.length / rows.length * 100) : 0;

  const bestDay = completedRows
    .map(r => ({ date: r.date, mins: workedMinutes(r.checkIn, r.checkOut) }))
    .sort((a, b) => b.mins - a.mins)[0];

  root.innerHTML = `
    <div class="stats-grid">
      <div class="stat-card blue">
        <div class="stat-top"><span class="stat-ic">${icon('calendar')}</span><span class="stat-label">Ngày có dữ liệu</span></div>
        <div class="stat-value">${rows.length}</div>
        <div class="stat-foot">30 ngày gần nhất</div>
      </div>
      <div class="stat-card green">
        <div class="stat-top"><span class="stat-ic">${icon('check-circle')}</span><span class="stat-label">Ngày hoàn thành</span></div>
        <div class="stat-value">${completedRows.length}</div>
        <div class="stat-foot">${rate}% trên tổng số ngày</div>
      </div>
      <div class="stat-card orange">
        <div class="stat-top"><span class="stat-ic">${icon('alert-triangle')}</span><span class="stat-label">Lần đi muộn</span></div>
        <div class="stat-value">${lateRows.length}</div>
        <div class="stat-foot">Sau 08:00:00</div>
      </div>
      <div class="stat-card violet">
        <div class="stat-top"><span class="stat-ic">${icon('timer')}</span><span class="stat-label">Tổng thời gian</span></div>
        <div class="stat-value">${esc(fmtHours(totalMins))}</div>
        <div class="stat-foot">Trung bình ${esc(fmtDuration(avgMins))}/ngày</div>
      </div>
    </div>

    <div class="dash-grid" style="margin-top:20px">
      <section class="card card-pad">
        <div class="card-head">
          <div>
            <div class="card-title-row">${icon('chart')}<span class="card-title">Thời gian làm việc 7 ngày</span></div>
            <div class="card-sub">So sánh số giờ theo từng ngày</div>
          </div>
          <span class="badge success">${esc(fmtHours(totalMins))} tổng</span>
        </div>
        ${weekChart(rows, 7, true)}
      </section>

      <div class="stack">
        <section class="card donut-card">
          <div class="donut" style="--p:${rate}">
            <div class="donut-value">${rate}%</div>
          </div>
          <div class="donut-meta">
            <strong>Tỷ lệ hoàn thành</strong>
            <p>${completedRows.length} ngày đủ giờ vào/ra trên ${rows.length} ngày có dữ liệu.</p>
          </div>
        </section>

        <section class="card card-pad">
          <div class="card-head">
            <div>
              <div class="card-title-row">${icon('sparkles')}<span class="card-title">Điểm nổi bật</span></div>
              <div class="card-sub">30 ngày gần nhất</div>
            </div>
          </div>
          <div class="metric-list">
            <div class="metric-row"><span>${icon('check-circle')} Ngày có mặt</span><strong>${presentRows.length}</strong></div>
            <div class="metric-row"><span>${icon('clock')} Giờ trung bình</span><strong>${esc(fmtDuration(avgMins))}</strong></div>
            <div class="metric-row"><span>${icon('timer')} Ngày làm dài nhất</span><strong>${bestDay ? `${esc(fmtDate(bestDay.date))} · ${esc(fmtDuration(bestDay.mins))}` : '—'}</strong></div>
            <div class="metric-row"><span>${icon('alert-triangle')} Tỷ lệ đi muộn</span><strong>${presentRows.length ? Math.round(lateRows.length / presentRows.length * 100) : 0}%</strong></div>
          </div>
        </section>
      </div>
    </div>`;
}

/* ============================================================
   LEAVES
   ============================================================ */
async function renderLeaves(root, adminMode) {
  const listPath = adminMode ? '/api/admin/leaves' : '/api/leave-list';

  root.innerHTML = `
    ${adminMode ? '' : `
    <section class="card form-card" style="margin-bottom:18px">
      <h3>Gửi đơn xin nghỉ</h3>
      <div class="form-sub">Đơn sẽ ở trạng thái chờ duyệt cho đến khi quản trị viên xử lý.</div>
      <form id="leave-form">
        <div class="form-grid">
          <div class="field"><label>Từ ngày</label><div class="input-wrap"><input type="date" id="leave-from" required /></div></div>
          <div class="field"><label>Đến ngày</label><div class="input-wrap"><input type="date" id="leave-to" required /></div></div>
          <div class="field full"><label>Loại đơn</label><div class="input-wrap">
            <select id="leave-type">
              <option value="NGHI_PHEP">Nghỉ phép</option>
              <option value="DI_MUON">Đi muộn</option>
              <option value="CONG_TAC">Công tác</option>
            </select>
          </div></div>
          <div class="field full"><label>Lý do</label><textarea id="leave-reason" placeholder="Ví dụ: Việc gia đình, khám bệnh…"></textarea></div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${icon('calendar')} Gửi đơn</button>
        </div>
      </form>
    </section>`}

    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('calendar')}<span class="card-title">${adminMode ? 'Đơn nghỉ phép toàn công ty' : 'Đơn nghỉ phép của bạn'}</span></div>
          <div class="card-sub" id="leave-summary">Đang tải…</div>
        </div>
        <span class="badge info" id="leave-count">—</span>
      </div>
      <div id="leave-body">
        <div class="sk-lines"><div class="skeleton sk-line"></div><div class="skeleton sk-line short"></div></div>
      </div>
    </section>`;

  if (!adminMode) {
    $('#leave-from').value = isoToday();
    $('#leave-to').value = isoToday();
    $('#leave-form').addEventListener('submit', async e => {
      e.preventDefault();
      const startDate = $('#leave-from').value;
      const endDate = $('#leave-to').value;
      const leaveType = $('#leave-type').value;
      const reason = $('#leave-reason').value.trim();
      if (!startDate || !endDate) return toast('Thiếu thông tin', 'Chọn ngày bắt đầu và kết thúc.', 'error');
      if (endDate < startDate) return toast('Ngày không hợp lệ', 'Ngày kết thúc phải sau ngày bắt đầu.', 'error');
      try {
        await api('/api/leaves', { method: 'POST', body: { startDate, endDate, leaveType, reason } });
        toast('Đã gửi đơn', 'Đơn xin nghỉ đang chờ duyệt.', 'success');
        $('#leave-reason').value = '';
        await loadLeaves();
      } catch (err) {
        toast('Không gửi được đơn', err.message, 'error');
      }
    });
  }

  async function loadLeaves() {
    const body = $('#leave-body');
    const data = await api(listPath);
    const rows = data.data || [];
    const summary = $('#leave-summary');
    const count = $('#leave-count');
    if (summary) summary.textContent = `${rows.length} đơn`;
    if (count) count.textContent = `${rows.length} đơn`;

    if (!rows.length) {
      body.innerHTML = emptyState('Chưa có đơn nghỉ phép', adminMode ? 'Chưa có nhân viên nào gửi đơn.' : 'Bạn chưa gửi đơn nghỉ phép nào.', 'calendar');
      return;
    }

    body.innerHTML = `
      <div class="table-wrap">
        <table class="data">
          <thead>
            <tr>
              ${adminMode ? '<th>Nhân viên</th>' : ''}
              <th>Loại đơn</th><th>Từ ngày</th><th>Đến ngày</th><th>Lý do</th><th>Trạng thái</th>
              <th class="text-right">Thao tác</th>
            </tr>
          </thead>
          <tbody>
            ${rows.map(r => `
              <tr>
                ${adminMode ? `<td>${userCell(r)}</td>` : ''}
                <td>${leaveTypeBadge(r.leaveType)}</td>
                <td class="strong nowrap">${esc(fmtDate(r.startDate))}</td>
                <td class="nowrap">${esc(fmtDate(r.endDate))}</td>
                <td>${esc(r.reason || '—')}</td>
                <td>${leaveStatusBadge(r.status)}</td>
                <td>
                  <div class="table-actions">
                    ${adminMode && r.status === 'PENDING' ? `
                      <button type="button" class="icon-btn" data-approve="${r.id}" title="Duyệt">${icon('check')}</button>
                      <button type="button" class="icon-btn danger" data-reject="${r.id}" title="Từ chối">${icon('x')}</button>
                    ` : !adminMode && r.status === 'PENDING' ? `
                      <button type="button" class="icon-btn danger" data-cancel="${r.id}" title="Hủy đơn">${icon('trash')}</button>
                    ` : '<span class="muted">—</span>'}
                  </div>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;

    if (adminMode) {
      $$('[data-approve]', body).forEach(btn => {
        btn.addEventListener('click', () => updateLeave(btn.dataset.approve, 'APPROVED'));
      });
      $$('[data-reject]', body).forEach(btn => {
        btn.addEventListener('click', () => updateLeave(btn.dataset.reject, 'REJECTED'));
      });
    } else {
      $$('[data-cancel]', body).forEach(btn => {
        btn.addEventListener('click', async () => {
          const ok = await confirmDialog({
            title: 'Hủy đơn nghỉ phép',
            message: 'Bạn chắc chắn muốn hủy đơn đang chờ duyệt này?',
            confirmText: 'Hủy đơn',
          });
          if (!ok) return;
          try {
            await api(`/api/leaves/${btn.dataset.cancel}`, { method: 'DELETE' });
            toast('Đã hủy đơn', '', 'info');
            await loadLeaves();
          } catch (err) {
            toast('Không hủy được', err.message, 'error');
          }
        });
      });
    }
  }

  async function updateLeave(id, status) {
    const ok = await confirmDialog({
      title: status === 'APPROVED' ? 'Duyệt đơn nghỉ phép' : 'Từ chối đơn nghỉ phép',
      message: status === 'APPROVED' ? 'Xác nhận duyệt đơn này?' : 'Xác nhận từ chối đơn này?',
      confirmText: status === 'APPROVED' ? 'Duyệt' : 'Từ chối',
      tone: status === 'APPROVED' ? 'success' : 'danger',
      iconName: status === 'APPROVED' ? 'check-circle' : 'x',
    });
    if (!ok) return;
    try {
      await api('/api/leave/approve', { method: 'PUT', body: { id: Number(id), status } });
      toast('Đã cập nhật đơn', '', 'success');
      await loadLeaves();
    } catch (err) {
      toast('Không cập nhật được', err.message, 'error');
    }
  }

  await loadLeaves();
}

/* ============================================================
   SHIFTS
   ============================================================ */
async function renderShifts(root) {
  root.innerHTML = `
    <section class="card form-card" style="margin-bottom:18px">
      <h3>Đăng ký ca làm việc</h3>
      <div class="form-sub">Chọn ngày và ca bạn muốn đăng ký. Mỗi ca / ngày chỉ đăng ký một lần.</div>
      <form id="shift-form">
        <div class="form-grid">
          <div class="field"><label>Ngày làm</label><div class="input-wrap"><input type="date" id="shift-date" required /></div></div>
          <div class="field"><label>Ca làm</label><div class="input-wrap">
            <select id="shift-type">
              <option value="SANG">Ca sáng · 08:00 – 12:00</option>
              <option value="CHIEU">Ca chiều · 13:00 – 17:00</option>
              <option value="TOI">Ca tối · 18:00 – 22:00</option>
            </select>
          </div></div>
          <div class="field full"><label>Ghi chú</label><textarea id="shift-note" placeholder="Ghi chú thêm (không bắt buộc)…"></textarea></div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${icon('plus')} Đăng ký ca</button>
        </div>
      </form>
    </section>

    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('briefcase')}<span class="card-title">Ca làm đã đăng ký</span></div>
          <div class="card-sub" id="shift-summary">Đang tải…</div>
        </div>
        <span class="badge info" id="shift-count">—</span>
      </div>
      <div id="shift-body">
        <div class="sk-lines"><div class="skeleton sk-line"></div><div class="skeleton sk-line short"></div></div>
      </div>
    </section>`;

  $('#shift-date').value = isoToday();

  async function loadShifts() {
    const body = $('#shift-body');
    const data = await api('/api/work-shifts/list');
    const rows = (data.data || []).slice().sort((a, b) => (b.shiftDate || '').localeCompare(a.shiftDate || ''));
    $('#shift-summary').textContent = `${rows.length} ca đã đăng ký`;
    $('#shift-count').textContent = `${rows.length} ca`;

    if (!rows.length) {
      body.innerHTML = emptyState('Chưa đăng ký ca nào', 'Chọn ngày và ca làm ở phía trên để bắt đầu.', 'briefcase');
      return;
    }

    body.innerHTML = `
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Ngày</th><th>Ca làm</th><th>Khung giờ</th><th>Ghi chú</th><th>Nhân viên</th><th class="text-right">Thao tác</th></tr></thead>
          <tbody>
            ${rows.map(r => `
              <tr>
                <td class="strong nowrap">${esc(fmtDate(r.shiftDate))}</td>
                <td><span class="badge ${r.shiftType === 'SANG' ? 'info' : r.shiftType === 'CHIEU' ? 'violet' : 'neutral'}">${esc(shiftLabel(r.shiftType))}</span></td>
                <td class="nowrap">${esc(shiftTime(r.shiftType))}</td>
                <td>${esc(r.note || '—')}</td>
                <td>${r.username === state.user.username ? '<span class="badge success">Bạn</span>' : esc(r.fullName || r.username)}</td>
                <td>
                  <div class="table-actions">
                    ${(r.username === state.user.username || state.user.role === 'ADMIN') ? `
                      <button type="button" class="icon-btn danger" data-del-shift="${r.id}" title="Xóa ca">${icon('trash')}</button>
                    ` : '<span class="muted">—</span>'}
                  </div>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;

    $$('[data-del-shift]', body).forEach(btn => {
      btn.addEventListener('click', async () => {
        const ok = await confirmDialog({
          title: 'Xóa ca làm việc',
          message: 'Bạn muốn xóa đăng ký ca này?',
          confirmText: 'Xóa',
        });
        if (!ok) return;
        try {
          await api(`/api/work-shifts/${btn.dataset.delShift}`, { method: 'DELETE' });
          toast('Đã xóa ca', '', 'info');
          await loadShifts();
        } catch (err) {
          toast('Không xóa được', err.message, 'error');
        }
      });
    });
  }

  $('#shift-form').addEventListener('submit', async e => {
    e.preventDefault();
    const shiftDate = $('#shift-date').value;
    const shiftType = $('#shift-type').value;
    const note = $('#shift-note').value.trim();
    if (!shiftDate) return toast('Thiếu ngày', 'Chọn ngày làm việc.', 'error');
    try {
      await api('/api/work-shifts/register', { method: 'POST', body: { shiftDate, shiftType, note } });
      toast('Đăng ký thành công', `${shiftLabel(shiftType)} · ${fmtDate(shiftDate)}`, 'success');
      $('#shift-note').value = '';
      await loadShifts();
    } catch (err) {
      toast('Không đăng ký được', err.message, 'error');
    }
  });

  await loadShifts();
}

/* ============================================================
   NOTES
   ============================================================ */
async function renderNotes(root, adminMode) {
  const listPath = adminMode ? '/api/notes/all' : '/api/notes/my';

  root.innerHTML = `
    ${adminMode ? '' : `
    <section class="card form-card" style="margin-bottom:18px">
      <h3>Ghi chú hôm nay</h3>
      <div class="form-sub">Lưu nội dung đã làm và kế hoạch tiếp theo. Mỗi ngày một ghi chú.</div>
      <form id="note-form">
        <div class="form-grid">
          <div class="field"><label>Ngày</label><div class="input-wrap"><input type="date" id="note-date" required /></div></div>
          <div class="field full"><label>Nội dung đã làm</label><textarea id="note-content" placeholder="Hôm nay bạn đã xử lý những việc gì?"></textarea></div>
          <div class="field full"><label>Kế hoạch tiếp theo</label><textarea id="note-plan" placeholder="Việc cần làm tiếp theo…"></textarea></div>
        </div>
        <div class="form-actions">
          <button type="submit" class="btn btn-primary">${icon('check')} Lưu ghi chú</button>
        </div>
      </form>
    </section>`}

    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('note')}<span class="card-title">${adminMode ? 'Ghi chú của nhân viên' : 'Ghi chú của bạn'}</span></div>
          <div class="card-sub" id="note-summary">Đang tải…</div>
        </div>
        <span class="badge info" id="note-count">—</span>
      </div>
      <div id="note-body">
        <div class="sk-lines"><div class="skeleton sk-line"></div><div class="skeleton sk-line short"></div></div>
      </div>
    </section>`;

  if (!adminMode) {
    $('#note-date').value = isoToday();
    try {
      const today = await api('/api/notes/today');
      if (today?.data) {
        $('#note-date').value = today.data.noteDate || isoToday();
        $('#note-content').value = today.data.content || '';
        $('#note-plan').value = today.data.plan || '';
      }
    } catch { /* không có ghi chú hôm nay */ }

    $('#note-form').addEventListener('submit', async e => {
      e.preventDefault();
      const noteDate = $('#note-date').value;
      const content = $('#note-content').value.trim();
      const plan = $('#note-plan').value.trim();
      if (!noteDate || !content) return toast('Thiếu nội dung', 'Chọn ngày và nhập nội dung ghi chú.', 'error');
      try {
        await api('/api/notes/save', { method: 'POST', body: { noteDate, content, plan } });
        toast('Đã lưu ghi chú', fmtDate(noteDate), 'success');
        await loadNotes();
      } catch (err) {
        toast('Không lưu được', err.message, 'error');
      }
    });
  }

  async function loadNotes() {
    const body = $('#note-body');
    const data = await api(listPath);
    const rows = (data.data || []).slice().sort((a, b) => (b.noteDate || '').localeCompare(a.noteDate || ''));
    $('#note-summary').textContent = `${rows.length} ghi chú`;
    $('#note-count').textContent = `${rows.length} ghi chú`;

    if (!rows.length) {
      body.innerHTML = emptyState('Chưa có ghi chú', adminMode ? 'Chưa có nhân viên nào ghi chú.' : 'Lưu ghi chú đầu tiên của bạn ở phía trên.', 'note');
      return;
    }

    body.innerHTML = `
      <div class="item-list">
        ${rows.map(r => `
          <article class="list-item">
            <div class="li-ic">${icon('note')}</div>
            <div class="li-body">
              <div class="li-top">
                <strong>${esc(fmtDate(r.noteDate))}</strong>
                ${adminMode ? `<span class="badge info">${esc(r.fullName || r.username)}</span>` : ''}
                ${r.createdAt ? `<span class="muted" style="font-size:11px">Tạo lúc ${esc(String(r.createdAt).replace('T', ' ').slice(0, 16))}</span>` : ''}
              </div>
              <div class="li-text">${esc(r.content)}</div>
              ${r.plan ? `<div class="li-plan"><strong>Kế hoạch:</strong> ${esc(r.plan)}</div>` : ''}
            </div>
            <div class="li-actions">
              <button type="button" class="icon-btn danger" data-del-note="${r.id}" title="Xóa ghi chú">${icon('trash')}</button>
            </div>
          </article>`).join('')}
      </div>`;

    $$('[data-del-note]', body).forEach(btn => {
      btn.addEventListener('click', async () => {
        const ok = await confirmDialog({
          title: 'Xóa ghi chú',
          message: 'Bạn chắc chắn muốn xóa ghi chú này?',
          confirmText: 'Xóa',
        });
        if (!ok) return;
        try {
          await api(`/api/notes/${btn.dataset.delNote}`, { method: 'DELETE' });
          toast('Đã xóa ghi chú', '', 'info');
          await loadNotes();
        } catch (err) {
          toast('Không xóa được', err.message, 'error');
        }
      });
    });
  }

  await loadNotes();
}

/* ============================================================
   PROFILE
   ============================================================ */
async function renderProfile(root) {
  const user = await api('/api/profile');
  state.user = { ...state.user, ...user };
  sessionStorage.setItem('cc.user', JSON.stringify(state.user));

  root.innerHTML = `
    <div class="profile-grid">
      <section class="card card-pad">
        <div class="profile-head">
          <div class="avatar avatar-lg">${esc(initials(user.fullName || user.username))}</div>
          <div>
            <h3>${esc(user.fullName || user.username)}</h3>
            <p>@${esc(user.username)} · ${user.role === 'ADMIN' ? 'Quản trị viên' : 'Nhân viên'}</p>
          </div>
        </div>
        <div class="kv">
          <div class="kv-row"><span>Tên đăng nhập</span><strong>@${esc(user.username)}</strong></div>
          <div class="kv-row"><span>Họ và tên</span><strong>${esc(user.fullName || '—')}</strong></div>
          <div class="kv-row"><span>Vai trò</span><strong>${roleBadge(user.role)}</strong></div>
          <div class="kv-row"><span>Giao thức</span><strong>HTTP/1.1 · Socket TCP</strong></div>
          <div class="kv-row"><span>Máy chủ</span><strong>${esc(location.host || 'localhost:2020')}</strong></div>
        </div>
      </section>

      <section class="card form-card">
        <h3>Đổi mật khẩu</h3>
        <div class="form-sub">Mật khẩu mới nên có ít nhất 6 ký tự.</div>
        <form id="password-form">
          <div class="field"><label>Mật khẩu hiện tại</label><div class="input-wrap"><input type="password" id="pw-old" autocomplete="current-password" required /></div></div>
          <div class="field"><label>Mật khẩu mới</label><div class="input-wrap"><input type="password" id="pw-new" autocomplete="new-password" required /></div></div>
          <div class="field"><label>Nhập lại mật khẩu mới</label><div class="input-wrap"><input type="password" id="pw-confirm" autocomplete="new-password" required /></div></div>
          <div class="form-actions">
            <button type="submit" class="btn btn-primary">${icon('key')} Cập nhật mật khẩu</button>
          </div>
        </form>
      </section>
    </div>`;

  $('#password-form').addEventListener('submit', async e => {
    e.preventDefault();
    const oldPassword = $('#pw-old').value;
    const newPassword = $('#pw-new').value;
    const confirm = $('#pw-confirm').value;

    if (!oldPassword || !newPassword) return toast('Thiếu thông tin', 'Nhập đầy đủ mật khẩu.', 'error');
    if (newPassword.length < 6) return toast('Mật khẩu quá ngắn', 'Cần ít nhất 6 ký tự.', 'error');
    if (newPassword !== confirm) return toast('Không khớp', 'Mật khẩu nhập lại không khớp.', 'error');

    try {
      await api('/api/change-password', { method: 'POST', body: { oldPassword, newPassword } });
      toast('Đã đổi mật khẩu', 'Lần đăng nhập sau dùng mật khẩu mới.', 'success');
      $('#pw-old').value = '';
      $('#pw-new').value = '';
      $('#pw-confirm').value = '';
    } catch (err) {
      toast('Không đổi được mật khẩu', err.message, 'error');
    }
  });
}

/* ============================================================
   ADMIN — USERS
   ============================================================ */
async function renderAdminUsers(root) {
  root.innerHTML = `
    <div class="filter-bar">
      <div class="field grow">
        <label>Tìm kiếm</label>
        <input type="text" id="u-search" placeholder="Tên hoặc username…" />
      </div>
      <button type="button" class="btn btn-primary" id="u-add">${icon('plus')} Thêm nhân viên</button>
    </div>

    <section class="card card-pad">
      <div class="card-head">
        <div>
          <div class="card-title-row">${icon('users')}<span class="card-title">Danh sách nhân viên</span></div>
          <div class="card-sub" id="u-summary">Đang tải…</div>
        </div>
        <span class="badge info" id="u-count">—</span>
      </div>
      <div id="u-body">
        <div class="sk-lines"><div class="skeleton sk-line"></div><div class="skeleton sk-line short"></div></div>
      </div>
    </section>`;

  let users = [];

  const draw = () => {
    const q = ($('#u-search').value || '').trim().toLowerCase();
    const rows = users.filter(u =>
      !q || u.username.toLowerCase().includes(q) || (u.fullName || '').toLowerCase().includes(q));

    $('#u-summary').textContent = `${users.length} tài khoản`;
    $('#u-count').textContent = `${rows.length} hiển thị`;

    const body = $('#u-body');
    if (!rows.length) {
      body.innerHTML = emptyState('Không tìm thấy', 'Thử từ khóa khác hoặc thêm nhân viên mới.', 'users');
      return;
    }

    body.innerHTML = `
      <div class="table-wrap">
        <table class="data">
          <thead><tr><th>Nhân viên</th><th>Username</th><th>Phòng ban</th><th>Số điện thoại</th><th>Vai trò</th><th class="text-right">Thao tác</th></tr></thead>
          <tbody>
            ${rows.map(u => `
              <tr>
                <td>${userCell(u)}</td>
                <td class="mono">@${esc(u.username)}</td>
                <td>${esc(u.department || '—')}</td>
                <td class="mono">${esc(u.phone || '—')}</td>
                <td>${roleBadge(u.role)}</td>
                <td>
                  <div class="table-actions">
                    <button type="button" class="icon-btn" data-edit-user="${esc(u.username)}" title="Sửa">${icon('edit')}</button>
                    ${u.username === state.user.username ? '' : `<button type="button" class="icon-btn danger" data-del-user="${esc(u.username)}" title="Xóa">${icon('trash')}</button>`}
                  </div>
                </td>
              </tr>`).join('')}
          </tbody>
        </table>
      </div>`;

    $$('[data-edit-user]', body).forEach(btn => {
      btn.addEventListener('click', () => editUser(users.find(u => u.username === btn.dataset.editUser)));
    });
    $$('[data-del-user]', body).forEach(btn => {
      btn.addEventListener('click', () => deleteUser(btn.dataset.delUser));
    });
  };

  const load = async () => {
    const data = await api('/api/admin/users');
    users = data.data || [];
    draw();
  };

  $('#u-search').addEventListener('input', draw);

  $('#u-add').addEventListener('click', async () => {
    const ok = await openModal({
      title: 'Thêm nhân viên',
      subtitle: 'Tạo tài khoản đăng nhập mới.',
      iconName: 'users',
      confirmText: 'Tạo tài khoản',
      wide: true,
      body: `
        <div class="form-grid">
          <div class="field"><label>Username</label><div class="input-wrap"><input id="nu-username" placeholder="vd: nhanvien6" /></div></div>
          <div class="field"><label>Mật khẩu</label><div class="input-wrap"><input id="nu-password" type="password" placeholder="Tối thiểu 6 ký tự" /></div></div>
          <div class="field full"><label>Họ và tên</label><div class="input-wrap"><input id="nu-fullname" placeholder="Nguyễn Văn A" /></div></div>
          <div class="field"><label>Phòng ban</label><div class="input-wrap"><input id="nu-department" placeholder="Phòng Kỹ thuật" /></div></div>
          <div class="field"><label>Số điện thoại</label><div class="input-wrap"><input id="nu-phone" placeholder="0901 234 567" /></div></div>
          <div class="field full"><label>Vai trò</label><div class="input-wrap"><select id="nu-role"><option value="USER">Nhân viên</option><option value="ADMIN">Quản trị viên</option></select></div></div>
        </div>`,
      onConfirm: async modal => {
        const username = $('#nu-username', modal).value.trim();
        const password = $('#nu-password', modal).value;
        const fullName = $('#nu-fullname', modal).value.trim();
        const department = $('#nu-department', modal).value.trim();
        const phone = $('#nu-phone', modal).value.trim();
        const role = $('#nu-role', modal).value;
        if (!username || !password || !fullName) throw new Error('Nhập đầy đủ username, mật khẩu và họ tên.');
        if (password.length < 6) throw new Error('Mật khẩu cần ít nhất 6 ký tự.');
        await api('/api/admin/user/add', { method: 'POST', body: { username, password, fullName, role, department, phone } });
        toast('Đã thêm nhân viên', `${fullName} (@${username})`, 'success');
        await load();
        return true;
      },
    });
    if (!ok) return;
  });

  async function editUser(u) {
    if (!u) return;
    await openModal({
      title: 'Sửa nhân viên',
      subtitle: `@${u.username}`,
      iconName: 'edit',
      confirmText: 'Lưu thay đổi',
      wide: true,
      body: `
        <div class="field"><label>Họ và tên</label><div class="input-wrap"><input id="eu-fullname" value="${esc(u.fullName || '')}" /></div></div>
        <div class="field"><label>Phòng ban</label><div class="input-wrap"><input id="eu-department" value="${esc(u.department || '')}" /></div></div>
        <div class="field"><label>Số điện thoại</label><div class="input-wrap"><input id="eu-phone" value="${esc(u.phone || '')}" /></div></div>
        <div class="field"><label>Vai trò</label><div class="input-wrap">
          <select id="eu-role">
            <option value="USER" ${u.role === 'USER' ? 'selected' : ''}>Nhân viên</option>
            <option value="ADMIN" ${u.role === 'ADMIN' ? 'selected' : ''}>Quản trị viên</option>
          </select>
        </div></div>`,
      onConfirm: async modal => {
        const fullName = $('#eu-fullname', modal).value.trim();
        const department = $('#eu-department', modal).value.trim();
        const phone = $('#eu-phone', modal).value.trim();
        const role = $('#eu-role', modal).value;
        if (!fullName) throw new Error('Họ tên không được để trống.');
        await api(`/api/admin/users/${encodeURIComponent(u.username)}`, { method: 'PATCH', body: { fullName, department, phone, role } });
        toast('Đã cập nhật', `@${u.username}`, 'success');
        await load();
        return true;
      },
    });
  }

  async function deleteUser(username) {
    const ok = await confirmDialog({
      title: 'Xóa nhân viên',
      message: `Bạn chắc chắn muốn xóa tài khoản <strong>@${esc(username)}</strong>? Thao tác này không thể hoàn tác.`,
      confirmText: 'Xóa tài khoản',
    });
    if (!ok) return;
    try {
      await api(`/api/admin/users/${encodeURIComponent(username)}`, { method: 'DELETE' });
      toast('Đã xóa tài khoản', `@${username}`, 'info');
      await load();
    } catch (err) {
      toast('Không xóa được', err.message, 'error');
    }
  }

  await load();
}

/* ============================================================
   INIT
   ============================================================ */
function bindGlobalEvents() {
  // Login
  $('#login-form').addEventListener('submit', async e => {
    e.preventDefault();
    const username = $('#login-username').value.trim();
    const password = $('#login-password').value;
    const errBox = $('#login-error');
    const btn = $('#login-submit');
    errBox.textContent = '';

    if (!username || !password) {
      errBox.textContent = 'Nhập đầy đủ tài khoản và mật khẩu.';
      return;
    }

    btn.disabled = true;
    btn.innerHTML = `<span>Đang đăng nhập…</span>`;
    try {
      await login(username, password);
      $('#login-password').value = '';
    } catch (err) {
      errBox.textContent = err.message || 'Đăng nhập thất bại.';
    } finally {
      btn.disabled = false;
      btn.innerHTML = `<span>Đăng nhập</span><svg class="icon"><use href="#i-arrow-right"/></svg>`;
    }
  });

  $('#toggle-password').addEventListener('click', () => {
    const input = $('#login-password');
    const isPassword = input.type === 'password';
    input.type = isPassword ? 'text' : 'password';
    $('#toggle-password').innerHTML = icon(isPassword ? 'eye-off' : 'eye');
  });

  $$('[data-demo-user]').forEach(btn => {
    btn.addEventListener('click', () => {
      $('#login-username').value = btn.dataset.demoUser;
      $('#login-password').value = btn.dataset.demoPass;
      $('#login-error').textContent = '';
      $('#login-submit').focus();
    });
  });

  // Logout
  $('#sidebar-logout')?.addEventListener('click', logout);
  $('#sidebar-logout-2')?.addEventListener('click', logout);

  // Theme
  $('#theme-btn')?.addEventListener('click', () => {
    const current = document.documentElement.dataset.theme;
    applyTheme(current === 'dark' ? 'light' : 'dark');
  });

  // Network Console
  $('#net-console-btn')?.addEventListener('click', netToggle);
  $('#net-close')?.addEventListener('click', netClose);
  $('#net-clear')?.addEventListener('click', netClear);
  window.addEventListener('keydown', e => {
    if (e.key === 'Escape' && netIsOpen()) netClose();
  });

  // Mobile sidebar
  $('#menu-btn')?.addEventListener('click', openSidebar);
  $('#sidebar-backdrop')?.addEventListener('click', closeSidebar);
  $$('#nav a.nav-item').forEach(a => a.addEventListener('click', closeSidebar));

  // Router
  window.addEventListener('hashchange', renderRoute);
}

function init() {
  let theme = 'light';
  try { theme = localStorage.getItem('cc.theme') || 'light'; } catch {}
  applyTheme(theme);

  const host = location.host || 'localhost:2020';
  const hostEl = $('#login-server-host');
  if (hostEl) hostEl.textContent = host;

  bindGlobalEvents();

  if (state.token) {
    api('/api/profile')
      .then(user => {
        state.user = { ...(state.user || {}), ...user };
        sessionStorage.setItem('cc.user', JSON.stringify(state.user));
        showApp();
      })
      .catch(() => {
        clearSession();
        showLogin();
      });
  } else {
    showLogin();
  }
}

init();
