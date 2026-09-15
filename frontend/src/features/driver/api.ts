import type { DailyConfirmation, Trip } from '../student/api'
import { apiFetch } from '../../lib/api'
import { today } from '../student/api'

export type LinkedStudent = { linkId: number; name: string; phone: string; institutionName: string | null; profileComplete: boolean; status: string }
export type Invite = { id: number; token: string; status: string; expiresAt: string }
export type Vehicle = { id: number; brand: string; model: string; year: number | null; licensePlate: string; passengerCapacity: number; color: string | null; defaultVehicle: boolean }

export const driverConfirmations = () => apiFetch<DailyConfirmation[]>(`/api/drivers/me/daily-confirmations?date=${today()}`)
export const driverTrips = () => apiFetch<Trip[]>(`/api/drivers/me/trips?date=${today()}`)
export const linkedStudents = () => apiFetch<LinkedStudent[]>('/api/drivers/me/students')
export const invites = () => apiFetch<Invite[]>('/api/drivers/me/invites')
export const createInvite = () => apiFetch<Invite>('/api/drivers/me/invites', { method: 'POST', body: JSON.stringify({ validityDays: 7, replaceCurrent: true }) })
export const vehicles = () => apiFetch<Vehicle[]>('/api/drivers/me/vehicles')
export const createVehicle = (body: Omit<Vehicle, 'id' | 'defaultVehicle'>) => apiFetch<Vehicle>('/api/drivers/me/vehicles', { method: 'POST', body: JSON.stringify(body) })
export const tripAction = (id: number, action: 'start' | 'completion' | 'replanning', body?: unknown) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/${action}`, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
export const cancelTrip = (id: number, reason: string) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/cancellation`, { method: 'POST', body: JSON.stringify({ reason }) })

export const setDefaultVehicle = (id: number) => apiFetch<Vehicle>(`/api/drivers/me/vehicles/${id}/default`, { method: 'PUT' })
