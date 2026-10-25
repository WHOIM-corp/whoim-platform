import { createBrowserRouter, Navigate } from 'react-router'
import { ROUTE_ACCESS } from '@/app/routeAccess.ts'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { RouteHandle } from '@/app/routeHandle.ts'
import { AdminGate } from '@/features/auth/components/AdminGate/AdminGate.tsx'
import { RequireAuth } from '@/features/auth/components/RequireAuth/RequireAuth.tsx'
import { RequirePosition } from '@/features/auth/components/RequirePosition/RequirePosition.tsx'
import { LoginPage } from '@/features/auth/pages/LoginPage.tsx'
import { NotFoundPage } from '@/features/system/pages/NotFoundPage.tsx'
import { CardDetailPage } from '@/features/workspace/pages/CardDetailPage.tsx'
import { ConsultPage } from '@/features/workspace/pages/ConsultPage.tsx'
import { HomePage } from '@/features/workspace/pages/HomePage.tsx'
import { PackageDetailPage } from '@/features/workspace/pages/PackageDetailPage.tsx'
import { PackageListPage } from '@/features/workspace/pages/PackageListPage.tsx'
import { QueuePage } from '@/features/workspace/pages/QueuePage.tsx'
import { AppLayout } from '@/layouts/AppLayout/AppLayout.tsx'

// 상세 경로(:packageId)는 부모 /packages 아래에 두어 같은 권한 가드를 공유한다.
function handleOf(crumb?: string): RouteHandle {
  return { crumb }
}

export const router = createBrowserRouter([
  { path: ROUTE_PATHS.login, element: <LoginPage /> },
  {
    element: <RequireAuth />,
    children: [
      { path: '/', element: <Navigate to={ROUTE_PATHS.home} replace /> },
      {
        element: <AppLayout />,
        children: [
          {
            element: <AdminGate />,
            children: [
              {
                element: <RequirePosition allow={ROUTE_ACCESS.home} />,
                children: [{ path: ROUTE_PATHS.home, element: <HomePage />, handle: handleOf() }],
              },
              {
                path: ROUTE_PATHS.packages,
                element: <RequirePosition allow={ROUTE_ACCESS.packages} />,
                handle: handleOf('Package'),
                children: [
                  { index: true, element: <PackageListPage /> },
                  {
                    path: ':packageId',
                    element: <PackageDetailPage />,
                    handle: handleOf('Package 상세'),
                  },
                ],
              },
              {
                element: <RequirePosition allow={ROUTE_ACCESS.queue} />,
                children: [
                  {
                    path: ROUTE_PATHS.queue,
                    element: <QueuePage />,
                    handle: handleOf('검수 대기'),
                  },
                ],
              },
              {
                element: <RequirePosition allow={ROUTE_ACCESS.consult} />,
                children: [
                  { path: ROUTE_PATHS.consult, element: <ConsultPage />, handle: handleOf('상담') },
                ],
              },
              {
                element: <RequirePosition allow={ROUTE_ACCESS.cardDetail} />,
                children: [
                  {
                    path: ROUTE_PATHS.cardDetail,
                    element: <CardDetailPage />,
                    handle: handleOf('Card 상세'),
                  },
                ],
              },
              { path: '*', element: <NotFoundPage /> },
            ],
          },
        ],
      },
    ],
  },
])
