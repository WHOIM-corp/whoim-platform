import styles from '@/shared/components/ProgressBar/ProgressBar.module.scss'

type ProgressBarProps = {
  value: number
  max: number
  label: string
}

export function ProgressBar({ value, max, label }: ProgressBarProps) {
  const ratio = max > 0 ? Math.min(Math.max(value / max, 0), 1) : 0
  return (
    <div className={styles.progress}>
      <div className={styles.top}>
        <span>{label}</span>
        <b>
          {value} / {max}
        </b>
      </div>
      <div
        className={styles.bar}
        role="progressbar"
        aria-label={label}
        aria-valuemin={0}
        aria-valuemax={max}
        aria-valuenow={value}
      >
        <span className={styles.fill} style={{ transform: `scaleX(${ratio})` }} />
      </div>
    </div>
  )
}
