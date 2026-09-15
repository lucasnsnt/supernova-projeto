import { createContext, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
import { apiFetch, setAccessToken } from '../lib/api'
import type { Account, AuthSession, RegisterPayload } from './types'
import { useQueryClient } from '@tanstack/react-query'

type Credentials = { email: string; password: string }
type AuthContextValue = {
  session: AuthSession | null
  loading: boolean
  login: (credentials: Credentials) => Promise<AuthSession>
  register: (payload: RegisterPayload) => Promise<AuthSession>
  logout: () => Promise<void>
}

const AuthContext = createContext<AuthContextValue | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const queryClient = useQueryClient()
  const [session, setSession] = useState<AuthSession | null>(null)
  const [loading, setLoading] = useState(true)

  useEffect(() => {
    apiFetch<Pick<AuthSession, 'accessToken' | 'tokenType' | 'accessTokenExpiresAt'>>(
      '/api/auth/refresh', { method: 'POST' },
    )
      .then(async (tokens) => {
        setAccessToken(tokens.accessToken)
        const account = await apiFetch<Account>('/api/me')
        return {
          ...tokens,
          userId: account.id,
          email: account.email,
          role: account.role,
          driverStatus: account.driverStatus,
          profileComplete: true,
        } as AuthSession
      })
      .then(setSession)
      .catch(() => setAccessToken(null))
      .finally(() => setLoading(false))
  }, [])

  const value = useMemo<AuthContextValue>(() => ({
    session,
    loading,
    async login(credentials) {
      const authenticated = await apiFetch<AuthSession>('/api/auth/login', {
        method: 'POST', body: JSON.stringify(credentials),
      })
      setAccessToken(authenticated.accessToken)
      queryClient.clear()
      setSession(authenticated)
      return authenticated
    },
    async register(payload) {
      const authenticated = await apiFetch<AuthSession>('/api/auth/register', {
        method: 'POST', body: JSON.stringify(payload),
      })
      setAccessToken(authenticated.accessToken)
      queryClient.clear()
      setSession(authenticated)
      return authenticated
    },
    async logout() {
      try {
        await apiFetch<void>('/api/auth/logout', { method: 'POST' })
      } finally {
        setAccessToken(null)
        queryClient.clear()
        setSession(null)
      }
    },
  }), [loading, session, queryClient])

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>
}

// oxlint-disable-next-line react/only-export-components
export function useAuth() {
  const context = useContext(AuthContext)
  if (!context) throw new Error('useAuth deve ser usado dentro de AuthProvider')
  return context
}
