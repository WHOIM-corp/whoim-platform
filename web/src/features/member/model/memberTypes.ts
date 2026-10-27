import type { Position } from '@/features/auth/model/authTypes.ts'

export type MemberSearchResult = {
  memberId: string
  name: string
  employeeNumber: string
  department: string
  jobTitle: string
  position: Position
}
