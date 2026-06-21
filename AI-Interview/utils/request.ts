// Unified uni-app request helpers for API, raw payloads, and SSE streams.
let DEFAULT_BASE_URL = '';
// #ifndef H5
DEFAULT_BASE_URL = 'http://10.80.168.43:8080';
// #endif

export function getBaseUrl(): string {
  return uni.getStorageSync('mianmiantong_base_url') || DEFAULT_BASE_URL;
}

export const BASE_URL = getBaseUrl();

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

export interface ApiResponse<T = unknown> {
  code: number;
  message: string;
  data: T;
}

export interface StreamFinishData {
  finished: boolean;
  questionIndex?: number;
  report?: Record<string, unknown>;
  message?: string;
  coding?: boolean;
  codingReviewDone?: boolean;
}

export interface MpSseCallbacks {
  onToken: (token: string) => void;
  onFinish: (data: StreamFinishData) => void;
  onError: (error: Error) => void;
  onEvent?: (event: string, data: string) => void;
}

function compactData(data?: Record<string, unknown>): Record<string, unknown> | undefined {
  if (!data) return undefined;
  return Object.fromEntries(
    Object.entries(data).filter(([, value]) => value !== null && value !== undefined && value !== ''),
  );
}

export function buildUrl(url: string, params?: Record<string, unknown>): string {
  const data = compactData(params);
  if (!data) return url;
  const query = Object.entries(data)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`)
    .join('&');
  if (!query) return url;
  return `${url}${url.includes('?') ? '&' : '?'}${query}`;
}

function authHeaders(header?: Record<string, string>): Record<string, string> {
  const token = getToken();
  const headers: Record<string, string> = {
    'Content-Type': 'application/json',
    ...header,
  };
  if (token) headers.Authorization = `Bearer ${token}`;
  return headers;
}

function handleUnauthorized(reject: (reason?: unknown) => void): void {
  removeToken();
  uni.reLaunch({ url: '/pages/login/login' });
  reject(new Error('Login expired'));
}

export function request<T = unknown>(options: RequestOptions): Promise<ApiResponse<T>> {
  const method = options.method || 'GET';
  const data = compactData(options.data);
  const requestUrl = getBaseUrl() + buildUrl(options.url, method === 'GET' ? data : undefined);

  return new Promise((resolve, reject) => {
    uni.request({
      url: requestUrl,
      method,
      data: method === 'GET' ? undefined : data,
      header: authHeaders(options.header),
      timeout: 30000,
      success: (res) => {
        if (res.statusCode === 401) {
          handleUnauthorized(reject);
          return;
        }
        resolve((res.data || {}) as ApiResponse<T>);
      },
      fail: (err) => reject(new Error(err.errMsg || 'Network request failed')),
    });
  });
}

export function get<T = unknown>(url: string, params?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'GET', data: params });
}

export function post<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'POST', data });
}

export function put<T = unknown>(url: string, data?: Record<string, unknown>): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'PUT', data });
}

export function del<T = unknown>(url: string): Promise<ApiResponse<T>> {
  return request<T>({ url, method: 'DELETE' });
}

export function rawRequest<T = unknown>(options: RequestOptions): Promise<T> {
  const method = options.method || 'GET';
  const data = compactData(options.data);
  const requestUrl = getBaseUrl() + buildUrl(options.url, method === 'GET' ? data : undefined);

  return new Promise((resolve, reject) => {
    uni.request({
      url: requestUrl,
      method,
      data: method === 'GET' ? undefined : data,
      header: authHeaders(options.header),
      timeout: 30000,
      success: (res) => {
        if (res.statusCode === 401) {
          handleUnauthorized(reject);
          return;
        }
        if (res.statusCode < 200 || res.statusCode >= 300) {
          const body = (res.data || {}) as { message?: string; error?: string };
          reject(new Error(body.message || body.error || `Request failed (${res.statusCode})`));
          return;
        }
        const body = res.data as T | ApiResponse<T>;
        if (body && typeof body === 'object' && 'code' in body && 'data' in body) {
          const apiBody = body as ApiResponse<T>;
          if (apiBody.code !== 200) {
            reject(new Error(apiBody.message || 'Request failed'));
            return;
          }
          resolve(apiBody.data);
          return;
        }
        resolve(body as T);
      },
      fail: (err) => reject(new Error(err.errMsg || 'Network request failed')),
    });
  });
}

export function rawPost<T = unknown>(url: string, data?: Record<string, unknown>): Promise<T> {
  return rawRequest<T>({ url, method: 'POST', data });
}

export function streamRequest(
  url: string,
  requestData: Record<string, unknown>,
  callbacks: MpSseCallbacks,
): { abort: () => void } {
  const headers = authHeaders({ Accept: 'text/event-stream' });
  const data = compactData(requestData) || {};
  let buffer = '';
  let receivedChunks = false;

  // #ifdef MP-WEIXIN
  const task = wx.request({
    url: getBaseUrl() + url,
    method: 'POST',
    data,
    header: headers,
    timeout: 600000,
    enableChunked: true,
    success: (res) => {
      if (res.statusCode === 401) {
        removeToken();
        uni.reLaunch({ url: '/pages/login/login' });
        callbacks.onError(new Error('Login expired'));
        return;
      }
      if (!receivedChunks) {
        const body = (res.data || {}) as { message?: string; error?: string };
        callbacks.onError(new Error(body.message || body.error || 'Stream request failed'));
      }
    },
    fail: (err) => callbacks.onError(new Error(err.errMsg || 'Stream request failed')),
  });

  task.onChunkReceived((res: { data: ArrayBuffer }) => {
    receivedChunks = true;
    buffer += arrayBufferToString(res.data);
    const parts = buffer.split(/\r?\n\r?\n/);
    buffer = parts.pop() || '';
    parts.forEach((part) => {
      if (part.trim()) parseSSEEvent(part, callbacks);
    });
  });

  return { abort: () => task.abort() };
  // #endif

  // #ifndef MP-WEIXIN
  post<Record<string, unknown>>(url, data)
    .then((res) => {
      const body = res.data || {};
      const token = body.question || body.content || body.message;
      if (token) callbacks.onToken(String(token));
      callbacks.onFinish({
        finished: Boolean(body.finished),
        questionIndex: body.questionIndex as number | undefined,
        report: body.report as Record<string, unknown> | undefined,
        coding: body.coding as boolean | undefined,
        codingReviewDone: body.codingReviewDone as boolean | undefined,
      });
    })
    .catch((error: unknown) => callbacks.onError(error instanceof Error ? error : new Error(String(error))));
  return { abort: () => {} };
  // #endif
}

function arrayBufferToString(buf: ArrayBuffer): string {
  try {
    return new TextDecoder('utf-8').decode(new Uint8Array(buf));
  } catch {
    const bytes = new Uint8Array(buf);
    const chunks: string[] = [];
    for (let i = 0; i < bytes.length; i += 4096) {
      chunks.push(String.fromCharCode(...bytes.subarray(i, i + 4096)));
    }
    return decodeURIComponent(escape(chunks.join('')));
  }
}

function parseSSEEvent(raw: string, callbacks: MpSseCallbacks): void {
  const lines = raw.split(/\r?\n/);
  let eventType = 'message';
  const dataLines: string[] = [];

  lines.forEach((line) => {
    if (line.startsWith('event:')) {
      eventType = line.slice(6).trim() || 'message';
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trimStart());
    }
  });

  const dataStr = dataLines.join('\n');
  callbacks.onEvent?.(eventType, dataStr);

  if (eventType === 'error') {
    callbacks.onError(new Error(dataStr || 'AI service error'));
    return;
  }
  if (eventType === 'finish' || eventType === 'done') {
    callbacks.onFinish(parseFinishData(dataStr));
    return;
  }
  emitToken(dataStr, callbacks);
}

function emitToken(dataStr: string, callbacks: MpSseCallbacks): void {
  if (!dataStr || dataStr === '[DONE]') return;
  try {
    const data = JSON.parse(dataStr) as Record<string, unknown>;
    const token = data.token ?? data.content ?? data.delta ?? data.question ?? data.message;
    if (token != null) callbacks.onToken(String(token));
    if (data.finished || data.report || data.codingReviewDone) callbacks.onFinish(data as StreamFinishData);
  } catch {
    callbacks.onToken(dataStr);
  }
}

function parseFinishData(dataStr: string): StreamFinishData {
  if (!dataStr || dataStr === '[DONE]') return { finished: true };
  try {
    return JSON.parse(dataStr) as StreamFinishData;
  } catch {
    return { finished: true, message: dataStr };
  }
}