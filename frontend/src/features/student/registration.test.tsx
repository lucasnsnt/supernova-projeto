import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import { cleanup, render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { MemoryRouter } from 'react-router-dom'
import { StudentProfilePage, profilePayload } from './StudentProfilePage'
import * as api from './profile-api'
import { institutions } from '../admin/api'

vi.mock('../../auth/AuthContext', () => ({ useAuth: () => ({ session: { userId: 1, role: 'STUDENT' } }) }))
vi.mock('./profile-api', () => ({ studentDetails: vi.fn(), studentLinks: vi.fn(), updateProfile: vi.fn(), selectInstitution: vi.fn(), previewInvite: vi.fn(), acceptInvite: vi.fn(), endLink: vi.fn() }))
vi.mock('../admin/api', () => ({ institutions: vi.fn() }))
const address: api.Address = { street: 'Rua A', number: '10', complement: '', neighborhood: 'Centro', city: 'Salvador', state: 'BA', zipCode: '40000000', latitude: -12.9, longitude: -38.5 }
const details: api.StudentDetails = { account: { name: 'Aluno', phone: '71999999999', dateOfBirth: '2000-01-01', email: 'aluno@example.test', address }, institution: null, profileStatus: { hasAddress: true, hasInstitution: false, hasOutboundSchedule: false, hasReturnSchedule: false, complete: false } }
const active: api.StudentLink = { id: 4, driverId: 2, driverName: 'Motorista Ana', status: 'ACTIVE', startDate: '2026-09-15', endDate: null }
function mount() {
  const client = new QueryClient({ defaultOptions: { queries: { retry: false }, mutations: { retry: false } } })
  render(<QueryClientProvider client={client}><MemoryRouter><StudentProfilePage /></MemoryRouter></QueryClientProvider>)
}
beforeEach(() => {
  vi.resetAllMocks()
  vi.mocked(api.studentDetails).mockResolvedValue(details)
  vi.mocked(api.studentLinks).mockResolvedValue([])
  vi.mocked(institutions).mockResolvedValue([{ id: 3, name: 'Faculdade X', type: 'UNIVERSITY', address: { street: 'Rua B', number: '2', city: 'Salvador', state: 'BA' } }])
})
afterEach(cleanup)

describe('cadastro do aluno', () => {
  it('mostra as pendências reais e salva a instituição selecionada', async () => {
    mount()
    expect(await screen.findByText('Selecione sua instituição')).toBeInTheDocument()
    expect(screen.getByText('Cadastre pelo menos um horário na agenda')).toBeInTheDocument()
    await screen.findByRole('option', { name: 'Faculdade X — Salvador' })
    await userEvent.selectOptions(screen.getByLabelText('Instituição'), '3')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar instituição' }))
    await waitFor(() => expect(api.selectInstitution).toHaveBeenCalledWith(3))
  })
  it('salva os dados sem perder coordenadas de um endereço inalterado', async () => {
    mount()
    const name = await screen.findByLabelText('Nome')
    await userEvent.clear(name)
    await userEvent.type(name, 'Novo nome')
    await userEvent.click(screen.getByRole('button', { name: 'Salvar dados' }))
    await waitFor(() => expect(api.updateProfile).toHaveBeenCalledWith(expect.objectContaining({ name: 'Novo nome', address })))
  })
  it('limpa coordenadas antigas quando o endereço muda', () => {
    const form = { name: 'Aluno', phone: '123', dateOfBirth: '2000-01-01', address: { ...address, street: 'Rua Nova' } }
    expect(profilePayload(form, address).address).toEqual({ ...form.address, latitude: null, longitude: null })
  })
  it('confere um convite, impede confirmação obsoleta e aceita o novo', async () => {
    vi.mocked(api.previewInvite).mockResolvedValue({ driverId: 2, driverName: 'Motorista Ana', expiresAt: '2026-12-01T20:00:00' })
    vi.mocked(api.acceptInvite).mockResolvedValue(active)
    mount()
    const input = await screen.findByLabelText('Código de convite')
    await userEvent.type(input, 'convite-a')
    await userEvent.click(screen.getByRole('button', { name: 'Conferir convite' }))
    await screen.findByRole('button', { name: 'Confirmar vínculo' })
    await userEvent.type(input, 'b')
    expect(screen.queryByRole('button', { name: 'Confirmar vínculo' })).not.toBeInTheDocument()
    expect(api.acceptInvite).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Conferir convite' }))
    await userEvent.click(await screen.findByRole('button', { name: 'Confirmar vínculo' }))
    await waitFor(() => expect(api.acceptInvite).toHaveBeenCalledWith('convite-ab', expect.anything()))
    expect(await screen.findByText('Vínculo com motorista confirmado.')).toBeInTheDocument()
  })
  it('exibe erro de convite sem permitir aceitar', async () => {
    vi.mocked(api.previewInvite).mockRejectedValue(new Error('Convite expirado'))
    mount()
    await userEvent.type(await screen.findByLabelText('Código de convite'), 'invalido')
    await userEvent.click(screen.getByRole('button', { name: 'Conferir convite' }))
    expect(await screen.findByText('Convite expirado')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: 'Confirmar vínculo' })).not.toBeInTheDocument()
  })
  it('só encerra o vínculo após confirmação explícita', async () => {
    vi.mocked(api.studentLinks).mockResolvedValue([active])
    vi.mocked(api.endLink).mockResolvedValue({ ...active, status: 'ENDED', endDate: '2026-09-15' })
    mount()
    await userEvent.click(await screen.findByRole('button', { name: 'Encerrar vínculo' }))
    await userEvent.click(screen.getByRole('button', { name: 'Manter vínculo' }))
    expect(api.endLink).not.toHaveBeenCalled()
    await userEvent.click(screen.getByRole('button', { name: 'Encerrar vínculo' }))
    await userEvent.click(screen.getByRole('button', { name: 'Confirmar encerramento' }))
    await waitFor(() => expect(api.endLink).toHaveBeenCalledWith(4, expect.anything()))
    expect(await screen.findByText('Vínculo encerrado.')).toBeInTheDocument()
  })
  it('não trata falha de consulta como cadastro pronto', async () => {
    vi.mocked(api.studentDetails).mockRejectedValue(new Error('Offline'))
    mount()
    expect(await screen.findByText(/Não foi possível verificar seu cadastro/)).toBeInTheDocument()
    expect(screen.queryByLabelText('Nome')).not.toBeInTheDocument()
  })
  it('considera completo um cadastro com apenas ida e motorista ativo', async () => {
    vi.mocked(api.studentDetails).mockResolvedValue({ ...details, profileStatus: { ...details.profileStatus, hasInstitution: true, hasOutboundSchedule: true, complete: true } })
    vi.mocked(api.studentLinks).mockResolvedValue([active])
    mount()
    await screen.findByLabelText('Nome')
    await screen.findByRole('button', { name: 'Encerrar vínculo' })
    expect(screen.queryByText('Complete seu cadastro para viajar')).not.toBeInTheDocument()
    expect(screen.queryByLabelText('Código de convite')).not.toBeInTheDocument()
  })
  it('permite preencher um endereço ausente', async () => {
    vi.mocked(api.studentDetails).mockResolvedValue({ ...details, account: { ...details.account, address: null }, profileStatus: { ...details.profileStatus, hasAddress: false } })
    mount()
    expect(await screen.findByText('Informe seu endereço')).toBeInTheDocument()
    expect(await screen.findByLabelText('Rua')).toHaveValue('')
  })
})
