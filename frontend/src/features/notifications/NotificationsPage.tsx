import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { markAllRead, markNotificationRead, notifications } from './api'

export function NotificationsPage() {
  const client = useQueryClient()
  const list = useQuery({ queryKey: ['notifications'], queryFn: notifications, refetchInterval: 15_000 })
  const refresh = () => { void client.invalidateQueries({ queryKey: ['notifications'] }); void client.invalidateQueries({ queryKey: ['unread-notifications'] }) }
  const read = useMutation({ mutationFn: markNotificationRead, onSuccess: refresh })
  const readAll = useMutation({ mutationFn: markAllRead, onSuccess: refresh })
  return <div className="page-stack"><header className="page-heading row-heading"><div><p className="eyebrow">Atualizações</p><h1>Notificações</h1><p className="muted">Avisos e mudanças importantes da sua operação.</p></div><button className="secondary-button" onClick={() => readAll.mutate()} disabled={readAll.isPending}>Marcar todas como lidas</button></header><div className="card-list">{list.isLoading && <p>Carregando…</p>}{list.data?.map((item) => <button className={`notification-card ${item.readAt ? '' : 'unread'}`} key={item.id} onClick={() => !item.readAt && read.mutate(item.id)}><span className="notification-dot" /><span><strong>{item.title}</strong><span>{item.message}</span><small>{new Date(item.createdAt).toLocaleString('pt-BR')}</small></span></button>)}{!list.isLoading && !list.data?.length && <p className="empty-copy">Você ainda não possui notificações.</p>}</div></div>
}
