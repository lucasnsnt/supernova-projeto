import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../auth/types'
import { useQuery } from '@tanstack/react-query'
import { unreadCount } from '../features/notifications/api'

type NavigationItem = { to: string; label: string; symbol: string }
const navigation: Record<Role, NavigationItem[]> = {
  STUDENT: [{ to: '/', label: 'Hoje', symbol: '⌂' }, { to: '/agenda', label: 'Agenda', symbol: '▣' }, { to: '/viagens', label: 'Viagens', symbol: '↗' }, { to: '/meu-cadastro', label: 'Meu cadastro', symbol: '◉' }],
  DRIVER: [{ to: '/', label: 'Hoje', symbol: '⌂' }, { to: '/alunos', label: 'Alunos', symbol: '◉' }, { to: '/viagens', label: 'Viagens', symbol: '↗' }, { to: '/meu-cadastro', label: 'Meu cadastro', symbol: '◇' }],
  ADMIN: [{ to: '/', label: 'Resumo', symbol: '⌂' }, { to: '/motoristas', label: 'Motoristas', symbol: '◉' }, { to: '/instituicoes', label: 'Instituições', symbol: '▣' }],
}

export function AppShell() {
  const { session, logout } = useAuth()
  const unread = useQuery({ queryKey: ['unread-notifications'], queryFn: unreadCount, refetchInterval: 15_000, enabled: Boolean(session) })
  if (!session) return null
  const items = navigation[session.role]
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand"><span>S</span> Supernova</NavLink>
        <nav className="desktop-nav" aria-label="Navegação principal">{items.map((item) => <NavigationLink key={item.to} item={item} />)}</nav>
        <div className="topbar-actions">
          <NavLink to="/notificacoes" className="icon-button notification-link" aria-label={`${unread.data?.count ?? 0} notificações não lidas`}>●{Boolean(unread.data?.count) && <b>{unread.data?.count}</b>}</NavLink>
          <button className="text-button" onClick={() => void logout()}>Sair</button>
        </div>
      </header>
      <main className="app-content"><Outlet /></main>
      <nav className="bottom-nav" aria-label="Navegação principal">{items.map((item) => <NavigationLink key={item.to} item={item} />)}</nav>
    </div>
  )
}

function NavigationLink({ item }: { item: NavigationItem }) {
  return <NavLink to={item.to} end={item.to === '/'}><span aria-hidden="true">{item.symbol}</span>{item.label}</NavLink>
}
