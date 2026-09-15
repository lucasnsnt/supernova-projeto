import { apiFetch } from '../../lib/api'
import type { Institution } from '../admin/api'

export type Address = { street: string; number: string; complement: string | null; neighborhood: string; city: string; state: string; zipCode: string; latitude: number | null; longitude: number | null }
export type ProfilePayload = { name: string; phone: string; dateOfBirth: string; address: Address }
export type ProfileStatus = { hasAddress: boolean; hasInstitution: boolean; hasOutboundSchedule: boolean; hasReturnSchedule: boolean; complete: boolean }
export type StudentDetails = { account: ProfilePayload & { email: string; address: Address | null }; institution: Institution | null; profileStatus: ProfileStatus }
export type StudentLink = { id: number; driverId: number; driverName: string; status: 'PENDING' | 'ACTIVE' | 'REJECTED' | 'ENDED'; startDate: string; endDate: string | null }
export type InvitePreview = { driverId: number; driverName: string; expiresAt: string }
export const studentDetails = () => apiFetch<StudentDetails>('/api/students/me')
export const studentLinks = () => apiFetch<StudentLink[]>('/api/students/me/links')
export const updateProfile = (payload: ProfilePayload) => apiFetch('/api/students/me/profile', { method: 'PUT', body: JSON.stringify(payload) })
export const selectInstitution = (institutionId: number) => apiFetch('/api/students/me/institution', { method: 'PUT', body: JSON.stringify({ institutionId }) })
export const previewInvite = (token: string) => apiFetch<InvitePreview>('/api/invites/preview', { method: 'POST', body: JSON.stringify({ token }) })
export const acceptInvite = (token: string) => apiFetch<StudentLink>('/api/students/me/links', { method: 'POST', body: JSON.stringify({ token }) })
export const endLink = (id: number) => apiFetch<StudentLink>(`/api/students/me/links/${id}`, { method: 'DELETE' })
