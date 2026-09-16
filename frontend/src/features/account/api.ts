import { apiFetch } from '../../lib/api'

export type AccountDetails = { id: number; name: string; email: string; phone: string | null; role: string }
type Authorization = { registrationToken: string; expiresAt: string }

export const accountDetails = () => apiFetch<AccountDetails>('/api/me')
export const updateAccountProfile = (name: string, phone: string) => apiFetch<AccountDetails>('/api/me/profile', { method: 'PATCH', body: JSON.stringify({ name, phone }) })
export const updatePassword = (currentPassword: string, newPassword: string) => apiFetch<void>('/api/me/password', { method: 'PATCH', body: JSON.stringify({ currentPassword, newPassword }) })
export const requestEmailCode = (email: string) => apiFetch<void>('/api/auth/email-verification', { method: 'POST', body: JSON.stringify({ email }) })
export const confirmEmailCode = (email: string, code: string) => apiFetch<Authorization>('/api/auth/email-verification/confirm', { method: 'POST', body: JSON.stringify({ email, code }) })
export const updateEmail = (email: string, registrationToken: string) => apiFetch<AccountDetails>('/api/me/email', { method: 'PATCH', body: JSON.stringify({ email, registrationToken }) })
