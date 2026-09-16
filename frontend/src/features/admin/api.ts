import { apiFetch } from '../../lib/api'

export type Address = { street: string; number: string; complement: string | null; neighborhood: string; city: string; state: string; zipCode: string; latitude: number | null; longitude: number | null }
export type Driver = { id: number; name: string; email: string; phone: string; dateOfBirth: string; cnh: string; status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'; statusReason: string | null; reviewedAt: string | null; address: Address; operationalAddress: Address }
export type Institution = { id: number; name: string; type: string; address: Address }
export type InstitutionPayload = { name: string; type: string; address: Omit<Address, 'latitude' | 'longitude'> & { latitude?: number | null; longitude?: number | null } }
export type AdminStudent = { id: number; name: string; email: string; phone: string; registrationDate: string; address: Address | null; institutionId: number | null; institutionName: string | null; scheduleCount: number; driverId: number | null; driverName: string | null; linkStatus: 'PENDING' | 'ACTIVE' | null; profileComplete: boolean }

export const drivers = () => apiFetch<Driver[]>('/api/admin/drivers')
export const approveDriver = (id: number) => apiFetch<Driver>(`/api/admin/drivers/${id}/approval`, { method: 'POST' })
export const rejectDriver = (id: number, reason: string) => apiFetch<Driver>(`/api/admin/drivers/${id}/rejection`, { method: 'POST', body: JSON.stringify({ reason }) })
export const suspendDriver = (id: number, reason: string) => apiFetch<Driver>(`/api/admin/drivers/${id}/suspension`, { method: 'POST', body: JSON.stringify({ reason }) })
export const reactivateDriver = (id: number) => apiFetch<Driver>(`/api/admin/drivers/${id}/reactivation`, { method: 'POST' })
export const institutions = () => apiFetch<Institution[]>('/api/institutions')
export const createInstitution = (payload: InstitutionPayload) => apiFetch<Institution>('/api/institutions', { method: 'POST', body: JSON.stringify(payload) })
export const updateInstitution = (id: number, payload: InstitutionPayload) => apiFetch<Institution>(`/api/institutions/${id}`, { method: 'PUT', body: JSON.stringify(payload) })
export const deleteInstitution = (id: number) => apiFetch<void>(`/api/institutions/${id}`, { method: 'DELETE' })
export const students = () => apiFetch<AdminStudent[]>('/api/admin/students')
