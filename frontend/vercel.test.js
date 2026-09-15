import { describe, expect, it } from 'vitest'
import { config } from './vercel.mjs'

describe('configuração do proxy Vercel', () => {
  it('declara a variável para resolução pela camada de rotas da Vercel', () => {
    expect(config.rewrites).toEqual([
      { source: '/api/:path*', destination: '$URL_BACKEND/api/:path*', env: ['URL_BACKEND'] },
      { source: '/(.*)', destination: '/index.html' },
    ])
  })

  it('não contém uma origem de produção hardcoded', () => {
    expect(JSON.stringify(config)).not.toContain('lucasnsnt.ink')
  })
})
