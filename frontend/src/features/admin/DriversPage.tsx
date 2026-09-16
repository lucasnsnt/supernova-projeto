import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { approveDriver, drivers, reactivateDriver, rejectDriver, suspendDriver, type Driver } from './api'

export function DriversPage() {
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['admin-drivers'], queryFn: drivers })
  const refresh = () => void client.invalidateQueries({ queryKey: ['admin-drivers'] })
  const approve = useMutation({ mutationFn: approveDriver, onSuccess: refresh })
  const reject = useMutation({ mutationFn: ({ id, reason }: { id: number; reason: string }) => rejectDriver(id, reason), onSuccess: refresh })
  const suspend = useMutation({ mutationFn: ({ id, reason }: { id: number; reason: string }) => suspendDriver(id, reason), onSuccess: refresh })
  const reactivate = useMutation({ mutationFn: reactivateDriver, onSuccess: refresh })
  function requestRejection(id: number) { const reason = window.prompt('Informe o motivo da rejeição'); if (reason?.trim()) reject.mutate({ id, reason: reason.trim() }) }
  function requestSuspension(id: number) { const reason = window.prompt('Informe o motivo da suspensão'); if (reason?.trim()) suspend.mutate({ id, reason: reason.trim() }) }
  const busy = approve.isPending || reject.isPending || suspend.isPending || reactivate.isPending
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Administração</p><h1>Motoristas</h1><p className="muted">Analise os dados cadastrais e controle a liberação da operação.</p></header>{[list.error, approve.error, reject.error, suspend.error, reactivate.error].filter(Boolean).map((error, index) => <p role="alert" className="form-error" key={index}>{error?.message}</p>)}<div className="card-list">{list.isLoading && <p>Carregando…</p>}{list.data?.map((driver) => <DriverCard key={driver.id} driver={driver} busy={busy} approve={() => approve.mutate(driver.id)} reject={() => requestRejection(driver.id)} suspend={() => requestSuspension(driver.id)} reactivate={() => reactivate.mutate(driver.id)} />)}{!list.isLoading && !list.data?.length && <p className="empty-copy">Nenhum motorista cadastrado.</p>}</div></div>
}

function DriverCard({ driver, busy, approve, reject, suspend, reactivate }: { driver: Driver; busy: boolean; approve: () => void; reject: () => void; suspend: () => void; reactivate: () => void }) {
  const address = driver.address
  return <article className="trip-card large"><div><span className="tag neutral">{statusLabel[driver.status]}</span><h3>{driver.name}</h3><p className="muted">{driver.email} · {driver.phone}</p><p>CNH <strong>{driver.cnh}</strong> · nascimento {new Intl.DateTimeFormat('pt-BR', { timeZone: 'UTC' }).format(new Date(`${driver.dateOfBirth}T00:00:00Z`))}</p><p>{address.street}, {address.number}{address.complement ? `, ${address.complement}` : ''} · {address.neighborhood}, {address.city}/{address.state} · CEP {address.zipCode}</p>{driver.statusReason && <p className="form-error">Motivo: {driver.statusReason}</p>}{driver.reviewedAt && <p className="muted">Última análise: {new Intl.DateTimeFormat('pt-BR', { dateStyle: 'short', timeStyle: 'short' }).format(new Date(driver.reviewedAt))}</p>}</div><div className="button-row">{driver.status === 'PENDING' && <><button className="secondary-button" disabled={busy} onClick={reject}>Rejeitar</button><button className="primary-button compact" disabled={busy} onClick={approve}>Aprovar</button></>}{driver.status === 'APPROVED' && <button className="secondary-button" disabled={busy} onClick={suspend}>Suspender</button>}{driver.status === 'SUSPENDED' && <button className="primary-button compact" disabled={busy} onClick={reactivate}>Reativar</button>}</div></article>
}

const statusLabel = { PENDING: 'Pendente', APPROVED: 'Aprovado', REJECTED: 'Rejeitado', SUSPENDED: 'Suspenso' }
