import { useState, type FormEvent } from 'react'
import { Link, Navigate, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { RegisterPayload } from '../auth/types'
import { apiFetch } from '../lib/api'

type Step = 'email' | 'code' | 'profile'
type Authorization = { registrationToken: string; expiresAt: string }

const emptyProfile = {
  name: '', password: '', phone: '', dateOfBirth: '', role: 'STUDENT', cnh: '',
  driverInviteToken: '', street: '', number: '', complement: '', neighborhood: '',
  city: '', state: '', zipCode: '',
} satisfies Record<string, string>

export function RegisterPage() {
  const { register, session } = useAuth()
  const navigate = useNavigate()
  const [step, setStep] = useState<Step>('email')
  const [email, setEmail] = useState('')
  const [code, setCode] = useState('')
  const [registrationToken, setRegistrationToken] = useState('')
  const [profile, setProfile] = useState(emptyProfile)
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (session) return <Navigate to="/" replace />

  async function requestCode(event: FormEvent) {
    event.preventDefault()
    await perform(async () => {
      await apiFetch<void>('/api/auth/email-verification', {
        method: 'POST', body: JSON.stringify({ email }),
      })
      setStep('code')
    })
  }

  async function confirmCode(event: FormEvent) {
    event.preventDefault()
    await perform(async () => {
      const authorization = await apiFetch<Authorization>('/api/auth/email-verification/confirm', {
        method: 'POST', body: JSON.stringify({ email, code }),
      })
      setRegistrationToken(authorization.registrationToken)
      setStep('profile')
    })
  }

  async function createAccount(event: FormEvent) {
    event.preventDefault()
    await perform(async () => {
      const payload: RegisterPayload = {
        registrationToken, email, name: profile.name, password: profile.password,
        phone: profile.phone, dateOfBirth: profile.dateOfBirth,
        role: profile.role as RegisterPayload['role'],
        address: {
          street: profile.street, number: profile.number, complement: profile.complement || undefined,
          neighborhood: profile.neighborhood, city: profile.city,
          state: profile.state.toUpperCase(), zipCode: profile.zipCode,
        },
        cnh: profile.role === 'DRIVER' ? profile.cnh : undefined,
        driverInviteToken: profile.role === 'STUDENT' && profile.driverInviteToken
          ? profile.driverInviteToken : undefined,
      }
      await register(payload)
      navigate('/', { replace: true })
    })
  }

  async function perform(action: () => Promise<void>) {
    setError('')
    setSubmitting(true)
    try {
      await action()
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível continuar.')
    } finally {
      setSubmitting(false)
    }
  }

  function update(field: keyof typeof profile, value: string) {
    setProfile((current) => ({ ...current, [field]: value }))
  }

  return (
    <main className="auth-page registration-page">
      <section className="auth-card registration-card" aria-labelledby="register-title">
        <div className="brand-mark" aria-hidden="true">S</div>
        <p className="eyebrow">Etapa {step === 'email' ? '1' : step === 'code' ? '2' : '3'} de 3</p>
        <h1 id="register-title">{step === 'email' ? 'Vamos começar' : step === 'code' ? 'Confira seu e-mail' : 'Complete seu cadastro'}</h1>
        {step === 'email' && <form className="form-stack" onSubmit={requestCode}>
          <p className="muted">Enviaremos um código para confirmar que o e-mail é seu.</p>
          <Field label="E-mail"><input type="email" value={email} onChange={(event) => setEmail(event.target.value)} autoComplete="email" required /></Field>
          <SubmitButton loading={submitting}>Enviar código</SubmitButton>
        </form>}
        {step === 'code' && <form className="form-stack" onSubmit={confirmCode}>
          <p className="muted">Digite o código de 6 números enviado para <strong>{email}</strong>.</p>
          <Field label="Código"><input inputMode="numeric" pattern="\d{6}" maxLength={6} value={code} onChange={(event) => setCode(event.target.value.replace(/\D/g, ''))} required /></Field>
          <SubmitButton loading={submitting}>Confirmar código</SubmitButton>
          <button type="button" className="text-button" onClick={() => setStep('email')}>Alterar e-mail</button>
        </form>}
        {step === 'profile' && <form className="form-stack" onSubmit={createAccount}>
          <div className="role-choice">
            <button type="button" className={profile.role === 'STUDENT' ? 'selected' : ''} onClick={() => update('role', 'STUDENT')}>Sou aluno</button>
            <button type="button" className={profile.role === 'DRIVER' ? 'selected' : ''} onClick={() => update('role', 'DRIVER')}>Sou motorista</button>
          </div>
          <div className="form-grid">
            <Field label="Nome completo"><input value={profile.name} onChange={(event) => update('name', event.target.value)} autoComplete="name" required /></Field>
            <Field label="Telefone"><input value={profile.phone} onChange={(event) => update('phone', event.target.value)} autoComplete="tel" required /></Field>
            <Field label="Nascimento"><input type="date" value={profile.dateOfBirth} onChange={(event) => update('dateOfBirth', event.target.value)} required /></Field>
            <Field label="Senha"><input type="password" minLength={8} value={profile.password} onChange={(event) => update('password', event.target.value)} autoComplete="new-password" required /></Field>
          </div>
          <h2 className="form-section-title">Endereço</h2>
          <div className="form-grid">
            <Field label="Rua"><input value={profile.street} onChange={(event) => update('street', event.target.value)} required /></Field>
            <Field label="Número"><input value={profile.number} onChange={(event) => update('number', event.target.value)} required /></Field>
            <Field label="Complemento"><input value={profile.complement} onChange={(event) => update('complement', event.target.value)} /></Field>
            <Field label="Bairro"><input value={profile.neighborhood} onChange={(event) => update('neighborhood', event.target.value)} required /></Field>
            <Field label="Cidade"><input value={profile.city} onChange={(event) => update('city', event.target.value)} required /></Field>
            <Field label="Estado"><input maxLength={2} value={profile.state} onChange={(event) => update('state', event.target.value)} required /></Field>
            <Field label="CEP"><input value={profile.zipCode} onChange={(event) => update('zipCode', event.target.value)} required /></Field>
          </div>
          {profile.role === 'DRIVER'
            ? <Field label="CNH"><input value={profile.cnh} onChange={(event) => update('cnh', event.target.value)} required /></Field>
            : <Field label="Convite do motorista (opcional)"><input value={profile.driverInviteToken} onChange={(event) => update('driverInviteToken', event.target.value)} /></Field>}
          <p className="password-hint">A senha deve conter maiúscula, minúscula, número e caractere especial.</p>
          <SubmitButton loading={submitting}>Criar conta</SubmitButton>
        </form>}
        {error && <p className="form-error standalone" role="alert">{error}</p>}
        <p className="auth-footer">Já tem uma conta? <Link to="/login">Entrar</Link></p>
      </section>
    </main>
  )
}

function Field({ label, children }: { label: string; children: React.ReactNode }) {
  return <label>{label}{children}</label>
}

function SubmitButton({ loading, children }: { loading: boolean; children: React.ReactNode }) {
  return <button className="primary-button" disabled={loading}>{loading ? 'Aguarde…' : children}</button>
}
