import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StudentsPage } from './StudentsPage'
import * as api from './api'
vi.mock('./api', () => ({ linkedStudents: vi.fn(), invites: vi.fn(), vehicles: vi.fn(), createInvite: vi.fn(), createVehicle: vi.fn(), setDefaultVehicle: vi.fn() }))
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.linkedStudents).mockResolvedValue([])
  vi.mocked(api.invites).mockResolvedValue([])
  vi.mocked(api.vehicles).mockResolvedValue([
    { id: 1, brand: 'Fiat', model: 'Ducato', year: null, licensePlate: 'ABC1234', passengerCapacity: 12, color: null, defaultVehicle: true },
    { id: 2, brand: 'Ford', model: 'Transit', year: null, licensePlate: 'DEF1234', passengerCapacity: 15, color: null, defaultVehicle: false },
  ])
})
afterEach(cleanup)
function mount() { render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><StudentsPage /></QueryClientProvider>) }
it('troca o veículo padrão e consulta a configuração persistida', async () => {
  vi.mocked(api.setDefaultVehicle).mockImplementation(async () => {
    vi.mocked(api.vehicles).mockResolvedValue([])
    return { id: 2 } as api.Vehicle
  })
  mount()
  await userEvent.click(await screen.findByRole('button', { name: 'Usar como padrão' }))
  await waitFor(() => expect(api.setDefaultVehicle).toHaveBeenCalledWith(2, expect.anything()))
  await waitFor(() => expect(api.vehicles).toHaveBeenCalledTimes(2))
})
it('mostra uma falha ao gerar convite', async () => {
  vi.mocked(api.createInvite).mockRejectedValue(new Error('Motorista precisa estar aprovado'))
  mount()
  await userEvent.click(screen.getByRole('button', { name: 'Gerar convite' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Motorista precisa estar aprovado')
})
