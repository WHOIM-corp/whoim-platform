import { generatePath, Link, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useAuthStore } from '@/features/auth/model/authStore.ts'
import type { CardQueue } from '@/features/card/model/cardQueue.ts'
import { PackageStatusBadge } from '@/features/handover/components/PackageStatusBadge/PackageStatusBadge.tsx'
import { countCardsWithStatus } from '@/features/handover/model/packageCardCounts.ts'
import type { HandoverPackage } from '@/features/handover/model/handoverTypes.ts'
import { formatShortDate } from '@/shared/lib/formatDate.ts'
import styles from '@/features/handover/components/PackageTable/PackageTable.module.scss'

type PackageTableProps = {
  packages: HandoverPackage[]
  // 주어지면 이 상태의 Card 수를 열로 보여준다 (승인자는 검수 대기).
  queue?: CardQueue
}

// 전임자는 Gap 목록에서 시작하고, 그 밖의 직책은 Card 목록을 연다.
function detailPathOf(handoverPackage: HandoverPackage, isPredecessor: boolean): string {
  const route = isPredecessor ? ROUTE_PATHS.packageGaps : ROUTE_PATHS.packageDetail
  return generatePath(route, { packageId: handoverPackage.id })
}

export function PackageTable({ packages, queue }: PackageTableProps) {
  const navigate = useNavigate()
  const isPredecessor = useAuthStore((state) => state.member?.position === 'PREDECESSOR')
  // 후임자는 자기에게 지정된 Package만 보므로 후임자 열이 필요 없다.
  const isSuccessor = useAuthStore((state) => state.member?.position === 'SUCCESSOR')
  return (
    <div className={styles.box}>
      <table className={styles.table}>
        <thead>
          <tr>
            <th scope="col">Package</th>
            <th scope="col">상태</th>
            <th scope="col">전임자</th>
            {!isSuccessor && <th scope="col">후임자</th>}
            <th scope="col">Card</th>
            {queue && <th scope="col">{queue.label}</th>}
            <th scope="col">만든 날</th>
          </tr>
        </thead>
        <tbody>
          {packages.map((handoverPackage) => {
            const queueCount = queue ? countCardsWithStatus(handoverPackage, queue.status) : 0
            return (
              <tr
                key={handoverPackage.id}
                className={styles.row}
                onClick={() => navigate(detailPathOf(handoverPackage, isPredecessor))}
              >
                <td>
                  <Link
                    className={styles.name}
                    to={detailPathOf(handoverPackage, isPredecessor)}
                    onClick={(event) => event.stopPropagation()}
                  >
                    {handoverPackage.name}
                  </Link>
                </td>
                <td>
                  <PackageStatusBadge status={handoverPackage.status} />
                </td>
                <td>{handoverPackage.predecessor.name}</td>
                {!isSuccessor && (
                  <td>
                    {handoverPackage.successor === null ? (
                      <span className={styles.muted}>미지정</span>
                    ) : (
                      handoverPackage.successor.name
                    )}
                  </td>
                )}
                <td>{handoverPackage.cards.length}장</td>
                {queue && <td>{queueCount === 0 ? <span className={styles.muted}>-</span> : queueCount}</td>}
                <td className={styles.muted}>
                  <time dateTime={handoverPackage.createdAt}>{formatShortDate(handoverPackage.createdAt)}</time>
                </td>
              </tr>
            )
          })}
        </tbody>
      </table>
    </div>
  )
}
