import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useEffect, useId, useState, type FormEvent } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { institutions, type Institution } from '../admin/api'
import { createDriverRoute, driverRouteEnrollments, driverRoutePreviews, driverRoutes, reviewRouteEnrollment, vehicles, type RecurringRoute, type RouteDirection, type RoutePreview } from '../driver/api'
import { availableRoutes, requestRouteEnrollment, studentRouteEnrollments, studentRoutePreviews } from '../student/api'

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
  const previews = useQuery({ queryKey: ['driver-route-previews'], queryFn: () => driverRoutePreviews() })
  const routeVehicles = useQuery({ queryKey: ['driver-vehicles'], queryFn: vehicles })
  const routeInstitutions = useQuery({ queryKey: ['institutions'], queryFn: institutions })
  const [showForm, setShowForm] = useState(false)
  const [selectedRoute, setSelectedRoute] = useState<RecurringRoute | null>(null)
  const enrollments = useQuery({ queryKey: ['route-enrollments', selectedRoute?.id], queryFn: () => driverRouteEnrollments(selectedRoute!.id), enabled: Boolean(selectedRoute) })
  const review = useMutation({ mutationFn: ({ id, status }: { id: number; status: 'APPROVED' | 'REJECTED' }) => reviewRouteEnrollment(id, status), onSuccess: () => void client.invalidateQueries({ queryKey: ['route-enrollments', selectedRoute?.id] }) })
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Sua operação</p><h1>Rotas e horários</h1><p className="muted">Defina sua saída fixa, as instituições por onde passa e aprove os alunos uma única vez.</p></header>
    {routes.error && <p role="alert" className="form-error">{routes.error.message}</p>}
    <section className="route-intro"><span>1</span><div><strong>O horário é seu</strong><p>Os alunos entram na rota; a saída não é calculada a partir da aula deles.</p></div></section>
    <PreviewSection previews={previews.data} loading={previews.isLoading} />
    <section><div className="section-heading"><h2>Suas rotas</h2><button className="primary-button compact" onClick={() => setShowForm(true)}>Criar rota</button></div>
      <div className="route-list">{routes.data?.map(route => <button key={route.id} className={`route-card ${selectedRoute?.id === route.id ? 'selected' : ''}`} onClick={() => setSelectedRoute(route)}><RouteSummary route={route} /><span aria-hidden="true">›</span></button>)}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Ainda não há rota. Crie a primeira com sua saída e as instituições atendidas.</p>}</div>
    </section>
    {selectedRoute && <section className="route-enrollment-panel"><div className="section-heading"><div><p className="section-kicker">{selectedRoute.name}</p><h2>Pedidos de vaga</h2></div><button className="text-button" onClick={() => setSelectedRoute(null)}>Fechar</button></div>
      {enrollments.isLoading && <p className="muted">Carregando pedidos…</p>}{enrollments.error && <p className="form-error">{enrollments.error.message}</p>}
      <div className="card-list">{enrollments.data?.map(item => <article className="trip-card" key={item.id}><div><strong>{item.studentName}</strong><p className="muted">{legLabel(item.outboundEnabled, item.returnEnabled)}</p></div>{item.status === 'PENDING' ? <div className="button-row"><button className="secondary-button" disabled={review.isPending} onClick={() => review.mutate({ id: item.id, status: 'REJECTED' })}>Recusar</button><button className="primary-button compact" disabled={review.isPending} onClick={() => review.mutate({ id: item.id, status: 'APPROVED' })}>Aprovar</button></div> : <span className={`tag ${item.status === 'APPROVED' ? 'neutral' : ''}`}>{item.status === 'APPROVED' ? 'Aprovado' : 'Recusado'}</span>}</article>)}{!enrollments.isLoading && !enrollments.data?.length && <p className="empty-copy">Nenhum pedido de vaga nesta rota.</p>}</div>
    </section>}
    {showForm && <RouteForm routeVehicles={routeVehicles.data ?? []} routeInstitutions={routeInstitutions.data ?? []} institutionsLoading={routeInstitutions.isLoading} institutionsError={routeInstitutions.error?.message} onClose={() => setShowForm(false)} onCreated={() => { setShowForm(false); void client.invalidateQueries({ queryKey: ['driver-routes'] }) }} />}
  </div>
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
  const client = useQueryClient()
  const routes = useQuery({ queryKey: ['available-routes'], queryFn: availableRoutes })
  const previews = useQuery({ queryKey: ['student-route-previews'], queryFn: () => studentRoutePreviews() })
  const enrollments = useQuery({ queryKey: ['student-route-enrollments'], queryFn: studentRouteEnrollments })
  const [choice, setChoice] = useState<{ routeId: number; outbound: boolean; returning: boolean } | null>(null)
  const request = useMutation({ mutationFn: () => requestRouteEnrollment(choice!.routeId, choice!.outbound, choice!.returning), onSuccess: () => { setChoice(null); void client.invalidateQueries({ queryKey: ['student-route-enrollments'] }) } })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Seu transporte</p><h1>Escolha sua rota</h1><p className="muted">Veja as rotas do seu motorista que passam pela sua instituição e peça sua vaga.</p></header>
    {routes.error && <p className="form-error">{routes.error.message}</p>}
    <PreviewSection previews={previews.data} loading={previews.isLoading} student />
    <section><div className="section-heading"><h2>Disponíveis para você</h2></div><div className="route-list">{routes.data?.map(route => { const enrollment = enrollments.data?.find(item => item.routeId === route.id); return <article className="student-route-card" key={route.id}><RouteSummary route={route} />{enrollment ? <span className={`tag ${enrollment.status === 'APPROVED' ? 'neutral' : ''}`}>{enrollment.status === 'PENDING' ? 'Pedido enviado' : enrollment.status === 'APPROVED' ? 'Na sua rota' : 'Recusado'}</span> : <button className="primary-button compact" onClick={() => setChoice({ routeId: route.id, outbound: true, returning: true })}>Pedir vaga</button>}</article> })}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Quando seu motorista criar uma rota que passe pela sua instituição, ela aparecerá aqui.</p>}</div></section>
    <section><div className="section-heading"><h2>Seus pedidos</h2></div><div className="card-list">{enrollments.data?.map(item => <article className="trip-card" key={item.id}><div><strong>{item.routeName}</strong><p className="muted">{legLabel(item.outboundEnabled, item.returnEnabled)}</p></div><span className={`tag ${item.status === 'APPROVED' ? 'neutral' : ''}`}>{item.status === 'PENDING' ? 'Aguardando' : item.status === 'APPROVED' ? 'Aprovado' : 'Recusado'}</span></article>)}</div></section>
    {choice && <div className="modal-backdrop"><section className="modal-card"><p className="section-kicker">Solicitar vaga</p><h2>Quais trechos você usa?</h2><p className="muted">Você poderá responder às confirmações do dia depois que o motorista aprovar.</p><label className="route-check"><input type="checkbox" checked={choice.outbound} onChange={e => setChoice({ ...choice, outbound: e.target.checked })} /> Ida</label><label className="route-check"><input type="checkbox" checked={choice.returning} onChange={e => setChoice({ ...choice, returning: e.target.checked })} /> Volta</label>{request.error && <p className="form-error">{request.error.message}</p>}<div className="modal-actions"><button className="secondary-button" onClick={() => setChoice(null)}>Cancelar</button><button className="primary-button compact" disabled={request.isPending || (!choice.outbound && !choice.returning)} onClick={() => request.mutate()}>{request.isPending ? 'Enviando…' : 'Enviar pedido'}</button></div></section></div>}
  </div>
}

