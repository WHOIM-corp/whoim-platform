import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { canAccessPath } from '@/features/auth/lib/canAccessPath.ts'
import { isInternalPath } from '@/features/auth/lib/safeRedirect.ts'
import type { Position } from '@/features/auth/model/authTypes.ts'

export function readRedirectSource(state: unknown): string | null {
  if (typeof state === 'object' && state !== null && 'from' in state) {
    return typeof state.from === 'string' ? state.from : null
  }
  return null
}

// 로그인 뒤 갈 곳: 넘어온 내부 경로를 새로 로그인한 계정이 열 수 있을 때만 그곳, 아니면 홈.
export function resolveRedirectTarget(state: unknown, position: Position | null): string {
  const source = readRedirectSource(state)
  return source !== null && isInternalPath(source) && canAccessPath(source, position)
    ? source
    : ROUTE_PATHS.home
}
