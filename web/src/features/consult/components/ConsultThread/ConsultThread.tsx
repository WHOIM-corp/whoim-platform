import { SearchX } from 'lucide-react'
import { Fragment } from 'react'
import { AiNote } from '@/features/consult/components/AiNote/AiNote.tsx'
import { ConsultCardView } from '@/features/consult/components/ConsultCardView/ConsultCardView.tsx'
import { FollowUpForm } from '@/features/consult/components/FollowUpForm/FollowUpForm.tsx'
import { SituationForm } from '@/features/consult/components/SituationForm/SituationForm.tsx'
import type { ConsultFlow } from '@/features/consult/hooks/useConsultFlow.ts'
import type { ConsultAnswer } from '@/features/consult/model/consultTypes.ts'
import { ErrorNotice } from '@/shared/components/ErrorNotice/ErrorNotice.tsx'
import styles from '@/features/consult/components/ConsultThread/ConsultThread.module.scss'

type ConsultThreadProps = {
  flow: ConsultFlow
  // 안내 문구에 쓰는 로그인한 후임자의 이름이다. 없으면 "본인"으로 쓴다.
  memberName: string | null
}

// 후보는 있었지만 맞는 Card가 없을 때만 쓴다(공개된 Card가 아예 없으면 다시 적어도 같다).
const RETRY_HINT = '상황을 다르게 적어 다시 전달할 수 있습니다.'

function introText(count: number, memberName: string | null): string {
  const owner = memberName === null ? '본인의 ' : `${memberName}님의 `
  return `말씀하신 상황과 가까운 전임자의 판단 ${count}건을 찾았습니다. 아래는 지시가 아니라 전임자가 당시 내린 판단과 근거입니다. 최종 판단은 ${owner}몫입니다.`
}

function answersRecap(answers: ConsultAnswer[]): string {
  if (answers.length === 0) {
    return '추가 답변 없음'
  }
  return answers.map(({ question, answer }) => `${question} → ${answer}`).join('\n')
}

function MeBubble({ text }: { text: string }) {
  return (
    <div className={styles.rowMe}>
      <div className={styles.me}>{text}</div>
    </div>
  )
}

// 상담 단계가 위에서 아래로 쌓이는 화면이다. 상태는 useConsultFlow가 가진다.
export function ConsultThread({ flow, memberName }: ConsultThreadProps) {
  const { step, error, isPending } = flow

  function renderError() {
    if (error === null) {
      return null
    }
    if (error.status === 503) {
      return (
        <ErrorNotice
          title="상담을 이어가지 못했습니다"
          message="AI 응답이 지연되고 있습니다. 같은 내용으로 다시 시도할 수 있습니다."
          actionLabel="다시 시도"
          onAction={flow.retry}
        />
      )
    }
    return (
      <ErrorNotice
        title="상담을 이어가지 못했습니다"
        message={error.message}
        actionLabel="입력 고치기"
        onAction={flow.editAgain}
      />
    )
  }

  function renderNoResult(message: string | null, hint: string | null) {
    return (
      <section className={styles.empty}>
        <SearchX size={20} aria-hidden="true" />
        <div>
          <p className={styles.emptyTitle}>{message ?? '참고할 수 있는 공개된 Card가 없습니다'}</p>
          {hint !== null && <p className={styles.emptyHint}>{hint}</p>}
        </div>
      </section>
    )
  }

  function renderResult() {
    const suggestions = flow.suggestions
    if (suggestions === null) {
      return null
    }
    if (suggestions.cards.length === 0) {
      return renderNoResult(suggestions.message, RETRY_HINT)
    }
    return (
      <>
        <p className={styles.intro}>{introText(suggestions.cards.length, memberName)}</p>
        {suggestions.cards.map((card) => (
          <Fragment key={card.id}>
            <AiNote text={card.explanation} />
            <ConsultCardView card={card} />
          </Fragment>
        ))}
      </>
    )
  }

  const waiting = isPending && error === null

  return (
    <div className={styles.thread} aria-live="polite">
      {step === 'situation' ? (
        <SituationForm
          value={flow.situationDraft}
          disabled={isPending}
          onChange={flow.changeSituation}
          onSubmit={flow.submitSituation}
        />
      ) : (
        flow.submittedSituation !== null && <MeBubble text={flow.submittedSituation} />
      )}
      {step === 'noCandidate' && renderNoResult(flow.noCandidateMessage, null)}
      {step === 'followUp' && (
        <FollowUpForm
          questions={flow.questions}
          answers={flow.answerDrafts}
          disabled={isPending}
          onChangeAnswer={flow.changeAnswer}
          onSubmit={flow.submitAnswers}
        />
      )}
      {flow.submittedAnswers !== null && <MeBubble text={answersRecap(flow.submittedAnswers)} />}
      {waiting && (
        <div className={styles.ai} role="status">
          <span className={styles.dots} aria-hidden="true">
            <i />
            <i />
            <i />
          </span>
          <span className={styles.thinking}>
            {step === 'asking' ? '상황을 살펴보는 중' : '전임자의 판단을 찾는 중'}
          </span>
        </div>
      )}
      {!isPending && renderError()}
      {step === 'result' && renderResult()}
    </div>
  )
}
