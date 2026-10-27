import { useState } from 'react'
import { useConsultQuestions } from '@/features/consult/hooks/useConsultQuestions.ts'
import { useConsultSuggestions } from '@/features/consult/hooks/useConsultSuggestions.ts'
import { nonBlankAnswers } from '@/features/consult/model/consultAnswers.ts'
import type {
  ConsultAnswer,
  ConsultQuestions,
  ConsultSuggestions,
} from '@/features/consult/model/consultTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'

// situation: 상황 입력, asking: 추가 질문 대기, noCandidate: 공개된 Card 없음,
// followUp: 추가 질문 답변, suggesting: 결과 대기, result: 결과
export type ConsultStep =
  'situation' | 'asking' | 'noCandidate' | 'followUp' | 'suggesting' | 'result'

export type ConsultFlow = {
  step: ConsultStep
  situationDraft: string
  submittedSituation: string | null
  questions: string[]
  answerDrafts: string[]
  submittedAnswers: ConsultAnswer[] | null
  noCandidateMessage: string | null
  suggestions: ConsultSuggestions | null
  isPending: boolean
  error: ApiError | null
  changeSituation: (value: string) => void
  submitSituation: () => void
  changeAnswer: (index: number, value: string) => void
  submitAnswers: () => void
  retry: () => void
  editAgain: () => void
  reset: () => void
}

function stepOf(
  submittedSituation: string | null,
  questionsResult: ConsultQuestions | null,
  submittedAnswers: ConsultAnswer[] | null,
  suggestions: ConsultSuggestions | null,
): ConsultStep {
  if (submittedSituation === null) {
    return 'situation'
  }
  if (questionsResult === null) {
    return 'asking'
  }
  // 질문이 비고 안내가 있으면 후보 Card가 없어 AI를 부르지 않은 경우다.
  if (questionsResult.questions.length === 0 && questionsResult.message !== null) {
    return 'noCandidate'
  }
  if (submittedAnswers === null) {
    return 'followUp'
  }
  return suggestions === null ? 'suggesting' : 'result'
}

// 상담 한 번의 단계 상태다. 서버는 아무것도 저장하지 않으므로 화면 상태가 전부다.
export function useConsultFlow(): ConsultFlow {
  const questionsMutation = useConsultQuestions()
  const suggestionsMutation = useConsultSuggestions()
  const [situationDraft, setSituationDraft] = useState('')
  const [submittedSituation, setSubmittedSituation] = useState<string | null>(null)
  const [questionsResult, setQuestionsResult] = useState<ConsultQuestions | null>(null)
  const [answerDrafts, setAnswerDrafts] = useState<string[]>([])
  const [submittedAnswers, setSubmittedAnswers] = useState<ConsultAnswer[] | null>(null)
  const [suggestions, setSuggestions] = useState<ConsultSuggestions | null>(null)

  const step = stepOf(submittedSituation, questionsResult, submittedAnswers, suggestions)
  const isPending = questionsMutation.isPending || suggestionsMutation.isPending
  const error =
    step === 'asking'
      ? questionsMutation.error
      : step === 'suggesting'
        ? suggestionsMutation.error
        : null

  function askQuestions(situation: string) {
    questionsMutation.submit(situation, {
      onSuccess: (data) => {
        setQuestionsResult(data)
        setAnswerDrafts(data.questions.map(() => ''))
      },
    })
  }

  function askSuggestions(situation: string, answers: ConsultAnswer[]) {
    suggestionsMutation.submit({ situation, answers }, { onSuccess: setSuggestions })
  }

  function submitSituation() {
    const situation = situationDraft.trim()
    if (situation === '' || isPending) {
      return
    }
    setSubmittedSituation(situation)
    askQuestions(situation)
  }

  function changeAnswer(index: number, value: string) {
    setAnswerDrafts((current) => current.map((draft, at) => (at === index ? value : draft)))
  }

  function submitAnswers() {
    if (submittedSituation === null || questionsResult === null || isPending) {
      return
    }
    const answers = nonBlankAnswers(questionsResult.questions, answerDrafts)
    setSubmittedAnswers(answers)
    askSuggestions(submittedSituation, answers)
  }

  // 같은 단계를 같은 입력으로 다시 보낸다(503 LLM_UNAVAILABLE).
  function retry() {
    if (submittedSituation === null || isPending) {
      return
    }
    if (step === 'asking') {
      askQuestions(submittedSituation)
    } else if (step === 'suggesting' && submittedAnswers !== null) {
      askSuggestions(submittedSituation, submittedAnswers)
    }
  }

  // 400·403처럼 다시 보내도 같은 결과인 오류는 입력 단계로 돌아가 고칠 수 있게 한다(입력값은 유지).
  function editAgain() {
    if (isPending) {
      return
    }
    if (step === 'asking') {
      questionsMutation.reset()
      setSubmittedSituation(null)
    } else if (step === 'suggesting') {
      suggestionsMutation.reset()
      setSubmittedAnswers(null)
    }
  }

  function reset() {
    if (isPending) {
      return
    }
    questionsMutation.reset()
    suggestionsMutation.reset()
    setSituationDraft('')
    setSubmittedSituation(null)
    setQuestionsResult(null)
    setAnswerDrafts([])
    setSubmittedAnswers(null)
    setSuggestions(null)
  }

  return {
    step,
    situationDraft,
    submittedSituation,
    questions: questionsResult?.questions ?? [],
    answerDrafts,
    submittedAnswers,
    noCandidateMessage: step === 'noCandidate' ? (questionsResult?.message ?? null) : null,
    suggestions,
    isPending,
    error,
    changeSituation: setSituationDraft,
    submitSituation,
    changeAnswer,
    submitAnswers,
    retry,
    editAgain,
    reset,
  }
}
