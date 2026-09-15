import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { answerConfirmation, studentConfirmations, studentTrips, time } from './api'

export function StudentTodayPage() {
  const queryClient = useQueryClient()
  const confirmations = useQuery({ queryKey: ['student-confirmations', 'today'], queryFn: () => studentConfirmations() })
  const trips = useQuery({ queryKey: ['student-trips', 'today'], queryFn: () => studentTrips() })
  const answer = useMutation({
    mutationFn: ({ id, value }: { id: number; value: 'YES' | 'NO' }) => answerConfirmation(id, value),
    onSuccess: () => void queryClient.invalidateQueries({ queryKey: ['student-confirmations'] }),
  })

  const pending = confirmations.data?.filter((item) => item.status === 'PENDING') ?? []
  return <div className="page-stack">
    <header className="page-heading"><p className="eyebrow">Seu transporte hoje</p><h1>Olá, vamos organizar seu dia</h1><p className="muted">Responda às confirmações pendentes e acompanhe suas viagens.</p></header>
    {confirmations.isLoading && <section className="status-card">Carregando confirmações…</section>}
    {confirmations.isError && <section className="alert-card">Não foi possível carregar suas confirmações.</section>}
    {pending.map((item) => <section className="action-card" key={item.id}>
      <div><span className="tag">Resposta necessária</span><h2>Você vai na {item.direction === 'IDA' ? 'ida' : 'volta'}?</h2><p className="muted">Horário da agenda: {item.scheduledTime.slice(0, 5)} · responda até {time(item.responseDeadline)}</p></div>
      <div className="button-row"><button className="secondary-button" disabled={answer.isPending} onClick={() => answer.mutate({ id: item.id, value: 'NO' })}>Não vou</button><button className="primary-button compact" disabled={answer.isPending} onClick={() => answer.mutate({ id: item.id, value: 'YES' })}>Sim, eu vou</button></div>
    </section>)}
    {!confirmations.isLoading && pending.length === 0 && <section className="status-card"><div className="status-icon">✓</div><div><h2>Nenhuma confirmação pendente</h2><p className="muted">Suas respostas do dia estão em dia.</p></div></section>}
    <section><div className="section-heading"><h2>Viagens de hoje</h2></div><div className="card-list">
      {trips.data?.map((trip) => <article className="trip-card" key={trip.id}><div><span className="tag neutral">{trip.direction === 'IDA' ? 'Ida' : 'Volta'}</span><h3>{time(trip.departureAt)}</h3><p className="muted">{trip.vehicle ? `${trip.vehicle.model} · ${trip.vehicle.licensePlate}` : 'Veículo a definir'}</p></div><strong className="trip-status">{trip.status.replaceAll('_', ' ')}</strong></article>)}
      {!trips.isLoading && trips.data?.length === 0 && <p className="empty-copy">Nenhuma viagem planejada para hoje.</p>}
    </div></section>
  </div>
}
