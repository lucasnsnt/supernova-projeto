import { afterEach, describe, expect, it, vi } from 'vitest'

afterEach(() => {
  vi.unstubAllEnvs()
  vi.resetModules()
})

describe('configuração do proxy Vercel', () => {
  it('usa a origem do ambiente e preserva o fallback da SPA', async () => {
    vi.stubEnv('URL_BACKEND', 'https://api.example.test/')
    const { config } = await import('./vercel.mjs')
    expect(config.rewrites).toEqual([
      { source: '/api/:path*', destination: 'https://api.example.test/api/:path*' },
      { source: '/(.*)', destination: '/index.html' },
    ])
  })

  it('não usa produção silenciosamente quando a variável falta', async () => {
    vi.stubEnv('URL_BACKEND', '')
    await expect(import('./vercel.mjs')).rejects.toThrow('Configure URL_BACKEND')
  })

  it.each(['http://api.example.test', 'https://api.example.test/api', 'https://user:password@api.example.test'])('recusa uma origem inválida: %s', async (origin) => {
    vi.stubEnv('URL_BACKEND', origin)
    await expect(import('./vercel.mjs')).rejects.toThrow('origem HTTPS')
  })
})
