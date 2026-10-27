import { generatePath, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useGaps } from '@/features/gap/hooks/useGaps.ts'
import { useHandoverPackage } from '@/features/handover/hooks/useHandoverPackage.ts'
import { SegmentedControl } from '@/shared/components/SegmentedControl/SegmentedControl.tsx'
import styles from '@/features/gap/components/PackageTabs/PackageTabs.module.scss'

type PackageTab = 'gaps' | 'cards'

type PackageTabsProps = {
  packageId: string
  active: PackageTab
}

const TAB_PATHS: Record<PackageTab, string> = {
  gaps: ROUTE_PATHS.packageGaps,
  cards: ROUTE_PATHS.packageDetail,
}

export function PackageTabs({ packageId, active }: PackageTabsProps) {
  const navigate = useNavigate()
  const gapCount = useGaps(packageId).data?.length
  const cardCount = useHandoverPackage(packageId).data?.cards.length

  return (
    <div className={styles.tabs}>
      <SegmentedControl
        items={[
          { value: 'gaps', label: 'Gap 목록', count: gapCount },
          { value: 'cards', label: 'Card', count: cardCount },
        ]}
        value={active}
        onChange={(next) => navigate(generatePath(TAB_PATHS[next], { packageId }))}
        ariaLabel="Package 보기 전환"
      />
    </div>
  )
}
