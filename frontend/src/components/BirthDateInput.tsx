import { useEffect, useRef } from 'react'
import { birthDateError, formatBirthDate } from '../lib/birthDate'

export function BirthDateInput({ value, onChange }: { value: string; onChange: (value: string) => void }) {
  const input = useRef<HTMLInputElement>(null)
  useEffect(() => { input.current?.setCustomValidity(birthDateError(value)) }, [value])
  return <input ref={input} type="text" inputMode="numeric" placeholder="DD/MM/AAAA" autoComplete="bday" maxLength={10} required value={value} onChange={event => {
    const formatted = formatBirthDate(event.target.value)
    event.target.setCustomValidity(birthDateError(formatted))
    onChange(formatted)
  }} />
}
