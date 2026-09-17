import { useEffect, useRef, useState } from 'react'
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
    script.onerror = () => reject(new Error('Não foi possível carregar o mapa.'))
    document.head.appendChild(script)
  })
  return mapsPromise
}

export function LiveTripMap({ trip, location }: { trip: Trip; location: TripLocation | null }) {
  const element = useRef<HTMLDivElement>(null)
  const map = useRef<google.maps.Map | null>(null)
  const driverMarker = useRef<google.maps.Marker | null>(null)
  const overlays = useRef<Array<google.maps.Marker | google.maps.Polyline>>([])
  const [error, setError] = useState('')
  const initialTrip = useRef(trip)
  const initialLocation = useRef(location)

  useEffect(() => {
    let mounted = true
    void loadMaps().then(() => {
      if (!mounted || !element.current || map.current) return
      const points = routePoints(initialTrip.current)
      const firstLocation = initialLocation.current
      const center = firstLocation ? { lat: firstLocation.latitude, lng: firstLocation.longitude } : points[0] ?? { lat: -12.9714, lng: -38.5014 }
      map.current = new google.maps.Map(element.current, { center, zoom: 14, streetViewControl: false, mapTypeControl: false, fullscreenControl: false })
      if (initialTrip.current.encodedPolyline) {
        const line = new google.maps.Polyline({ map: map.current, path: decodePolyline(initialTrip.current.encodedPolyline), strokeColor: '#226246', strokeOpacity: .9, strokeWeight: 5 })
        overlays.current.push(line)
      }
      points.forEach((position, index) => overlays.current.push(new google.maps.Marker({ map: map.current!, position, label: String(index + 1), title: `Parada ${index + 1}` })))
      if (points.length > 1) { const bounds = new google.maps.LatLngBounds(); points.forEach(point => bounds.extend(point)); map.current.fitBounds(bounds, 48) }
    }).catch(reason => mounted && setError(reason instanceof Error ? reason.message : 'Mapa indisponível.'))
    return () => { mounted = false; overlays.current.forEach(item => item.setMap(null)); overlays.current = []; driverMarker.current?.setMap(null); map.current = null }
  }, [trip.id, trip.encodedPolyline])

  useEffect(() => {
    if (!location || !map.current || !window.google?.maps) return
    const position = { lat: location.latitude, lng: location.longitude }
    if (!driverMarker.current) driverMarker.current = new google.maps.Marker({ map: map.current, position, title: 'Motorista', icon: { path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW, fillColor: '#226246', fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2, scale: 7, rotation: location.heading ?? 0 } })
    else { driverMarker.current.setPosition(position); driverMarker.current.setIcon({ path: google.maps.SymbolPath.FORWARD_CLOSED_ARROW, fillColor: '#226246', fillOpacity: 1, strokeColor: '#fff', strokeWeight: 2, scale: 7, rotation: location.heading ?? 0 }) }
  }, [location])

  return <>{error && <p role="alert" className="form-error map-error">{error}</p>}<div ref={element} className="live-map" aria-label="Mapa da viagem em tempo real" /></>
}

function routePoints(trip: Trip) {
  const ordered = [...trip.participants].sort((a, b) => a.pickupOrder - b.pickupOrder)
  return [...ordered.map(item => item.pickupAddress), ...ordered.sort((a, b) => a.dropoffOrder - b.dropoffOrder).map(item => item.dropoffAddress)]
    .filter((address): address is NonNullable<typeof address> => address?.latitude != null && address?.longitude != null)
    .map(address => ({ lat: address.latitude!, lng: address.longitude! }))
}

function decodePolyline(encoded: string) {
  const path: google.maps.LatLngLiteral[] = []; let index = 0; let lat = 0; let lng = 0
  while (index < encoded.length) {
    const read = () => { let result = 0; let shift = 0; let byte: number; do { byte = encoded.charCodeAt(index++) - 63; result |= (byte & 0x1f) << shift; shift += 5 } while (byte >= 0x20); return (result & 1) ? ~(result >> 1) : result >> 1 }
    lat += read(); lng += read(); path.push({ lat: lat / 1e5, lng: lng / 1e5 })
  }
  return path
}
