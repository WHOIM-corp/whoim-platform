import { useState } from 'react'
import { generatePath, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { Gap } from '@/features/gap/model/gapTypes.ts'
import { useCreateCardFromInterview } from '@/features/interview/hooks/useCreateCardFromInterview.ts'
import { useInterview } from '@/features/interview/hooks/useInterview.ts'
import { useRetryInterview } from '@/features/interview/hooks/useRetryInterview.ts'
import { useSubmitAnswer } from '@/features/interview/hooks/useSubmitAnswer.ts'
import type { Interview } from '@/features/interview/model/interviewTypes.ts'
import { hasUnprocessedAnswer } from '@/features/interview/model/traceFill.ts'
import type { ApiError } from '@/shared/api/apiError.ts'

// 서버 InterviewAnswerRequest.MAX_ANSWER_LENGTH와 같은 값이다.
export const INTERVIEW_ANSWER_MAX_LENGTH = 4000
export const UNKNOWN_ANSWER = '모르겠습니다'

export type InterviewSession = {
  interview: Interview | undefined
  isLoading: boolean
  loadError: ApiError | null
  reload: () => void
  draft: string
  setDraft: (draft: string) => void
  // 보낸 뒤 응답을 기다리는 답변이다. 응답이 오면 서버 turns에 들어가 사라진다.
  pendingAnswer: string | null
  sendDraft: () => void
  sendUnknown: () => void
  retry: () => void
  createCard: () => void
  openCard: (cardId: string) => void
  isThinking: boolean
  isCreatingCard: boolean
  // 답변이 저장됐지만 AI가 처리하지 못했다. 새로 고침 뒤에도 서버 데이터로 이 상태를 알아낸다.
  needsRetry: boolean
  // 재시도 안내와 별개로 보여줄 답변·재시도 오류(409 등)다.
  answerError: ApiError | null
  cardError: ApiError | null
}

function isLlmUnavailable(error: ApiError | null): boolean {
  return error?.status === 503
}

// 인터뷰 한 건의 답변·재시도·Card 만들기를 묶는다. 오류는 화면 안에 보이고 409는 낡은 데이터를 다시 불러온다.
export function useInterviewSession(
  gap: Gap,
  interviewId: string,
  onGapsStale: () => void,
): InterviewSession {
  const navigate = useNavigate()
  const interviewQuery = useInterview(interviewId)
  const submitAnswer = useSubmitAnswer()
  const retryInterview = useRetryInterview()
  const createCardFromInterview = useCreateCardFromInterview()
  const [draft, setDraft] = useState('')
  const [pendingAnswer, setPendingAnswer] = useState<string | null>(null)

  const interview = interviewQuery.data
  const needsRetry =
    (interview !== undefined && hasUnprocessedAnswer(interview)) ||
    isLlmUnavailable(submitAnswer.error)

  function refreshOnConflict(error: ApiError) {
    if (error.status === 409) {
      interviewQuery.refetch()
      onGapsStale()
    }
  }

  function send(answer: string, restoreDraft: string) {
    const trimmed = answer.trim()
    if (trimmed === '' || trimmed.length > INTERVIEW_ANSWER_MAX_LENGTH) {
      return
    }
    retryInterview.reset()
    setPendingAnswer(trimmed)
    submitAnswer.submit(
      { id: interviewId, answer: trimmed },
      {
        onSuccess: () => setPendingAnswer(null),
        onError: (error) => {
          setPendingAnswer(null)
          if (isLlmUnavailable(error)) {
            // 답변은 서버에 저장됐다. 입력은 비우고, 저장된 답변이 보이도록 다시 불러온다.
            interviewQuery.refetch()
            return
          }
          setDraft(restoreDraft)
          refreshOnConflict(error)
        },
      },
    )
  }

  function sendDraft() {
    const previousDraft = draft
    if (previousDraft.trim() === '') {
      return
    }
    setDraft('')
    send(previousDraft, previousDraft)
  }

  function retry() {
    retryInterview.submit(interviewId, {
      onSuccess: () => {
        submitAnswer.reset()
        onGapsStale()
      },
      onError: (error) => {
        // 409면 재처리할 답변이 없다. 남은 503 표시를 지우고 서버 상태를 다시 받는다.
        if (error.status === 409) {
          submitAnswer.reset()
        }
        refreshOnConflict(error)
      },
    })
  }

  function openCard(cardId: string) {
    navigate(generatePath(ROUTE_PATHS.cardDetail, { cardId }))
  }

  function createCard() {
    createCardFromInterview.submit(interviewId, {
      onSuccess: (card) => openCard(card.id),
      // 이미 Card가 있으면 Gap 목록을 다시 받아 cardId를 얻는다.
      onError: (error) => {
        if (error.status === 409) {
          onGapsStale()
        }
      },
    })
  }

  const retryError = retryInterview.error
  const submitError = isLlmUnavailable(submitAnswer.error) ? null : submitAnswer.error

  return {
    interview,
    isLoading: interviewQuery.isLoading,
    loadError: interviewQuery.error,
    reload: interviewQuery.refetch,
    draft,
    setDraft,
    pendingAnswer,
    sendDraft,
    sendUnknown: () => send(UNKNOWN_ANSWER, draft),
    retry,
    createCard,
    openCard,
    isThinking: submitAnswer.isPending || retryInterview.isPending,
    isCreatingCard: createCardFromInterview.isPending,
    needsRetry,
    answerError: isLlmUnavailable(retryError) ? null : (retryError ?? submitError),
    cardError: gap.cardId === null ? createCardFromInterview.error : null,
  }
}
