import styles from '@/shared/components/CardSkeletonGrid/CardSkeletonGrid.module.scss'

type CardSkeletonGridProps = {
  count?: number
}

export function CardSkeletonGrid({ count = 4 }: CardSkeletonGridProps) {
  return (
    <div className={styles.grid} role="status" aria-busy="true" aria-label="불러오는 중">
      {Array.from({ length: count }, (_, index) => (
        <div key={index} className={styles.card} aria-hidden="true">
          <div className={`${styles.block} ${styles.head}`} />
          <div className={styles.body}>
            <div className={`${styles.block} ${styles.title}`} />
            <div className={`${styles.block} ${styles.title} ${styles.short}`} />
            <div className={`${styles.block} ${styles.chips}`} />
            <div className={`${styles.block} ${styles.line}`} />
            <div className={`${styles.block} ${styles.line}`} />
            <div className={`${styles.block} ${styles.line} ${styles.short}`} />
          </div>
        </div>
      ))}
    </div>
  )
}
