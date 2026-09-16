import { apiFetch } from '../../lib/api'

export type LocatableAddress = { street: string; number: string; complement: string | null; neighborhood: string; city: string; state: string; zipCode: string }
export const previewGeocode = (address: LocatableAddress) => apiFetch<{ latitude: number; longitude: number }>('/api/geocoding/preview', { method: 'POST', body: JSON.stringify({ ...address, latitude: null, longitude: null }) })
