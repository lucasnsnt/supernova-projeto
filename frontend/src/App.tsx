import { QueryClient, QueryClientProvider } from '@tanstack/react-query'
import { BrowserRouter, Navigate, Route, Routes } from 'react-router-dom'
import { AuthProvider } from './auth/AuthContext'
import { ProtectedRoute } from './auth/ProtectedRoute'
import { AppShell } from './layout/AppShell'
import { HomePage } from './pages/HomePage'
import { LoginPage } from './pages/LoginPage'
import { RegisterPage } from './pages/RegisterPage'
import { SchedulePage } from './features/student/SchedulePage'
import { TripsPage } from './pages/TripsPage'
import { StudentsPage } from './features/driver/StudentsPage'
import { NotificationsPage } from './features/notifications/NotificationsPage'
import { DriversPage } from './features/admin/DriversPage'
import { InstitutionsPage } from './features/admin/InstitutionsPage'

const queryClient = new QueryClient({ defaultOptions: { queries: { retry: 1, staleTime: 30_000 } } })

export default function App() {
  return <QueryClientProvider client={queryClient}><BrowserRouter><AuthProvider><Routes>
    <Route path="/login" element={<LoginPage />} />
    <Route path="/cadastro" element={<RegisterPage />} />
    <Route element={<ProtectedRoute />}><Route element={<AppShell />}>
      <Route index element={<HomePage />} />
      <Route path="agenda" element={<SchedulePage />} />
      <Route path="viagens" element={<TripsPage />} />
      <Route path="alunos" element={<StudentsPage />} />
      <Route path="motoristas" element={<DriversPage />} />
      <Route path="instituicoes" element={<InstitutionsPage />} />
      <Route path="notificacoes" element={<NotificationsPage />} />
    </Route></Route>
    <Route path="*" element={<Navigate to="/" replace />} />
  </Routes></AuthProvider></BrowserRouter></QueryClientProvider>
}
