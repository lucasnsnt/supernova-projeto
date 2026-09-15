import { apiFetch } from '../../lib/api'

export type Direction = 'IDA' | 'VOLTA'
export type ConfirmationStatus = 'PENDING' | 'YES' | 'NO' | 'NO_RESPONSE'
export type DailyConfirmation = {
  id: number; studentName?: string; serviceDate: string; direction: Direction; scheduledTime: string
  preliminaryDepartureAt: string; responseDeadline: string; status: ConfirmationStatus
}
export type Schedule = { id: number; dayOfWeek: string; time: string; direction: Direction }
export type Trip = {
  id: number; serviceDate: string; direction: Direction; status: string
  departureAt: string | null; planningIssue: string | null
  vehicle: { model: string; licensePlate: string } | null
  participants: Array<{ studentName: string; institutionName: string; estimatedPickupAt: string | null }>
}

export const today = () => new Date().toLocaleDateString('en-CA', { timeZone: 'America/Bahia' })
export const studentConfirmations = (date = today()) => apiFetch<DailyConfirmation[]>(`/api/students/me/daily-confirmations?date=${date}`)
export const answerConfirmation = (id: number, answer: 'YES' | 'NO') => apiFetch<DailyConfirmation>(`/api/students/me/daily-confirmations/${id}/answer`, { method: 'PUT', body: JSON.stringify({ answer }) })
export const studentTrips = (date = today()) => apiFetch<Trip[]>(`/api/students/me/trips?date=${date}`)
export const studentSchedules = () => apiFetch<Schedule[]>('/api/students/me/schedules')
export const saveSchedule = (day: string, outboundTime: string | null, returnTime: string | null) => apiFetch<Schedule[]>(`/api/students/me/schedules/${day}`, { method: 'PUT', body: JSON.stringify({ outboundTime, returnTime }) })
export const removeSchedule = (day: string) => apiFetch<void>(`/api/students/me/schedules/${day}`, { method: 'DELETE' })

export function time(value: string | null) {
  if (!value) return '—'
  return new Intl.DateTimeFormat('pt-BR', { hour: '2-digit', minute: '2-digit' }).format(new Date(value))
}
