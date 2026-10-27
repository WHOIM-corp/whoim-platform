import { GapCard } from '@/features/gap/components/GapCard/GapCard.tsx'
import type { Gap } from '@/features/gap/model/gapTypes.ts'
import styles from '@/features/gap/components/GapGrid/GapGrid.module.scss'

type GapGridProps = {
  packageId: string
  gaps: Gap[]
  onStale: () => void
}

export function GapGrid({ packageId, gaps, onStale }: GapGridProps) {
  return (
    <div className={styles.grid}>
      {gaps.map((gap) => (
        <GapCard key={gap.id} packageId={packageId} gap={gap} onStale={onStale} />
      ))}
    </div>
  )
}
