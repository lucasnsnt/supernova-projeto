import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { ConfirmDialog } from '../components/ConfirmDialog'
import { parseBahiaDateTime, studentTrips, time, today, type Trip } from '../features/student/api'
import { cancelTrip, tripAction, driverOperationalRoutePreview, driverTrips, vehicles, changeTripVehicle } from '../features/driver/api'

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
      <DateNavigator value={date} onChange={setDate} />
      <div className="card-list">
        {trips.isLoading && <p>Carregando…</p>}
        {trips.error && <p role="alert" className="form-error">{trips.error.message}</p>}
        {trips.data?.map(trip => <TripCard key={trip.id} trip={trip} driver={driver && session?.driverStatus === 'APPROVED'} onChanged={refresh} />)}
        {!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy">Nenhuma viagem encontrada.</p>}
      </div>
    </>}
  </div>
}

function DateNavigator({ value, onChange }: { value: string; onChange: (date: string) => void }) {
  const current = parseDate(value)
  const currentToday = today()
  const move = (days: number) => {
    const next = new Date(current)
    next.setUTCDate(next.getUTCDate() + days)
    onChange(next.toISOString().slice(0, 10))
  }
  const label = value === currentToday ? 'Hoje' : new Intl.DateTimeFormat('pt-BR', { weekday: 'long' }).format(current)
  const fullDate = new Intl.DateTimeFormat('pt-BR', { day: '2-digit', month: 'long', year: 'numeric', timeZone: 'UTC' }).format(current)
  return <section className="date-navigator" aria-label="Escolher data da viagem">
    <button type="button" className="date-arrow" aria-label="Dia anterior" onClick={() => move(-1)}>‹</button>
    <div className="date-current"><span>{label}</span><strong>{fullDate}</strong></div>
    <button type="button" className="date-arrow" aria-label="Próximo dia" onClick={() => move(1)}>›</button>
    {value !== currentToday && <button type="button" className="today-button" onClick={() => onChange(currentToday)}>Voltar para hoje</button>}
  </section>
}

function parseDate(value: string) { return new Date(`${value}T12:00:00Z`) }

