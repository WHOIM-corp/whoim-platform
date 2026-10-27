import { Check, CheckCircle2, X } from 'lucide-react'
import { useState } from 'react'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { RejectDialog } from '@/features/card/components/RejectDialog/RejectDialog.tsx'
import type { CardActionsState } from '@/features/card/hooks/useCardActions.ts'
import type { CardDetail } from '@/features/card/model/cardTypes.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import styles from '@/features/card/components/CardActions/CardActions.module.scss'

type CardActionsProps = {
  card: CardDetail
  actions: CardActionsState
}

// 승인자는 검수 대기 Card에 반려·승인, 전임자는 초안에 작성 완료를 한다.
// 반려 대화상자는 상태 분기 밖에 두어, 409 뒤 다시 불러온 Card 상태가 바뀌어도 닫히지 않고 서버 메시지를 보여준다.
export function CardActions({ card, actions }: CardActionsProps) {
  const position = useAuthStore((state) => state.member?.position ?? null)
  const [rejecting, setRejecting] = useState(false)

  function openReject() {
    actions.resetReject()
    setRejecting(true)
  }

  function closeReject() {
    actions.resetReject()
    setRejecting(false)
  }

  let buttons = null
  if (position === 'REVIEWER' && card.status === 'PREDECESSOR_CONFIRMED') {
    buttons = (
      <div className={styles.actions}>
        <Button variant="danger" disabled={actions.isPending} onClick={openReject}>
          <X size={16} aria-hidden="true" />
          반려
        </Button>
        <Button variant="primary" loading={actions.isPending} onClick={actions.approve}>
          {!actions.isPending && <Check size={16} aria-hidden="true" />}
          승인
        </Button>
      </div>
    )
  } else if (position === 'PREDECESSOR' && card.status === 'DRAFT') {
    buttons = (
      <div className={styles.actions}>
        <Button variant="primary" loading={actions.isPending} onClick={actions.confirm}>
          {!actions.isPending && <CheckCircle2 size={16} aria-hidden="true" />}
          작성 완료
        </Button>
      </div>
    )
  }

  return (
    <>
      {buttons}
      {rejecting && (
        <RejectDialog
          card={card}
          isPending={actions.isRejectPending}
          error={actions.rejectError}
          onSubmit={(reason) => actions.reject(reason, () => setRejecting(false))}
          onClose={closeReject}
        />
      )}
    </>
  )
}
