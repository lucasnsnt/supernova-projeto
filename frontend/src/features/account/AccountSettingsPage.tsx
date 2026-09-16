import { useState, type FormEvent } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { PasswordRequirements } from '../../auth/PasswordRequirements'
import { accountDetails, confirmEmailCode, requestEmailCode, updateAccountProfile, updateEmail, updatePassword } from './api'

export function AccountSettingsPage() {
  const account = useQuery({ queryKey: ['account-settings'], queryFn: accountDetails })
  if (account.isLoading) return <p>Carregando…</p>
  if (account.isError) return <p role="alert" className="form-error">{account.error.message}</p>
  if (!account.data) return null
  return <AccountForms initial={account.data} />
}

function AccountForms({ initial }: { initial: Awaited<ReturnType<typeof accountDetails>> }) {
  const client = useQueryClient()
  const [name, setName] = useState(initial.name)
  const [phone, setPhone] = useState(initial.phone ?? '')
  const [email, setEmail] = useState(initial.email)
  const [code, setCode] = useState('')
  const [emailStep, setEmailStep] = useState<'request' | 'confirm'>('request')
  const [currentPassword, setCurrentPassword] = useState('')
  const [newPassword, setNewPassword] = useState('')

  const profile = useMutation({ mutationFn: () => updateAccountProfile(name, phone), onSuccess: data => client.setQueryData(['account-settings'], data) })
  const emailChange = useMutation({ mutationFn: async () => {
    const authorization = await confirmEmailCode(email, code)
    return updateEmail(email, authorization.registrationToken)
  }, onSuccess: data => { client.setQueryData(['account-settings'], data); setEmailStep('request'); setCode('') } })
  const emailRequest = useMutation({ mutationFn: () => requestEmailCode(email), onSuccess: () => setEmailStep('confirm') })
  const password = useMutation({ mutationFn: () => updatePassword(currentPassword, newPassword), onSuccess: () => { setCurrentPassword(''); setNewPassword('') } })

  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Conta</p><h1>Minha conta</h1><p className="muted">Atualize os dados de acesso da administração.</p></header>
    <SettingsForm title="Dados pessoais" submit={event => { event.preventDefault(); profile.mutate() }} pending={profile.isPending} error={profile.error} success={profile.isSuccess} successText="Dados atualizados."><label>Nome<input value={name} onChange={event => setName(event.target.value)} required /></label><label>Telefone<input value={phone} onChange={event => setPhone(event.target.value)} required /></label></SettingsForm>
    <SettingsForm title="E-mail" submit={event => { event.preventDefault(); if (emailStep === 'request') emailRequest.mutate(); else emailChange.mutate() }} pending={emailRequest.isPending || emailChange.isPending} error={emailRequest.error ?? emailChange.error} success={emailChange.isSuccess} successText="E-mail atualizado. Entre novamente para renovar os dados da sessão."><label>Novo e-mail<input type="email" value={email} onChange={event => { setEmail(event.target.value); setEmailStep('request') }} required /></label>{emailStep === 'confirm' && <label>Código recebido<input inputMode="numeric" pattern="\d{6}" maxLength={6} value={code} onChange={event => setCode(event.target.value.replace(/\D/g, ''))} required /></label>}<button className="primary-button compact" disabled={emailRequest.isPending || emailChange.isPending}>{emailStep === 'request' ? 'Enviar código' : 'Confirmar novo e-mail'}</button></SettingsForm>
    <SettingsForm title="Senha" submit={event => { event.preventDefault(); password.mutate() }} pending={password.isPending} error={password.error} success={password.isSuccess} successText="Senha atualizada."><label>Senha atual<input type="password" autoComplete="current-password" value={currentPassword} onChange={event => setCurrentPassword(event.target.value)} required /></label><label>Nova senha<input type="password" autoComplete="new-password" minLength={8} maxLength={64} value={newPassword} onChange={event => setNewPassword(event.target.value)} required /></label><PasswordRequirements id="new-password-requirements" password={newPassword} /></SettingsForm>
  </div>
}

function SettingsForm({ title, submit, pending, error, success, successText, children }: { title: string; submit: (event: FormEvent) => void; pending: boolean; error: Error | null; success: boolean; successText: string; children: React.ReactNode }) {
  return <section className="profile-card"><h2>{title}</h2><form className="form-stack" onSubmit={submit}>{children}{error && <p role="alert" className="form-error">{error.message}</p>}{success && <p role="status">{successText}</p>}{title !== 'E-mail' && <button className="primary-button compact" disabled={pending}>{pending ? 'Salvando…' : 'Salvar'}</button>}</form></section>
}
