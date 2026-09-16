import { useEffect, useRef, useState } from 'react'
import { useMutation } from '@tanstack/react-query'
import { previewGeocode, type LocatableAddress } from './api'

let googlePromise: Promise<void> | null = null
function loadGoogleMaps() {
  if (window.google?.maps) return Promise.resolve()
  if (googlePromise) return googlePromise
  const key = import.meta.env.VITE_GOOGLE_MAPS_API_KEY
  if (!key) return Promise.reject(new Error('Mapa não configurado neste ambiente.'))
  googlePromise = new Promise((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://maps.googleapis.com/maps/api/js?key=${encodeURIComponent(key)}`
    script.async = true
    script.onload = () => resolve()
    script.onerror = () => {
      googlePromise = null
      script.remove()
      reject(new Error('Não foi possível carregar o mapa.'))
    }
    document.head.appendChild(script)
  })
  return googlePromise
}

export function AddressMap({ address, latitude, longitude, onConfirm }: { address: LocatableAddress; latitude: number | null; longitude: number | null; onConfirm: (latitude: number, longitude: number) => void }) {
  const container = useRef<HTMLDivElement>(null)
  const map = useRef<google.maps.Map | null>(null)
  const marker = useRef<google.maps.Marker | null>(null)
  const [candidate, setCandidate] = useState(latitude != null && longitude != null ? { latitude, longitude } : null)
  const [loadError, setLoadError] = useState('')
  const locate = useMutation({ mutationFn: () => previewGeocode(address), onSuccess: setCandidate })
  const addressKey = [address.street, address.number, address.neighborhood, address.city, address.state, address.zipCode].join('|')

  useEffect(() => {
    setCandidate(latitude != null && longitude != null ? { latitude, longitude } : null)
    setLoadError('')
    locate.reset()
    // The individual address values intentionally invalidate a previously located point.
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [addressKey, latitude, longitude])

  useEffect(() => {
    if (!candidate || !container.current) return
    let active = true
    void loadGoogleMaps().then(() => {
      if (!active || !container.current) return
      const position = { lat: candidate.latitude, lng: candidate.longitude }
      if (!map.current) map.current = new google.maps.Map(container.current, { center: position, zoom: 17, streetViewControl: false, mapTypeControl: false })
      else { map.current.setCenter(position); map.current.setZoom(17) }
      if (!marker.current) {
        marker.current = new google.maps.Marker({ map: map.current, position, draggable: true, title: 'Ponto de embarque' })
        marker.current.addListener('dragend', () => { const point = marker.current?.getPosition(); if (point) setCandidate({ latitude: point.lat(), longitude: point.lng() }) })
        map.current.addListener('click', (event: google.maps.MapMouseEvent) => { if (event.latLng) setCandidate({ latitude: event.latLng.lat(), longitude: event.latLng.lng() }) })
      } else marker.current.setPosition(position)
    }).catch(error => setLoadError(error instanceof Error ? error.message : 'Não foi possível carregar o mapa.'))
    return () => { active = false }
  }, [candidate])

  const incomplete = !address.street || !address.number || !address.city || !address.state || !address.zipCode
  return <div className="address-map"><div className="button-row"><button type="button" className="secondary-button" disabled={incomplete || locate.isPending} onClick={() => locate.mutate()}>{locate.isPending ? 'Localizando…' : candidate ? 'Localizar novamente' : 'Localizar no mapa'}</button>{candidate && <button type="button" className="primary-button compact" onClick={() => onConfirm(candidate.latitude, candidate.longitude)}>Confirmar este ponto</button>}</div>
    {candidate && <><p className="muted">Arraste o marcador ou clique no mapa para ajustar o ponto exato.</p><div ref={container} className="map-canvas" aria-label="Mapa para confirmação do endereço" /></>}
    {latitude != null && longitude != null && <p role="status">Ponto confirmado para este cadastro.</p>}
    {(locate.error || loadError) && <p role="alert" className="form-error">{locate.error?.message ?? loadError}</p>}
  </div>
}
