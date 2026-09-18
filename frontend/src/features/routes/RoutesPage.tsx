import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useId, useState, type FormEvent } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { ConfirmDialog } from '../../components/ConfirmDialog'
import { institutions, type Institution } from '../admin/api'
import { createDriverRoute, driverRouteEnrollments, driverRoutes, removeDriverRouteEnrollment, vehicles, type RecurringRoute, type RouteDirection, type RouteEnrollment } from '../driver/api'
import { availableRoutes, leaveRouteEnrollment, requestRouteEnrollment, studentRouteEnrollments, studentRouteRoster } from '../student/api'

const days = [
  ['MONDAY', 'Seg'], ['TUESDAY', 'Ter'], ['WEDNESDAY', 'Qua'], ['THURSDAY', 'Qui'], ['FRIDAY', 'Sex'], ['SATURDAY', 'Sáb'], ['SUNDAY', 'Dom'],
] as const
type DraftSchedule = { id: number; dayOfWeek: string; direction: RouteDirection; departureTime: string; responseDeadlineTime: string }
type DraftStop = { institutionId: number }
type TimePickerTarget = { scheduleId: number; label: string }

export function RoutesPage() {
  const { session } = useAuth()
  if (session?.role === 'DRIVER') return <DriverRoutes />
  if (session?.role === 'STUDENT') return <StudentRoutes />
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Operação</p><h1>Rotas</h1><p className="muted">As rotas são configuradas pelos motoristas.</p></header></div>
}

function DriverRoutes() {
  const client = useQueryClient()
  const routes = useQuery({ queryKey: ['driver-routes'], queryFn: driverRoutes })
  const routeVehicles = useQuery({ queryKey: ['driver-vehicles'], queryFn: vehicles })
  const routeInstitutions = useQuery({ queryKey: ['institutions'], queryFn: institutions })
  const [showForm, setShowForm] = useState(false)
  const [selectedRoute, setSelectedRoute] = useState<RecurringRoute | null>(null)
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Sua operação</p><h1>Rotas e horários</h1><p className="muted">Defina sua saída fixa e as instituições atendidas. Alunos vinculados entram imediatamente quando escolhem uma rota compatível.</p></header>
    {routes.error && <p role="alert" className="form-error">{routes.error.message}</p>}
    <section className="route-intro"><span>1</span><div><strong>O horário é seu</strong><p>Você pode iniciar pelo preview a qualquer momento. Fora da janela de 30 minutos antes ou depois, o sistema pede uma confirmação extra.</p></div></section>
    <section><div className="section-heading"><h2>Suas rotas</h2><button className="primary-button compact" onClick={() => setShowForm(true)}>Criar rota</button></div>
      <div className="route-list">{routes.data?.map(route => <button key={route.id} className="route-card" onClick={() => setSelectedRoute(route)} aria-label={`Abrir detalhes da rota ${route.name}`}><RouteSummary route={route} /><span aria-hidden="true">›</span></button>)}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Ainda não há rota. Crie a primeira com sua saída e as instituições atendidas.</p>}</div>
    </section>
    {selectedRoute && <DriverRouteDetails route={selectedRoute} onClose={() => setSelectedRoute(null)} />}
    {showForm && <RouteForm routeVehicles={routeVehicles.data ?? []} routeInstitutions={routeInstitutions.data ?? []} institutionsLoading={routeInstitutions.isLoading} institutionsError={routeInstitutions.error?.message} onClose={() => setShowForm(false)} onCreated={() => { setShowForm(false); void client.invalidateQueries({ queryKey: ['driver-routes'] }) }} />}
  </div>
}

