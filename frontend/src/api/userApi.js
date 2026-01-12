import { apiClient, unwrapApiResponse } from './apiClient';

/**
 * POST /users
 * @param {{ username: string, password: string, role?: string }} payload
 */
export async function registerUser(payload) {
  const resp = await apiClient.post('/users', {
    username: payload.username,
    password: payload.password,
    role: payload.role ?? 'user',
  });
  return unwrapApiResponse(resp.data);
}

/**
 * POST /auth/login
 * @param {{ username: string, password: string }} payload
 * @returns {Promise<{ token: string, expireTime: string, userInfo: { userId: number, username: string, role: string, phone: string|null, points: number } }>} 
 */
export async function login(payload) {
  const resp = await apiClient.post('/auth/login', {
    username: payload.username,
    password: payload.password,
  });
  return unwrapApiResponse(resp.data);
}

/**
 * GET /users/{username}
 * 注意：后端目前未在 Controller 层显式校验 token，但文档要求携带 Bearer
 * @param {string} username
 */
export async function getUserInfo(username) {
  const resp = await apiClient.get(`/users/${encodeURIComponent(username)}`);
  return unwrapApiResponse(resp.data);
}



/**
 * GET /users/:userId/records
 * 获取指定用户的对战记录列表
 * @param {string | number} userId
 */
export async function getUserRecords(userId) {
  const resp = await apiClient.get(`/users/${userId}/records`);
  return unwrapApiResponse(resp.data);
}