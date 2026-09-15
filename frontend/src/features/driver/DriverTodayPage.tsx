import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../../auth/AuthContext'
import { driverConfirmations, driverTrips } from './api'
import { time } from '../student/api'

export function DriverTodayPage() {
  const { session } = useAuth()
  const enabled = session?.driverStatus === 'APPROVED'
  const confirmations = useQuery({ queryKey: ['driver-confirmations', 'today'], queryFn: driverConfirmations, refetchInterval: 15_000, enabled })
  const trips = useQuery({ queryKey: ['trips', 'DRIVER', 'today'], queryFn: () => driverTrips(), refetchInterval: 15_000, enabled })
  if (!enabled) return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Cadastro do motorista</p><h1>Análise {session?.driverStatus?.toLowerCase() ?? 'pendente'}</h1><p className="muted">As ferramentas operacionais serão liberadas após a aprovação do seu cadastro.</p></header><section className="status-card"><div className="status-icon">i</div><div><h2>Aguardando liberação</h2><p className="muted">Você já pode entrar e acompanhar a situação da conta.</p></div></section></div>
  const responses = confirmations.data ?? []
  return <div className="page-stack">{[confirmations.error, trips.error].filter(Boolean).map((error, index) => <p role="alert" className="form-error" key={index}>{error?.message}</p>)}<header className="page-heading"><p className="eyebrow">Sua operação hoje</p><h1>Visão do dia</h1><p className="muted">Acompanhe quem vai e as próximas saídas.</p></header><div className="summary-grid"><Summary label="Confirmados" value={responses.filter((item) => item.status === 'YES').length} /><Summary label="Não vão" value={responses.filter((item) => item.status === 'NO').length} /><Summary label="Aguardando" value={responses.filter((item) => item.status === 'PENDING').length} /></div><section><div className="section-heading"><h2>Respostas dos alunos</h2></div><div className="card-list">{responses.map((item) => <article className="trip-card" key={item.id}><div><strong>{item.studentName}</strong><p className="muted">{item.direction === 'IDA' ? 'Ida' : 'Volta'} · {item.scheduledTime.slice(0, 5)}</p></div><span className={`response ${item.status.toLowerCase()}`}>{item.status.replace('_', ' ')}</span></article>)}{!confirmations.isLoading && !responses.length && <p className="empty-copy">Nenhuma confirmação para hoje.</p>}</div></section><section><div className="section-heading"><h2>Próximas viagens</h2></div><div className="card-list">{trips.data?.map((trip) => <article className="trip-card" key={trip.id}><div><strong>{trip.direction === 'IDA' ? 'Ida' : 'Volta'} às {time(trip.departureAt)}</strong><p className="muted">{trip.participants.length} aluno(s)</p></div><span className="trip-status">{trip.status}</span></article>)}</div></section></div>
}

function Summary({ label, value }: { label: string; value: number }) { return <article className="summary-card"><strong>{value}</strong><span>{label}</span></article> }
