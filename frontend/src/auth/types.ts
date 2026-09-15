export type Role = 'STUDENT' | 'DRIVER' | 'ADMIN'

export type AuthSession = {
  accessToken: string
  tokenType: 'Bearer'
  accessTokenExpiresAt: string
  userId: number
  email: string
  role: Role
  driverStatus: 'PENDING' | 'APPROVED' | 'REJECTED' | 'SUSPENDED' | null
  profileComplete: boolean
}

export type Account = {
  id: number
  name: string
  email: string
  role: Role
  driverStatus: AuthSession['driverStatus']
}
