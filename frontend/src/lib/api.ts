export type ApiErrorBody = { message?: string; detail?: string; errors?: Record<string, string> }

export class ApiError extends Error {
  readonly status: number
  readonly body: ApiErrorBody | null

  constructor(status: number, body: ApiErrorBody | null) {
    super(body?.detail ?? body?.message ?? 'Não foi possível concluir a solicitação.')
    this.status = status
    this.body = body
  }
}

let accessToken: string | null = sessionStorage.getItem('supernova_access_token')
type CsrfToken = { headerName: string; token: string }
let csrfRequest: Promise<CsrfToken> | null = null
let refreshRequest: Promise<void> | null = null

export function setAccessToken(token: string | null) {
  accessToken = token
  if (token) sessionStorage.setItem('supernova_access_token', token)
  else sessionStorage.removeItem('supernova_access_token')
}

async function csrfHeaders(): Promise<Record<string, string>> {
  // Não reutilizar tokens entre operações: login/logout podem trocar o cookie.
  // Operações simultâneas compartilham apenas a requisição em andamento.
  if (!csrfRequest) {
    csrfRequest = fetch('/api/auth/csrf', { credentials: 'include', cache: 'no-store' })
      .then(async response => {
        if (!response.ok) throw new ApiError(response.status, {
          detail: 'Não foi possível atualizar a segurança da página. Recarregue a página e tente novamente.',
        })
        return await response.json() as CsrfToken
      })
      .finally(() => { csrfRequest = null })
  }
  const csrf = await csrfRequest
  return { [csrf.headerName]: csrf.token }
}

export async function apiFetch<T>(path: string, options: RequestInit = {}, retryAuthentication = true): Promise<T> {
  const requestToken = accessToken
  const method = (options.method ?? 'GET').toUpperCase()
  const changesState = !['GET', 'HEAD', 'OPTIONS'].includes(method)
  const headers = new Headers(options.headers)
  if (options.body && !headers.has('Content-Type')) headers.set('Content-Type', 'application/json')
  // Autenticação pública não deve ser bloqueada por um JWT antigo no navegador.
  if (accessToken && !path.startsWith('/api/auth/')) headers.set('Authorization', `Bearer ${accessToken}`)
  if (changesState) {
    const securityHeaders = await csrfHeaders()
    Object.entries(securityHeaders).forEach(([name, value]) => headers.set(name, value))
  }

  const response = await fetch(path, { ...options, headers, credentials: 'include' })
  if (response.status === 401 && accessToken && !path.startsWith('/api/auth/') && retryAuthentication) {
    if (requestToken !== accessToken) return apiFetch<T>(path, options, false)
    if (!refreshRequest) {
      refreshRequest = apiFetch<{ accessToken: string }>('/api/auth/refresh', { method: 'POST' })
        .then(tokens => setAccessToken(tokens.accessToken))
        .finally(() => { refreshRequest = null })
    }
    await refreshRequest
    return apiFetch<T>(path, options, false)
  }
  if (!response.ok) {
    let body: ApiErrorBody | null = null
    try {
      body = (await response.json()) as ApiErrorBody
    } catch {
      // Respostas vazias continuam representadas pelo status HTTP.
    }
    if (response.status === 403 && changesState && path.startsWith('/api/auth/') && !body?.detail && !body?.message) {
      body = { detail: 'A validação de segurança da página falhou. Recarregue a página e tente novamente.' }
    }
    throw new ApiError(response.status, body)
  }
  const responseBody = await response.text()
  if (!responseBody) return undefined as T
  return JSON.parse(responseBody) as T
}
