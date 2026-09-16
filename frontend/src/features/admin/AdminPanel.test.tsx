import { afterEach, beforeEach, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { AdminOverviewPage } from './AdminOverviewPage'
import { AdminStudentsPage } from './AdminStudentsPage'
import { InstitutionsPage } from './InstitutionsPage'
import * as api from './api'

vi.mock('./api', () => ({ drivers: vi.fn(), students: vi.fn(), institutions: vi.fn(), createInstitution: vi.fn(), updateInstitution: vi.fn(), deleteInstitution: vi.fn() }))
const address: api.Address = { street: 'Av. Murilo Dantas', number: '300', complement: null, neighborhood: 'Farolândia', city: 'Aracaju', state: 'SE', zipCode: '49032-490', latitude: -10.95, longitude: -37.05 }
const institution: api.Institution = { id: 3, name: 'Universidade Teste', type: 'UNIVERSITY', address }
const student: api.AdminStudent = { id: 4, name: 'Ana', email: 'ana@example.com', phone: '79999999999', registrationDate: '2026-09-16T09:00:00', address, institutionId: 3, institutionName: institution.name, scheduleCount: 2, driverId: null, driverName: null, linkStatus: null, profileComplete: true }

beforeEach(() => { vi.resetAllMocks(); vi.mocked(api.drivers).mockResolvedValue([]); vi.mocked(api.students).mockResolvedValue([student]); vi.mocked(api.institutions).mockResolvedValue([institution]); vi.mocked(api.updateInstitution).mockResolvedValue(institution) })
afterEach(cleanup)
function mount(element: React.ReactNode) { render(<QueryClientProvider client={new QueryClient({ defaultOptions: { queries: { retry: false } } })}><MemoryRouter>{element}</MemoryRouter></QueryClientProvider>) }

it('explica os fluxos e resume pendências reais', async () => {
  mount(<AdminOverviewPage />)
  expect(await screen.findByText('Como uma conta fica pronta')).toBeVisible()
  expect(await screen.findByText('1 com pendências')).toBeVisible()
  expect(screen.getByText(/Administração analisa/)).toBeVisible()
})

it('mostra por que um aluno ainda não está pronto', async () => {
  mount(<AdminStudentsPage />)
  expect(await screen.findByText('Cadastro incompleto')).toBeVisible()
  expect(screen.getByText('○ Motorista: pendente')).toBeVisible()
  expect(screen.getByText('✓ Agenda: 2 horário(s)')).toBeVisible()
})

it('carrega a instituição para edição e salva a correção', async () => {
  mount(<InstitutionsPage />)
  await userEvent.click(await screen.findByRole('button', { name: 'Editar' }))
  const name = screen.getByLabelText('Nome')
  await userEvent.clear(name)
  await userEvent.type(name, 'Universidade Corrigida')
  await userEvent.click(screen.getByRole('button', { name: 'Salvar alterações' }))
  await waitFor(() => expect(api.updateInstitution).toHaveBeenCalledWith(3, expect.objectContaining({ name: 'Universidade Corrigida' })))
})
