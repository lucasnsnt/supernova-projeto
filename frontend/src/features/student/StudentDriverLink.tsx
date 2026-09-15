import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { acceptInvite, endLink, previewInvite } from './profile-api'
import { useStudentRegistration } from './StudentReadiness'

export function StudentDriverLink() {
  const client = useQueryClient()
  const { links } = useStudentRegistration()
  const [token, setToken] = useState('')
  const [ending, setEnding] = useState<number | null>(null)
  const [message, setMessage] = useState('')
  const refresh = () => client.invalidateQueries({ queryKey: ['student-registration'] })
  const preview = useMutation({ mutationFn: (value: string) => previewInvite(value).then(invite => ({ invite, token: value })) })
  const accept = useMutation({ mutationFn: acceptInvite, onSuccess: async () => { setToken(''); preview.reset(); setMessage('Vínculo com motorista confirmado.'); await refresh() } })
  const end = useMutation({ mutationFn: endLink, onSuccess: async () => { setEnding(null); setMessage('Vínculo encerrado.'); await refresh() } })
  const active = links.data?.find(link => link.status === 'ACTIVE')
  const busy = preview.isPending || accept.isPending || end.isPending
  return <section className="profile-card"><h2>Seu motorista</h2>
    {links.isPending && <p role="status">Carregando vínculos…</p>}
    {links.isError && <p role="alert">Não foi possível carregar os vínculos. <button onClick={() => void links.refetch()}>Tentar novamente</button></p>}
    {active && <><p>Motorista atual: <strong>{active.driverName}</strong></p><p className="muted">Desde {formatDate(active.startDate)}</p>
      {ending === active.id ? <div role="group" aria-label="Confirmar encerramento"><p>Deseja encerrar o vínculo com {active.driverName}? Você deixará de receber novas confirmações deste motorista.</p><div className="button-row"><button className="secondary-button" disabled={busy} onClick={() => setEnding(null)}>Manter vínculo</button><button className="primary-button compact" disabled={busy} onClick={() => end.mutate(active.id)}>Confirmar encerramento</button></div></div> : <button className="secondary-button" disabled={busy} onClick={() => { end.reset(); setMessage(''); setEnding(active.id) }}>Encerrar vínculo</button>}
    </>}
    {links.isSuccess && !active && <form className="form-stack" onSubmit={event => { event.preventDefault(); setMessage(''); accept.reset(); preview.mutate(token.trim()) }}>
      <p className="muted">Peça o código de convite ao motorista e confira os dados antes de confirmar.</p>
      <label>Código de convite<input required value={token} disabled={busy} onChange={event => { setToken(event.target.value); preview.reset(); accept.reset(); setMessage('') }} /></label>
      <button className="secondary-button" disabled={!token.trim() || busy}>{preview.isPending ? 'Conferindo…' : 'Conferir convite'}</button>
      {preview.data && preview.data.token === token.trim() && <div><p>Convite de <strong>{preview.data.invite.driverName}</strong></p><p className="muted">Válido até {new Date(preview.data.invite.expiresAt).toLocaleString('pt-BR')}</p><button type="button" className="primary-button compact" disabled={busy} onClick={() => accept.mutate(preview.data!.token)}>Confirmar vínculo</button></div>}
    </form>}
    {[preview, accept, end].map((mutation, index) => mutation.isError && <p key={index} className="form-error" role="alert">{mutation.error.message}</p>)}
    {message && <p role="status">{message}</p>}
    {links.data && links.data.length > 0 && <><h3>Histórico de vínculos</h3><ul className="readiness-list">{links.data.map(link => <li key={link.id}>{link.driverName} · {({ ACTIVE: 'Ativo', ENDED: 'Encerrado', PENDING: 'Pendente', REJECTED: 'Recusado' })[link.status]} · {formatDate(link.startDate)}{link.endDate && ` até ${formatDate(link.endDate)}`}</li>)}</ul></>}
  </section>
}

function formatDate(value: string) { return new Date(`${value}T12:00:00`).toLocaleDateString('pt-BR') }
