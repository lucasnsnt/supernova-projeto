import { useAuth } from '../auth/AuthContext'
import { StudentTodayPage } from '../features/student/StudentTodayPage'
import { DriverTodayPage } from '../features/driver/DriverTodayPage'
import { AdminOverviewPage } from '../features/admin/AdminOverviewPage'

export function HomePage() {
  const { session } = useAuth()
  if (!session) return null
  if (session.role === 'STUDENT') return <StudentTodayPage />
  if (session.role === 'DRIVER') return <DriverTodayPage />
  return <AdminOverviewPage />
}
