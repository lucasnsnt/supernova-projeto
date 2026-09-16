import { useEffect } from 'react'

export function ConfirmDialog({ open, title, message, confirmLabel = 'Confirmar', busy = false, onCancel, onConfirm }: { open: boolean; title: string; message: string; confirmLabel?: string; busy?: boolean; onCancel: () => void; onConfirm: () => void }) {
  useEffect(() => {
    if (!open) return
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape') onCancel() }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [open, onCancel])
  if (!open) return null
  return <div className="modal-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onCancel() }}><section className="modal-card" role="dialog" aria-modal="true" aria-labelledby="confirm-title"><div className="modal-icon">!</div><h2 id="confirm-title">{title}</h2><p className="muted">{message}</p><div className="modal-actions"><button className="secondary-button" onClick={onCancel} disabled={busy}>Cancelar</button><button className="danger-button" onClick={onConfirm} disabled={busy}>{busy ? 'Aguarde…' : confirmLabel}</button></div></section></div>
}
