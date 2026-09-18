import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { answerConfirmation, confirmationLabel, directionLabel, studentConfirmations, studentTrips, time, today, tomorrow } from './api'
import { StudentReadiness } from './StudentReadiness'

export function StudentTodayPage() {
  const queryClient = useQueryClient()
  const confirmations = useQuery({ queryKey: ['student-confirmations', today(), tomorrow()], queryFn: async () => (await Promise.all([studentConfirmations(today()), studentConfirmations(tomorrow())])).flat(), refetchInterval: 15_000 })
  const trips = useQuery({ queryKey: ['trips', 'STUDENT', today()], queryFn: () => studentTrips(), refetchInterval: 15_000 })
  const answer = useMutation({
    mutationFn: ({ id, value }: { id: number; value: 'YES' | 'NO' }) => answerConfirmation(id, value),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['student-confirmations'] }),
  })

  const allConfirmations = confirmations.data ?? []
  const pending = allConfirmations.filter((item) => item.status === 'PENDING')
  const todayConfirmations = allConfirmations.filter((item) => item.serviceDate === today())
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Seu transporte hoje</p><h1>Olá, vamos organizar seu dia</h1><p className="muted">Confirme se vai usar cada saída escolhida e acompanhe suas viagens.</p></header>
    <StudentReadiness />
    {confirmations.isLoading && <section className="status-card">Carregando confirmações…</section>}
    {confirmations.isError && <section className="alert-card">Não foi possível carregar suas confirmações.</section>}
    {answer.error && <p role="alert" className="form-error">{answer.error.message}</p>}
    {pending.map((item) => <section className="action-card" key={item.id}>
      <div><span className="tag">Resposta necessária</span><h2>Você vai na {item.direction === 'IDA' ? 'ida' : 'volta'}?</h2><p className="muted">{item.serviceDate.split('-').reverse().join('/')} · Saída da rota: {item.scheduledTime.slice(0, 5)} · {item.direction === 'IDA' ? 'aula começa' : 'aula termina'} às {item.academicTime.slice(0, 5)} · responda até {time(item.responseDeadline)}</p></div>
      <div className="button-row"><button className="secondary-button" disabled={answer.isPending} onClick={() => answer.mutate({ id: item.id, value: 'NO' })}>Não vou</button><button className="primary-button compact" disabled={answer.isPending} onClick={() => answer.mutate({ id: item.id, value: 'YES' })}>Sim, eu vou</button></div>
    </section>)}
    {confirmations.isSuccess && pending.length === 0 && <section className="status-card"><div className="status-icon">✓</div><div><h2>Respostas em dia</h2><p className="muted">Você não tem nenhuma confirmação pendente.</p></div></section>}
    <section><div className="section-heading"><div><p className="section-kicker">Sua programação</p><h2>Hoje</h2></div></div><div className="preview-list student-day-list">
      {todayConfirmations.map(item => <article className="preview-card" key={item.id}><header><div><span className="direction-pill">{directionLabel(item.direction)}</span><h3>{item.scheduledTime.slice(0, 5)}</h3></div><span className={`response ${item.status.toLowerCase()}`}>{confirmationLabel[item.status]}</span></header><p className="preview-destination">{item.institutionName || 'Instituição cadastrada'}</p><p className="muted preview-explanation">Horário acadêmico: {item.academicTime.slice(0, 5)}. {item.status === 'YES' ? 'Sua presença está confirmada e já entra na prévia do motorista.' : item.status === 'PENDING' ? `Confirme até ${time(item.responseDeadline)} para entrar na rota do dia.` : item.status === 'NO' ? 'Você informou que não fará esta viagem.' : 'O prazo terminou sem confirmação; esta viagem não entrará na rota.'}</p></article>)}
      {!confirmations.isLoading && !todayConfirmations.length && <p className="empty-copy">Nenhuma saída prevista nas rotas que você escolheu hoje.</p>}
    </div></section>
    <section><div className="section-heading"><h2>Rotas prontas</h2></div><div className="card-list">
      {trips.error && <p role="alert" className="form-error">{trips.error.message}</p>}
      {trips.data?.map((trip) => <article className="trip-card" key={trip.id}><div><span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>{time(trip.departureAt)}</h3><p className="muted">{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo a definir'}</p></div><strong className="trip-status">{trip.status.replaceAll('_', ' ')}</strong></article>)}
      {!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy compact-empty">Ainda não há rota calculada. Sua programação acima continua válida.</p>}
    </div></section>
  </div>
}
