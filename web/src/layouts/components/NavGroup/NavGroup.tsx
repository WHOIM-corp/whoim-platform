import { NavItem } from '@/layouts/components/NavItem/NavItem.tsx'
import type { NavGroupConfig } from '@/layouts/navigation/navConfig.ts'
import styles from '@/layouts/components/NavGroup/NavGroup.module.scss'

type NavGroupProps = {
  group: NavGroupConfig
  activeKey: string | null
  registerItem: (key: string) => (element: HTMLElement | null) => void
}

export function NavGroup({ group, activeKey, registerItem }: NavGroupProps) {
  return (
    <div className={styles.group}>
      <p className={styles.label}>{group.label}</p>
      <ul>
        {group.items.map((item) => (
          <NavItem
            key={item.key}
            item={item}
            active={item.key === activeKey}
            registerRef={registerItem(item.key)}
          />
        ))}
      </ul>
    </div>
  )
}
