import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { directionLabel, time, today } from '../student/api'
import { driverConfirmations, driverRoutePreviews, driverTrips, type RoutePreview } from './api'
import { RouteOperationModal } from './RouteOperationModal'

export function DriverTodayPage() {
  const { session } = useAuth()
  const navigate = useNavigate()
  const enabled = session?.driverStatus === 'APPROVED'
  const confirmations = useQuery({ queryKey: ['driver-confirmations', 'today'], queryFn: driverConfirmations, refetchInterval: 15_000, enabled })
  const previews = useQuery({ queryKey: ['driver-route-previews', today()], queryFn: () => driverRoutePreviews(), refetchInterval: 15_000, enabled })
  const trips = useQuery({ queryKey: ['trips', 'DRIVER', 'today'], queryFn: () => driverTrips(), refetchInterval: 15_000, enabled })
  const [selected, setSelected] = useState<RoutePreview | null>(null)

  if (!enabled) return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Cadastro do motorista</p><h1>Análise {session?.driverStatus?.toLowerCase() ?? 'pendente'}</h1><p className="muted">As ferramentas operacionais serão liberadas após a aprovação do seu cadastro.</p></header><section className="status-card"><div className="status-icon">i</div><div><h2>Aguardando liberação</h2><p className="muted">Você já pode entrar e acompanhar a situação da conta.</p></div></section></div>

  const responses = confirmations.data ?? []
  const unanswered = responses.filter(item => item.status === 'PENDING' || item.status === 'NO_RESPONSE').length
  return <div className="page-stack">
    {[confirmations.error, previews.error, trips.error].filter(Boolean).map((error, index) => <p role="alert" className="form-error" key={index}>{error?.message}</p>)}
    <header className="page-heading"><p className="eyebrow">Sua operação hoje</p><h1>Visão do dia</h1><p className="muted">Abra uma saída para conferir o trajeto real e iniciar. A saída fixa continua sendo a referência da operação.</p></header>
    <div className="summary-grid"><Summary label="Confirmados" value={responses.filter(item => item.status === 'YES').length} /><Summary label="Não vão" value={responses.filter(item => item.status === 'NO').length} /><Summary label="Sem resposta" value={unanswered} /></div>
    <section><div className="section-heading"><div><p className="section-kicker">Saídas de hoje</p><h2>Prévia das rotas</h2></div></div>
      <div className="preview-list">{previews.data?.map(item => <button type="button" className="preview-card preview-card-button" key={`${item.routeId}-${item.direction}-${item.departureTime}`} onClick={() => setSelected(item)} aria-label={`Abrir prévia de ${item.routeName}, ${directionLabel(item.direction)} às ${item.departureTime.slice(0, 5)}`}>
        <header><div><span className="direction-pill">{directionLabel(item.direction)}</span><h3>{item.departureTime.slice(0, 5)}</h3></div><span className="preview-count">{item.passengers.length} aluno{item.passengers.length === 1 ? '' : 's'}</span></header>
        <p className="preview-destination">{item.routeName}</p>
        <div className="student-preview-list">{item.passengers.map(person => <div className="student-preview" key={person.studentId}><span className="status-dot yes" /><div><strong>{person.studentName}</strong><small>{person.institutionName ?? 'Instituição'}</small></div></div>)}{item.passengers.length === 0 && <p className="preview-empty">Nenhum aluno confirmado até agora.</p>}</div>
        <footer><span>{item.stops.map(stop => stop.institutionName).join(' · ') || 'Instituições da rota'}</span><strong>Abrir prévia ›</strong></footer>
      </button>)}{!previews.isLoading && !previews.data?.length && <p className="empty-copy">Nenhuma saída fixa cadastrada para hoje.</p>}</div>
    </section>
    <section><div className="section-heading"><h2>Viagens em andamento e concluídas</h2></div><div className="card-list">{trips.data?.map(trip => <button type="button" className="trip-card trip-card-button" key={trip.id} onClick={() => trip.status === 'IN_PROGRESS' && navigate(`/viagens/${trip.id}/ativa`)} disabled={trip.status !== 'IN_PROGRESS'}><div><strong>{directionLabel(trip.direction)} às {time(trip.plannedDepartureAt ?? trip.departureAt)}</strong><p className="muted">{trip.participants.length} aluno(s)</p></div><span className="trip-status">{trip.status.replaceAll('_', ' ')}</span></button>)}{!trips.isLoading && !trips.data?.length && <p className="empty-copy compact-empty">Abra uma saída acima para visualizar e iniciar a rota.</p>}</div></section>
    {selected && <RouteOperationModal routeId={selected.routeId} routeName={selected.routeName} serviceDate={selected.serviceDate} direction={selected.direction} onClose={() => setSelected(null)} />}
  </div>
}

function Summary({ label, value }: { label: string; value: number }) {
  return <article className="summary-card"><strong>{value}</strong><span>{label}</span></article>
}
