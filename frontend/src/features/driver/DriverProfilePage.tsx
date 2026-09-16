import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { BirthDateInput } from '../../components/BirthDateInput'
import { displayBirthDate, isoBirthDate } from '../../lib/birthDate'
import { driverProfile, resubmitProfile, setOperationalAddress, updateRejectedProfile, type Address, type DriverProfile } from './api'
import { PostalCodeInput } from '../../components/PostalCodeInput'
import { AddressMap } from '../location/AddressMap'

export function DriverProfilePage() {
  const { session } = useAuth()
  const profile = useQuery({ queryKey: ['driver-profile'], queryFn: driverProfile, enabled: session?.role === 'DRIVER' })
  if (session?.role !== 'DRIVER') return null
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Cadastro do motorista</p><h1>Meu cadastro</h1><p className="muted">Confira os dados enviados para análise administrativa.</p></header>
    {profile.isPending && <p role="status">Carregando dados…</p>}
    {profile.isError && <p role="alert" className="form-error">{profile.error.message}</p>}
    {profile.data && <Profile key={JSON.stringify(profile.data)} profile={profile.data} />}
  </div>
}

function Profile({ profile }: { profile: DriverProfile }) {
  const client = useQueryClient()
  const editable = profile.status === 'REJECTED'
  const [form, setForm] = useState({ name: profile.name, phone: profile.phone, dateOfBirth: displayBirthDate(profile.dateOfBirth), cnh: profile.cnh, address: { ...profile.address } })
  const save = useMutation({ mutationFn: () => updateRejectedProfile({ ...form, dateOfBirth: isoBirthDate(form.dateOfBirth)!, address: addressPayload(form.address, profile.address) }), onSuccess: () => void client.invalidateQueries({ queryKey: ['driver-profile'] }) })
  const resubmit = useMutation({ mutationFn: resubmitProfile, onSuccess: async () => { await Promise.all([client.invalidateQueries({ queryKey: ['driver-profile'] }), client.invalidateQueries({ queryKey: ['session-account'] })]) } })
  function submit(event: FormEvent) { event.preventDefault(); save.mutate() }
  return <>
    <section className="status-card"><div className="status-icon">i</div><div><h2>{statusLabel[profile.status]}</h2><p className="muted">{profile.statusReason ?? statusHelp[profile.status]}</p></div></section>
    <section className="profile-card"><h2>Dados pessoais</h2><p className="muted">{profile.email}</p><form className="form-stack" onSubmit={submit}>
      <div className="form-grid"><Field label="Nome"><input required disabled={!editable} value={form.name} onChange={event => setForm({ ...form, name: event.target.value })} /></Field><Field label="Telefone"><input required disabled={!editable} value={form.phone} onChange={event => setForm({ ...form, phone: event.target.value })} /></Field><Field label="Data de nascimento"><BirthDateInput disabled={!editable} value={form.dateOfBirth} onChange={value => setForm({ ...form, dateOfBirth: value })} /></Field><Field label="CNH"><input required disabled={!editable} value={form.cnh} onChange={event => setForm({ ...form, cnh: event.target.value })} /></Field></div>
      <h3>Endereço de cadastro</h3><AddressFields disabled={!editable} address={form.address} onChange={address => setForm({ ...form, address })} />
      {editable && <AddressMap address={form.address} latitude={form.address.latitude} longitude={form.address.longitude} onConfirm={(latitude, longitude) => setForm({ ...form, address: { ...form.address, latitude, longitude } })} />}
      {save.isError && <p role="alert" className="form-error">{save.error.message}</p>}{save.isSuccess && <p role="status">Correções salvas. Reenvie o cadastro para uma nova análise.</p>}
      {editable && <div className="button-row"><button className="secondary-button" disabled={save.isPending}>Salvar correções</button><button type="button" className="primary-button compact" disabled={save.isPending || resubmit.isPending} onClick={() => resubmit.mutate()}>Reenviar para análise</button></div>}
      {resubmit.isError && <p role="alert" className="form-error">{resubmit.error.message}</p>}
    </form></section>
    {profile.status === 'APPROVED' && <OperationalAddress profile={profile} />}
  </>
}

function AddressFields({ address, disabled, onChange }: { address: Address; disabled: boolean; onChange: (address: Address) => void }) {
  const fields = ['street', 'number', 'complement', 'neighborhood', 'city', 'state'] as const
  const labels = ['Rua', 'Número', 'Complemento', 'Bairro', 'Cidade', 'Estado (UF)']
  return <div className="form-grid">{fields.map((field, index) => <Field key={field} label={labels[index]}><input disabled={disabled} required={field !== 'complement'} maxLength={field === 'state' ? 2 : undefined} value={address[field] ?? ''} onChange={event => onChange({ ...address, [field]: event.target.value, ...(field === 'complement' ? {} : { latitude: null, longitude: null }) })} /></Field>)}<PostalCodeInput disabled={disabled} value={address.zipCode} onChange={zipCode => onChange({ ...address, zipCode, latitude: null, longitude: null })} onResolved={resolved => onChange({ ...address, ...resolved, latitude: null, longitude: null })} /></div>
}

function OperationalAddress({ profile }: { profile: DriverProfile }) {
  const client = useQueryClient()
  const [address, setAddress] = useState<Address>({ ...profile.operationalAddress })
  const save = useMutation({ mutationFn: () => setOperationalAddress({ ...address, state: address.state.toUpperCase() }), onSuccess: () => void client.invalidateQueries({ queryKey: ['driver-profile'] }) })
  return <section className="profile-card"><h2>Endereço de início da operação</h2><p className="muted">Informe e confirme no mapa o ponto de onde o veículo sairá.</p><form className="form-stack" onSubmit={event => { event.preventDefault(); save.mutate() }}><AddressFields disabled={false} address={address} onChange={setAddress} /><AddressMap address={address} latitude={address.latitude} longitude={address.longitude} onConfirm={(latitude, longitude) => setAddress({ ...address, latitude, longitude })} />{save.isError && <p role="alert" className="form-error">{save.error.message}</p>}{save.isSuccess && <p role="status">Ponto de saída salvo.</p>}<button className="primary-button compact" disabled={save.isPending || address.latitude == null || address.longitude == null}>{save.isPending ? 'Salvando…' : 'Salvar ponto de saída'}</button></form></section>
}

function Field({ label, children }: { label: string; children: React.ReactNode }) { return <label>{label}{children}</label> }

function addressPayload(address: Address, original: Address): Address {
  const changed = (['street', 'number', 'neighborhood', 'city', 'state', 'zipCode'] as const).some(field => address[field].trim() !== original[field].trim())
  return { ...address, state: address.state.toUpperCase(), latitude: changed ? null : original.latitude, longitude: changed ? null : original.longitude }
}

const statusLabel = { PENDING: 'Aguardando análise', APPROVED: 'Cadastro aprovado', REJECTED: 'Cadastro requer correção', SUSPENDED: 'Cadastro suspenso' }
const statusHelp = { PENDING: 'A administração ainda não analisou seus dados.', APPROVED: 'Sua conta está liberada para configurar a operação.', REJECTED: 'Corrija os dados indicados e reenvie o cadastro.', SUSPENDED: 'Entre em contato com a administração para regularizar a conta.' }
