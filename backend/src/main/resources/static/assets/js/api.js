import { state } from "./state.js";
let onUnauthorized = () => {};
export function setUnauthorizedHandler(handler) { onUnauthorized = handler; }
export class ApiError extends Error { constructor(message, status) { super(message); this.status = status; } }
export async function api(path, options = {}) {
  const token = state.session?.accessToken;
  const headers = new Headers(options.headers || {}); if (options.body && !headers.has("Content-Type")) headers.set("Content-Type", "application/json"); if (token) headers.set("Authorization", `Bearer ${token}`);
  let response; try { response = await fetch(path, { ...options, headers, signal: options.signal || AbortSignal.timeout(20000) }); } catch { throw new ApiError("Chưa nhận được phản hồi từ máy chủ. Đang thử kết nối lại.", 0); }
  const raw = await response.text(); let data = null; try { data = raw ? JSON.parse(raw) : null; } catch { throw new ApiError("Máy chủ trả về dữ liệu không hợp lệ.", response.status); }
  if (!response.ok) { const error = new ApiError(data?.error || data?.message || `Yêu cầu thất bại (${response.status}).`, response.status); if (response.status === 401 && token && token === state.session?.accessToken) onUnauthorized(); throw error; } return data;
}
