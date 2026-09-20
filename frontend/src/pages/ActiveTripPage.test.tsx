import { expect, it } from 'vitest'
import { googleMapsRouteUrl } from './ActiveTripPage'

it('abre as paradas restantes no Maps preservando a ordem calculada', () => {
  const url = googleMapsRouteUrl([
    { order: 1, title: 'Aluno no caminho', address: 'Casa Y', latitude: -10.91, longitude: -37.07 },
    { order: 2, title: 'Aluno prioritário', address: 'Casa X', latitude: -10.92, longitude: -37.08 },
    { order: 3, title: 'Instituição', address: 'Unit', latitude: -10.93, longitude: -37.09 },
  ], { latitude: -10.90, longitude: -37.06, accuracy: 5, heading: null, recordedAt: '2026-09-20T17:00:00', updatedAt: '2026-09-20T17:00:00' })

  const parsed = new URL(url!)
  expect(parsed.searchParams.get('origin')).toBe('-10.9,-37.06')
  expect(parsed.searchParams.get('waypoints')).toBe('-10.91,-37.07|-10.92,-37.08')
  expect(parsed.searchParams.get('destination')).toBe('-10.93,-37.09')
  expect(parsed.searchParams.get('dir_action')).toBe('navigate')
})
