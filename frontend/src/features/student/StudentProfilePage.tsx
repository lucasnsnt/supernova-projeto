import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { Navigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { institutions } from '../admin/api'
import { selectInstitution, updateProfile, type Address, type StudentDetails, type ProfilePayload } from './profile-api'
import { StudentReadiness, useStudentRegistration } from './StudentReadiness'
import { StudentDriverLink } from './StudentDriverLink'
import { BirthDateInput } from '../../components/BirthDateInput'
import { displayBirthDate, isoBirthDate } from '../../lib/birthDate'
import { PostalCodeInput } from '../../components/PostalCodeInput'
import { AddressMap } from '../location/AddressMap'
import { LogoutButton } from '../../components/LogoutButton'

const emptyAddress: Address = { street: '', number: '', complement: '', neighborhood: '', city: '', state: '', zipCode: '', latitude: null, longitude: null }
// oxlint-disable-next-line react/only-export-components
export function profilePayload(form: ProfilePayload, original: Address | null): ProfilePayload {
  const locationFields = ['street', 'number', 'neighborhood', 'city', 'state', 'zipCode'] as const
  const changed = locationFields.some(field => form.address[field].trim() !== (original?.[field] ?? '').trim())
  // Typing a new address clears its old point in the form.  Once the person
  // confirms a new pin, however, those freshly selected coordinates must win.
  const hasConfirmedPoint = form.address.latitude != null && form.address.longitude != null
    && (form.address.latitude !== original?.latitude || form.address.longitude !== original?.longitude)
  return {
    ...form,
    address: {
      ...form.address,
      state: form.address.state.toUpperCase(),
      latitude: hasConfirmedPoint ? form.address.latitude : changed ? null : original?.latitude ?? null,
      longitude: hasConfirmedPoint ? form.address.longitude : changed ? null : original?.longitude ?? null,
    },
  }
}

export function StudentProfilePage() {
  const { session } = useAuth()
  const { details } = useStudentRegistration()
  if (session?.role !== 'STUDENT') return <Navigate to="/" replace />
  return <div className="page-stack"><header className="page-heading profile-page-heading"><div><p className="eyebrow">Seu transporte</p><h1>Meu cadastro</h1><p className="muted">Mantenha seus dados, instituição e motorista atualizados.</p></div><LogoutButton /></header>
    <StudentReadiness />
    {details.isPending && <p role="status">Carregando dados…</p>}
    {details.isError && <p role="alert">Não foi possível carregar seus dados.</p>}
    {details.data && <><ProfileForm key={JSON.stringify(details.data.account)} details={details.data} /><InstitutionForm key={details.data.institution?.id ?? 'none'} currentId={details.data.institution?.id} /></>}
    <StudentDriverLink />
  </div>
}

function ProfileForm({ details }: { details: StudentDetails }) {
  const client = useQueryClient()
  const [form, setForm] = useState<ProfilePayload>({ name: details.account.name, phone: details.account.phone, dateOfBirth: displayBirthDate(details.account.dateOfBirth), address: { ...(details.account.address ?? emptyAddress) } })
  const save = useMutation({ mutationFn: () => updateProfile(profilePayload({ ...form, dateOfBirth: isoBirthDate(form.dateOfBirth)! }, details.account.address)), onSuccess: () => void client.invalidateQueries({ queryKey: ['student-registration'] }) })
  function submit(event: FormEvent) { event.preventDefault(); save.mutate() }
  return <section className="profile-card"><h2>Dados pessoais e endereço</h2><p className="muted">{details.account.email}</p><form className="form-stack" onSubmit={submit}>
    <div className="form-grid">{(['name', 'phone'] as const).map((field, index) => <label key={field}>{['Nome', 'Telefone'][index]}<input required type={field === 'phone' ? 'tel' : 'text'} value={form[field]} onChange={event => { save.reset(); setForm({ ...form, [field]: event.target.value }) }} /></label>)}<label>Data de nascimento<BirthDateInput value={form.dateOfBirth} onChange={value => { save.reset(); setForm({ ...form, dateOfBirth: value }) }} /></label></div>
    <div className="form-grid">{(['street', 'number', 'complement', 'neighborhood', 'city', 'state'] as const).map((field, index) => <label key={field}>{['Rua', 'Número', 'Complemento', 'Bairro', 'Cidade', 'Estado (UF)'][index]}<input required={field !== 'complement'} maxLength={field === 'state' ? 2 : undefined} value={form.address[field] ?? ''} onChange={event => { save.reset(); setForm({ ...form, address: { ...form.address, [field]: event.target.value, ...(field === 'complement' ? {} : { latitude: null, longitude: null }) } }) }} /></label>)}<PostalCodeInput value={form.address.zipCode} onChange={value => setForm({ ...form, address: { ...form.address, zipCode: value, latitude: null, longitude: null } })} onResolved={address => setForm({ ...form, address: { ...form.address, ...address, latitude: null, longitude: null } })} /></div>
    <AddressMap address={form.address} latitude={form.address.latitude} longitude={form.address.longitude} onConfirm={(latitude, longitude) => setForm({ ...form, address: { ...form.address, latitude, longitude } })} />
    {save.isError && <p className="form-error" role="alert">{save.error.message}</p>}{save.isSuccess && <p role="status">Dados salvos.</p>}
    <button className="primary-button compact" disabled={save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar dados'}</button>
  </form></section>
}

function InstitutionForm({ currentId }: { currentId?: number }) {
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['institutions'], queryFn: institutions })
  const [selected, setSelected] = useState(currentId?.toString() ?? '')
  const save = useMutation({ mutationFn: () => selectInstitution(Number(selected)), onSuccess: () => void client.invalidateQueries({ queryKey: ['student-registration'] }) })
  return <section className="profile-card"><h2>Instituição de ensino</h2><form className="form-stack" onSubmit={event => { event.preventDefault(); save.mutate() }}>
    <label>Instituição<select required value={selected} disabled={list.isPending || list.isError || save.isPending} onChange={event => { save.reset(); setSelected(event.target.value) }}><option value="">Selecione uma instituição</option>{list.data?.map(item => <option key={item.id} value={item.id}>{item.name} — {item.address.city}</option>)}</select></label>
    {list.isPending && <p role="status">Carregando instituições…</p>}{list.isError && <p role="alert">Não foi possível carregar instituições. <button type="button" onClick={() => void list.refetch()}>Tentar novamente</button></p>}{list.data?.length === 0 && <p>Nenhuma instituição disponível. Solicite o cadastro ao administrador.</p>}
    {save.isError && <p role="alert" className="form-error">{save.error.message}</p>}
    <button className="primary-button compact" disabled={!selected || Number(selected) === currentId || save.isPending || list.isError}>Salvar instituição</button>
  </form></section>
}
