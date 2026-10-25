import { House, Inbox, Layers, MessagesSquare } from 'lucide-react'
import type { LucideIcon } from 'lucide-react'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { Position } from '@/features/auth/model/authTypes.ts'

export type NavItemConfig = { key: string; label: string; to: string; icon: LucideIcon }
export type NavGroupConfig = { label: string; items: NavItemConfig[] }

const HOME_ITEM: NavItemConfig = { key: 'home', label: '홈', to: ROUTE_PATHS.home, icon: House }

const PACKAGE_GROUP: NavGroupConfig = {
  label: 'Package',
  items: [{ key: 'packages', label: '전체 Package', to: ROUTE_PATHS.packages, icon: Layers }],
}

function queueItem(label: string): NavItemConfig {
  return { key: 'queue', label, to: ROUTE_PATHS.queue, icon: Inbox }
}

const CONSULT_ITEM: NavItemConfig = {
  key: 'consult',
  label: '상담',
  to: ROUTE_PATHS.consult,
  icon: MessagesSquare,
}

const WORK_ITEMS: Record<Position, NavItemConfig[]> = {
  REVIEWER: [HOME_ITEM, queueItem('검수 대기')],
  PREDECESSOR: [HOME_ITEM, queueItem('확인 대기')],
  SUCCESSOR: [HOME_ITEM, CONSULT_ITEM],
}

// 관리자(position이 null)는 업무 화면이 없어 항목을 두지 않는다.
export function navGroupsFor(position: Position | null): NavGroupConfig[] {
  if (position === null) {
    return []
  }
  return [{ label: '내 작업', items: WORK_ITEMS[position] }, PACKAGE_GROUP]
}
