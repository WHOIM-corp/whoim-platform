import { CircleCheck, FileText } from 'lucide-react'
import type { InterviewEndReason } from '@/features/interview/model/interviewTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/interview/components/InterviewDone/InterviewDone.module.scss'

type InterviewDoneProps = {
  endReason: InterviewEndReason | null
  cardId: string | null
  isCreating: boolean
  error: ApiError | null
  onCreateCard: () => void
  onOpenCard: (cardId: string) => void
}

function describe(cardId: string | null, isCreating: boolean): string {
  if (cardId !== null) {
    return 'Card 초안이 만들어졌습니다. 내용을 확인하고 "작성 완료"를 눌러 주세요.'
  }
  if (isCreating) {
    return '생각 중입니다. 답변을 바탕으로 Card 초안을 만들고 있습니다.'
  }
  return '답변을 바탕으로 Card 초안을 만들 수 있습니다.'
}

// 인터뷰 종료 뒤 Card를 만들거나(LLM, 503 가능) 이미 만든 Card로 이동한다.
export function InterviewDone({
  endReason,
  cardId,
  isCreating,
  error,
  onCreateCard,
  onOpenCard,
}: InterviewDoneProps) {
  return (
    <>
      <div className={styles.done}>
        <CircleCheck className={styles.icon} size={24} aria-hidden="true" />
        <div className={styles.body}>
          <b>인터뷰가 끝났습니다</b>
          <p>{describe(cardId, isCreating)}</p>
          {endReason === 'TURN_LIMIT' && <p>일부 항목이 비어 있을 수 있습니다</p>}
        </div>
        {cardId !== null ? (
          <Button className={styles.action} variant="primary" onClick={() => onOpenCard(cardId)}>
            <FileText size={16} aria-hidden="true" />
            Card 확인하기
          </Button>
        ) : (
          <Button
            className={styles.action}
            variant="primary"
            loading={isCreating}
            onClick={onCreateCard}
          >
            {!isCreating && <FileText size={16} aria-hidden="true" />}
            Card 만들기
          </Button>
        )}
      </div>
      {error !== null && (
        <div className={styles.notice}>
          <ErrorNotice
            title={error.status === 503 ? 'AI가 응답하지 못했습니다' : 'Card를 만들지 못했습니다'}
            message={error.message}
            actionLabel={error.status === 503 ? '다시 시도' : undefined}
            onAction={onCreateCard}
          />
        </div>
      )}
    </>
  )
}
