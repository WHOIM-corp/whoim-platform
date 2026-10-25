import type { AuthMember, Position, Role } from '@/features/auth/model/authTypes.ts'

function isRole(value: unknown): value is Role {
  return value === 'USER' || value === 'ADMIN'
}

function isPosition(value: unknown): value is Position {
  return value === 'PREDECESSOR' || value === 'SUCCESSOR' || value === 'REVIEWER'
}

export function isAuthMember(value: unknown): value is AuthMember {
  return (
    typeof value === 'object' &&
    value !== null &&
    'memberId' in value &&
    'username' in value &&
    'name' in value &&
    'role' in value &&
    'position' in value &&
    typeof value.memberId === 'string' &&
    typeof value.username === 'string' &&
    typeof value.name === 'string' &&
    isRole(value.role) &&
    (value.position === null || isPosition(value.position))
  )
}

export function isPersistedAuth(
  value: unknown,
): value is { accessToken: string; member: AuthMember } {
  return (
    typeof value === 'object' &&
    value !== null &&
    'accessToken' in value &&
    'member' in value &&
    typeof value.accessToken === 'string' &&
    value.accessToken !== '' &&
    isAuthMember(value.member)
  )
}
