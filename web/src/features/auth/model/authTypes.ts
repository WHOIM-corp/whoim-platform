export type Position = 'PREDECESSOR' | 'SUCCESSOR' | 'REVIEWER'
export type Role = 'USER' | 'ADMIN'

export type AuthMember = {
  memberId: string
  username: string
  name: string
  role: Role
  position: Position | null
}

export type LoginRequest = { username: string; password: string }
export type LoginResponse = { accessToken: string; member: AuthMember }

export const positionLabels: Record<Position, string> = {
  PREDECESSOR: '전임자',
  SUCCESSOR: '후임자',
  REVIEWER: '승인자',
}
