export async function readJsonResponse<T = any>(
  response: Response,
  fallbackMessage = '请求失败'
): Promise<T> {
  const text = await response.text()

  if (!text.trim()) {
    throw new Error(response.ok
      ? '服务器返回空响应，请确认后端服务是否正常'
      : `${fallbackMessage}（HTTP ${response.status}）`)
  }

  try {
    return JSON.parse(text) as T
  } catch {
    const preview = text.replace(/\s+/g, ' ').slice(0, 120)
    throw new Error(`${fallbackMessage}: 响应不是合法 JSON${preview ? `（${preview}）` : ''}`)
  }
}
