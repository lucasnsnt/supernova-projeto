import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { drivers, institutions, students } from './api'

export function AdminOverviewPage() {
  const driverList = useQuery({ queryKey: ['admin-drivers'], queryFn: drivers })
  const studentList = useQuery({ queryKey: ['admin-students'], queryFn: students })
  const institutionList = useQuery({ queryKey: ['institutions'], queryFn: institutions })
  const error = driverList.error ?? studentList.error ?? institutionList.error
  const pendingDrivers = driverList.data?.filter(item => item.status === 'PENDING').length ?? 0
  const incompleteStudents = studentList.data?.filter(item => !item.profileComplete || item.linkStatus !== 'ACTIVE').length ?? 0
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Administração</p><h1>Visão da operação</h1><p className="muted">Acompanhe cadastros, pendências e o caminho até a primeira viagem.</p></header>
    {error && <p role="alert" className="form-error">{error.message}</p>}
    <div className="card-list"><Metric label="Motoristas" value={driverList.data?.length} detail={`${pendingDrivers} aguardando análise`} to="/motoristas" /><Metric label="Alunos" value={studentList.data?.length} detail={`${incompleteStudents} com pendências`} to="/alunos-admin" /><Metric label="Instituições" value={institutionList.data?.length} detail="destinos disponíveis" to="/instituicoes" /></div>
    <section className="profile-card"><h2>Como uma conta fica pronta</h2><div className="card-list"><Flow title="Motorista" steps={['Confirma o e-mail e informa dados, endereço e CNH', 'Administração analisa e aprova ou solicita correção', 'Motorista cadastra e define o veículo padrão', 'Motorista gera o convite para os alunos']} /><Flow title="Aluno" steps={['Confirma o e-mail e informa dados e endereço', 'Seleciona a instituição de ensino', 'Configura os horários da agenda', 'Aceita o convite do motorista e confirma os dias']} /></div></section>
  </div>
}

function Metric({ label, value, detail, to }: { label: string; value?: number; detail: string; to: string }) { return <article className="trip-card"><div><p className="eyebrow">{label}</p><h2>{value ?? '—'}</h2><p className="muted">{detail}</p></div><Link className="secondary-button" to={to}>Ver detalhes</Link></article> }
function Flow({ title, steps }: { title: string; steps: string[] }) { return <article className="trip-card large"><div><h3>{title}</h3><ol>{steps.map(step => <li key={step}>{step}</li>)}</ol></div></article> }
