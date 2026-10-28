import { FileText } from 'lucide-react'
import { generatePath, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { GapProgressBadge } from '@/features/gap/components/GapProgressBadge/GapProgressBadge.tsx'
import { gapTypeLabels } from '@/features/gap/model/gapLabels.ts'
import type { Gap } from '@/features/gap/model/gapTypes.ts'
import { useStartInterview } from '@/features/interview/hooks/useStartInterview.ts'
import { Button } from '@/shared/components/Button/Button.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/gap/components/GapCard/GapCard.module.scss'

type GapCardProps = {
  packageId: string
  gap: Gap
  // 서버와 화면의 Gap 상태가 어긋났을 때(409) 목록을 다시 불러온다.
  onStale: () => void
}

export function GapCard({ packageId, gap, onStale }: GapCardProps) {
  const navigate = useNavigate()
  const startInterview = useStartInterview()
  const { cardId } = gap
  const isRejected = gap.progress === 'REJECTED'
  const isFinishedWithoutCard = (gap.progress === 'COMPLETED' || gap.progress === 'PARTIAL') && cardId === null
  const interviewPath = generatePath(ROUTE_PATHS.packageGapInterview, { packageId, gapId: gap.id })

  function start() {
    startInterview.submit(gap.id, {
      onSuccess: () => navigate(interviewPath),
      onError: (error) => {
        if (error.status === 409) {
          onStale()
          startInterview.reset()
        }
      },
    })
  }

  function renderNote() {
    if (isRejected) {
      return <p className={styles.note}>반려된 판단이라 다시 인터뷰할 수 없습니다</p>
    }
    if (gap.progress === 'PARTIAL') {
      return (
        <p className={styles.note}>
          {cardId === null ? '일부 항목이 비어 있습니다' : '일부 항목이 비어 있는 채로 확인했습니다'}
        </p>
      )
    }
    if (startInterview.isPending) {
      return <p className={styles.note}>생각 중입니다. 첫 질문을 준비하고 있습니다.</p>
    }
    return null
  }

  function renderActions() {
    const actions = []
    if (cardId !== null) {
      actions.push(
        <Button key="card" onClick={() => navigate(generatePath(ROUTE_PATHS.cardDetail, { cardId }))}>
          Card 보기
        </Button>,
      )
    }
    if (gap.interviewId === null && !isRejected) {
      actions.push(
        <Button key="start" variant="primary" loading={startInterview.isPending} onClick={start}>
          인터뷰 시작
        </Button>,
      )
    } else if (gap.interviewId !== null && cardId === null && !isRejected) {
      actions.push(
        <Button key="resume" variant="primary" onClick={() => navigate(interviewPath)}>
          {isFinishedWithoutCard ? 'Card 만들기' : '이어서 답변'}
        </Button>,
      )
    }
    return actions
  }

  const location = gap.anchors[0]?.fileName

  return (
    <article className={styles.card}>
      <div className={styles.face}>
        <div className={styles.head}>
          <span className={styles.type}>{gapTypeLabels[gap.type]}</span>
          <GapProgressBadge progress={gap.progress} />
        </div>
        <div className={styles.body}>
          <h2 className={styles.title}>{gap.summary}</h2>
          <p className={styles.question}>
            <b>AI가 준비한 질문</b>
            {gap.question}
          </p>
          {renderNote()}
          {startInterview.error !== null && (
            <ErrorNotice
              compact
              message={startInterview.error.message}
              actionLabel={startInterview.error.status === 503 ? '다시 시도' : undefined}
              onAction={start}
            />
          )}
        </div>
        <div className={styles.foot}>
          {location !== undefined && (
            <span className={styles.location} title={location}>
              <FileText size={12} aria-hidden="true" />
              <span className={styles.locationText}>{location}</span>
            </span>
          )}
          <span className={styles.actions}>{renderActions()}</span>
        </div>
      </div>
    </article>
  )
}
