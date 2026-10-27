import { matchPath } from 'react-router'
import { ROUTE_ACCESS } from '@/app/routeAccess.ts'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { Position } from '@/features/auth/model/authTypes.ts'

const ROUTE_KEYS = Object.keys(ROUTE_ACCESS) as (keyof typeof ROUTE_ACCESS)[]

// 고정 구간(:로 시작하지 않는 구간)이 많을수록 구체적인 패턴이다.
function fixedSegmentCount(pattern: string): number {
  return pattern.split('/').filter((segment) => segment !== '' && !segment.startsWith(':')).length
}

// 경로가 알려진 화면이고 해당 position이 열 수 있을 때만 true (쿼리·해시는 무시).
// 여러 패턴이 맞으면 가장 구체적인 패턴 하나로 판정한다(/packages/new는 packageDetail이 아니다).
export function canAccessPath(path: string, position: Position | null): boolean {
  if (position === null) {
    return false
  }
  const pathname = path.split(/[?#]/)[0]
  const matchedKeys = ROUTE_KEYS.filter(
    (key) => matchPath({ path: ROUTE_PATHS[key], end: true }, pathname) !== null,
  )
  if (matchedKeys.length === 0) {
    return false
  }
  const mostSpecificKey = matchedKeys.reduce((best, key) =>
    fixedSegmentCount(ROUTE_PATHS[key]) > fixedSegmentCount(ROUTE_PATHS[best]) ? key : best,
  )
  const allowed: readonly Position[] = ROUTE_ACCESS[mostSpecificKey]
  return allowed.includes(position)
}
