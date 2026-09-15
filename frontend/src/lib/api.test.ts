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
})
