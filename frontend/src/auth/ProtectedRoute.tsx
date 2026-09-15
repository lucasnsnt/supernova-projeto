import { Navigate, Outlet, useLocation } from 'react-router-dom'
import { useAuth } from './AuthContext'

export function ProtectedRoute() {
  const { session, loading } = useAuth()
  const location = useLocation()
  if (loading) return <div className="full-state">Carregando sua sessão…</div>
  if (!session) return <Navigate to="/login" replace state={{ from: location }} />
  return <Outlet />
}
