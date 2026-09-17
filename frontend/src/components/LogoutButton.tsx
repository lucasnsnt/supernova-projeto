import { useAuth } from '../auth/AuthContext'

export function LogoutButton() {
  const { logout } = useAuth()
  return <button type="button" className="logout-button" onClick={() => void logout()}>Sair</button>
}
