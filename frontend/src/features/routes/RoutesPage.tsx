import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { useState, type FormEvent } from 'react'
import { useAuth } from '../../auth/AuthContext'
import { institutions, type Institution } from '../admin/api'
import { createDriverRoute, driverRouteEnrollments, driverRoutes, reviewRouteEnrollment, vehicles, type RecurringRoute, type RouteDirection } from '../driver/api'
import { availableRoutes, requestRouteEnrollment, studentRouteEnrollments } from '../student/api'

const days = [
  ['MONDAY', 'Seg'], ['TUESDAY', 'Ter'], ['WEDNESDAY', 'Qua'], ['THURSDAY', 'Qui'], ['FRIDAY', 'Sex'], ['SATURDAY', 'Sáb'], ['SUNDAY', 'Dom'],
] as const
type DraftSchedule = { dayOfWeek: string; direction: RouteDirection; departureTime: string; responseDeadlineTime: string }
type DraftStop = { institutionId: number; outboundArrivalBy: string; returnDepartureAt: string }

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
  const enrollments = useQuery({ queryKey: ['route-enrollments', selectedRoute?.id], queryFn: () => driverRouteEnrollments(selectedRoute!.id), enabled: Boolean(selectedRoute) })
  const review = useMutation({ mutationFn: ({ id, status }: { id: number; status: 'APPROVED' | 'REJECTED' }) => reviewRouteEnrollment(id, status), onSuccess: () => void client.invalidateQueries({ queryKey: ['route-enrollments', selectedRoute?.id] }) })
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Sua operação</p><h1>Rotas e horários</h1><p className="muted">Defina sua saída fixa, as instituições por onde passa e aprove os alunos uma única vez.</p></header>
    {routes.error && <p role="alert" className="form-error">{routes.error.message}</p>}
    <section className="route-intro"><span>1</span><div><strong>O horário é seu</strong><p>Os alunos entram na rota; a saída não é calculada a partir da aula deles.</p></div></section>
    <section><div className="section-heading"><h2>Suas rotas</h2><button className="primary-button compact" onClick={() => setShowForm(true)}>Criar rota</button></div>
      <div className="route-list">{routes.data?.map(route => <button key={route.id} className={`route-card ${selectedRoute?.id === route.id ? 'selected' : ''}`} onClick={() => setSelectedRoute(route)}><RouteSummary route={route} /><span aria-hidden="true">›</span></button>)}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Ainda não há rota. Crie a primeira com sua saída e as instituições atendidas.</p>}</div>
    </section>
    {selectedRoute && <section className="route-enrollment-panel"><div className="section-heading"><div><p className="section-kicker">{selectedRoute.name}</p><h2>Pedidos de vaga</h2></div><button className="text-button" onClick={() => setSelectedRoute(null)}>Fechar</button></div>
      {enrollments.isLoading && <p className="muted">Carregando pedidos…</p>}{enrollments.error && <p className="form-error">{enrollments.error.message}</p>}
      <div className="card-list">{enrollments.data?.map(item => <article className="trip-card" key={item.id}><div><strong>{item.studentName}</strong><p className="muted">{legLabel(item.outboundEnabled, item.returnEnabled)}</p></div>{item.status === 'PENDING' ? <div className="button-row"><button className="secondary-button" disabled={review.isPending} onClick={() => review.mutate({ id: item.id, status: 'REJECTED' })}>Recusar</button><button className="primary-button compact" disabled={review.isPending} onClick={() => review.mutate({ id: item.id, status: 'APPROVED' })}>Aprovar</button></div> : <span className={`tag ${item.status === 'APPROVED' ? 'neutral' : ''}`}>{item.status === 'APPROVED' ? 'Aprovado' : 'Recusado'}</span>}</article>)}{!enrollments.isLoading && !enrollments.data?.length && <p className="empty-copy">Nenhum pedido de vaga nesta rota.</p>}</div>
    </section>}
    {showForm && <RouteForm routeVehicles={routeVehicles.data ?? []} routeInstitutions={routeInstitutions.data ?? []} onClose={() => setShowForm(false)} onCreated={() => { setShowForm(false); void client.invalidateQueries({ queryKey: ['driver-routes'] }) }} />}
  </div>
}

