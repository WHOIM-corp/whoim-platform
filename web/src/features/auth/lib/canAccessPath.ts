import { matchPath } from 'react-router'
import { ROUTE_ACCESS } from '@/app/routeAccess.ts'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { Position } from '@/features/auth/model/authTypes.ts'

const ROUTE_KEYS = Object.keys(ROUTE_ACCESS) as (keyof typeof ROUTE_ACCESS)[]

// 경로가 알려진 화면이고 해당 position이 열 수 있을 때만 true (쿼리·해시는 무시).
export function canAccessPath(path: string, position: Position | null): boolean {
  if (position === null) {
    return false
  }
  const pathname = path.split(/[?#]/)[0]
  return ROUTE_KEYS.some((key) => {
    const allowed: readonly Position[] = ROUTE_ACCESS[key]
    return (
      matchPath({ path: ROUTE_PATHS[key], end: true }, pathname) !== null &&
      allowed.includes(position)
    )
  })
}
