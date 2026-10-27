import { useApproveCard } from '@/features/card/hooks/useApproveCard.ts'
import { useConfirmCard } from '@/features/card/hooks/useConfirmCard.ts'
import { useInvalidateCardRelated } from '@/features/card/hooks/useInvalidateCardRelated.ts'
import { useRejectCard } from '@/features/card/hooks/useRejectCard.ts'
import type { ApiError } from '@/shared/api/apiError.ts'

export type CardActionsState = {
  approve: () => void
  confirm: () => void
  reject: (reason: string, onSuccess: () => void) => void
  // 승인·작성 완료·반려 중 하나라도 진행 중이면 true다. 카드 아래 버튼이 모두 이것을 따른다.
  isPending: boolean
  isRejectPending: boolean
  // 승인·작성 완료 실패는 카드 위, 반려 실패는 반려 대화상자 안에 보여준다.
  error: ApiError | null
  rejectError: ApiError | null
  resetReject: () => void
}

// 승인·작성 완료는 확인 없이 바로 보낸다. 실패하면 화면 데이터가 낡았을 수 있어 Card·Package·Gap을 다시 불러온다.
export function useCardActions(cardId: string): CardActionsState {
  const approveMutation = useApproveCard()
  const confirmMutation = useConfirmCard()
  const rejectMutation = useRejectCard()
  const invalidateCardRelated = useInvalidateCardRelated()

  function refresh() {
    void invalidateCardRelated()
  }

  return {
    approve: () => {
      confirmMutation.reset()
      approveMutation.submit(cardId, { onError: refresh })
    },
    confirm: () => {
      approveMutation.reset()
      confirmMutation.submit(cardId, { onError: refresh })
    },
    reject: (reason, onSuccess) => {
      approveMutation.reset()
      confirmMutation.reset()
      rejectMutation.submit({ id: cardId, reason }, { onSuccess, onError: refresh })
    },
    isPending: approveMutation.isPending || confirmMutation.isPending || rejectMutation.isPending,
    isRejectPending: rejectMutation.isPending,
    error: approveMutation.error ?? confirmMutation.error,
    rejectError: rejectMutation.error,
    resetReject: rejectMutation.reset,
  }
}
