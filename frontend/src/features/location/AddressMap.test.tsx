import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { render, screen } from '@testing-library/react'
import { expect, it } from 'vitest'
import { AddressMap } from './AddressMap'

const firstAddress = {
  street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro',
  city: 'Salvador', state: 'BA', zipCode: '40000-000',
}

function Wrapper({ children }: { children: React.ReactNode }) {
  return <QueryClientProvider client={new QueryClient()}>{children}</QueryClientProvider>
}

it('descarta o ponto candidato quando o endereço é alterado', () => {
  const view = render(
    <AddressMap address={firstAddress} latitude={-12.97} longitude={-38.50} onConfirm={() => undefined} />,
    { wrapper: Wrapper },
  )

  expect(screen.getByRole('button', { name: 'Confirmar este ponto' })).toBeInTheDocument()

  view.rerender(
    <AddressMap address={{ ...firstAddress, number: '20' }} latitude={null} longitude={null} onConfirm={() => undefined} />,
  )

  expect(screen.queryByRole('button', { name: 'Confirmar este ponto' })).not.toBeInTheDocument()
  expect(screen.queryByLabelText('Mapa para confirmação do endereço')).not.toBeInTheDocument()
})
