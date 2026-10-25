import styles from '@/layouts/components/TrafficLights/TrafficLights.module.scss'

export function TrafficLights() {
  return (
    <span className={styles.lights} aria-hidden="true">
      <i className={`${styles.light} ${styles.close}`} />
      <i className={`${styles.light} ${styles.minimize}`} />
      <i className={`${styles.light} ${styles.zoom}`} />
    </span>
  )
}