function DriverRouteDetails({ route, onClose }: { route: RecurringRoute; onClose: () => void }) {
  const titleId = useId()
  const client = useQueryClient()
  const [studentToRemove, setStudentToRemove] = useState<RouteEnrollment | null>(null)
  const roster = useQuery({ queryKey: ['route-enrollments', route.id], queryFn: () => driverRouteEnrollments(route.id) })
  const remove = useMutation({
    mutationFn: removeDriverRouteEnrollment,
    onSuccess: async () => {
      setStudentToRemove(null)
      await client.invalidateQueries({ queryKey: ['route-enrollments', route.id] })
      await client.invalidateQueries({ queryKey: ['driver-route-previews'] })
    },
  })
  useModalEscape(onClose, remove.isPending)
  return <div className="modal-backdrop route-detail-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !remove.isPending) onClose() }}>
    <section className="route-detail-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <header className="section-heading"><div><p className="section-kicker">Detalhes da rota</p><h2 id={titleId}>{route.name}</h2></div><button type="button" className="text-button" onClick={onClose}>Fechar</button></header>
      <RouteInformation route={route} />
      <section className="route-roster"><h3>Alunos na rota</h3><p className="muted">Selecione um aluno para removê-lo desta rota.</p>
        {roster.isLoading && <p className="muted">Carregando alunos…</p>}
        {roster.error && <p className="form-error" role="alert">{roster.error.message}</p>}
        <div className="route-roster-list">{roster.data?.map(item => <button type="button" key={item.id} className="route-student-button" onClick={() => setStudentToRemove(item)}><span><strong>{item.studentName}</strong><small>{item.institutionName ?? 'Instituição não informada'} · {legLabel(item.outboundEnabled, item.returnEnabled)}</small></span><span className="remove-hint">Remover</span></button>)}{!roster.isLoading && !roster.data?.length && <p className="empty-copy compact-empty">Nenhum aluno entrou nesta rota.</p>}</div>
      </section>
      {remove.error && <p className="form-error" role="alert">{remove.error.message}</p>}
    </section>
    <ConfirmDialog open={Boolean(studentToRemove)} title="Remover aluno da rota?" message={`${studentToRemove?.studentName ?? 'Este aluno'} deixará de receber as confirmações diárias desta rota.`} confirmLabel="Remover aluno" busy={remove.isPending} onCancel={() => setStudentToRemove(null)} onConfirm={() => studentToRemove && remove.mutate(studentToRemove.id)} />
  </div>
}

function RouteInformation({ route }: { route: RecurringRoute }) {
  return <div className="route-information">
    <div><span>Veículo</span><strong>{route.vehicleLabel}</strong></div>
    <div><span>Instituições atendidas</span><strong>{route.institutions.map(item => item.institutionName).join(' · ')}</strong></div>
    <div><span>Dias e saídas</span><div className="route-schedule-chips">{route.schedules.map((item, index) => <span key={`${item.dayOfWeek}-${item.direction}-${index}`}>{shortDay(item.dayOfWeek)} · {item.direction === 'IDA' ? 'Ida' : 'Volta'} {shortTime(item.departureTime)}</span>)}</div></div>
  </div>
}

function useModalEscape(onClose: () => void, busy: boolean) {
  useEffect(() => {
    const close = (event: KeyboardEvent) => { if (event.key === 'Escape' && !busy) onClose() }
    window.addEventListener('keydown', close)
    return () => window.removeEventListener('keydown', close)
  }, [busy, onClose])
}