function RouteForm({ routeVehicles, routeInstitutions, onClose, onCreated }: { routeVehicles: Array<{ id: number; brand: string; model: string; licensePlate: string }>; routeInstitutions: Institution[]; onClose: () => void; onCreated: () => void }) {
  const [name, setName] = useState('')
  const [vehicleId, setVehicleId] = useState('')
  const [schedules, setSchedules] = useState<DraftSchedule[]>([])
  const [stops, setStops] = useState<DraftStop[]>([])
  const save = useMutation({ mutationFn: () => createDriverRoute({ name, vehicleId: Number(vehicleId), schedules, institutions: stops.map((stop, index) => ({ ...stop, stopOrder: index + 1, outboundArrivalBy: stop.outboundArrivalBy || null, returnDepartureAt: stop.returnDepartureAt || null })) }), onSuccess: onCreated })
  const addSchedule = (dayOfWeek: string, direction: RouteDirection) => setSchedules(current => current.some(item => item.dayOfWeek === dayOfWeek && item.direction === direction) ? current : [...current, { dayOfWeek, direction, departureTime: '', responseDeadlineTime: '' }])
  const updateSchedule = (dayOfWeek: string, direction: RouteDirection, field: 'departureTime' | 'responseDeadlineTime', value: string) => setSchedules(current => current.map(item => item.dayOfWeek === dayOfWeek && item.direction === direction ? { ...item, [field]: value } : item))
  function submit(event: FormEvent) { event.preventDefault(); save.mutate() }
  return <div className="modal-backdrop route-modal-backdrop"><form className="route-form" onSubmit={submit}><div className="section-heading"><div><p className="section-kicker">Nova rota</p><h2>Monte sua rotina</h2></div><button type="button" className="text-button" onClick={onClose}>Fechar</button></div><p className="muted">Comece pelo seu horário de saída. A prévia e as confirmações serão geradas a partir daqui.</p>
    <label className="compact-field">Nome da rota<input required maxLength={120} placeholder="Ex.: Noite - Zona Sul" value={name} onChange={e => setName(e.target.value)} /></label>
    <label className="compact-field">Veículo<select required value={vehicleId} onChange={e => setVehicleId(e.target.value)}><option value="">Selecione</option>{routeVehicles.map(vehicle => <option key={vehicle.id} value={vehicle.id}>{vehicle.brand} {vehicle.model} · {vehicle.licensePlate}</option>)}</select></label>
    <fieldset className="route-fieldset"><legend>Saídas recorrentes</legend><p className="muted">Adicione ida, volta ou as duas em cada dia.</p>{days.map(([day, label]) => <div className="route-day" key={day}><strong>{label}</strong>{(['IDA', 'VOLTA'] as const).map(direction => { const item = schedules.find(value => value.dayOfWeek === day && value.direction === direction); return item ? <div className="route-time-fields" key={direction}><span>{direction === 'IDA' ? 'Ida' : 'Volta'}</span><input aria-label={`${label} ${direction} saída`} type="time" required value={item.departureTime} onChange={e => updateSchedule(day, direction, 'departureTime', e.target.value)} /><input aria-label={`${label} ${direction} prazo`} type="time" required value={item.responseDeadlineTime} onChange={e => updateSchedule(day, direction, 'responseDeadlineTime', e.target.value)} /><button type="button" className="text-button" onClick={() => setSchedules(current => current.filter(value => value !== item))}>×</button></div> : <button type="button" className="add-slot" key={direction} onClick={() => addSchedule(day, direction)}>+ {direction === 'IDA' ? 'Ida' : 'Volta'}</button> })}</div>)}</fieldset>
    <fieldset className="route-fieldset"><legend>Instituições atendidas</legend><p className="muted">A ordem serve como referência; não há cálculo de rota nesta etapa.</p>{stops.map((stop, index) => <div className="route-stop" key={`${stop.institutionId}-${index}`}><strong>{index + 1}</strong><select value={stop.institutionId} onChange={e => setStops(current => current.map((item, i) => i === index ? { ...item, institutionId: Number(e.target.value) } : item))}>{routeInstitutions.filter(institution => institution.id === stop.institutionId || !stops.some(item => item.institutionId === institution.id)).map(institution => <option key={institution.id} value={institution.id}>{institution.name}</option>)}</select><input aria-label="Chegar até" type="time" value={stop.outboundArrivalBy} onChange={e => setStops(current => current.map((item, i) => i === index ? { ...item, outboundArrivalBy: e.target.value } : item))} /><input aria-label="Buscar às" type="time" value={stop.returnDepartureAt} onChange={e => setStops(current => current.map((item, i) => i === index ? { ...item, returnDepartureAt: e.target.value } : item))} /><button type="button" className="text-button" onClick={() => setStops(current => current.filter((_, i) => i !== index))}>Remover</button></div>)}{stops.length < routeInstitutions.length && <button type="button" className="secondary-button" onClick={() => { const next = routeInstitutions.find(institution => !stops.some(stop => stop.institutionId === institution.id)); if (next) setStops(current => [...current, { institutionId: next.id, outboundArrivalBy: '', returnDepartureAt: '' }]) }}>Adicionar instituição</button>}</fieldset>
    {save.error && <p className="form-error">{save.error.message}</p>}<button className="primary-button" disabled={save.isPending || !schedules.length || !stops.length}>{save.isPending ? 'Criando rota…' : 'Criar rota'}</button>
  </form></div>
}

