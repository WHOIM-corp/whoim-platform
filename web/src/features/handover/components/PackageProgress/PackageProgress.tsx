import { countCardsWithStatus } from '@/features/handover/model/packageCardCounts.ts'
import type { HandoverPackage } from '@/features/handover/model/handoverTypes.ts'
import { ProgressBar } from '@/shared/components/ProgressBar/ProgressBar.tsx'
import styles from '@/features/handover/components/PackageProgress/PackageProgress.module.scss'

type PackageProgressProps = {
  handoverPackage: HandoverPackage
  // 공개할 수 없는 이유. 주어지면 진행률 아래에 보여준다.
  blockReason?: string | null
}

// 반려된 Card는 활성 Card에서 빠지므로 진행률의 분모가 아니다.
export function PackageProgress({ handoverPackage, blockReason }: PackageProgressProps) {
  const { approvedCount, activeCount } = handoverPackage.progress
  const rejectedCount = countCardsWithStatus(handoverPackage, 'REJECTED')
  return (
    <div className={styles.progress}>
      <ProgressBar value={approvedCount} max={activeCount} label="승인 / 활성 Card" />
      {(rejectedCount > 0 || blockReason) && (
        <p className={styles.note}>
          {rejectedCount > 0 && <span>반려 {rejectedCount}장 제외</span>}
          {blockReason && <span>{blockReason}</span>}
        </p>
      )}
    </div>
  )
}