export function RouteForm({ routeVehicles, routeInstitutions, institutionsLoading = false, institutionsError, onClose, onCreated }: { routeVehicles: Array<{ id: number; brand: string; model: string; licensePlate: string }>; routeInstitutions: Institution[]; institutionsLoading?: boolean; institutionsError?: string; onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('')
  const [vehicleId, setVehicleId] = useState('')
  const [routeDay, setRouteDay] = useState('')
  const [schedules, setSchedules] = useState<DraftSchedule[]>([])
  const [stops, setStops] = useState<DraftStop[]>([])
  const [timePicker, setTimePicker] = useState<TimePickerTarget | null>(null)
  const [validationError, setValidationError] = useState('')
  const save = useMutation({ mutationFn: () => createDriverRoute({ name, vehicleId: Number(vehicleId), schedules: schedules.map(({ dayOfWeek, direction, departureTime, responseDeadlineTime }) => ({ dayOfWeek, direction, departureTime, responseDeadlineTime })), institutions: stops.map((stop, index) => ({ ...stop, stopOrder: index + 1 })) }), onSuccess: onCreated })
  const addSchedule = (dayOfWeek: string, direction: RouteDirection) => { setValidationError(''); setSchedules(current => current.some(item => item.dayOfWeek === dayOfWeek && item.direction === direction) ? current : [...current, { id: (current.at(-1)?.id ?? 0) + 1, dayOfWeek, direction, departureTime: '', responseDeadlineTime: '' }]) }
  const updateScheduleTime = (scheduleId: number, departureTime: string) => {
    setValidationError('')
    setSchedules(current => current.map(item => item.id === scheduleId ? { ...item, departureTime, responseDeadlineTime: subtractHour(departureTime) } : item))
  }
  function submit(event: FormEvent) {
    event.preventDefault()
    if (!name.trim()) return setValidationError('Informe um nome para a rota.')
    if (!vehicleId) return setValidationError('Selecione o veículo da rota.')
    if (!routeDay) return setValidationError('Selecione o dia em que a rota opera.')
    if (schedules.length !== 2 || schedules.some(item => item.dayOfWeek !== routeDay) || !schedules.some(item => item.direction === 'IDA') || !schedules.some(item => item.direction === 'VOLTA')) return setValidationError('Cada rota deve ter uma ida e uma volta no dia selecionado.')
    if (schedules.some(item => !item.departureTime)) return setValidationError('Defina todos os horários de ida e volta adicionados.')
    if (!stops.length) return setValidationError('Adicione pelo menos uma instituição atendida.')
    setValidationError('')
    save.mutate()
  }
  return <div className="modal-backdrop route-modal-backdrop"><form className="route-form" onSubmit={submit}><div className="section-heading"><div><p className="section-kicker">Nova rota</p><h2>Monte sua rotina</h2></div><button type="button" className="text-button" onClick={onClose}>Fechar</button></div><p className="muted">Comece pelo seu horário de saída. A prévia e as confirmações serão geradas a partir daqui.</p>
    <label className="compact-field">Nome da rota<input required maxLength={120} placeholder="Ex.: Noite - Zona Sul" value={name} onChange={e => setName(e.target.value)} /></label>
    <label className="compact-field">Veículo<select required value={vehicleId} onChange={e => setVehicleId(e.target.value)}><option value="">Selecione</option>{routeVehicles.map(vehicle => <option key={vehicle.id} value={vehicle.id}>{vehicle.brand} {vehicle.model} · {vehicle.licensePlate}</option>)}</select></label>
    <fieldset className="route-fieldset"><legend>Saídas recorrentes</legend><p className="muted">Esta criação representa uma rota: um dia, uma ida e uma volta. Para outro horário, mesmo no mesmo dia, crie outra rota.</p><label className="compact-field">Dia da rota<select value={routeDay} onChange={event => { setRouteDay(event.target.value); setSchedules([]); setValidationError('') }}><option value="">Selecione</option>{days.map(([day, label]) => <option key={day} value={day}>{label}</option>)}</select></label>{routeDay && (() => { const label = days.find(([day]) => day === routeDay)?.[1] ?? routeDay; return <div className="route-day"><strong>{label}</strong><div className="route-day-content"><div className="route-day-schedules">{schedules.map(item => <div className="route-time-card" key={item.id}><div className="route-time-card-heading"><strong>{item.direction === 'IDA' ? 'Ida' : 'Volta'}</strong><button type="button" className="remove-slot" aria-label={`Remover ${item.direction === 'IDA' ? 'ida' : 'volta'} de ${label}`} onClick={() => setSchedules(current => current.filter(value => value.id !== item.id))}>Remover</button></div><TimeButton label="Horário de saída" value={item.departureTime} onClick={() => setTimePicker({ scheduleId: item.id, label: `${label} · ${item.direction === 'IDA' ? 'Ida' : 'Volta'}` })} /></div> )}</div><div className="route-add-actions">{!schedules.some(item => item.direction === 'IDA') && <button type="button" className="add-slot" onClick={() => addSchedule(routeDay, 'IDA')}>+ Ida</button>}{!schedules.some(item => item.direction === 'VOLTA') && <button type="button" className="add-slot" onClick={() => addSchedule(routeDay, 'VOLTA')}>+ Volta</button>}</div></div></div> })()}</fieldset>
    <fieldset className="route-fieldset"><legend>Instituições atendidas</legend><p className="muted">Selecione as instituições que a rota atende. Quando os alunos confirmarem presença, o sistema calculará a sequência mais eficiente usando os endereços reais de embarque e destino.</p>{institutionsLoading ? <p className="muted">Carregando instituições…</p> : institutionsError ? <p className="form-error">Não foi possível carregar as instituições: {institutionsError}</p> : !routeInstitutions.length ? <p className="empty-copy compact-empty">Ainda não há instituições cadastradas. Peça para um administrador cadastrar a instituição antes de criar a rota.</p> : <>{stops.map((stop, index) => <div className="route-stop" key={`${stop.institutionId}-${index}`}><strong>{index + 1}</strong><select aria-label={`Instituição atendida ${index + 1}`} value={stop.institutionId} onChange={e => setStops(current => current.map((item, i) => i === index ? { ...item, institutionId: Number(e.target.value) } : item))}>{routeInstitutions.filter(institution => institution.id === stop.institutionId || !stops.some(item => item.institutionId === institution.id)).map(institution => <option key={institution.id} value={institution.id}>{institution.name}</option>)}</select><button type="button" className="text-button" onClick={() => setStops(current => current.filter((_, i) => i !== index))}>Remover</button></div>)}{stops.length < routeInstitutions.length && <button type="button" className="secondary-button" onClick={() => { const next = routeInstitutions.find(institution => !stops.some(stop => stop.institutionId === institution.id)); if (next) setStops(current => [...current, { institutionId: next.id }]) }}>Adicionar instituição</button>}</>}</fieldset>
    {(validationError || save.error) && <p className="form-error route-form-error" role="alert">{validationError || save.error?.message}</p>}<button className="primary-button" disabled={save.isPending}>{save.isPending ? 'Criando rota…' : 'Criar rota'}</button>
    {timePicker && <SimpleTimePicker title={timePicker.label} value={schedules.find(item => item.id === timePicker.scheduleId)?.departureTime ?? ''} onCancel={() => setTimePicker(null)} onConfirm={value => { updateScheduleTime(timePicker.scheduleId, value); setTimePicker(null) }} />}
  </form></div>
}

function TimeButton({ label, value, onClick }: { label: string; value: string; onClick: () => void }) {
  return <button type="button" className={`time-field-button ${value ? 'selected' : ''}`} onClick={onClick}><span>{label}</span><strong>{value || '--:--'}</strong></button>
}

function SimpleTimePicker({ title, value, onCancel, onConfirm }: { title: string; value: string; onCancel: () => void; onConfirm: (value: string) => void }) {
  const titleId = useId()
  const [hour, setHour] = useState(value.slice(0, 2) || '07')
  const [minute, setMinute] = useState(value.slice(3, 5) || '00')
  useEffect(() => {
    const closeOnEscape = (event: KeyboardEvent) => { if (event.key === 'Escape') onCancel() }
    window.addEventListener('keydown', closeOnEscape)
    return () => window.removeEventListener('keydown', closeOnEscape)
  }, [onCancel])
  return <div className="time-picker-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget) onCancel() }}><section className="time-picker" role="dialog" aria-modal="true" aria-labelledby={titleId}><p className="section-kicker">Selecionar horário</p><h3 id={titleId}>{title}</h3><div className="time-picker-controls"><label>Hora<select autoFocus value={hour} onChange={event => setHour(event.target.value)}>{Array.from({ length: 24 }, (_, index) => String(index).padStart(2, '0')).map(option => <option key={option}>{option}</option>)}</select></label><span aria-hidden="true">:</span><label>Minuto<select value={minute} onChange={event => setMinute(event.target.value)}>{['00', '05', '10', '15', '20', '25', '30', '35', '40', '45', '50', '55'].map(option => <option key={option}>{option}</option>)}</select></label></div><div className="time-picker-preview">{hour}:{minute}</div><div className="modal-actions"><button type="button" className="secondary-button" onClick={onCancel}>Cancelar</button><button type="button" className="primary-button compact" onClick={() => onConfirm(`${hour}:${minute}`)}>Definir horário</button></div></section></div>
}

function subtractHour(value: string) {
  const [hour, minute] = value.split(':').map(Number)
  return `${String((hour + 23) % 24).padStart(2, '0')}:${String(minute).padStart(2, '0')}`
}

function StudentRoutes() {
  const routes = useQuery({ queryKey: ['available-routes'], queryFn: availableRoutes })
  const enrollments = useQuery({ queryKey: ['student-route-enrollments'], queryFn: studentRouteEnrollments })
  const [selectedRoute, setSelectedRoute] = useState<RecurringRoute | null>(null)
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Seu transporte</p><h1>Escolha sua rota</h1><p className="muted">Você verá apenas rotas do seu motorista que atendem à sua instituição. Ao escolher uma, sua entrada é imediata.</p></header>
    {routes.error && <p className="form-error">{routes.error.message}</p>}
    <section><div className="section-heading"><h2>Rotas compatíveis</h2></div><div className="route-list">{routes.data?.map(route => { const enrollment = enrollments.data?.find(item => item.routeId === route.id); return <button type="button" className="route-card" key={route.id} onClick={() => setSelectedRoute(route)} aria-label={`Abrir detalhes da rota ${route.name}`}><RouteSummary route={route} /><span className="route-card-side">{enrollment ? <small className="tag neutral">Na sua rota</small> : null}<b aria-hidden="true">›</b></span></button> })}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Aceite o convite do motorista e selecione sua instituição. Rotas compatíveis aparecerão aqui.</p>}</div></section>
    {selectedRoute && <StudentRouteDetails route={selectedRoute} enrollment={enrollments.data?.find(item => item.routeId === selectedRoute.id) ?? null} onClose={() => setSelectedRoute(null)} />}
  </div>
}

