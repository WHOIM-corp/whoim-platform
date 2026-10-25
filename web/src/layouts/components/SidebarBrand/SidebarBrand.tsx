import { Link } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { TrafficLights } from '@/layouts/components/TrafficLights/TrafficLights.tsx'
import styles from '@/layouts/components/SidebarBrand/SidebarBrand.module.scss'

export function SidebarBrand() {
  return (
    <div className={styles.brand}>
      <div className={styles.chrome}>
        <TrafficLights />
      </div>
      <Link to={ROUTE_PATHS.home} className={styles.wordmark}>
        WHO I&apos;M
      </Link>
    </div>
  )
}
