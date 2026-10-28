import { ChevronRight } from 'lucide-react'
import { Fragment } from 'react'
import { Link, useMatches } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { RouteHandle } from '@/app/routeHandle.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import styles from '@/layouts/components/Breadcrumbs/Breadcrumbs.module.scss'

type Crumb = { label: string; to: string }

const HOME_CRUMB: Crumb = { label: '홈', to: ROUTE_PATHS.home }

function isRouteHandle(value: unknown): value is RouteHandle {
  return typeof value === 'object' && value !== null
}

export function Breadcrumbs() {
  const matches = useMatches()
  const position = useAuthStore((state) => state.member?.position ?? null)
  const crumbs: Crumb[] = [HOME_CRUMB]
  for (const match of matches) {
    if (isRouteHandle(match.handle) && match.handle.crumb) {
      const { crumb } = match.handle
      crumbs.push({ label: typeof crumb === 'string' ? crumb : crumb(position), to: match.pathname })
    }
  }

  return (
    <nav className={styles.crumbs} aria-label="현재 위치">
      {crumbs.map((crumb, index) => (
        <Fragment key={crumb.to}>
          {index > 0 && <ChevronRight size={14} strokeWidth={1.5} aria-hidden="true" />}
          {index === crumbs.length - 1 ? (
            <strong>{crumb.label}</strong>
          ) : (
            <Link to={crumb.to}>{crumb.label}</Link>
          )}
        </Fragment>
      ))}
    </nav>
  )
}
