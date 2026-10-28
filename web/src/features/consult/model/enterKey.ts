import type { KeyboardEvent } from 'react'

// 한글 조합 중 Enter(isComposing, keyCode 229)는 글자 확정용이라 전송·이동으로 쓰지 않는다.
export function isCommitEnter(event: KeyboardEvent<HTMLElement>): boolean {
  return (
    event.key === 'Enter' &&
    !event.shiftKey &&
    !event.nativeEvent.isComposing &&
    event.keyCode !== 229
  )
}
