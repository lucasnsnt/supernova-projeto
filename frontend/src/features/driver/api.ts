import type { DailyConfirmation, Trip } from '../student/api'
import { apiFetch } from '../../lib/api'
import { today } from '../student/api'

export type LinkedStudent = { linkId: number; name: string; phone: string; institutionName: string | null; profileComplete: boolean; status: string }
export type Invite = { id: number; token: string; status: string; expiresAt: string }
export type Vehicle = { id: number; brand: string; model: string; year: number | null; licensePlate: string; passengerCapacity: number; color: string | null; defaultVehicle: boolean }
export type Address = { street: string; number: string; complement: string | null; neighborhood: string; city: string; state: string; zipCode: string; latitude: number | null; longitude: number | null }
export type DriverProfile = { id: number; name: string; email: string; phone: string; dateOfBirth: string; address: Address; operationalAddress: Address; cnh: string; status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'; statusReason: string | null; reviewedAt: string | null }
export type DriverProfilePayload = { name: string; phone: string; dateOfBirth: string; cnh: string; address: Address }

export const driverConfirmations = () => apiFetch<DailyConfirmation[]>(`/api/drivers/me/daily-confirmations?date=${today()}`)
export const driverTrips = (date = today()) => apiFetch<Trip[]>(`/api/drivers/me/trips?date=${date}`)
export const linkedStudents = () => apiFetch<LinkedStudent[]>('/api/drivers/me/students')
export const invites = () => apiFetch<Invite[]>('/api/drivers/me/invites')
export const createInvite = () => apiFetch<Invite>('/api/drivers/me/invites', { method: 'POST', body: JSON.stringify({ validityDays: 7, replaceCurrent: true }) })
export const vehicles = () => apiFetch<Vehicle[]>('/api/drivers/me/vehicles')
export const driverProfile = () => apiFetch<DriverProfile>('/api/drivers/me')
export const updateRejectedProfile = (body: DriverProfilePayload) => apiFetch('/api/drivers/me/profile', { method: 'PUT', body: JSON.stringify(body) })
export const resubmitProfile = () => apiFetch<DriverProfile>('/api/drivers/me/review-submissions', { method: 'POST' })
export const setOperationalAddress = (body: Address) => apiFetch<DriverProfile>('/api/drivers/me/operational-address', { method: 'PUT', body: JSON.stringify(body) })
export const useRegistrationAddress = () => apiFetch<DriverProfile>('/api/drivers/me/operational-address', { method: 'DELETE' })
export const createVehicle = (body: Omit<Vehicle, 'id' | 'defaultVehicle'>) => apiFetch<Vehicle>('/api/drivers/me/vehicles', { method: 'POST', body: JSON.stringify(body) })
export const updateVehicle = (id: number, body: Omit<Vehicle, 'id' | 'defaultVehicle'>) => apiFetch<Vehicle>(`/api/drivers/me/vehicles/${id}`, { method: 'PUT', body: JSON.stringify(body) })
export const deleteVehicle = (id: number) => apiFetch<void>(`/api/drivers/me/vehicles/${id}`, { method: 'DELETE' })
export const tripAction = (id: number, action: 'start' | 'completion' | 'replanning', body?: unknown) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/${action}`, { method: 'POST', body: body ? JSON.stringify(body) : undefined })
export const cancelTrip = (id: number, reason: string) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/cancellation`, { method: 'POST', body: JSON.stringify({ reason }) })

export const setDefaultVehicle = (id: number) => apiFetch<Vehicle>(`/api/drivers/me/vehicles/${id}/default`, { method: 'PUT' })

export const changeTripVehicle = (id: number, vehicleId: number) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/vehicle`, { method: 'PUT', body: JSON.stringify({ vehicleId }) })
export const updateDeparture = (id: number, departureAt: string, reason: string | null) => apiFetch<Trip>(`/api/drivers/me/trips/${id}/departure`, { method: 'PUT', body: JSON.stringify({ departureAt, reason }) })
