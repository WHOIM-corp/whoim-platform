import type { ConsultAnswer } from '@/features/consult/model/consultTypes.ts'

// 서버가 빈 답변을 400으로 거절하므로, 비어 있는 답변은 질문째 빼고 보낸다.
export function nonBlankAnswers(questions: string[], answers: string[]): ConsultAnswer[] {
  return questions.flatMap((question, index) => {
    const answer = answers[index]?.trim() ?? ''
    return answer === '' ? [] : [{ question, answer }]
  })
}