function StudentRouteDetails({ route, enrollment, onClose }: { route: RecurringRoute; enrollment: RouteEnrollment | null; onClose: () => void }) {
  const titleId = useId()
  const client = useQueryClient()
  const [outbound, setOutbound] = useState(enrollment?.outboundEnabled ?? true)
  const [returning, setReturning] = useState(enrollment?.returnEnabled ?? true)
  const [confirmLeave, setConfirmLeave] = useState(false)
  const roster = useQuery({ queryKey: ['student-route-roster', route.id], queryFn: () => studentRouteRoster(route.id) })
  const join = useMutation({ mutationFn: () => requestRouteEnrollment(route.id, outbound, returning), onSuccess: async () => { await client.invalidateQueries({ queryKey: ['student-route-enrollments'] }); await client.invalidateQueries({ queryKey: ['student-route-roster', route.id] }) } })
  const leave = useMutation({ mutationFn: () => leaveRouteEnrollment(enrollment!.id), onSuccess: async () => { setConfirmLeave(false); await client.invalidateQueries({ queryKey: ['student-route-enrollments'] }); await client.invalidateQueries({ queryKey: ['student-route-roster', route.id] }); onClose() } })
  useModalEscape(onClose, join.isPending || leave.isPending)
  return <div className="modal-backdrop route-detail-backdrop" role="presentation" onMouseDown={event => { if (event.target === event.currentTarget && !join.isPending && !leave.isPending) onClose() }}>
    <section className="route-detail-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
      <header className="section-heading"><div><p className="section-kicker">Detalhes da rota</p><h2 id={titleId}>{route.name}</h2></div><button type="button" className="text-button" onClick={onClose}>Fechar</button></header>
      <RouteInformation route={route} />
      <section className="route-roster"><h3>Alunos na rota</h3><div className="route-roster-list">{roster.data?.map(item => <div className="route-student-row" key={item.id}><span><strong>{item.studentName}</strong><small>{item.institutionName ?? 'Instituição'} · {legLabel(item.outboundEnabled, item.returnEnabled)}</small></span></div>)}{!roster.isLoading && !roster.data?.length && <p className="empty-copy compact-empty">Ainda não há alunos nesta rota.</p>}</div></section>
      {enrollment ? <div className="route-membership"><p className="save-feedback">✓ Você está nesta rota · {legLabel(enrollment.outboundEnabled, enrollment.returnEnabled)}</p><button type="button" className="secondary-button" onClick={() => setConfirmLeave(true)}>Sair da rota</button></div> : <div className="route-membership"><h3>Entrar na rota</h3><p className="muted">Escolha os trechos usados. Não é necessário aguardar aprovação do motorista.</p><label className="route-check"><input type="checkbox" checked={outbound} onChange={event => setOutbound(event.target.checked)} /> Ida</label><label className="route-check"><input type="checkbox" checked={returning} onChange={event => setReturning(event.target.checked)} /> Volta</label><button type="button" className="primary-button" disabled={join.isPending || (!outbound && !returning)} onClick={() => join.mutate()}>{join.isPending ? 'Entrando…' : 'Entrar nesta rota'}</button></div>}
      {(join.error || leave.error || roster.error) && <p className="form-error" role="alert">{(join.error ?? leave.error ?? roster.error)?.message}</p>}
    </section>
    <ConfirmDialog open={confirmLeave} title="Sair desta rota?" message="Você deixará de receber as confirmações diárias desta rota. Poderá entrar novamente enquanto ela continuar disponível." confirmLabel="Sair da rota" busy={leave.isPending} onCancel={() => setConfirmLeave(false)} onConfirm={() => leave.mutate()} />
  </div>
}

function RouteSummary({ route }: { route: RecurringRoute }) { return <div className="route-summary"><p className="section-kicker">{route.vehicleLabel}</p><h3>{route.name}</h3><p className="muted">{route.institutions.map(stop => stop.institutionName).join(' · ')}</p><div className="route-schedule-chips">{route.schedules.slice(0, 4).map((item, index) => <span key={`${item.dayOfWeek}-${item.direction}-${index}`}>{shortDay(item.dayOfWeek)} {item.direction === 'IDA' ? '↑' : '↓'} {shortTime(item.departureTime)}</span>)}{route.schedules.length > 4 && <span>+{route.schedules.length - 4}</span>}</div></div> }
function shortDay(day: string) { return days.find(([value]) => value === day)?.[1] ?? day }
function shortTime(value: string) { return value.slice(0, 5) }
function legLabel(outbound: boolean, returning: boolean) { return outbound && returning ? 'Ida e volta' : outbound ? 'Somente ida' : 'Somente volta' }
