import { useAuth } from '../auth/AuthContext'

const copy = {
  STUDENT: { eyebrow: 'Seu transporte hoje', title: 'Tudo certo por enquanto', description: 'Quando houver uma confirmação ou viagem, ela aparecerá aqui.' },
  DRIVER: { eyebrow: 'Sua operação hoje', title: 'Visão do dia', description: 'As respostas dos alunos e as próximas viagens aparecerão aqui.' },
  ADMIN: { eyebrow: 'Administração', title: 'Resumo da operação', description: 'Acompanhe motoristas pendentes e instituições cadastradas.' },
}

export function HomePage() {
  const { session } = useAuth()
  if (!session) return null
  const content = copy[session.role]
  return <div className="page-stack"><header className="page-heading"><p className="eyebrow">{content.eyebrow}</p><h1>{content.title}</h1><p className="muted">{content.description}</p></header><section className="status-card"><div className="status-icon" aria-hidden="true">✓</div><div><h2>Nenhuma ação pendente</h2><p className="muted">Atualizaremos esta tela conforme o dia avançar.</p></div></section></div>
}
