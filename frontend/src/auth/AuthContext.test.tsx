import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { AuthProvider, useAuth } from './AuthContext'
import { apiFetch } from '../lib/api'
vi.mock('../lib/api', () => ({ apiFetch: vi.fn(), setAccessToken: vi.fn(), AUTH_EXPIRED_EVENT: 'supernova:auth-expired' }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
function Consumer() { const { session, login } = useAuth(); return <><p>{session?.driverStatus}</p><button onClick={() => void login({ email: 'driver@example.test', password: 'test' })}>Entrar</button></> }
it('atualiza aprovação usando o cadastro atual sem novo login', async () => {
  vi.mocked(apiFetch).mockImplementation(async (path) => {
    if (path === '/api/auth/refresh') throw new Error('Sem sessão')
    if (path === '/api/auth/login') return { userId: 1, role: 'DRIVER', driverStatus: 'PENDING', accessToken: 'test' }
    return { id: 1, role: 'DRIVER', driverStatus: 'APPROVED' }
  })
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><AuthProvider><Consumer /></AuthProvider></QueryClientProvider>)
  await userEvent.click(screen.getByRole('button', { name: 'Entrar' }))
  expect(await screen.findByText('APPROVED')).toBeInTheDocument()
})
