import { useQuery } from '@tanstack/react-query'
import { students } from './api'

export function AdminStudentsPage() {
  const list = useQuery({ queryKey: ['admin-students'], queryFn: students })
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">Administração</p><h1>Alunos</h1><p className="muted">Acompanhe cada etapa necessária para o aluno participar das viagens.</p></header>
    {list.isPending && <p role="status">Carregando alunos…</p>}{list.isError && <p role="alert" className="form-error">{list.error.message}</p>}
    <div className="card-list">{list.data?.map(student => <article className="trip-card large" key={student.id}><div><span className={`tag ${student.profileComplete && student.linkStatus === 'ACTIVE' ? 'neutral' : ''}`}>{student.profileComplete && student.linkStatus === 'ACTIVE' ? 'Pronto para operar' : 'Cadastro incompleto'}</span><h3>{student.name}</h3><p className="muted">{student.email} · {student.phone}</p><p>{student.address ? `${student.address.street}, ${student.address.number} · ${student.address.city}/${student.address.state}` : 'Endereço pendente'}</p><div className="button-row"><Step ready={Boolean(student.institutionId)}>Instituição: {student.institutionName ?? 'pendente'}</Step><Step ready={student.scheduleCount > 0}>Agenda: {student.scheduleCount ? `${student.scheduleCount} horário(s)` : 'pendente'}</Step><Step ready={student.linkStatus === 'ACTIVE'}>Motorista: {student.driverName ? `${student.driverName} (${student.linkStatus})` : 'pendente'}</Step></div></div></article>)}{list.data?.length === 0 && <p className="empty-copy">Nenhum aluno cadastrado.</p>}</div>
  </div>
}

function Step({ ready, children }: { ready: boolean; children: React.ReactNode }) { return <span className="tag neutral">{ready ? '✓' : '○'} {children}</span> }
