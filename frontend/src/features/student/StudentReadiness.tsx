import { useQuery } from '@tanstack/react-query'
import { Link } from 'react-router-dom'
import { useAuth } from '../../auth/AuthContext'
import { studentDetails, studentLinks } from './profile-api'

// oxlint-disable-next-line react/only-export-components
export function useStudentRegistration() {
  const { session } = useAuth()
  const enabled = session?.role === 'STUDENT'
  const details = useQuery({ queryKey: ['student-registration', session?.userId, 'details'], queryFn: studentDetails, enabled })
  const links = useQuery({ queryKey: ['student-registration', session?.userId, 'links'], queryFn: studentLinks, enabled })
  return { details, links }
}

export function StudentReadiness() {
  const { details, links } = useStudentRegistration()
  if (details.isPending || links.isPending) return <p role="status">Verificando seu cadastro…</p>
  if (details.isError || links.isError) return <section className="alert-card" role="alert">Não foi possível verificar seu cadastro. <button className="text-button" onClick={() => { void details.refetch(); void links.refetch() }}>Tentar novamente</button></section>
  const status = details.data.profileStatus
  const active = links.data.some(link => link.status === 'ACTIVE')
  if (status.complete && active) return null
  return <section className="profile-card"><h2>Complete seu cadastro para viajar</h2><ul className="readiness-list">
    {!status.hasAddress && <li><Link to="/meu-cadastro">Informe seu endereço</Link></li>}
    {!status.hasInstitution && <li><Link to="/meu-cadastro">Selecione sua instituição</Link></li>}
    {!status.hasOutboundSchedule && !status.hasReturnSchedule && <li><Link to="/agenda">Cadastre pelo menos um horário na agenda</Link></li>}
    {!active && <li><Link to="/meu-cadastro">Vincule-se a um motorista pelo convite</Link></li>}
  </ul><p className="muted">As confirmações dependem do cadastro completo e de um motorista ativo.</p></section>
}
