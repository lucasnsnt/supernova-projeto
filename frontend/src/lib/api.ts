export type ApiErrorBody = { message?: string; errors?: Record<string, string> }

export class ApiError extends Error {
  readonly status: number
  readonly body: ApiErrorBody | null

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.message ?? 'Não foi possível concluir a solicitação.')
    this.status = status
    this.body = body
  }
}

let accessToken: string | null = sessionStorage.getItem('supernova_access_token')
let csrf: { headerName: string; token: string } | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
  if (token) sessionStorage.setItem('supernova_access_token', token)
  else sessionStorage.removeItem('supernova_access_token')
}

async function csrfHeaders(): Promise<Record<string, string>> {
  if (!csrf) {
    const response = await fetch('/api/auth/csrf', { credentials: 'include' })
    if (!response.ok) throw new ApiError(response.status, null)
    csrf = (await response.json()) as { headerName: string; token: string }
  }
  return { [csrf.headerName]: csrf.token }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}): Promise<T> {
  const method = (options.method ?? 'GET').toUpperCase()
  const changesState = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  if (accessToken) headers.set('Authorization', `Bearer ${accessToken}`)
  if (changesState) {
    const securityHeaders = await csrfHeaders()
    Object.entries(securityHeaders).forEach(([name, value]) => headers.set(name, value))
  }

  const response = await fetch(path, { ...options, headers, credentials: 'include' })
  if (!response.ok) {
    let body: ApiErrorBody | null = null
    try {
      body = (await response.json()) as ApiErrorBody
    } catch {
      // Respostas vazias continuam representadas pelo status HTTP.
    }
    throw new ApiError(response.status, body)
  }
  if (response.status === 204) return undefined as T
  return (await response.json()) as T
}
