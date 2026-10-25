import { LoaderCircle } from 'lucide-react'
import type { ButtonHTMLAttributes } from 'react'
import styles from '@/shared/components/Button/Button.module.scss'
import { buttonClassName, type ButtonVariant } from '@/shared/components/Button/buttonClassName.ts'

type ButtonProps = ButtonHTMLAttributes<HTMLButtonElement> & {
  variant?: ButtonVariant
  loading?: boolean
}

export function Button({
  variant = 'secondary',
  loading = false,
  disabled,
  className,
  children,
  type = 'button',
  ...rest
}: ButtonProps) {
  const classNames = [buttonClassName(variant), className].filter(Boolean).join(' ')
  return (
    <button {...rest} type={type} className={classNames} disabled={disabled || loading}>
      {loading && <LoaderCircle className={styles.spinner} size={16} aria-hidden="true" />}
      {children}
    </button>
  )
}
