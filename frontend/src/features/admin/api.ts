import { apiFetch } from '../../lib/api'

export type Driver = { id: number; name: string; email: string; phone: string; cnh: string; status: string; statusReason: string | null }
export type Institution = { id: number; name: string; type: string; address: { street: string; number: string; city: string; state: string } }
export type InstitutionPayload = { name: string; type: string; address: { street: string; number: string; neighborhood: string; city: string; state: string; zipCode: string } }

export const drivers = () => apiFetch<Driver[]>('/api/admin/drivers')
export const approveDriver = (id: number) => apiFetch<Driver>(`/api/admin/drivers/${id}/approval`, { method: 'POST' })
export const rejectDriver = (id: number, reason: string) => apiFetch<Driver>(`/api/admin/drivers/${id}/rejection`, { method: 'POST', body: JSON.stringify({ reason }) })
export const institutions = () => apiFetch<Institution[]>('/api/institutions')
export const createInstitution = (payload: InstitutionPayload) => apiFetch<Institution>('/api/institutions', { method: 'POST', body: JSON.stringify(payload) })