function RouteSummary({ route }: { route: RecurringRoute }) { return <div className="route-summary"><p className="section-kicker">{route.vehicleLabel}</p><h3>{route.name}</h3><p className="muted">{route.institutions.map(stop => stop.institutionName).join(' · ')}</p><div className="route-schedule-chips">{route.schedules.slice(0, 4).map((item, index) => <span key={`${item.dayOfWeek}-${item.direction}-${index}`}>{shortDay(item.dayOfWeek)} {item.direction === 'IDA' ? '↑' : '↓'} {shortTime(item.departureTime)}</span>)}{route.schedules.length > 4 && <span>+{route.schedules.length - 4}</span>}</div></div> }
function PreviewSection({ previews, loading, student = false }: { previews: RoutePreview[] | undefined; loading: boolean; student?: boolean }) { return <section><div className="section-heading"><div><p className="section-kicker">Hoje</p><h2>Sua programação</h2></div></div>{loading ? <p className="muted">Montando prévia…</p> : <div className="preview-list">{previews?.map(item => <article className="preview-card" key={`${item.routeId}-${item.direction}-${item.departureTime}`}><header><div><p className="section-kicker">{item.routeName}</p><h3>{shortTime(item.departureTime)}</h3></div><span className="direction-pill">{item.direction === 'IDA' ? 'Ida' : 'Volta'}</span></header><p className="preview-destination">{item.stops.map(stop => stop.institutionName).join(' · ')}</p><div className="student-preview-list">{(student ? item.passengers.slice(0, 1) : item.passengers).map(person => <div className="student-preview" key={person.studentId}><span className="status-dot yes" /><div><strong>{student ? 'Sua vaga aprovada' : person.studentName}</strong><small>{person.institutionName ?? 'Instituição'}</small></div></div>)}</div><footer>Prévia sem rota calculada · {item.passengers.length} {item.passengers.length === 1 ? 'aluno' : 'alunos'}</footer></article>)}{!previews?.length && <p className="empty-copy">Nenhuma saída recorrente aprovada para hoje.</p>}</div>}</section> }
function shortDay(day: string) { return days.find(([value]) => value === day)?.[1] ?? day }
function shortTime(value: string) { return value.slice(0, 5) }
function legLabel(outbound: boolean, returning: boolean) { return outbound && returning ? 'Ida e volta' : outbound ? 'Somente ida' : 'Somente volta' }
