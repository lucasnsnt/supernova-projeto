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
  it('limita cada criação a um dia, uma ida e uma volta', async () => {
    const user = userEvent.setup()
    render(<QueryClientProvider client={new QueryClient()}><RouteForm
      routeVehicles={[]}
      routeInstitutions={[]}
      onClose={vi.fn()}
      onCreated={vi.fn()}
    /></QueryClientProvider>)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Dia da rota' }), 'MONDAY')
    const addOutbound = screen.getByRole('button', { name: '+ Ida' })
    await user.click(addOutbound)

    expect(screen.getByText('Ida')).toBeVisible()
    expect(screen.getAllByRole('button', { name: /Horário de saída/ })).toHaveLength(1)
    expect(screen.queryByRole('button', { name: '+ Ida' })).toBeNull()
    expect(screen.getByRole('button', { name: '+ Volta' })).toBeEnabled()
  })

  it('abre o seletor simplificado em vez do campo de hora nativo', async () => {
    const user = userEvent.setup()
    render(<QueryClientProvider client={new QueryClient()}><RouteForm
      routeVehicles={[]}
      routeInstitutions={[]}
      onClose={vi.fn()}
      onCreated={vi.fn()}
    /></QueryClientProvider>)

    await user.selectOptions(screen.getByRole('combobox', { name: 'Dia da rota' }), 'MONDAY')
    await user.click(screen.getByRole('button', { name: '+ Volta' }))
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
