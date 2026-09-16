import { apiFetch } from '../../lib/api'

export type Address = { street: string; number: string; complement: string | null; neighborhood: string; city: string; state: string; zipCode: string; latitude: number | null; longitude: number | null }
export type Driver = { id: number; name: string; email: string; phone: string; dateOfBirth: string; cnh: string; status: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED'; statusReason: string | null; reviewedAt: string | null; address: Address; operationalAddress: Address }
export type Institution = { id: number; name: string; type: string; address: { street: string; number: string; city: string; state: string } }
export type InstitutionPayload = { name: string; type: string; address: { street: string; number: string; neighborhood: string; city: string; state: string; zipCode: string } }

export const drivers = () => apiFetch<Driver[]>('/api/admin/drivers')
export const approveDriver = (id: number) => apiFetch<Driver>(`/api/admin/drivers/${id}/approval`, { method: 'POST' })
export const rejectDriver = (id: number, reason: string) => apiFetch<Driver>(`/api/admin/drivers/${id}/rejection`, { method: 'POST', body: JSON.stringify({ reason }) })
export const suspendDriver = (id: number, reason: string) => apiFetch<Driver>(`/api/admin/drivers/${id}/suspension`, { method: 'POST', body: JSON.stringify({ reason }) })
export const reactivateDriver = (id: number) => apiFetch<Driver>(`/api/admin/drivers/${id}/reactivation`, { method: 'POST' })
export const institutions = () => apiFetch<Institution[]>('/api/institutions')
export const createInstitution = (payload: InstitutionPayload) => apiFetch<Institution>('/api/institutions', { method: 'POST', body: JSON.stringify(payload) })
