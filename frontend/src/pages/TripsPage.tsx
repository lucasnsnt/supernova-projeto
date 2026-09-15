import { useQuery } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { apiFetch } from '../lib/api'
import { studentTrips, time, today, type Trip } from '../features/student/api'

export function TripsPage() {
  const { session } = useAuth()
  const trips = useQuery({ queryKey: ['trips', session?.role, 'today'], queryFn: () => session?.role === 'DRIVER' ? apiFetch<Trip[]>(`/api/drivers/me/trips?date=${today()}`) : studentTrips(), enabled: session?.role !== 'ADMIN' })
  if (session?.role === 'ADMIN') return <div className="page-stack"><header className="page-heading"><h1>Viagens</h1></header></div>
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Operação diária</p><h1>Viagens de hoje</h1><p className="muted">Horários, veículo e situação atual das viagens.</p></header><div className="card-list">{trips.isLoading && <p>Carregando…</p>}{trips.data?.map((trip) => <article className="trip-card large" key={trip.id}><div><span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>Saída {time(trip.departureAt)}</h3><p className="muted">{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo a definir'}</p>{trip.planningIssue && <p className="form-error">{trip.planningIssue}</p>}</div><strong className="trip-status">{trip.status.replaceAll('_', ' ')}</strong></article>)}{!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy">Nenhuma viagem encontrada.</p>}</div></div>
}
