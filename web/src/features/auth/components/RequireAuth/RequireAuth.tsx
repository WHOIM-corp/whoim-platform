import { Navigate, Outlet, useLocation } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'

export function RequireAuth() {
  const member = useAuthStore((state) => state.member)
  const location = useLocation()
  if (member === null) {
    return (
      <Navigate
        to={ROUTE_PATHS.login}
        replace
        state={{ from: location.pathname + location.search }}
      />
    )
  }
  return <Outlet />
}
