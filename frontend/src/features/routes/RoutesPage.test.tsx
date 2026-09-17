import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { RouteForm } from './RoutesPage'

vi.mock('../driver/api', async importOriginal => ({
  ...await importOriginal<typeof import('../driver/api')>(),
  createDriverRoute: vi.fn(),
}))

afterEach(cleanup)

describe('RouteForm', () => {
  it('limita cada rota a uma ida e uma volta por dia', async () => {
    const user = userEvent.setup()
    render(<QueryClientProvider client={new QueryClient()}><RouteForm
      routeVehicles={[]}
      routeInstitutions={[]}
      onClose={vi.fn()}
      onCreated={vi.fn()}
    /></QueryClientProvider>)

    const addOutbound = screen.getAllByRole('button', { name: '+ Ida' })[0]
    await user.click(addOutbound)

    expect(screen.getByText('Ida')).toBeVisible()
    expect(screen.getAllByRole('button', { name: /Horário de saída/ })).toHaveLength(1)
    expect(screen.queryAllByRole('button', { name: '+ Ida' })).toHaveLength(6)
    expect(screen.getAllByRole('button', { name: '+ Volta' })[0]).toBeEnabled()
  })

  it('abre o seletor simplificado em vez do campo de hora nativo', async () => {
    const user = userEvent.setup()
    render(<QueryClientProvider client={new QueryClient()}><RouteForm
      routeVehicles={[]}
      routeInstitutions={[]}
      onClose={vi.fn()}
      onCreated={vi.fn()}
    /></QueryClientProvider>)

    await user.click(screen.getAllByRole('button', { name: '+ Volta' })[0])
    await user.click(screen.getByRole('button', { name: /Horário de saída/ }))

    expect(screen.getByRole('dialog', { name: /Seg · Volta/ })).toBeVisible()
    expect(screen.getByLabelText('Hora')).toBeVisible()
    expect(screen.getByLabelText('Minuto')).toBeVisible()
    expect(document.querySelector('.route-time-card input[type="time"]')).toBeNull()
  })

  it('mostra a lista de instituições ao adicionar uma parada', async () => {
    const user = userEvent.setup()
    render(<QueryClientProvider client={new QueryClient()}><RouteForm
      routeVehicles={[]}
      routeInstitutions={[{ id: 1, name: 'Faculdade Central', type: 'UNIVERSITY', address: {} as never }]}
      onClose={vi.fn()}
      onCreated={vi.fn()}
    /></QueryClientProvider>)

    await user.click(screen.getByRole('button', { name: 'Adicionar instituição' }))

    expect(screen.getByRole('combobox', { name: 'Instituição atendida 1' })).toHaveValue('1')
    expect(screen.getByRole('option', { name: 'Faculdade Central' })).toBeVisible()
    expect(document.querySelector('.route-stop input[type="time"]')).toBeNull()
  })
})
