import { Globe } from 'lucide-react'
import { usePublishPackage } from '@/features/handover/hooks/usePublishPackage.ts'
import { countCardsWithStatus } from '@/features/handover/model/packageCardCounts.ts'
import type { HandoverPackage } from '@/features/handover/model/handoverTypes.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { Dialog } from '@/shared/components/Dialog/Dialog.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/handover/components/PublishDialog/PublishDialog.module.scss'

type PublishDialogProps = {
  handoverPackage: HandoverPackage
  onClose: () => void
  // 요청이 실패하면 화면의 Package가 낡았을 수 있어 다시 불러온다.
  onFailure: () => void
}

// 열 때 마운트하고 닫을 때 언마운트해 오류 상태가 다음 열기로 넘어가지 않게 한다.
export function PublishDialog({ handoverPackage, onClose, onFailure }: PublishDialogProps) {
  const publish = usePublishPackage()
  const { approvedCount, activeCount } = handoverPackage.progress
  const successorName = handoverPackage.successor?.name ?? '후임자'

  function submit() {
    publish.submit(handoverPackage.id, { onSuccess: onClose, onError: onFailure })
  }

  return (
    <Dialog
      open
      title="Package 공개"
      description={`공개하면 후임자 ${successorName}에게 바로 보이고, 이후에는 수정할 수 없습니다. 일부 Card만 골라 공개할 수는 없습니다.`}
      onClose={onClose}
      dismissible={!publish.isPending}
    >
      <dl className={styles.summary}>
        <div>
          <dt>Package</dt>
          <dd>{handoverPackage.name}</dd>
        </div>
        <div>
          <dt>승인된 Card</dt>
          <dd>
            {approvedCount} / {activeCount}
          </dd>
        </div>
        <div>
          <dt>반려로 제외</dt>
          <dd>{countCardsWithStatus(handoverPackage, 'REJECTED')}장</dd>
        </div>
      </dl>
      {publish.error && <ErrorNotice title="공개하지 못했습니다" message={publish.error.message} />}
      <div className={styles.actions}>
        <Button onClick={onClose} disabled={publish.isPending} autoFocus>
          취소
        </Button>
        <Button variant="primary" onClick={submit} loading={publish.isPending}>
          {!publish.isPending && <Globe size={16} aria-hidden="true" />}
          공개
        </Button>
      </div>
    </Dialog>
  )
}
