/**
 * 统一管理登录态（JWT Token + 用户信息）
 * 约定：后端返回结构为 ApiResponse<LoginResponse>
 */

const TOKEN_KEY = 'campusguess.token';
const AUTH_KEY = 'campusguess.auth';

// 说明：
// - 默认使用 localStorage（同浏览器多标签共享登录态，符合多数产品预期）
// - 为了本地测试“同一浏览器开两个账号”，支持在某个标签页启用 sessionStorage 模式
//   用法：首次打开该标签页时带参数 ?storage=session（或 ?storage=local 切回）
//   该选择会写入本标签页的 sessionStorage 并在路由跳转后保持
const STORAGE_MODE_KEY = 'campusguess.storageMode';

function syncStorageModeFromUrl() {
  try {
    const mode = new URLSearchParams(window.location.search).get('storage');
    if (mode === 'session' || mode === 'local') {
      sessionStorage.setItem(STORAGE_MODE_KEY, mode);
    }
  } catch {
    // ignore
  }
}

function getStorage() {
  syncStorageModeFromUrl();

  try {
    const mode = sessionStorage.getItem(STORAGE_MODE_KEY);
    if (mode === 'session') return sessionStorage;
  } catch {
    // ignore
  }

  return localStorage;
}

export function getToken() {
  return getStorage().getItem(TOKEN_KEY) || '';
}

export function setToken(token) {
  if (!token) {
    getStorage().removeItem(TOKEN_KEY);
    return;
  }
  getStorage().setItem(TOKEN_KEY, token);
}

export function clearAuth() {
  const storage = getStorage();
  storage.removeItem(TOKEN_KEY);
  storage.removeItem(AUTH_KEY);
}

export function setAuth(auth) {
  // auth: { token, expireTime, userInfo }
  if (!auth?.token) {
    clearAuth();
    return;
  }
  // 兼容旧结构：为昵称提供默认值
  const normalized = {
    ...auth,
    displayName: auth.displayName || auth?.userInfo?.username || '',
  };

  setToken(normalized.token);
  getStorage().setItem(AUTH_KEY, JSON.stringify(normalized));
}

export function getAuth() {
  try {
    const raw = getStorage().getItem(AUTH_KEY);
    return raw ? JSON.parse(raw) : null;
  } catch {
    return null;
  }
}

export function getCurrentUserInfo() {
  const auth = getAuth();
  return auth?.userInfo ?? null;
}

// 昵称（仅前端显示用，不影响登录 username）
export function getDisplayName() {
  const auth = getAuth();
  return auth?.displayName || auth?.userInfo?.username || '';
}

export function setDisplayName(displayName) {
  const auth = getAuth();
  if (!auth) return;
  const next = { ...auth, displayName: displayName || auth?.userInfo?.username || '' };
  getStorage().setItem(AUTH_KEY, JSON.stringify(next));
}
