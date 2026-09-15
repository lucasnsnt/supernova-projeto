import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { removeSchedule, saveSchedule, studentSchedules, type Schedule } from './api'

const days = [
  ['MONDAY', 'Segunda'], ['TUESDAY', 'Terça'], ['WEDNESDAY', 'Quarta'],
  ['THURSDAY', 'Quinta'], ['FRIDAY', 'Sexta'], ['SATURDAY', 'Sábado'],
] as const

export function SchedulePage() {
  const schedules = useQuery({ queryKey: ['student-schedules'], queryFn: studentSchedules })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Sua rotina</p><h1>Agenda semanal</h1><p className="muted">Informe quando sua aula começa e termina em cada dia.</p></header><div className="schedule-list">{days.map(([value, label]) => {
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
  const refresh = () => void queryClient.invalidateQueries({ queryKey: ['student-schedules'] })
  const save = useMutation({ mutationFn: () => saveSchedule(day, outbound || null, returnTime || null), onSuccess: refresh })
  const remove = useMutation({ mutationFn: () => removeSchedule(day), onSuccess: refresh })
  function submit(event: FormEvent) { event.preventDefault(); save.mutate() }
  return <form className="schedule-row" onSubmit={submit}><strong>{label}</strong><label>Entrada<input type="time" value={outbound} onChange={(event) => setOutbound(event.target.value)} /></label><label>Saída<input type="time" value={returnTime} onChange={(event) => setReturnTime(event.target.value)} /></label><div className="button-row"><button className="secondary-button" type="button" disabled={!schedules.length || remove.isPending} onClick={() => remove.mutate()}>Limpar</button><button className="primary-button compact" disabled={(!outbound && !returnTime) || save.isPending}>Salvar</button></div></form>
}
