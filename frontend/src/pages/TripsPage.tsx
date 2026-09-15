import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useAuth } from '../auth/AuthContext'
import { apiFetch } from '../lib/api'
import { studentTrips, time, today, type Trip } from '../features/student/api'
import { cancelTrip, tripAction } from '../features/driver/api'

export function TripsPage() {
  const { session } = useAuth()
  const client = useQueryClient()
  const trips = useQuery({ queryKey: ['trips', session?.role, 'today'], queryFn: () => session?.role === 'DRIVER' ? apiFetch<Trip[]>(`/api/drivers/me/trips?date=${today()}`) : studentTrips(), enabled: session?.role !== 'ADMIN' })
  if (session?.role === 'ADMIN') return <div className="page-stack"><header className="page-heading"><h1>Viagens</h1></header></div>
  const refresh = () => void client.invalidateQueries({ queryKey: ['trips'] })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Operação diária</p><h1>Viagens de hoje</h1><p className="muted">Horários, veículo e situação atual das viagens.</p></header><div className="card-list">{trips.isLoading && <p>Carregando…</p>}{trips.data?.map((trip) => <TripCard key={trip.id} trip={trip} driver={session?.role === 'DRIVER'} onChanged={refresh} />)}{!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy">Nenhuma viagem encontrada.</p>}</div></div>
}

function TripCard({ trip, driver, onChanged }: { trip: Trip; driver: boolean; onChanged: () => void }) {
  const action = useMutation({ mutationFn: (value: 'start' | 'completion' | 'replanning') => tripAction(trip.id, value), onSuccess: onChanged })
  const cancel = useMutation({ mutationFn: (reason: string) => cancelTrip(trip.id, reason), onSuccess: onChanged })
  function requestCancellation() { const reason = window.prompt('Informe o motivo do cancelamento'); if (reason?.trim()) cancel.mutate(reason.trim()) }
  return <article className="trip-card large"><div><span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>Saída {time(trip.departureAt)}</h3><p className="muted">{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo a definir'}</p>{trip.planningIssue && <p className="form-error">{trip.planningIssue}</p>}<p className="muted">{trip.participants.length} aluno(s)</p></div><div className="trip-actions"><strong className="trip-status">{trip.status.replaceAll('_', ' ')}</strong>{driver && trip.status === 'PLANNED' && <button className="primary-button compact" onClick={() => action.mutate('start')}>Iniciar</button>}{driver && trip.status === 'IN_PROGRESS' && <button className="primary-button compact" onClick={() => action.mutate('completion')}>Concluir</button>}{driver && trip.status === 'NEEDS_ATTENTION' && <button className="primary-button compact" onClick={() => action.mutate('replanning')}>Recalcular</button>}{driver && ['PLANNED', 'NEEDS_ATTENTION'].includes(trip.status) && <button className="secondary-button" onClick={requestCancellation}>Cancelar</button>}</div></article>
}
