import { matchPath, useLocation } from 'react-router'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import { NavGroup } from '@/layouts/components/NavGroup/NavGroup.tsx'
import { navGroupsFor } from '@/layouts/navigation/navConfig.ts'
import { LiquidPill } from '@/shared/components/LiquidPill/LiquidPill.tsx'
import { useSlidingIndicator } from '@/shared/hooks/useSlidingIndicator.ts'
import styles from '@/layouts/components/SidebarNav/SidebarNav.module.scss'

export function SidebarNav() {
  const position = useAuthStore((state) => state.member?.position ?? null)
  const { pathname } = useLocation()
  const groups = navGroupsFor(position)
  const activeKey =
    groups
      .flatMap((group) => group.items)
      .find((item) => matchPath({ path: item.to, end: false }, pathname) !== null)?.key ?? null
  const { containerRef, registerItem, box } = useSlidingIndicator(activeKey)

  // 알약은 항목과 같은 위치 기준 컨테이너 안에 둬야 좌표가 맞는다.
  return (
    <nav ref={containerRef} className={styles.nav} aria-label="주 메뉴">
      <LiquidPill box={box} />
      {groups.map((group) => (
        <NavGroup
          key={group.label}
          group={group}
          activeKey={activeKey}
          registerItem={registerItem}
        />
      ))}
    </nav>
  )
}
