export function digits(value: string, limit: number) {
  return value.replace(/\D/g, '').slice(0, limit)
}

export function phoneMask(value: string) {
  const raw = digits(value, 11)
  if (raw.length <= 2) return raw
  if (raw.length <= 6) return `(${raw.slice(0, 2)}) ${raw.slice(2)}`
  if (raw.length <= 10) return `(${raw.slice(0, 2)}) ${raw.slice(2, 6)}-${raw.slice(6)}`
  return `(${raw.slice(0, 2)}) ${raw.slice(2, 7)}-${raw.slice(7)}`
}

export function licensePlateMask(value: string) {
  return value.toUpperCase().replace(/[^A-Z0-9]/g, '').slice(0, 7)
}

export const validPhone = (value: string) => /^\d{10,11}$/.test(value.replace(/\D/g, ''))
export const validCnh = (value: string) => /^\d{11}$/.test(value.replace(/\D/g, ''))
export const validLicensePlate = (value: string) => /^[A-Z]{3}(?:\d[A-Z]\d{2}|\d{4})$/.test(value)
