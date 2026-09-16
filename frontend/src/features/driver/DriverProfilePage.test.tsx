import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DriverProfilePage } from './DriverProfilePage'
import * as api from './api'

vi.mock('../../auth/AuthContext', () => ({ useAuth: () => ({ session: { role: 'DRIVER' } }) }))
vi.mock('./api', () => ({ driverProfile: vi.fn(), updateRejectedProfile: vi.fn(), resubmitProfile: vi.fn() }))

const profile: api.DriverProfile = {
  id: 1, name: 'João Motorista', email: 'joao@example.com', phone: '79999999999', dateOfBirth: '1990-05-10', cnh: '12345678900', status: 'REJECTED', statusReason: 'Confira a CNH', reviewedAt: '2026-09-16T10:00:00',
  address: { street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro', city: 'Aracaju', state: 'SE', zipCode: '49000-000', latitude: null, longitude: null },
  operationalAddress: { street: 'Rua A', number: '10', complement: null, neighborhood: 'Centro', city: 'Aracaju', state: 'SE', zipCode: '49000-000', latitude: null, longitude: null },
}

beforeEach(() => { vi.resetAllMocks(); vi.mocked(api.driverProfile).mockResolvedValue(profile); vi.mocked(api.updateRejectedProfile).mockResolvedValue({}); vi.mocked(api.resubmitProfile).mockResolvedValue({ ...profile, status: 'PENDING' }) })
afterEach(cleanup)
function mount() { render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><DriverProfilePage /></QueryClientProvider>) }

it('permite corrigir e reenviar cadastro rejeitado', async () => {
  mount()
  expect(await screen.findByText('Confira a CNH')).toBeVisible()
  const cnh = screen.getByLabelText('CNH')
  await userEvent.clear(cnh)
  await userEvent.type(cnh, '00987654321')
  await userEvent.click(screen.getByRole('button', { name: 'Salvar correções' }))
  await waitFor(() => expect(api.updateRejectedProfile).toHaveBeenCalledWith(expect.objectContaining({ cnh: '00987654321' })))
  await userEvent.click(screen.getByRole('button', { name: 'Reenviar para análise' }))
  await waitFor(() => expect(api.resubmitProfile).toHaveBeenCalled())
})
