import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { driverConfirmations, driverTrips } from './api'
import { confirmationLabel, directionLabel, time, type DailyConfirmation } from '../student/api'

export function DriverTodayPage() {
  const { session } = useAuth()
  const enabled = session?.driverStatus === 'APPROVED'
  const confirmations = useQuery({ queryKey: ['driver-confirmations', 'today'], queryFn: driverConfirmations, refetchInterval: 15_000, enabled })
  const trips = useQuery({ queryKey: ['trips', 'DRIVER', 'today'], queryFn: () => driverTrips(), refetchInterval: 15_000, enabled })
  if (!enabled) return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Cadastro do motorista</p><h1>Análise {session?.driverStatus?.toLowerCase() ?? 'pendente'}</h1><p className="muted">As ferramentas operacionais serão liberadas após a aprovação do seu cadastro.</p></header><section className="status-card"><div className="status-icon">i</div><div><h2>Aguardando liberação</h2><p className="muted">Você já pode entrar e acompanhar a situação da conta.</p></div></section></div>
  const responses = confirmations.data ?? []
  const groups = groupPreview(responses)
  const unanswered = responses.filter((item) => item.status === 'PENDING' || item.status === 'NO_RESPONSE').length
  return <div className="page-stack">{[confirmations.error, trips.error].filter(Boolean).map((error, index) => <p role="alert" className="form-error" key={index}>{error?.message}</p>)}<header className="page-heading"><p className="eyebrow">Sua operação hoje</p><h1>Visão do dia</h1><p className="muted">Veja a previsão do dia. A rota aparece após o encerramento das respostas.</p></header><div className="summary-grid"><Summary label="Confirmados" value={responses.filter((item) => item.status === 'YES').length} /><Summary label="Não vão" value={responses.filter((item) => item.status === 'NO').length} /><Summary label="Sem resposta" value={unanswered} /></div><section><div className="section-heading"><div><p className="section-kicker">Antes da rota</p><h2>Prévia das saídas</h2></div></div><div className="preview-list">{groups.map((group) => <article className="preview-card" key={group.key}><header><div><span className="direction-pill">{directionLabel(group.direction)}</span><h3>{group.scheduledTime.slice(0, 5)}</h3></div><span className="preview-count">{group.items.length} aluno{group.items.length === 1 ? '' : 's'}</span></header><p className="preview-destination">{unique(group.items.map(item => item.institutionName).filter(Boolean) as string[]).join(' · ') || 'Instituição a confirmar'}</p><div className="student-preview-list">{group.items.map(item => <div className="student-preview" key={item.id}><span className={`status-dot ${item.status.toLowerCase()}`} /><div><strong>{item.studentName}</strong><small>{confirmationLabel[item.status]}</small></div></div>)}</div><footer>Rota liberada após {time(group.items[0].responseDeadline)}</footer></article>)}{!confirmations.isLoading && !groups.length && <p className="empty-copy">Nenhuma saída prevista para hoje.</p>}</div></section><section><div className="section-heading"><h2>Rotas prontas</h2></div><div className="card-list">{trips.data?.map((trip) => <article className="trip-card" key={trip.id}><div><strong>{directionLabel(trip.direction)} às {time(trip.departureAt)}</strong><p className="muted">{trip.participants.length} aluno(s)</p></div><span className="trip-status">{trip.status.replaceAll('_', ' ')}</span></article>)}{!trips.isLoading && !trips.data?.length && <p className="empty-copy compact-empty">As rotas serão montadas quando o prazo das respostas terminar.</p>}</div></section></div>
}

function Summary({ label, value }: { label: string; value: number }) { return <article className="summary-card"><strong>{value}</strong><span>{label}</span></article> }

function groupPreview(items: DailyConfirmation[]) {
  const groups = new Map<string, DailyConfirmation[]>()
  items.forEach(item => {
    const key = `${item.direction}-${item.scheduledTime}`
    groups.set(key, [...(groups.get(key) ?? []), item])
  })
  return [...groups.entries()].map(([key, grouped]) => ({ key, direction: grouped[0].direction, scheduledTime: grouped[0].scheduledTime, items: grouped }))
}

function unique(values: string[]) { return [...new Set(values)] }
