import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import * as studentApi from '../student/api'
import { RouteForm, RoutesPage } from './RoutesPage'

vi.mock('../../auth/AuthContext', () => ({
  useAuth: () => ({ session: { role: 'STUDENT' } }),
}))

vi.mock('../student/api', async importOriginal => ({
  ...await importOriginal<typeof import('../student/api')>(),
  availableRoutes: vi.fn(),
  studentRouteEnrollments: vi.fn(),
  studentRouteRoster: vi.fn(),
  leaveRouteEnrollment: vi.fn(),
  requestRouteEnrollment: vi.fn(),
}))

vi.mock('../driver/api', async importOriginal => ({
  ...await importOriginal<typeof import('../driver/api')>(),
  createDriverRoute: vi.fn(),
}))

afterEach(cleanup)

beforeEach(() => vi.clearAllMocks())

describe('participação do aluno na rota', () => {
  it('permite sair e entrar novamente na mesma rota', async () => {
    const user = userEvent.setup()
    const route = {
      id: 9, name: 'Rota X', active: true, vehicleId: 3, vehicleLabel: 'Van ABC1D23',
      schedules: [{ dayOfWeek: 'MONDAY' as const, direction: 'IDA' as const, departureTime: '07:00', responseDeadlineTime: '06:00' }],
      institutions: [{ institutionId: 2, institutionName: 'Faculdade Central', stopOrder: 1 }],
    }
    const enrollment = {
      id: 14, routeId: 9, routeName: 'Rota X', driverName: 'João', studentId: 5, studentName: 'Ana',
      institutionName: 'Faculdade Central', outboundEnabled: true, returnEnabled: true, requestedAt: '2026-09-19T10:00:00',
    }
    vi.mocked(studentApi.availableRoutes).mockResolvedValue([route])
    vi.mocked(studentApi.studentRouteEnrollments)
      .mockResolvedValueOnce([enrollment])
      .mockResolvedValueOnce([])
      .mockResolvedValueOnce([enrollment])
    vi.mocked(studentApi.studentRouteRoster).mockResolvedValue([enrollment])
    vi.mocked(studentApi.leaveRouteEnrollment).mockResolvedValue(undefined)
    vi.mocked(studentApi.requestRouteEnrollment).mockResolvedValue(enrollment)

    render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}>
      <RoutesPage />
    </QueryClientProvider>)

    await user.click(await screen.findByRole('button', { name: 'Abrir detalhes da rota Rota X' }))
    await user.click(screen.getByRole('button', { name: 'Sair da rota' }))
    await user.click(screen.getAllByRole('button', { name: 'Sair da rota' }).at(-1)!)
    expect(await screen.findByRole('button', { name: 'Abrir detalhes da rota Rota X' })).toBeVisible()
    expect(screen.queryByText('Na sua rota')).toBeNull()

    await user.click(screen.getByRole('button', { name: 'Abrir detalhes da rota Rota X' }))
    await user.click(screen.getByRole('button', { name: 'Entrar nesta rota' }))

    expect(await screen.findByText(/Você está nesta rota/)).toBeVisible()
    expect(studentApi.leaveRouteEnrollment).toHaveBeenCalledWith(14)
    expect(studentApi.requestRouteEnrollment).toHaveBeenCalledWith(9, true, true)
  })
})

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
