import type { IndicatorBox } from '@/shared/hooks/useSlidingIndicator.ts'
import styles from '@/shared/components/LiquidPill/LiquidPill.module.scss'

type LiquidPillProps = {
  box: IndicatorBox | null
}

// box가 없으면 display: none이라, 처음 나타날 때는 전환 없이 제자리에 놓이고 이후 변경부터 미끄러진다.
export function LiquidPill({ box }: LiquidPillProps) {
  return (
    <span
      aria-hidden="true"
      hidden={box === null}
      className={styles.pill}
      style={box ?? undefined}
    />
  )
}
