import { useEffect, useMemo, useRef, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useNavigate, useParams } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import { LiveTripMap } from '../features/location/LiveTripMap'
import { cancelTrip, completeTripStop, driverTripTracking, tripAction, updateTripLocation } from '../features/driver/api'
import { studentTripTracking, type Trip } from '../features/student/api'
import { ConfirmDialog } from '../components/ConfirmDialog'

type GpsState = 'connecting' | 'sharing' | 'failed' | 'denied'

export function ActiveTripPage() {
  const id = Number(useParams().tripId)
  const { session } = useAuth()
  const driver = session?.role === 'DRIVER'
  const tracking = useQuery({ queryKey: ['trip-tracking', id, session?.role], queryFn: () => driver ? driverTripTracking(id) : studentTripTracking(id), enabled: Number.isFinite(id), refetchInterval: 12_000, staleTime: 0 })

  if (tracking.isLoading) return <div className="active-trip-state">Carregando viagem…</div>
  if (tracking.error || !tracking.data) return <div className="active-trip-state"><p role="alert" className="form-error">{tracking.error?.message ?? 'Viagem não encontrada.'}</p></div>
  if (tracking.data.trip.status !== 'IN_PROGRESS') return <EndedTrip status={tracking.data.trip.status} />
  return driver ? <DriverActiveTrip initial={tracking.data.trip} /> : <StudentActiveTrip trip={tracking.data.trip} location={tracking.data.location} />
}

function EndedTrip({ status }: { status: string }) {
  const navigate = useNavigate()
  return <div className="active-trip-state"><section className="status-card"><div className="status-icon">✓</div><div><h2>{status === 'CANCELLED' ? 'Viagem cancelada' : 'Viagem encerrada'}</h2><p className="muted">A localização do motorista não está mais disponível.</p><button className="secondary-button" onClick={() => navigate('/viagens')}>Voltar às viagens</button></div></section></div>
}

