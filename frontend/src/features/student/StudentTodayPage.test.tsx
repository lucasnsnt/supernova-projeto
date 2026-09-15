import { afterEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { StudentTodayPage } from './StudentTodayPage'
import { answerConfirmation, studentConfirmations, studentTrips, tomorrow } from './api'
vi.mock('./StudentReadiness', () => ({ StudentReadiness: () => null }))
vi.mock('./api', async importOriginal => ({ ...await importOriginal<typeof import('./api')>(), studentConfirmations: vi.fn(), studentTrips: vi.fn(), answerConfirmation: vi.fn() }))
afterEach(() => { cleanup(); vi.resetAllMocks() })
it('mostra e responde confirmação de amanhã liberada na noite anterior', async () => {
  vi.mocked(studentTrips).mockResolvedValue([])
  vi.mocked(studentConfirmations).mockImplementation(async date => date === tomorrow() ? [{ id: 7, serviceDate: tomorrow(), direction: 'IDA', status: 'PENDING', scheduledTime: '07:00:00', preliminaryDepartureAt: `${tomorrow()}T06:00:00`, responseDeadline: `${tomorrow()}T05:00:00` }] : [])
  vi.mocked(answerConfirmation).mockRejectedValue(new Error('O prazo terminou'))
  render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><StudentTodayPage /></QueryClientProvider>)
  await userEvent.click(await screen.findByRole('button', { name: 'Sim, eu vou' }))
  await waitFor(() => expect(answerConfirmation).toHaveBeenCalledWith(7, 'YES'))
  expect(await screen.findByRole('alert')).toHaveTextContent('O prazo terminou')
})
