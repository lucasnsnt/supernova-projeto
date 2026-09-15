import { apiFetch } from '../../lib/api'

export type Notification = { id: number; type: string; title: string; message: string; tripId: number | null; createdAt: string; readAt: string | null }
export const notifications = () => apiFetch<Notification[]>('/api/me/notifications')
export const unreadCount = () => apiFetch<{ count: number }>('/api/me/notifications/unread-count')
export const markNotificationRead = (id: number) => apiFetch<Notification>(`/api/me/notifications/${id}/read`, { method: 'PATCH' })
export const markAllRead = () => apiFetch<void>('/api/me/notifications/read-all', { method: 'PATCH' })
