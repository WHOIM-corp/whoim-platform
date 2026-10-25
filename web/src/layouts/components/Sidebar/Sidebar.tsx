import { SidebarAccount } from '@/layouts/components/SidebarAccount/SidebarAccount.tsx'
import { SidebarBrand } from '@/layouts/components/SidebarBrand/SidebarBrand.tsx'
import { SidebarNav } from '@/layouts/components/SidebarNav/SidebarNav.tsx'
import styles from '@/layouts/components/Sidebar/Sidebar.module.scss'

export function Sidebar() {
  return (
    <aside className={styles.sidebar}>
      <SidebarBrand />
      <SidebarNav />
      <SidebarAccount />
    </aside>
  )
}
