import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { approveDriver, drivers, rejectDriver } from './api'

export function DriversPage() {
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['admin-drivers'], queryFn: drivers })
  const refresh = () => void client.invalidateQueries({ queryKey: ['admin-drivers'] })
  const approve = useMutation({ mutationFn: approveDriver, onSuccess: refresh })
  const reject = useMutation({ mutationFn: ({ id, reason }: { id: number; reason: string }) => rejectDriver(id, reason), onSuccess: refresh })
  function requestRejection(id: number) { const reason = window.prompt('Informe o motivo da rejeição'); if (reason?.trim()) reject.mutate({ id, reason: reason.trim() }) }
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Administração</p><h1>Motoristas</h1><p className="muted">Analise e libere os motoristas que utilizarão a plataforma.</p></header>{[list.error, approve.error, reject.error].filter(Boolean).map((error, index) => <p role="alert" className="form-error" key={index}>{error?.message}</p>)}<div className="card-list">{list.isLoading && <p>Carregando…</p>}{list.data?.map((driver) => <article className="trip-card large" key={driver.id}><div><span className="tag neutral">{driver.status}</span><h3>{driver.name}</h3><p className="muted">{driver.email} · CNH {driver.cnh}</p>{driver.statusReason && <p className="form-error">{driver.statusReason}</p>}</div>{driver.status === 'PENDING' && <div className="button-row"><button className="secondary-button" disabled={approve.isPending || reject.isPending} onClick={() => requestRejection(driver.id)}>Rejeitar</button><button className="primary-button compact" disabled={approve.isPending || reject.isPending} onClick={() => approve.mutate(driver.id)}>Aprovar</button></div>}</article>)}{!list.isLoading && !list.data?.length && <p className="empty-copy">Nenhum motorista cadastrado.</p>}</div></div>
}
