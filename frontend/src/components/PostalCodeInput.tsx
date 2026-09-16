import { useState } from 'react'
import { formatPostalCode, lookupPostalCode, type PostalCodeAddress } from '../lib/postalCode'

export function PostalCodeInput({ value, disabled = false, onChange, onResolved }: { value: string; disabled?: boolean; onChange: (value: string) => void; onResolved: (address: PostalCodeAddress) => void }) {
  const [status, setStatus] = useState<'idle' | 'loading' | 'error'>('idle')
  const [message, setMessage] = useState('')
  async function resolve() {
    if (value.replace(/\D/g, '').length !== 8) return
    setStatus('loading'); setMessage('')
    try { onResolved(await lookupPostalCode(value)); setStatus('idle') }
    catch (error) { setStatus('error'); setMessage(error instanceof Error ? error.message : 'Não foi possível consultar o CEP.') }
  }
  return <label>CEP<input aria-label="CEP" disabled={disabled} inputMode="numeric" placeholder="00000-000" value={value} onChange={event => { setStatus('idle'); setMessage(''); onChange(formatPostalCode(event.target.value)) }} onBlur={() => void resolve()} required />{status === 'loading' && <small>Consultando CEP…</small>}{status === 'error' && <small className="form-error" role="alert">{message} Preencha o endereço manualmente.</small>}</label>
}
