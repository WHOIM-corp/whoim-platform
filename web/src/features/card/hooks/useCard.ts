import { getCard } from '@/features/card/api/cardApi.ts'
import { cardKeys } from '@/features/card/model/cardKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useCard(id: string) {
  return useApiQuery({ queryKey: cardKeys.detail(id), queryFn: () => getCard(id) })
}
