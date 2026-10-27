import { createBrowserRouter, Navigate } from 'react-router'
import { ROUTE_ACCESS } from '@/app/routeAccess.ts'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import type { RouteHandle } from '@/app/routeHandle.ts'
import { AdminGate } from '@/features/auth/components/AdminGate/AdminGate.tsx'
import { RequireAuth } from '@/features/auth/components/RequireAuth/RequireAuth.tsx'
import { RequirePosition } from '@/features/auth/components/RequirePosition/RequirePosition.tsx'
import type { Position } from '@/features/auth/model/authTypes.ts'
import { CARD_QUEUE_BY_POSITION } from '@/features/card/model/cardQueue.ts'
import { CardDetailPage } from '@/features/card/pages/CardDetailPage.tsx'
import { QueuePage } from '@/features/card/pages/QueuePage.tsx'
import { PackageGapsPage } from '@/features/gap/pages/PackageGapsPage.tsx'
import { NewPackagePage } from '@/features/handover/pages/NewPackagePage.tsx'
import { PackageDetailPage } from '@/features/handover/pages/PackageDetailPage.tsx'
import { PackageListPage } from '@/features/handover/pages/PackageListPage.tsx'
import { InterviewPage } from '@/features/interview/pages/InterviewPage.tsx'
import { LoginPage } from '@/features/auth/pages/LoginPage.tsx'
import { NotFoundPage } from '@/features/system/pages/NotFoundPage.tsx'
import { ConsultPage } from '@/features/consult/pages/ConsultPage.tsx'
import { HomePage } from '@/features/workspace/pages/HomePage.tsx'
import { AppLayout } from '@/layouts/AppLayout/AppLayout.tsx'

// 상세 경로(:packageId)는 부모 /packages 아래에 두어 같은 권한 가드를 공유한다.
function handleOf(crumb?: RouteHandle['crumb']): RouteHandle {
  return { crumb }
}

// 큐 이름은 직책마다 다르다(승인자 검수 대기, 전임자 확인 대기).
function queueCrumb(position: Position | null): string {
  return (position && CARD_QUEUE_BY_POSITION[position]?.label) ?? '검수 대기'
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
                    element: <RequirePosition allow={ROUTE_ACCESS.packageNew} />,
                    children: [
                      { path: 'new', element: <NewPackagePage />, handle: handleOf('새 Package') },
                    ],
                  },
                  {
                    element: <RequirePosition allow={ROUTE_ACCESS.packageGaps} />,
                    children: [
                      {
                        path: ':packageId/gaps',
                        element: <PackageGapsPage />,
                        handle: handleOf('Gap'),
                      },
                    ],
                  },
                  {
                    element: <RequirePosition allow={ROUTE_ACCESS.packageGapInterview} />,
                    children: [
                      {
                        path: ':packageId/gaps/:gapId/interview',
                        element: <InterviewPage />,
                        handle: handleOf('인터뷰'),
                      },
                    ],
                  },
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
                    handle: handleOf(queueCrumb),
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
