import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { CardQueueView } from '@/features/card/components/CardQueueView/CardQueueView.tsx'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { ForbiddenPage } from '@/features/system/pages/ForbiddenPage.tsx'

export function QueuePage() {
  const position = useAuthStore((state) => state.member?.position ?? null)
  const queue = position === null ? undefined : CARD_QUEUE_BY_POSITION[position]
  return queue === undefined ? <ForbiddenPage /> : <CardQueueView queue={queue} />
}