function StudentRoutes() {
  const client = useQueryClient()
  const routes = useQuery({ queryKey: ['available-routes'], queryFn: availableRoutes })
  const enrollments = useQuery({ queryKey: ['student-route-enrollments'], queryFn: studentRouteEnrollments })
  const [choice, setChoice] = useState<{ routeId: number; outbound: boolean; returning: boolean } | null>(null)
  const request = useMutation({ mutationFn: () => requestRouteEnrollment(choice!.routeId, choice!.outbound, choice!.returning), onSuccess: () => { setChoice(null); void client.invalidateQueries({ queryKey: ['student-route-enrollments'] }) } })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Seu transporte</p><h1>Escolha sua rota</h1><p className="muted">Veja as rotas do seu motorista que passam pela sua instituição e peça sua vaga.</p></header>
    {routes.error && <p className="form-error">{routes.error.message}</p>}
    <section><div className="section-heading"><h2>Disponíveis para você</h2></div><div className="route-list">{routes.data?.map(route => { const enrollment = enrollments.data?.find(item => item.routeId === route.id); return <article className="student-route-card" key={route.id}><RouteSummary route={route} />{enrollment ? <span className={`tag ${enrollment.status === 'APPROVED' ? 'neutral' : ''}`}>{enrollment.status === 'PENDING' ? 'Pedido enviado' : enrollment.status === 'APPROVED' ? 'Na sua rota' : 'Recusado'}</span> : <button className="primary-button compact" onClick={() => setChoice({ routeId: route.id, outbound: true, returning: true })}>Pedir vaga</button>}</article> })}{!routes.isLoading && !routes.data?.length && <p className="empty-copy">Quando seu motorista criar uma rota que passe pela sua instituição, ela aparecerá aqui.</p>}</div></section>
    <section><div className="section-heading"><h2>Seus pedidos</h2></div><div className="card-list">{enrollments.data?.map(item => <article className="trip-card" key={item.id}><div><strong>{item.routeName}</strong><p className="muted">{legLabel(item.outboundEnabled, item.returnEnabled)}</p></div><span className={`tag ${item.status === 'APPROVED' ? 'neutral' : ''}`}>{item.status === 'PENDING' ? 'Aguardando' : item.status === 'APPROVED' ? 'Aprovado' : 'Recusado'}</span></article>)}</div></section>
    {choice && <div className="modal-backdrop"><section className="modal-card"><p className="section-kicker">Solicitar vaga</p><h2>Quais trechos você usa?</h2><p className="muted">Você poderá responder às confirmações do dia depois que o motorista aprovar.</p><label className="route-check"><input type="checkbox" checked={choice.outbound} onChange={e => setChoice({ ...choice, outbound: e.target.checked })} /> Ida</label><label className="route-check"><input type="checkbox" checked={choice.returning} onChange={e => setChoice({ ...choice, returning: e.target.checked })} /> Volta</label>{request.error && <p className="form-error">{request.error.message}</p>}<div className="modal-actions"><button className="secondary-button" onClick={() => setChoice(null)}>Cancelar</button><button className="primary-button compact" disabled={request.isPending || (!choice.outbound && !choice.returning)} onClick={() => request.mutate()}>{request.isPending ? 'Enviando…' : 'Enviar pedido'}</button></div></section></div>}
  </div>
}

function RouteSummary({ route }: { route: RecurringRoute }) { return <div className="route-summary"><p className="section-kicker">{route.vehicleLabel}</p><h3>{route.name}</h3><p className="muted">{route.institutions.map(stop => stop.institutionName).join(' · ')}</p><div className="route-schedule-chips">{route.schedules.slice(0, 4).map((item, index) => <span key={`${item.dayOfWeek}-${item.direction}-${index}`}>{shortDay(item.dayOfWeek)} {item.direction === 'IDA' ? '↑' : '↓'} {shortTime(item.departureTime)}</span>)}{route.schedules.length > 4 && <span>+{route.schedules.length - 4}</span>}</div></div> }
function shortDay(day: string) { return days.find(([value]) => value === day)?.[1] ?? day }
function shortTime(value: string) { return value.slice(0, 5) }
function legLabel(outbound: boolean, returning: boolean) { return outbound && returning ? 'Ida e volta' : outbound ? 'Somente ida' : 'Somente volta' }
