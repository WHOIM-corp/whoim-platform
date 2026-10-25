import { Outlet } from 'react-router'
import { Breadcrumbs } from '@/layouts/components/Breadcrumbs/Breadcrumbs.tsx'
import { Sidebar } from '@/layouts/components/Sidebar/Sidebar.tsx'
import { Toolbar } from '@/layouts/components/Toolbar/Toolbar.tsx'
import { WindowFrame } from '@/layouts/components/WindowFrame/WindowFrame.tsx'
import styles from '@/layouts/AppLayout/AppLayout.module.scss'

export function AppLayout() {
  return (
    <WindowFrame>
      <div className={styles.grid}>
        <Sidebar />
        <div className={styles.main}>
          <Toolbar>
            <Breadcrumbs />
          </Toolbar>
          <main className={styles.content}>
            <Outlet />
          </main>
        </div>
      </div>
    </WindowFrame>
  )
}
