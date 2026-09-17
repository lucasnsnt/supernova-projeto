import { apiFetch } from '../../lib/api'
import type { Address } from './profile-api'
import type { RecurringRoute, RouteEnrollment, RoutePreview } from '../driver/api'

export type Direction = 'IDA' | 'VOLTA'
export type ConfirmationStatus = 'PENDING' | 'YES' | 'NO' | 'NO_RESPONSE'
export type DailyConfirmation = {
  id: number; studentName?: string; institutionName?: string | null; serviceDate: string; direction: Direction; scheduledTime: string
  preliminaryDepartureAt: string; responseDeadline: string; status: ConfirmationStatus
}
export type Schedule = { id: number; dayOfWeek: string; time: string; direction: Direction }
export type Trip = {
  id: number; serviceDate: string; direction: Direction; status: string
  departureAt: string | null; planningIssue: string | null
  startedAt: string | null; completedAt: string | null; cancellationReason: string | null
  vehicle: { id: number; model: string; licensePlate: string } | null
  participants: Array<{ studentId: number; studentName: string; institutionName: string | null; pickupOrder: number; dropoffOrder: number; estimatedPickupAt: string | null; estimatedDropoffAt: string | null; pickupAddress: Address | null; dropoffAddress: Address | null }>
}

export const today = () => new Date().toLocaleDateString('en-CA', { timeZone: 'America/Bahia' })
export const tomorrow = () => {
  const date = new Date(`${today()}T12:00:00Z`)
  date.setUTCDate(date.getUTCDate() + 1)
  return date.toISOString().slice(0, 10)
}
export const studentConfirmations = (date = today()) => apiFetch<DailyConfirmation[]>(`/api/students/me/daily-confirmations?date=${date}`)
export const answerConfirmation = (id: number, answer: 'YES' | 'NO') => apiFetch<DailyConfirmation>(`/api/students/me/daily-confirmations/${id}/answer`, { method: 'PUT', body: JSON.stringify({ answer }) })
export const studentTrips = (date = today()) => apiFetch<Trip[]>(`/api/students/me/trips?date=${date}`)
export const availableRoutes = () => apiFetch<RecurringRoute[]>('/api/students/me/available-routes')
export const studentRoutePreviews = (date = today()) => apiFetch<RoutePreview[]>(`/api/students/me/route-previews?date=${date}`)
export const studentRouteEnrollments = () => apiFetch<RouteEnrollment[]>('/api/students/me/route-enrollments')
export const requestRouteEnrollment = (routeId: number, outboundEnabled: boolean, returnEnabled: boolean) => apiFetch<RouteEnrollment>('/api/students/me/route-enrollments', { method: 'POST', body: JSON.stringify({ routeId, outboundEnabled, returnEnabled }) })
export const studentSchedules = () => apiFetch<Schedule[]>('/api/students/me/schedules')
export const saveSchedule = (day: string, outboundTime: string | null, returnTime: string | null) => apiFetch<Schedule[]>(`/api/students/me/schedules/${day}`, { method: 'PUT', body: JSON.stringify({ outboundTime, returnTime }) })
export const removeSchedule = (day: string) => apiFetch<void>(`/api/students/me/schedules/${day}`, { method: 'DELETE' })

export function time(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}

export const directionLabel = (direction: Direction) => direction === 'IDA' ? 'Ida' : 'Volta'
export const confirmationLabel: Record<ConfirmationStatus, string> = {
  PENDING: 'Aguardando resposta',
  YES: 'Confirmado',
  NO: 'Não vai',
  NO_RESPONSE: 'Prazo encerrado',
}
