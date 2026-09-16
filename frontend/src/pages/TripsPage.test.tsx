import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { TripsPage } from './TripsPage'
import * as api from '../features/driver/api'
import { studentTrips, type Trip } from '../features/student/api'
let role = 'DRIVER'
vi.mock('../auth/AuthContext', () => ({ useAuth: () => ({ session: { role, driverStatus: 'APPROVED' } }) }))
vi.mock('../features/driver/api', () => ({ driverTrips: vi.fn(), tripAction: vi.fn(), cancelTrip: vi.fn(), vehicles: vi.fn(), changeTripVehicle: vi.fn(), updateDeparture: vi.fn() }))
vi.mock('../features/student/api', async importOriginal => ({ ...await importOriginal<typeof import('../features/student/api')>(), studentTrips: vi.fn() }))
const trip: Trip = { id: 4, serviceDate: '2026-09-15', direction: 'VOLTA', status: 'PLANNED', departureAt: '2026-09-15T12:00:00', planningIssue: null, startedAt: null, completedAt: null, cancellationReason: null, vehicle: { id: 1, model: 'Ducato', licensePlate: 'ABC1234' }, participants: [{ studentId: 2, studentName: 'Ana', institutionName: 'Faculdade', pickupOrder: 1, dropoffOrder: 2, estimatedPickupAt: '2026-09-15T12:10:00', estimatedDropoffAt: '2026-09-15T12:30:00', pickupAddress: null, dropoffAddress: null }] }
function mount() { render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><TripsPage /></QueryClientProvider>) }
beforeEach(() => { vi.resetAllMocks(); role = 'DRIVER'; vi.mocked(api.driverTrips).mockResolvedValue([trip]); vi.mocked(api.vehicles).mockResolvedValue([]) })
afterEach(cleanup)
it('inicia e conclui após consultar cada transição persistida', async () => {
  vi.mocked(api.tripAction).mockImplementation(async (_id, action) => {
    const changed = { ...trip, status: action === 'start' ? 'IN_PROGRESS' : 'COMPLETED', startedAt: '2026-09-15T12:00:00', completedAt: action === 'completion' ? '2026-09-15T12:40:00' : null }
    vi.mocked(api.driverTrips).mockResolvedValue([changed]); return changed
  })
  mount()
  expect(await screen.findByText('Ana')).toBeInTheDocument()
  await userEvent.click(await screen.findByRole('button', { name: 'Iniciar' }))
  await userEvent.click(await screen.findByRole('button', { name: 'Concluir' }))
  expect(await screen.findByText('Concluída')).toBeInTheDocument()
  expect(screen.getByText('Concluída às 12:40')).toBeInTheDocument()
  expect(api.tripAction).toHaveBeenCalledTimes(2)
})
it('mostra erro ao iniciar e mantém a viagem planejada', async () => {
  vi.mocked(api.tripAction).mockRejectedValue(new Error('Motorista suspenso'))
  mount()
  await userEvent.click(await screen.findByRole('button', { name: 'Iniciar' }))
  expect(await screen.findByRole('alert')).toHaveTextContent('Motorista suspenso')
  expect(screen.queryByRole('button', { name: 'Concluir' })).not.toBeInTheDocument()
})
it('consulta a data escolhida para acompanhar o histórico', async () => {
  mount()
  await screen.findByText('Ana')
  await userEvent.click(screen.getByRole('button', { name: 'Dia anterior' }))
  await waitFor(() => expect(api.driverTrips).toHaveBeenCalledWith('2026-09-15'))
})
it('aluno acompanha suas paradas sem comandos do motorista', async () => {
  role = 'STUDENT'; vi.mocked(studentTrips).mockResolvedValue([trip]); mount()
  expect(await screen.findByText('Ana')).toBeInTheDocument()
  expect(screen.queryByRole('button', { name: 'Iniciar' })).not.toBeInTheDocument()
  expect(screen.getByText(/Embarque 12:10/)).toBeInTheDocument()
})
