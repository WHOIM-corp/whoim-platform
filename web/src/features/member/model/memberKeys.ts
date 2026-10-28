import type { Position } from '@/features/auth/model/authTypes.ts'

export const memberKeys = {
  search: (position: Position, keyword: string) => ['members', 'search', position, keyword] as const,
}
