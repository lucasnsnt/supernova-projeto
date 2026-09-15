import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useAuth } from '../auth/AuthContext'
import { studentTrips, time, today, type Trip } from '../features/student/api'
import { cancelTrip, tripAction, driverTrips, vehicles, changeTripVehicle, updateDeparture } from '../features/driver/api'

export function TripsPage() {
  const { session } = useAuth()
  const client = useQueryClient()
  const [date, setDate] = useState(today)
  const driver = session?.role === 'DRIVER'
  const allowed = session?.role === 'STUDENT' || (driver && ['APPROVED', 'SUSPENDED'].includes(session?.driverStatus ?? ''))
  const trips = useQuery({
    queryKey: ['trips', session?.role, date],
    queryFn: () => driver ? driverTrips(date) : studentTrips(date),
    enabled: allowed, refetchInterval: 15_000,
  })
  const refresh = () => void client.invalidateQueries({ queryKey: ['trips'] })
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Operação diária</p><h1>Viagens</h1><p className="muted">Acompanhe horários, paradas e situação das viagens.</p></header>
    {!allowed ? <p>As viagens estarão disponíveis após a aprovação do motorista.</p> : <>
      <label>Data da viagem <input type="date" value={date} onChange={event => { if (event.target.value) setDate(event.target.value) }} /></label>
      <div className="card-list">
        {trips.isLoading && <p>Carregando…</p>}
        {trips.error && <p role="alert" className="form-error">{trips.error.message}</p>}
        {trips.data?.map(trip => <TripCard key={trip.id} trip={trip} driver={driver && session?.driverStatus === 'APPROVED'} onChanged={refresh} />)}
        {!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy">Nenhuma viagem encontrada.</p>}
      </div>
    </>}
  </div>
}

function TripCard({ trip, driver, onChanged }: { trip: Trip; driver: boolean; onChanged: () => void }) {
  const action = useMutation({ mutationFn: (value: 'start' | 'completion' | 'replanning') => tripAction(trip.id, value), onSuccess: onChanged })
  const cancel = useMutation({ mutationFn: (reason: string) => cancelTrip(trip.id, reason), onSuccess: onChanged })
  const busy = action.isPending || cancel.isPending
  const beforeStart = ['PLANNED', 'NEEDS_ATTENTION'].includes(trip.status)
  function requestCancellation() { const reason = window.prompt('Informe o motivo do cancelamento'); if (reason?.trim()) cancel.mutate(reason.trim()) }
  return <article className="trip-card large">
    <div>
      <span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>Saída {time(trip.departureAt)}</h3>
      <p className="muted">{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo a definir'}</p>
      {trip.planningIssue && <p className="form-error">{trip.planningIssue}</p>}
      {trip.cancellationReason && <p>Cancelamento: {trip.cancellationReason}</p>}
      {trip.startedAt && <p>Iniciada às {time(trip.startedAt)}</p>}
      {trip.completedAt && <p>Concluída às {time(trip.completedAt)}</p>}
      <ol>{[...trip.participants].sort((a, b) => a.pickupOrder - b.pickupOrder).map((participant, index) => <li key={participant.studentId ?? index}>
        <strong>{participant.studentName}</strong> · {participant.institutionName}
        <p className="muted">Embarque {time(participant.estimatedPickupAt)} · Desembarque {time(participant.estimatedDropoffAt)}</p>
        <p>Parada {participant.pickupOrder} — Embarque: {formatAddress(participant.pickupAddress)}</p>
        <p>Parada {participant.dropoffOrder} — Desembarque: {formatAddress(participant.dropoffAddress)}</p>
      </li>)}</ol>
      {driver && beforeStart && <TripConfiguration trip={trip} onChanged={onChanged} />}
      {(action.error || cancel.error) && <p role="alert" className="form-error">{(action.error ?? cancel.error)?.message}</p>}
    </div>
    <div className="trip-actions"><strong className="trip-status">{statuses[trip.status] ?? trip.status}</strong>
      {driver && trip.status === 'PLANNED' && <button disabled={busy} className="primary-button compact" onClick={() => action.mutate('start')}>Iniciar</button>}
      {driver && trip.status === 'IN_PROGRESS' && <button disabled={busy} className="primary-button compact" onClick={() => action.mutate('completion')}>Concluir</button>}
      {driver && trip.status === 'NEEDS_ATTENTION' && <button disabled={busy} className="primary-button compact" onClick={() => action.mutate('replanning')}>Recalcular</button>}
      {driver && beforeStart && <button disabled={busy} className="secondary-button" onClick={requestCancellation}>Cancelar</button>}
    </div>
  </article>
}

function TripConfiguration({ trip, onChanged }: { trip: Trip; onChanged: () => void }) {
  const list = useQuery({ queryKey: ['driver-vehicles'], queryFn: vehicles })
  const [vehicleId, setVehicleId] = useState('')
  const [departureAt, setDepartureAt] = useState('')
  const [reason, setReason] = useState('')
  const vehicle = useMutation({ mutationFn: () => changeTripVehicle(trip.id, Number(vehicleId)), onSuccess: onChanged })
  const departure = useMutation({ mutationFn: () => updateDeparture(trip.id, departureAt, reason.trim() || null), onSuccess: onChanged })
  function submitDeparture(event: FormEvent) { event.preventDefault(); departure.mutate() }
  return <details><summary>Ajustar veículo ou saída</summary>
    <form className="inline-form" onSubmit={event => { event.preventDefault(); vehicle.mutate() }}>
      <label>Veículo da viagem<select required value={vehicleId} onChange={event => setVehicleId(event.target.value)}><option value="">Selecione</option>{list.data?.map(item => <option key={item.id} value={item.id}>{item.model} · {item.licensePlate} · {item.passengerCapacity} lugares</option>)}</select></label>
      <button disabled={vehicle.isPending || !vehicleId} className="secondary-button">Salvar veículo</button>
    </form>
    <form className="inline-form" onSubmit={submitDeparture}>
      <label>Nova saída<input type="datetime-local" required value={departureAt} onChange={event => setDepartureAt(event.target.value)} /></label>
      <label>Motivo do atraso<input value={reason} onChange={event => setReason(event.target.value)} /></label>
      <button disabled={departure.isPending} className="secondary-button">Salvar saída</button>
    </form>
    {[list.error, vehicle.error, departure.error].filter(Boolean).map((error, index) => <p key={index} role="alert" className="form-error">{error?.message}</p>)}
  </details>
}
const statuses: Record<string, string> = { PLANNED: 'Planejada', NEEDS_ATTENTION: 'Precisa de atenção', IN_PROGRESS: 'Em andamento', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }

function formatAddress(address: Trip['participants'][number]['pickupAddress']) {
  return address ? `${address.street}, ${address.number}${address.complement ? ` · ${address.complement}` : ''} · ${address.neighborhood} · ${address.city}/${address.state}` : 'Endereço indisponível'
}
