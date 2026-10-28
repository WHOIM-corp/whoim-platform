import { listCards } from '@/features/card/api/cardApi.ts'
import { cardKeys } from '@/features/card/model/cardKeys.ts'
import type { CardStatus } from '@/features/card/model/cardTypes.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useCards(status?: CardStatus) {
  return useApiQuery({ queryKey: cardKeys.list(status), queryFn: () => listCards(status) })
}
