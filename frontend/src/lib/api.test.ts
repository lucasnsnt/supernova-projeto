import { beforeEach, describe, expect, it, vi } from 'vitest'
import { apiFetch, setAccessToken } from './api'

describe('apiFetch', () => {
  beforeEach(() => {
    sessionStorage.clear()
    setAccessToken(null)
    vi.restoreAllMocks()
  })

  it('envia o token e interpreta a resposta JSON', async () => {
    setAccessToken('access-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ name: 'Lucas' }), {
        status: 200,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    await expect(apiFetch<{ name: string }>('/api/me')).resolves.toEqual({ name: 'Lucas' })
    const request = fetchMock.mock.calls[0]
    expect(request[0]).toBe('/api/me')
    expect(new Headers(request[1]?.headers).get('Authorization')).toBe('Bearer access-token')
  })

  it('preserva a mensagem de erro devolvida pela API', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(
      new Response(JSON.stringify({ detail: 'Credenciais inválidas' }), {
        status: 401,
        headers: { 'Content-Type': 'application/json' },
      }),
    )

    await expect(apiFetch('/api/me')).rejects.toEqual(
      expect.objectContaining({ status: 401, message: 'Credenciais inválidas' }),
    )
  })

  it('aceita uma resposta bem-sucedida sem corpo', async () => {
    vi.spyOn(globalThis, 'fetch').mockResolvedValue(new Response(null, { status: 202 }))

    await expect(apiFetch<void>('/api/auth/email-verification')).resolves.toBeUndefined()
  })

  it('busca um novo token para cada operação após troca de sessão', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'before-login' }))
      .mockResolvedValueOnce(Response.json({ accessToken: 'new-access-token' }))
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'after-login' }))
      .mockResolvedValueOnce(new Response(null, { status: 202 }))
    await apiFetch('/api/auth/login', { method: 'POST', body: '{}' })
    setAccessToken('new-access-token')
    await apiFetch('/api/auth/email-verification', { method: 'POST', body: '{}' })
    expect(fetchMock.mock.calls.map(call => call[0])).toEqual([
      '/api/auth/csrf', '/api/auth/login', '/api/auth/csrf', '/api/auth/email-verification',
    ])
    expect(new Headers(fetchMock.mock.calls[3][1]?.headers).get('X-XSRF-TOKEN')).toBe('after-login')
    expect(new Headers(fetchMock.mock.calls[3][1]?.headers).has('Authorization')).toBe(false)
  })

  it('não envia um JWT antigo às rotas públicas de autenticação', async () => {
    setAccessToken('expired-token')
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(new Response(null, { status: 202 }))
    await apiFetch('/api/auth/email-verification', { method: 'POST', body: '{}' })
    expect(new Headers(fetchMock.mock.calls[1][1]?.headers).has('Authorization')).toBe(false)
  })

  it('orienta recarregar em falha de segurança sem repetir um cadastro', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' }))
      .mockResolvedValueOnce(new Response(null, { status: 403 }))
    await expect(apiFetch('/api/auth/register', { method: 'POST', body: '{}' })).rejects.toMatchObject({
      status: 403, message: 'A validação de segurança da página falhou. Recarregue a página e tente novamente.',
    })
    expect(fetchMock).toHaveBeenCalledTimes(2)
  })

  it('permite uma nova tentativa se a busca do token falhar', async () => {
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(new Response(null, { status: 503 }))
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'recovered' }))
      .mockResolvedValueOnce(new Response(null, { status: 202 }))
    await expect(apiFetch('/api/auth/email-verification', { method: 'POST' })).rejects.toThrow('Recarregue a página')
    await expect(apiFetch('/api/auth/email-verification', { method: 'POST' })).resolves.toBeUndefined()
    expect(fetchMock).toHaveBeenCalledTimes(3)
  })
  it('renova sessão expirada e repete a operação com CSRF atualizado', async () => {
    setAccessToken('expired')
    const fetchMock = vi.spyOn(globalThis, 'fetch')
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'first' }))
      .mockResolvedValueOnce(new Response(null, { status: 401 }))
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'refresh' }))
      .mockResolvedValueOnce(Response.json({ accessToken: 'renewed' }))
      .mockResolvedValueOnce(Response.json({ headerName: 'X-XSRF-TOKEN', token: 'retry' }))
      .mockResolvedValueOnce(Response.json({ status: 'COMPLETED' }))
    await expect(apiFetch('/api/drivers/me/trips/1/completion', { method: 'POST' })).resolves.toEqual({ status: 'COMPLETED' })
    expect(new Headers(fetchMock.mock.calls[5][1]?.headers).get('Authorization')).toBe('Bearer renewed')
    expect(new Headers(fetchMock.mock.calls[5][1]?.headers).get('X-XSRF-TOKEN')).toBe('retry')
  })
  it('compartilha a renovação entre consultas simultâneas', async () => {
    setAccessToken('expired')
    let renewed = false
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (path) => {
      if (path === '/api/auth/csrf') return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' })
      if (path === '/api/auth/refresh') { renewed = true; return Response.json({ accessToken: 'renewed' }) }
      return renewed ? Response.json({ ok: true }) : new Response(null, { status: 401 })
    })
    await Promise.all([apiFetch('/api/me'), apiFetch('/api/drivers/me/trips')])
    expect(fetchMock.mock.calls.filter(([path]) => path === '/api/auth/refresh')).toHaveLength(1)
  })
  it('não repete indefinidamente quando a sessão renovada é recusada', async () => {
    setAccessToken('expired')
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async (path) => {
      if (path === '/api/auth/csrf') return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' })
      if (path === '/api/auth/refresh') return Response.json({ accessToken: 'renewed' })
      return new Response(null, { status: 401 })
    })
    await expect(apiFetch('/api/me')).rejects.toMatchObject({ status: 401 })
    expect(fetchMock.mock.calls.filter(([path]) => path === '/api/auth/refresh')).toHaveLength(1)
  })

  it('reutiliza o novo token quando uma resposta antiga chega após a renovação', async () => {
    setAccessToken('expired')
    let releaseOldResponse: (() => void) | undefined
    let slowCalls = 0
    const oldResponse = new Promise<void>(resolve => { releaseOldResponse = resolve })
    const fetchMock = vi.spyOn(globalThis, 'fetch').mockImplementation(async path => {
      if (path === '/api/slow' && slowCalls++ === 0) { await oldResponse; return new Response(null, { status: 401 }) }
      if (path === '/api/auth/csrf') return Response.json({ headerName: 'X-XSRF-TOKEN', token: 'csrf' })
      if (path === '/api/auth/refresh') return Response.json({ accessToken: 'renewed' })
      if (path === '/api/me' && sessionStorage.getItem('supernova_access_token') === 'expired') return new Response(null, { status: 401 })
      return Response.json({ ok: true })
    })
    const slow = apiFetch('/api/slow')
    await apiFetch('/api/me')
    releaseOldResponse?.()
    await slow
    expect(fetchMock.mock.calls.filter(([path]) => path === '/api/auth/refresh')).toHaveLength(1)
  })

})
