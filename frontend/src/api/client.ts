import { getToken } from '../auth'

export async function api<T>(path: string, options: RequestInit = {}): Promise<T> {
  const token = getToken()
  const headers = new Headers(options.headers)
  headers.set('Content-Type', 'application/json')
  if (token) headers.set('Authorization', `Bearer ${token}`)

  console.log('API Request:', { path, method: options.method || 'GET', hasToken: !!token })
  
  const res = await fetch(path, { ...options, headers })
  if (!res.ok) {
    const text = await res.text()
    console.error('API Error:', { status: res.status, path, response: text })
    throw new Error(text || `HTTP ${res.status}: ${res.statusText}`)
  }
  return res.status === 204 ? (undefined as T) : (await res.json())
}


