import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'

export function LoginPage() {
  const { login, session } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [error, setError] = useState('')
  const [submitting, setSubmitting] = useState(false)

  if (session) return <Navigate to="/" replace />

  async function submit(event: FormEvent) {
    event.preventDefault()
    setError('')
    setSubmitting(true)
    try {
      await login({ email, password })
      const destination = (location.state as { from?: { pathname?: string } } | null)
        ?.from?.pathname ?? '/'
      navigate(destination, { replace: true })
    } catch (requestError) {
      setError(requestError instanceof Error ? requestError.message : 'Não foi possível entrar.')
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <main className="auth-page">
      <section className="auth-card" aria-labelledby="login-title">
        <img className="brand-mark" src="/transmoovi-app-icon-512.png" alt="" />
        <p className="eyebrow">transmoovi</p>
        <h1 id="login-title">Entre na sua conta</h1>
        <p className="muted">Acompanhe sua rotina de transporte em um só lugar.</p>
        <form onSubmit={submit} className="form-stack">
          <label>E-mail<input type="email" autoComplete="email" value={email} onChange={(event) => setEmail(event.target.value)} required /></label>
          <label>Senha<input type="password" autoComplete="current-password" value={password} onChange={(event) => setPassword(event.target.value)} required /></label>
          {error && <p className="form-error" role="alert">{error}</p>}
          <button className="primary-button" disabled={submitting}>{submitting ? 'Entrando…' : 'Entrar'}</button>
        </form>
        <p className="auth-footer">Ainda não tem uma conta? <Link to="/cadastro">Cadastre-se</Link></p>
      </section>
    </main>
  )
}
