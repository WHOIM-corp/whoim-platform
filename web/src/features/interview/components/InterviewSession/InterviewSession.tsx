import { gapTypeLabels } from '@/features/gap/model/gapLabels.ts'
import type { Gap } from '@/features/gap/model/gapTypes.ts'
import { ChatLog } from '@/features/interview/components/ChatLog/ChatLog.tsx'
import { Composer } from '@/features/interview/components/Composer/Composer.tsx'
import { GapEvidence } from '@/features/interview/components/GapEvidence/GapEvidence.tsx'
import { InterviewDone } from '@/features/interview/components/InterviewDone/InterviewDone.tsx'
import { InterviewProgress } from '@/features/interview/components/InterviewProgress/InterviewProgress.tsx'
import { TraceFillPanel } from '@/features/interview/components/TraceFillPanel/TraceFillPanel.tsx'
import {
  INTERVIEW_ANSWER_MAX_LENGTH,
  useInterviewSession,
} from '@/features/interview/hooks/useInterviewSession.ts'
import { answeredTurnCount } from '@/features/interview/model/traceFill.ts'
import { QueryErrorView } from '@/features/system/components/QueryErrorView/QueryErrorView.tsx'
import { CardSkeletonGrid } from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.tsx'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import { GlassPanel } from '@/shared/components/GlassPanel/GlassPanel.tsx'
import styles from '@/features/interview/components/InterviewSession/InterviewSession.module.scss'

type InterviewSessionProps = {
  gap: Gap
  interviewId: string
  onGapsStale: () => void
}

// 대화(좌)와 이번 Gap의 근거·TRACE 채움 현황(우)을 배치한다. 동작은 useInterviewSession이 맡는다.
export function InterviewSession({ gap, interviewId, onGapsStale }: InterviewSessionProps) {
  const session = useInterviewSession(gap, interviewId, onGapsStale)
  const { interview } = session

  if (session.isLoading) {
    return <CardSkeletonGrid count={1} />
  }
  if (session.loadError !== null) {
    return (
      <QueryErrorView
        error={session.loadError}
        title="인터뷰를 불러오지 못했습니다"
        onRetry={session.reload}
      />
    )
  }
  if (interview === undefined) {
    return null
  }

  const isCompleted = interview.status === 'COMPLETED'
  const showRetry = !isCompleted && session.needsRetry && !session.isThinking

  return (
    <div className={styles.stage}>
      <div className={styles.chat}>
        <InterviewProgress answeredCount={answeredTurnCount(interview)} isCompleted={isCompleted} />
        <ChatLog
          turns={interview.turns}
          pendingAnswer={session.pendingAnswer}
          isThinking={session.isThinking || session.isCreatingCard}
        />
        {showRetry && (
          <div className={styles.notice}>
            <ErrorNotice
              title="AI가 응답하지 못했습니다"
              message="답변은 저장되었습니다. 다시 시도하면 같은 답변으로 이어갑니다."
              actionLabel="다시 시도"
              onAction={session.retry}
            />
          </div>
        )}
        {session.answerError !== null && (
          <div className={styles.notice}>
            <ErrorNotice title="답변을 처리하지 못했습니다" message={session.answerError.message} />
          </div>
        )}
        {isCompleted ? (
          <InterviewDone
            endReason={interview.endReason}
            cardId={gap.cardId}
            isCreating={session.isCreatingCard}
            error={session.cardError}
            onCreateCard={session.createCard}
            onOpenCard={session.openCard}
          />
        ) : (
          <Composer
            value={session.draft}
            maxLength={INTERVIEW_ANSWER_MAX_LENGTH}
            disabled={session.isThinking || session.needsRetry}
            onChange={session.setDraft}
            onSubmit={session.sendDraft}
            onUnknown={session.sendUnknown}
          />
        )}
      </div>
      <GlassPanel title="이번 Gap" subtitle={gapTypeLabels[gap.type]}>
        <GapEvidence anchors={gap.anchors} />
        <TraceFillPanel interview={interview} />
      </GlassPanel>
    </div>
  )
}
