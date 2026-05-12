// uni.request 封装 + JWT 拦截器
// 开发模式下后端不可用时静默返回空数据，不阻塞 UI 展示

const BASE_URL = 'http://192.168.137.134:8080';
const TOKEN_KEY = 'mianmiantong_token';
export function getToken(): string {
  return uni.getStorageSync(TOKEN_KEY) || '';
}

export function setToken(token: string): void {
  uni.setStorageSync(TOKEN_KEY, token);
}

export function removeToken(): void {
  uni.removeStorageSync(TOKEN_KEY);
}

interface RequestOptions {
  url: string;
  method?: 'GET' | 'POST' | 'PUT' | 'DELETE';
  data?: Record<string, unknown>;
  header?: Record<string, string>;
}

interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const token = getToken();
  const isDev = token.startsWith('dev-token-');

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...options.header,
  };

  if (token && !isDev) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  // 过滤 data 中的 null 值，避免序列化为 "null" 字符串导致后端报 500
  const data = options.data
    ? Object.fromEntries(Object.entries(options.data).filter(([, v]) => v != null))
    : undefined;

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + options.url,
      method: options.method || 'GET',
      data,
      header: headers,
      success: (res) => {
        // 后端返回 401 → 重新登录
        if (res.statusCode === 401) {
          removeToken();
          uni.reLaunch({ url: '/pages/login/login' });
          reject(new Error('登录已过期'));
          return;
        }

        const apiRes = (res.data || {}) as ApiResponse<T>;

        if (apiRes.code === 200) {
          resolve(apiRes);
          return;
        }

        // 非 200 静默返回空
        resolve({ code: 200, message: 'ok', data: (Array.isArray(apiRes.data) ? [] : apiRes.data) as unknown as T });
      },
      fail: () => {
        // 网络不通，静默返回空
        resolve({ code: 200, message: 'offline', data: [] as unknown as T });
      },
    });
  });
}

export function get<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'GET', data, showLoading: false });
}

export function post<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'POST', data, showLoading: false });
}

export function put<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'PUT', data, showLoading: false });
}

export function del<T = unknown>(url: string): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'DELETE', showLoading: false });
}

/* ========== 流式请求（SSE）========== */

interface StreamCallbacks {
  onToken: (token: string) => void
  onFinish: (data: StreamFinishData) => void
  onError: (error: Error) => void
}

interface StreamFinishData {
  finished: boolean
  questionIndex?: number
  report?: Record<string, unknown>
  message?: string
}

export function streamRequest(
  url: string,
  requestData: Record<string, unknown>,
  callbacks: StreamCallbacks,
): { abort: () => void } {
  const token = getToken();
  const isDev = token.startsWith('dev-token-');

  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
  };

  if (token && !isDev) {
    headers['Authorization'] = `Bearer ${token}`;
  }

  const filtered = Object.fromEntries(
    Object.entries(requestData).filter(([, v]) => v != null),
  );

  let buffer = '';
  let receivedChunks = false;

  // #ifdef MP-WEIXIN
  const task = wx.request({
    url: BASE_URL + url,
    method: 'POST',
    data: filtered,
    header: headers,
    timeout: 180000,
    enableChunked: true,
    success: (res) => {
      if (!receivedChunks) {
        const apiRes = (res.data || {}) as ApiResponse;
        callbacks.onError(new Error(apiRes.message || '请求失败'));
      }
    },
    fail: (err) => {
      callbacks.onError(new Error(err.errMsg || '网络连接失败'));
    },
  });

  task.onChunkReceived((res: WechatMiniprogram.OnChunkReceivedListenerResult) => {
    receivedChunks = true;
    const text = arrayBufferToString(res.data);
    buffer += text;

    const parts = buffer.split('\n\n');
    buffer = parts.pop() || '';

    for (const part of parts) {
      if (!part.trim()) continue;
      parseSSEEvent(part, callbacks);
    }
  });

  return { abort: () => task.abort() };
  // #endif

  // #ifndef MP-WEIXIN
  post<Record<string, unknown>>(url, requestData)
    .then((r) => {
      const d = r.data || {};
      if (d.question) {
        callbacks.onToken(d.question as string);
      }
      callbacks.onFinish({
        finished: (d.finished as boolean) || false,
        questionIndex: d.questionIndex as number,
        report: d.report as Record<string, unknown>,
      });
    })
    .catch((e) => callbacks.onError(e as Error));
  return { abort: () => {} };
  // #endif
}

function arrayBufferToString(buf: ArrayBuffer): string {
  const bytes = new Uint8Array(buf);
  let result = '';
  for (let i = 0; i < bytes.length; i++) {
    result += String.fromCharCode(bytes[i]);
  }
  return decodeURIComponent(escape(result));
}

function parseSSEEvent(raw: string, callbacks: StreamCallbacks): void {
  const lines = raw.split('\n');
  let eventType = 'message';
  const dataLines: string[] = [];

  for (const line of lines) {
    if (line.startsWith('event: ')) {
      eventType = line.slice(7).trim();
    } else if (line.startsWith('data: ')) {
      dataLines.push(line.slice(6));
    }
  }

  const dataStr = dataLines.join('\n');

  if (eventType === 'token') {
    callbacks.onToken(dataStr);
  } else if (eventType === 'error') {
    callbacks.onError(new Error(dataStr || 'AI服务异常'));
  } else if (eventType === 'finish') {
    try {
      callbacks.onFinish(JSON.parse(dataStr) as StreamFinishData);
    } catch {
      callbacks.onFinish({ finished: false });
    }
  }
}
