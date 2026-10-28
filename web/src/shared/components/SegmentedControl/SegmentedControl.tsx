import type { ReactNode, RefObject } from 'react'
import { LiquidPill } from '@/shared/components/LiquidPill/LiquidPill.tsx'
import { useSlidingIndicator } from '@/shared/hooks/useSlidingIndicator.ts'
import styles from '@/shared/components/SegmentedControl/SegmentedControl.module.scss'

type SegmentedControlItem<T extends string> = {
  value: T
  label: ReactNode
  count?: number
}

type SegmentedControlProps<T extends string> = {
  items: SegmentedControlItem<T>[]
  value: T
  onChange: (value: T) => void
  ariaLabel: string
}

export function SegmentedControl<T extends string>({
  items,
  value,
  onChange,
  ariaLabel,
}: SegmentedControlProps<T>) {
  const { containerRef, registerItem, box } = useSlidingIndicator(value)

  return (
    <div
      ref={containerRef as RefObject<HTMLDivElement>}
      className={styles.segmented}
      role="group"
      aria-label={ariaLabel}
    >
      <LiquidPill box={box} />
      {items.map((item) => (
        <button
          key={item.value}
          ref={registerItem(item.value)}
          type="button"
          className={styles.item}
          aria-pressed={item.value === value}
          onClick={() => onChange(item.value)}
        >
          {item.label}
          {item.count !== undefined && <span className={styles.count}>{item.count}</span>}
        </button>
      ))}
    </div>
  )
}