function TripCard({ trip, driver, onChanged }: { trip: Trip; driver: boolean; onChanged: () => void }) {
  const navigate = useNavigate()
  const [confirmOutsideWindow, setConfirmOutsideWindow] = useState(false)
  const [outsideLabel, setOutsideLabel] = useState('Iniciar agora')
  const action = useMutation({ mutationFn: ({ value, acknowledgeOutsideWindow = false }: { value: 'start' | 'completion' | 'replanning'; acknowledgeOutsideWindow?: boolean }) => tripAction(trip.id, value, value === 'start' ? { acknowledgeOutsideWindow } : undefined), onSuccess: (_, variables) => { onChanged(); if (variables.value === 'start') navigate(`/viagens/${trip.id}/ativa`) } })
  const checkStart = useMutation({ mutationFn: async () => {
    if (trip.routeId == null) throw new Error('Esta viagem não está vinculada a uma rota recorrente. Atualize a página antes de iniciar.')
    const preview = await driverOperationalRoutePreview(trip.routeId, trip.serviceDate, trip.direction)
    if (!preview.canStart || preview.planningIssue || preview.participants.length === 0) throw new Error(preview.planningIssue ?? 'Nenhum aluno confirmou presença nesta saída.')
    return preview
  }, onSuccess: preview => {
    if (preview.withinStartWindow) action.mutate({ value: 'start' })
    else {
      setOutsideLabel(parseBahiaDateTime(preview.scheduledDepartureAt).getTime() > Date.now() ? 'Iniciar antecipadamente' : 'Iniciar agora')
      setConfirmOutsideWindow(true)
    }
  } })
  const cancel = useMutation({ mutationFn: (reason: string) => cancelTrip(trip.id, reason), onSuccess: onChanged })
  const busy = action.isPending || checkStart.isPending || cancel.isPending
  const beforeStart = ['PLANNED', 'NEEDS_ATTENTION'].includes(trip.status)
  function requestCancellation() { const reason = window.prompt('Informe o motivo do cancelamento'); if (reason?.trim()) cancel.mutate(reason.trim()) }
  return <article className="trip-card large">
    <div>
      <span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>Saída prevista {time(trip.plannedDepartureAt ?? trip.departureAt)}</h3>
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
      {(action.error || checkStart.error || cancel.error) && <p role="alert" className="form-error">{(action.error ?? checkStart.error ?? cancel.error)?.message}</p>}
    </div>
    <div className="trip-actions"><strong className="trip-status">{statuses[trip.status] ?? trip.status}</strong>
      {driver && trip.status === 'PLANNED' && <button disabled={busy} className="primary-button compact" onClick={() => checkStart.mutate()}>{checkStart.isPending ? 'Conferindo prévia…' : 'Iniciar viagem'}</button>}
      {trip.status === 'IN_PROGRESS' && <button disabled={busy} className="primary-button compact" onClick={() => navigate(`/viagens/${trip.id}/ativa`)}>{driver ? 'Abrir viagem' : 'Acompanhar'}</button>}
      {driver && trip.status === 'NEEDS_ATTENTION' && <button disabled={busy} className="primary-button compact" onClick={() => action.mutate({ value: 'replanning' })}>Recalcular</button>}
      {driver && beforeStart && <button disabled={busy} className="secondary-button" onClick={requestCancellation}>Cancelar</button>}
    </div>
    <ConfirmDialog open={confirmOutsideWindow} title={outsideLabel === 'Iniciar antecipadamente' ? 'Fora do horário previsto' : 'Horário previsto já passou'} message={outsideLabel === 'Iniciar antecipadamente' ? `A saída está prevista para ${time(trip.plannedDepartureAt ?? trip.departureAt)}. A viagem começará agora.` : `A saída estava prevista para ${time(trip.plannedDepartureAt ?? trip.departureAt)}. Confirme para iniciar agora.`} confirmLabel={outsideLabel} busy={action.isPending} onCancel={() => setConfirmOutsideWindow(false)} onConfirm={() => action.mutate({ value: 'start', acknowledgeOutsideWindow: true })} />
  </article>
}

function TripConfiguration({ trip, onChanged }: { trip: Trip; onChanged: () => void }) {
  const list = useQuery({ queryKey: ['driver-vehicles'], queryFn: vehicles })
  const [vehicleId, setVehicleId] = useState('')
  const vehicle = useMutation({ mutationFn: () => changeTripVehicle(trip.id, Number(vehicleId)), onSuccess: onChanged })
  return <details><summary>Ajustar veículo</summary>
    <form className="inline-form" onSubmit={event => { event.preventDefault(); vehicle.mutate() }}>
      <label>Veículo da viagem<select required value={vehicleId} onChange={event => setVehicleId(event.target.value)}><option value="">Selecione</option>{list.data?.map(item => <option key={item.id} value={item.id}>{item.model} · {item.licensePlate} · {item.passengerCapacity} lugares</option>)}</select></label>
      <button disabled={vehicle.isPending || !vehicleId} className="secondary-button">Salvar veículo</button>
    </form>
    {[list.error, vehicle.error].filter(Boolean).map((error, index) => <p key={index} role="alert" className="form-error">{error?.message}</p>)}
  </details>
}
const statuses: Record<string, string> = { PLANNED: 'Planejada', NEEDS_ATTENTION: 'Precisa de atenção', IN_PROGRESS: 'Em andamento', COMPLETED: 'Concluída', CANCELLED: 'Cancelada' }

function formatAddress(address: Trip['participants'][number]['pickupAddress']) {
  return address ? `${address.street}, ${address.number}${address.complement ? ` · ${address.complement}` : ''} · ${address.neighborhood} · ${address.city}/${address.state}` : 'Endereço indisponível'
}
