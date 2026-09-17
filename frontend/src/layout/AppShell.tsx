import { NavLink, Outlet } from 'react-router-dom'
import { useAuth } from '../auth/AuthContext'
import type { Role } from '../auth/types'
import { useQuery } from '@tanstack/react-query'
import { unreadCount } from '../features/notifications/api'
import { lazy, Suspense, useState } from 'react'

const NotificationBell = lazy(() => import('../components/NotificationBell'))

type NavigationIcon = 'today' | 'agenda' | 'route' | 'students' | 'trips' | 'profile' | 'summary' | 'drivers' | 'institutions'
type NavigationItem = { to: string; label: string; symbol: string; icon?: NavigationIcon }
const navigation: Record<Role, NavigationItem[]> = {
  STUDENT: [{ to: '/', label: 'Hoje', symbol: '', icon: 'today' }, { to: '/rotas', label: 'Rotas', symbol: '', icon: 'route' }, { to: '/agenda', label: 'Agenda', symbol: '', icon: 'agenda' }, { to: '/viagens', label: 'Viagens', symbol: '', icon: 'trips' }, { to: '/meu-cadastro', label: 'Meu cadastro', symbol: '', icon: 'profile' }],
  DRIVER: [{ to: '/', label: 'Hoje', symbol: '', icon: 'today' }, { to: '/rotas', label: 'Rotas', symbol: '', icon: 'route' }, { to: '/alunos', label: 'Alunos', symbol: '', icon: 'students' }, { to: '/viagens', label: 'Viagens', symbol: '', icon: 'trips' }, { to: '/meu-cadastro', label: 'Meu cadastro', symbol: '', icon: 'profile' }],
  ADMIN: [{ to: '/', label: 'Resumo', symbol: '', icon: 'summary' }, { to: '/motoristas', label: 'Motoristas', symbol: '', icon: 'drivers' }, { to: '/alunos-admin', label: 'Alunos', symbol: '', icon: 'students' }, { to: '/instituicoes', label: 'Instituições', symbol: '', icon: 'institutions' }, { to: '/conta', label: 'Minha conta', symbol: '', icon: 'profile' }],
}

export function AppShell() {
  const { session, logout } = useAuth()
  const [pulsingIcon, setPulsingIcon] = useState<NavigationIcon | null>(null)
  const unread = useQuery({ queryKey: ['unread-notifications'], queryFn: unreadCount, refetchInterval: 15_000, enabled: Boolean(session) })
  if (!session) return null
  const items = navigation[session.role]
  function playNavigationPulse(icon: NavigationIcon) {
    setPulsingIcon(null)
    window.requestAnimationFrame(() => setPulsingIcon(icon))
    window.setTimeout(() => setPulsingIcon(null), 820)
  }
  return (
    <div className="app-shell">
      <header className="topbar">
        <NavLink to="/" className="brand"><img src="/transmoovi-logo.png" alt="" /><span>transmoovi</span></NavLink>
        <nav className="desktop-nav" aria-label="Navegação principal">{items.map((item) => <NavigationLink key={item.to} item={item} pulsingIcon={pulsingIcon} onNavigationClick={playNavigationPulse} />)}</nav>
        <div className="topbar-actions">
          <NavLink to="/notificacoes" className="icon-button notification-link" aria-label={`${unread.data?.count ?? 0} notificações não lidas`}><Suspense fallback={<span className="notification-placeholder" />}><NotificationBell unread={Boolean(unread.data?.count)} /></Suspense>{Boolean(unread.data?.count) && <b>{unread.data?.count}</b>}</NavLink>
          <button className="text-button" onClick={() => void logout()}>Sair</button>
        </div>
      </header>
      <main className="app-content"><Outlet /></main>
      <nav className="bottom-nav" aria-label="Navegação principal">{items.map((item) => <NavigationLink key={item.to} item={item} pulsingIcon={pulsingIcon} onNavigationClick={playNavigationPulse} />)}</nav>
    </div>
  )
}

function NavigationLink({ item, pulsingIcon, onNavigationClick }: { item: NavigationItem; pulsingIcon: NavigationIcon | null; onNavigationClick: (icon: NavigationIcon) => void }) {
  return <NavLink to={item.to} end={item.to === '/'} onClick={item.icon ? () => onNavigationClick(item.icon!) : undefined}>{item.icon
    ? <span className={`nav-icon nav-icon--${item.icon}${pulsingIcon === item.icon ? ' is-pulsing' : ''}`} aria-hidden="true"><span className="nav-icon-art" /><span className="nav-icon-pulse" /></span>
    : <span aria-hidden="true">{item.symbol}</span>}{item.label}</NavLink>
}
