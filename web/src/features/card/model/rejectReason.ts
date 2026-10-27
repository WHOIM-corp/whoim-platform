// 서버 CardRejectRequest(@NotBlank, @Size(max = 500))와 같은 규칙이다.
export const REJECT_REASON_MAX_LENGTH = 500

export function rejectReasonError(reason: string): string | null {
  const trimmed = reason.trim()
  if (trimmed === '') {
    return '반려 사유를 입력해 주세요.'
  }
  if (trimmed.length > REJECT_REASON_MAX_LENGTH) {
    return `반려 사유는 ${REJECT_REASON_MAX_LENGTH}자 이하로 입력해 주세요.`
  }
  return null
}
