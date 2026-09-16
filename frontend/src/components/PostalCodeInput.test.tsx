import { afterEach, expect, it, vi } from 'vitest'
import { useState } from 'react'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { PostalCodeInput } from './PostalCodeInput'

afterEach(() => { cleanup(); vi.restoreAllMocks() })

it('formata o CEP e preenche o endereço encontrado', async () => {
  const resolved = vi.fn()
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ logradouro: 'Av. Murilo Dantas', bairro: 'Farolândia', localidade: 'Aracaju', uf: 'SE' }) }))
  function Harness() { const [value, setValue] = useState(''); return <PostalCodeInput value={value} onChange={setValue} onResolved={resolved} /> }
  render(<Harness />)
  await userEvent.type(screen.getByLabelText('CEP'), '49032490')
  expect(screen.getByLabelText('CEP')).toHaveValue('49032-490')
  await userEvent.tab()
  expect(resolved).toHaveBeenCalledWith({ street: 'Av. Murilo Dantas', neighborhood: 'Farolândia', city: 'Aracaju', state: 'SE' })
})

it('permite preenchimento manual quando o CEP não existe', async () => {
  vi.stubGlobal('fetch', vi.fn().mockResolvedValue({ ok: true, json: async () => ({ erro: true }) }))
  render(<PostalCodeInput value="00000-000" onChange={vi.fn()} onResolved={vi.fn()} />)
  await userEvent.click(screen.getByLabelText('CEP'))
  await userEvent.tab()
  expect(await screen.findByRole('alert')).toHaveTextContent('CEP não encontrado')
  expect(screen.getByRole('alert')).toHaveTextContent('manualmente')
})
