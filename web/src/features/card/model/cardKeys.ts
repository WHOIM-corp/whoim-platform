import type { CardStatus } from '@/features/card/model/cardTypes.ts'

export const cardKeys = {
  all: ['cards'] as const,
  list: (status?: CardStatus) => [...cardKeys.all, 'list', status ?? 'ALL'] as const,
  detail: (id: string) => [...cardKeys.all, 'detail', id] as const,
}
