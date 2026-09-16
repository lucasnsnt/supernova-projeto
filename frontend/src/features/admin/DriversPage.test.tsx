import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { DriversPage } from './DriversPage'
import * as api from './api'

vi.mock('./api', () => ({ drivers: vi.fn(), approveDriver: vi.fn(), rejectDriver: vi.fn(), suspendDriver: vi.fn(), reactivateDriver: vi.fn() }))
const address = { street: 'Av. Hermes Fontes', number: '100', complement: null, neighborhood: 'Suíssa', city: 'Aracaju', state: 'SE', zipCode: '49050-000', latitude: null, longitude: null }
const driver: api.Driver = { id: 7, name: 'João', email: 'joao@example.com', phone: '79999999999', dateOfBirth: '1990-05-10', cnh: '12345678900', status: 'APPROVED', statusReason: null, reviewedAt: '2026-09-16T10:00:00Z', address, operationalAddress: address }

beforeEach(() => { vi.resetAllMocks(); vi.mocked(api.drivers).mockResolvedValue([driver]); vi.mocked(api.suspendDriver).mockResolvedValue({ ...driver, status: 'SUSPENDED' }); vi.spyOn(window, 'prompt').mockReturnValue('Documento vencido') })
afterEach(() => { cleanup(); vi.restoreAllMocks() })
function mount() { render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><DriversPage /></QueryClientProvider>) }

it('exibe os dados avaliados e permite suspender', async () => {
  mount()
  expect(await screen.findByText(/Av. Hermes Fontes/)).toBeVisible()
  expect(screen.getByText(/CNH/)).toHaveTextContent('12345678900')
  await userEvent.click(screen.getByRole('button', { name: 'Suspender' }))
  await waitFor(() => expect(api.suspendDriver).toHaveBeenCalledWith(7, 'Documento vencido'))
})