function DriverActiveTrip({ initial }: { initial: Trip }) {
  const id = initial.id
  const navigate = useNavigate()
  const client = useQueryClient()
  const [gps, setGps] = useState<GpsState>('connecting')
  const [localLocation, setLocalLocation] = useState<Awaited<ReturnType<typeof driverTripTracking>>['location']>(null)
  const [confirming, setConfirming] = useState<'finish' | 'cancel' | null>(null)
  const [cancelReason, setCancelReason] = useState('')
  const lastSent = useRef<{ at: number; latitude: number; longitude: number } | null>(null)
  const tracking = useQuery({ queryKey: ['trip-tracking', id, 'DRIVER-active'], queryFn: () => driverTripTracking(id), refetchInterval: 12_000, initialData: { trip: initial, location: null }, staleTime: 0 })
  const send = useMutation({ mutationFn: (position: GeolocationPosition) => updateTripLocation(id, { latitude: position.coords.latitude, longitude: position.coords.longitude, accuracy: position.coords.accuracy, heading: position.coords.heading, recordedAt: new Date(position.timestamp).toISOString().slice(0, 19) }), onSuccess: data => { setGps('sharing'); setLocalLocation(data.location) }, onError: () => setGps('failed') })
  const stop = useMutation({ mutationFn: () => completeTripStop(id), onSuccess: data => client.setQueryData(['trip-tracking', id, 'DRIVER-active'], (old: typeof tracking.data) => old ? { ...old, trip: data } : old) })
  const finish = useMutation({ mutationFn: () => tripAction(id, 'completion'), onSuccess: leave })
  const cancel = useMutation({ mutationFn: () => cancelTrip(id, cancelReason.trim()), onSuccess: leave })
  const trip = tracking.data.trip
  const stops = useMemo(() => tripStops(trip), [trip])
  const next = stops[trip.completedStopCount]
  const mapsUrl = googleMapsRouteUrl(stops.slice(trip.completedStopCount), localLocation ?? tracking.data.location)

  function leave() { void client.invalidateQueries({ queryKey: ['trips'] }); navigate('/viagens') }

  useEffect(() => {
    if (trip.status !== 'IN_PROGRESS') return
    if (!navigator.geolocation) { setGps('failed'); return }
    setGps('connecting')
    const watch = navigator.geolocation.watchPosition(position => {
      const previous = lastSent.current
      const now = Date.now()
      const shouldSend = !previous || (now - previous.at >= 10_000 && distance(previous, position.coords) >= 8) || now - previous.at >= 30_000
      if (!shouldSend) return
      lastSent.current = { at: now, latitude: position.coords.latitude, longitude: position.coords.longitude }
      send.mutate(position)
    }, error => setGps(error.code === error.PERMISSION_DENIED ? 'denied' : 'failed'), { enableHighAccuracy: true, maximumAge: 5_000, timeout: 15_000 })
    return () => navigator.geolocation.clearWatch(watch)
    // Mutation identity changes on render; the trip id/status are the lifecycle boundaries.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [id, trip.status])

  return <main className="active-trip-page">
    <div className="active-map-wrap"><LiveTripMap trip={trip} location={localLocation ?? tracking.data.location} /><button className="map-back" onClick={() => navigate('/viagens')} aria-label="Voltar">‹</button><GpsBadge state={gps} /></div>
    <section className="trip-control-sheet">
      <div className="trip-progress-heading"><div><p className="section-kicker">Viagem em andamento</p><h1>{next ? `Próxima: ${next.title}` : 'Todas as paradas concluídas'}</h1><p className="muted">{next?.address ?? 'Você já pode encerrar a viagem.'}</p></div><strong>{trip.completedStopCount}/{stops.length}</strong></div>
      <div className="progress-track"><span style={{ width: `${stops.length ? trip.completedStopCount / stops.length * 100 : 100}%` }} /></div>
      {mapsUrl && <><a className="primary-button maps-navigation" href={mapsUrl} target="_blank" rel="noreferrer">Abrir rota no Google Maps</a><p className="maps-tracking-warning">O celular pode pausar o rastreamento enquanto o Maps estiver em primeiro plano.</p></>}
      <div className="active-actions">{next && <button className="primary-button" disabled={stop.isPending} onClick={() => stop.mutate()}>{stop.isPending ? 'Salvando…' : 'Concluir parada'}</button>}<button className="secondary-button" onClick={() => setConfirming('finish')}>Encerrar viagem</button><button className="text-button danger-text" onClick={() => setConfirming('cancel')}>Cancelar viagem</button></div>
      {(stop.error || finish.error || cancel.error) && <p role="alert" className="form-error">{(stop.error ?? finish.error ?? cancel.error)?.message}</p>}
    </section>
    <ConfirmDialog open={confirming === 'finish'} title="Encerrar viagem?" message="O compartilhamento da localização será encerrado e os alunos não verão mais sua posição." confirmLabel="Encerrar viagem" busy={finish.isPending} onCancel={() => setConfirming(null)} onConfirm={() => finish.mutate()} />
    {confirming === 'cancel' && <div className="modal-backdrop" role="presentation"><section className="modal-card" role="dialog" aria-modal="true"><div className="modal-icon">!</div><h2>Cancelar viagem?</h2><p className="muted">O acompanhamento será encerrado para todos os participantes.</p><label className="cancel-reason">Motivo<input autoFocus value={cancelReason} onChange={event => setCancelReason(event.target.value)} /></label><div className="modal-actions"><button className="secondary-button" onClick={() => setConfirming(null)}>Voltar</button><button className="danger-button" disabled={!cancelReason.trim() || cancel.isPending} onClick={() => cancel.mutate()}>{cancel.isPending ? 'Aguarde…' : 'Cancelar viagem'}</button></div></section></div>}
  </main>
}

function StudentActiveTrip({ trip, location }: { trip: Trip; location: Awaited<ReturnType<typeof studentTripTracking>>['location'] }) {
  const navigate = useNavigate()
  const [, tick] = useState(0)
  useEffect(() => { const timer = window.setInterval(() => tick(value => value + 1), 1_000); return () => window.clearInterval(timer) }, [])
  return <main className="active-trip-page student-tracking"><div className="active-map-wrap"><LiveTripMap trip={trip} location={location} /><button className="map-back" onClick={() => navigate('/viagens')} aria-label="Voltar">‹</button></div><section className="trip-control-sheet"><p className="section-kicker">Viagem em andamento</p><h1>Motorista a caminho</h1><p className="muted">{location ? `Localização atualizada há ${secondsAgo(location.updatedAt)} segundos` : 'Aguardando a primeira localização do motorista…'}</p><div className="driver-live-line"><span className={location ? 'live-dot' : 'waiting-dot'} /><strong>{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo da viagem'}</strong></div><div className="progress-track"><span style={{ width: `${trip.participants.length ? trip.completedStopCount / (trip.participants.length * 2) * 100 : 0}%` }} /></div></section></main>
}

function GpsBadge({ state }: { state: GpsState }) {
  const label = { connecting: 'GPS conectando', sharing: 'GPS compartilhando', failed: 'GPS falhou', denied: 'Permissão de GPS negada' }[state]
  return <div className={`gps-badge ${state}`}><span />{label}</div>
}

function tripStops(trip: Trip) {
  return trip.participants.flatMap(item => [
    stop(item.pickupOrder, item.studentName, item.pickupAddress),
    stop(item.dropoffOrder, item.institutionName ?? item.studentName, item.dropoffAddress),
  ]).sort((a, b) => a.order - b.order)
}
function stop(order: number, title: string, address: Trip['participants'][number]['pickupAddress']) { return { order, title, address: formatAddress(address), latitude: address?.latitude ?? null, longitude: address?.longitude ?? null } }
export function googleMapsRouteUrl(stops: ReturnType<typeof tripStops>, location: Awaited<ReturnType<typeof driverTripTracking>>['location']) {
  const segment = stops.filter(item => item.latitude != null && item.longitude != null).slice(0, 10)
  if (!segment.length) return null
  const destination = segment.at(-1)!
  const params = new URLSearchParams({ api: '1', destination: `${destination.latitude},${destination.longitude}`, travelmode: 'driving', dir_action: 'navigate' })
  if (location) params.set('origin', `${location.latitude},${location.longitude}`)
  if (segment.length > 1) params.set('waypoints', segment.slice(0, -1).map(item => `${item.latitude},${item.longitude}`).join('|'))
  return `https://www.google.com/maps/dir/?${params.toString()}`
}
function formatAddress(address: Trip['participants'][number]['pickupAddress']) { return address ? `${address.street}, ${address.number} · ${address.neighborhood}` : 'Endereço não informado' }
function secondsAgo(value: string) { return Math.max(0, Math.floor((Date.now() - new Date(value).getTime()) / 1_000)) }
function distance(previous: { latitude: number; longitude: number }, current: { latitude: number; longitude: number }) { const rad = Math.PI / 180; const x = (current.longitude - previous.longitude) * rad * Math.cos((current.latitude + previous.latitude) * rad / 2); const y = (current.latitude - previous.latitude) * rad; return Math.sqrt(x * x + y * y) * 6_371_000 }
