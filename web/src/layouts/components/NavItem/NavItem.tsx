import { Link } from 'react-router'
import type { NavItemConfig } from '@/layouts/navigation/navConfig.ts'
import styles from '@/layouts/components/NavItem/NavItem.module.scss'

type NavItemProps = {
  item: NavItemConfig
  active: boolean
  registerRef: (element: HTMLElement | null) => void
}

export function NavItem({ item, active, registerRef }: NavItemProps) {
  const Icon = item.icon
  return (
    <li>
      <Link
        ref={registerRef}
        to={item.to}
        className={styles.item}
        aria-current={active ? 'page' : undefined}
      >
        <Icon size={16} strokeWidth={1.5} aria-hidden="true" />
        <span className={styles.name}>{item.label}</span>
      </Link>
    </li>
  )
}
