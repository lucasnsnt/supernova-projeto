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

export type RegisterPayload = {
  registrationToken: string
  name: string
  email: string
  password: string
  phone: string
  dateOfBirth: string
  role: Exclude<Role, 'ADMIN'>
  address: {
    street: string
    number: string
    complement?: string
    neighborhood: string
    city: string
    state: string
    zipCode: string
  }
  cnh?: string
  driverInviteToken?: string
}
