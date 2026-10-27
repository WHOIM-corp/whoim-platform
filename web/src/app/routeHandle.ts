import type { Position } from '@/features/auth/model/authTypes.ts'

// 직책마다 이름이 다른 화면(예: 검수 대기·확인 대기)은 crumb를 직책으로 고른다.
export type RouteHandle = { crumb?: string | ((position: Position | null) => string) }
