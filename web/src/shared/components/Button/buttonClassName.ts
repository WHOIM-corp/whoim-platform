import styles from '@/shared/components/Button/Button.module.scss'

export type ButtonVariant = 'primary' | 'secondary' | 'danger' | 'dangerSolid'

// 링크처럼 버튼이 아닌 요소에도 같은 모양을 입히기 위해 클래스 이름을 한 곳에서 만든다.
export function buttonClassName(variant: ButtonVariant = 'secondary'): string {
  return `${styles.button} ${styles[variant]}`
}
