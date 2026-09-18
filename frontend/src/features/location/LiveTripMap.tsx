import { useEffect, useMemo, useRef, useState } from 'react'
import type { Trip, TripLocation } from '../student/api'

let mapsPromise: Promise<void> | null = null

function loadMaps() {
  if (window.google?.maps) return Promise.resolve()
  if (mapsPromise) return mapsPromise
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  if (!key) return Promise.reject(new Error('Google Maps não configurado neste ambiente.'))
  mapsPromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}`
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      mapsPromise = null
      reject(new Error('Não foi possível carregar o mapa.'))
    }
    document.head.appendChild(script)
  })
  return mapsPromise
}

export function LiveTripMap({ trip, location }: { trip: Trip; location: TripLocation | null }) {
  return <RouteMap trip={trip} location={location} label="Mapa da viagem em tempo real" />
}

export function PlannedRouteMap({ trip, onAvailabilityChange }: { trip: Trip; onAvailabilityChange?: (available: boolean) => void }) {
  return <RouteMap trip={trip} location={null} label="Mapa da prévia da rota" onAvailabilityChange={onAvailabilityChange} />
}

function RouteMap({ trip, location, label, onAvailabilityChange }: { trip: Trip; location: TripLocation | null; label: string; onAvailabilityChange?: (available: boolean) => void }) {
  const element = useRef<HTMLDivElement>(null)
  const map = useRef<google.maps.Map | null>(null)
  const driverMarker = useRef<google.maps.Marker | null>(null)
  const overlays = useRef<Array<google.maps.Marker | google.maps.Polyline>>([])
  const [error, setError] = useState('')
  const [retryKey, setRetryKey] = useState(0)
  const initialLocation = useRef(location)
  const points = useMemo(() => routePoints(trip.participants), [trip.participants])
  const routeSignature = useMemo(() => JSON.stringify({ polyline: trip.encodedPolyline, points }), [points, trip.encodedPolyline])

  useEffect(() => {
    let mounted = true
    onAvailabilityChange?.(false)
    void loadMaps().then(() => {
      if (!mounted || !element.current) return
      const firstLocation = initialLocation.current
      const center = firstLocation ? { lat: firstLocation.latitude, lng: firstLocation.longitude } : points[0]?.position ?? { lat: -12.9714, lng: -38.5014 }
      map.current = new google.maps.Map(element.current, { center, zoom: 14, streetViewControl: false, mapTypeControl: false, fullscreenControl: false })
      if (trip.encodedPolyline) {
        overlays.current.push(new google.maps.Polyline({ map: map.current, path: decodePolyline(trip.encodedPolyline), strokeColor: '#0878ec', strokeOpacity: .9, strokeWeight: 5 }))
      }
      points.forEach((point, index) => overlays.current.push(new google.maps.Marker({ map: map.current!, position: point.position, label: String(index + 1), title: point.title })))
      if (points.length > 1) {
        const bounds = new google.maps.LatLngBounds()
        points.forEach(point => bounds.extend(point.position))
        map.current.fitBounds(bounds, 48)
      }
      onAvailabilityChange?.(true)
    }).catch(reason => {
      if (!mounted) return
      setError(reason instanceof Error ? reason.message : 'Mapa indisponível.')
      onAvailabilityChange?.(false)
    })
    return () => {
      mounted = false
      overlays.current.forEach(item => item.setMap(null))
      overlays.current = []
      driverMarker.current?.setMap(null)
      driverMarker.current = null
      map.current = null
    }
  }, [onAvailabilityChange, points, retryKey, routeSignature, trip.encodedPolyline])

  useEffect(() => {
    if (!location || !map.current || !window.google?.maps) return
    const position = { lat: location.latitude, lng: location.longitude }
    if (!driverMarker.current) driverMarker.current = new google.maps.Marker({ map: map.current, position, title: 'Motorista', icon: { path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW, fillColor: '#0878ec', fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2, scale: 7, rotation: location.heading ?? 0 } })
    else {
      driverMarker.current.setPosition(position)
      driverMarker.current.setIcon({ path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW, fillColor: '#0878ec', fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2, scale: 7, rotation: location.heading ?? 0 })
    }
  }, [location])

  return <div className="route-map-shell">
    {error && <div className="map-unavailable" role="alert"><p>{error}</p><button type="button" className="secondary-button" onClick={() => { mapsPromise = null; setError(''); setRetryKey(value => value + 1) }}>Tentar novamente</button></div>}
    <div ref={element} className="live-map" aria-label={label} />
  </div>
}

function routePoints(participants: Trip['participants']) {
  return participants.flatMap(participant => [
    { order: participant.pickupOrder, address: participant.pickupAddress, title: `Embarque de ${participant.studentName}` },
    { order: participant.dropoffOrder, address: participant.dropoffAddress, title: `Desembarque de ${participant.studentName}` },
  ])
    .filter((stop): stop is typeof stop & { address: NonNullable<typeof stop.address> } => stop.address?.latitude != null && stop.address?.longitude != null)
    .sort((a, b) => a.order - b.order)
    .map(stop => ({ position: { lat: stop.address.latitude!, lng: stop.address.longitude! }, title: stop.title }))
}

function decodePolyline(encoded: string) {
  const path: google.maps.LatLngLiteral[] = []
  let index = 0
  let lat = 0
  let lng = 0
  while (index < encoded.length) {
    const read = () => {
      let result = 0
      let shift = 0
      let byte: number
      do {
        byte = encoded.charCodeAt(index++) - 63
        result |= (byte & 0x1f) << shift
        shift += 5
      } while (byte >= 0x20)
      return (result & 1) ? ~(result >> 1) : result >> 1
    }
    lat += read()
    lng += read()
    path.push({ lat: lat / 1e5, lng: lng / 1e5 })
  }
  return path
}
