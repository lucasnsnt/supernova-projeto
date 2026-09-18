import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { removeSchedule, saveSchedule, studentSchedules, type Schedule } from './api'

const days = [
  ['MONDAY', 'Segunda'], ['TUESDAY', 'Terça'], ['WEDNESDAY', 'Quarta'],
  ['THURSDAY', 'Quinta'], ['FRIDAY', 'Sexta'], ['SATURDAY', 'Sábado'],
] as const

export function SchedulePage() {
  const schedules = useQuery({ queryKey: ['student-schedules'], queryFn: studentSchedules })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Sua rotina acadêmica</p><h1>Horários de aula</h1><p className="muted">Informe quando sua aula começa e termina. Esses horários ajudam a ordenar embarques e desembarques nas rotas que você escolher.</p></header><div className="schedule-legend"><span><b>Início</b> define o limite de chegada</span><span><b>Fim</b> define quando você pode ser buscado</span></div>{schedules.isLoading && <div className="status-card">Carregando seus horários…</div>}{schedules.isError && <div className="alert-card">Não foi possível carregar seus horários.</div>}<div className="schedule-list">{days.map(([value, label]) => {
    const current = (schedules.data ?? []).filter((item) => item.dayOfWeek === value)
    const version = current.map((item) => `${item.direction}-${item.time}`).join('|')
    return <ScheduleRow key={`${value}-${version}`} day={value} label={label} schedules={current} />
  })}</div></div>
}

function ScheduleRow({ day, label, schedules }: { day: string; label: string; schedules: Schedule[] }) {
  const queryClient = useQueryClient()
  const [outbound, setOutbound] = useState(
    schedules.find((item) => item.direction === 'IDA')?.time.slice(0, 5) ?? '',
  )
  const [returnTime, setReturnTime] = useState(
    schedules.find((item) => item.direction === 'VOLTA')?.time.slice(0, 5) ?? '',
  )
  const [message, setMessage] = useState('')
  const refresh = () => { void queryClient.invalidateQueries({ queryKey: ['student-schedules'] }); void queryClient.invalidateQueries({ queryKey: ['student-registration'] }) }
  const save = useMutation({ mutationFn: () => saveSchedule(day, outbound || null, returnTime || null), onSuccess: () => { setMessage('Horários salvos'); refresh() } })
  const remove = useMutation({ mutationFn: () => removeSchedule(day), onSuccess: () => { setOutbound(''); setReturnTime(''); setMessage('Dia removido da agenda'); refresh() } })
  function submit(event: FormEvent) { event.preventDefault(); save.mutate() }
  const active = Boolean(outbound || returnTime || schedules.length)
  return <form className={`schedule-row ${active ? 'active' : ''}`} onSubmit={submit}><div className="schedule-day"><span className="schedule-check">{active ? '✓' : ''}</span><strong>{label}</strong><small>{active ? 'Dia de aula' : 'Sem aula'}</small></div><label>Início da aula<input type="time" value={outbound} onChange={(event) => { setMessage(''); setOutbound(event.target.value) }} /></label><label>Fim da aula<input type="time" value={returnTime} onChange={(event) => { setMessage(''); setReturnTime(event.target.value) }} /></label><div className="button-row"><button className="secondary-button" type="button" disabled={!schedules.length || remove.isPending} onClick={() => remove.mutate()}>{remove.isPending ? 'Limpando…' : 'Limpar'}</button><button className="primary-button compact" disabled={(!outbound && !returnTime) || save.isPending}>{save.isPending ? 'Salvando…' : 'Salvar'}</button></div>{message && <p className="save-feedback" role="status">✓ {message}</p>}{(save.error || remove.error) && <p className="form-error" role="alert">{save.error?.message || remove.error?.message}</p>}</form>
}
