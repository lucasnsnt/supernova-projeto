import { useEffect, useId, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { PlannedRouteMap } from '../location/LiveTripMap'
import { directionLabel, parseBahiaDateTime, time, type Trip } from '../student/api'
import { driverOperationalRoutePreview, startDriverRoute, type OperationalRoutePreview, type RouteDirection } from './api'

type RouteOperationModalProps = {
  routeId: number
  routeName: string
  serviceDate: string
  direction: RouteDirection
  onClose: () => void
}

export function RouteOperationModal({ routeId, routeName, serviceDate, direction, onClose }: RouteOperationModalProps) {
  const titleId = useId()
  const navigate = useNavigate()
  const client = useQueryClient()
  const [confirmOutsideWindow, setConfirmOutsideWindow] = useState(false)
  const preview = useQuery({
    queryKey: ['driver-operational-route-preview', routeId, serviceDate, direction],
    queryFn: () => driverOperationalRoutePreview(routeId, serviceDate, direction),
    retry: false,
  })
  const start = useMutation({
    mutationFn: (acknowledgeOutsideWindow: boolean) => startDriverRoute(routeId, serviceDate, direction, acknowledgeOutsideWindow),
    onSuccess: async trip => {
      await client.invalidateQueries({ queryKey: ['trips'] })
      await client.invalidateQueries({ queryKey: ['driver-route-previews'] })
      navigate(`/viagens/${trip.id}/ativa`)
    },
  })

  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape' && !start.isPending) onClose() }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [onClose, start.isPending])

  const data = preview.data
  const mapTrip = useMemo(() => data ? operationalPreviewTrip(data) : null, [data])
  const outsideLabel = data && parseBahiaDateTime(data.scheduledDepartureAt).getTime() > parseBahiaDateTime(data.departureAt).getTime() ? 'Iniciar antecipadamente' : 'Iniciar agora'
  const hasRealRoute = Boolean(data?.participants.length && !data.planningIssue)
  const canStart = Boolean(data?.canStart && hasRealRoute)
  const requestStart = () => {
    if (!data) return
    if (data.withinStartWindow) start.mutate(false)
    else setConfirmOutsideWindow(true)
  }

  return <div className="modal-backdrop route-operation-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !start.isPending) onClose() }}>
    <section className="route-operation-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <header className="section-heading">
        <div><p className="section-kicker">Prévia real da rota</p><h2 id={titleId}>{routeName}</h2></div>
        <button type="button" className="text-button" onClick={onClose} disabled={start.isPending}>Fechar</button>
      </header>
      {preview.isLoading && <div className="status-card">Calculando o trajeto com os alunos confirmados…</div>}
      {preview.error && <div className="preview-failure" role="alert"><p>{preview.error.message}</p><button type="button" className="secondary-button" onClick={() => void preview.refetch()}>Tentar novamente</button></div>}
      {data && <>
        <div className="operation-heading">
          <div><span className="direction-pill">{directionLabel(data.direction)}</span><strong>Saída fixa {time(data.scheduledDepartureAt)}</strong></div>
          <span className="preview-count">{data.participants.length} {data.participants.length === 1 ? 'aluno confirmado' : 'alunos confirmados'}</span>
        </div>
        {data.planningIssue && <div className="preview-failure" role="alert"><p>{data.planningIssue}</p><button type="button" className="secondary-button" onClick={() => void preview.refetch()}>Recalcular prévia</button></div>}
        {!data.planningIssue && data.participants.length === 0 && <div className="preview-failure" role="alert"><p>Nenhum aluno confirmou presença nesta saída.</p><button type="button" className="secondary-button" onClick={() => void preview.refetch()}>Atualizar prévia</button></div>}
        {mapTrip && hasRealRoute && <PlannedRouteMap trip={mapTrip} />}
        <RouteStopList trip={mapTrip} />
        {start.error && <p className="form-error" role="alert">{start.error.message}</p>}
        <footer className="operation-actions">
          {data.tripStatus === 'IN_PROGRESS' && data.tripId ? <button type="button" className="primary-button" onClick={() => navigate(`/viagens/${data.tripId}/ativa`)}>Abrir viagem</button> : <>
            <button type="button" className="primary-button" disabled={!canStart || start.isPending} onClick={requestStart}>{start.isPending ? 'Iniciando…' : data.withinStartWindow ? 'Iniciar rota' : outsideLabel}</button>
          </>}
        </footer>
      </>}
    </section>
    <ConfirmDialog
      open={confirmOutsideWindow}
      title={outsideLabel === 'Iniciar antecipadamente' ? 'Fora do horário previsto' : 'Horário previsto já passou'}
      message={outsideLabel === 'Iniciar antecipadamente' ? `A saída está prevista para ${time(data?.scheduledDepartureAt ?? null)}. A viagem começará agora e usará o horário atual.` : `A saída estava prevista para ${time(data?.scheduledDepartureAt ?? null)}. Confirme para iniciar com o horário atual.`}
      confirmLabel={outsideLabel}
      busy={start.isPending}
      onCancel={() => setConfirmOutsideWindow(false)}
      onConfirm={() => start.mutate(true)}
    />
  </div>
}

function RouteStopList({ trip }: { trip: Trip | null }) {
  if (!trip?.participants.length) return null
  const stops = trip.participants.flatMap(participant => [
    { order: participant.pickupOrder, label: `Embarque · ${participant.studentName}`, address: participant.pickupAddress, at: participant.estimatedPickupAt },
    { order: participant.dropoffOrder, label: `Desembarque · ${participant.studentName}`, address: participant.dropoffAddress, at: participant.estimatedDropoffAt },
  ]).sort((a, b) => a.order - b.order)
  return <section className="operation-stops" aria-label="Ordem das paradas"><h3>Ordem do trajeto</h3><ol>{stops.map(stop => <li key={`${stop.order}-${stop.label}`}><span>{stop.order}</span><div><strong>{stop.label}</strong><small>{formatStopAddress(stop.address)} · {time(stop.at)}</small></div></li>)}</ol></section>
}

function formatStopAddress(address: Trip['participants'][number]['pickupAddress']) {
  return address ? `${address.street}, ${address.number} · ${address.neighborhood} · ${address.city}/${address.state}` : 'Endereço indisponível'
}

function operationalPreviewTrip(data: OperationalRoutePreview): Trip {
  return {
    id: data.tripId ?? -data.routeId,
    routeId: data.routeId,
    serviceDate: data.serviceDate,
    direction: data.direction,
    status: data.tripStatus ?? 'PREVIEW',
    plannedDepartureAt: data.scheduledDepartureAt,
    departureAt: data.departureAt,
    planningIssue: data.planningIssue,
    startedAt: null,
    completedAt: null,
    cancellationReason: null,
    encodedPolyline: data.encodedPolyline,
    completedStopCount: 0,
    vehicle: null,
    participants: data.participants,
  }
}
