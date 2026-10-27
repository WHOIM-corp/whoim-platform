import { useEffect, useId, useRef } from 'react'
import type { MouseEvent, ReactNode, SyntheticEvent } from 'react'
import styles from '@/shared/components/Dialog/Dialog.module.scss'

type DialogProps = {
  open: boolean
  title: string
  description?: string
  onClose: () => void
  // false면 ESC와 배경 클릭으로 닫히지 않는다. 요청 중에 닫혀 결과를 잃지 않게 한다.
  dismissible?: boolean
  children: ReactNode
}

// 네이티브 <dialog>를 쓴다. ESC와 포커스 가둠은 브라우저가 처리하고, 닫히면 onClose를 부른다.
export function Dialog({ open, title, description, onClose, dismissible = true, children }: DialogProps) {
  const dialogRef = useRef<HTMLDialogElement>(null)
  const titleId = useId()
  const descriptionId = useId()

  useEffect(() => {
    const dialog = dialogRef.current
    if (dialog === null) {
      return
    }
    if (open && !dialog.open) {
      dialog.showModal()
    } else if (!open && dialog.open) {
      dialog.close()
    }
  }, [open])

  // 배경(::backdrop)을 누르면 이벤트 대상이 dialog 자신이 된다.
  function closeOnBackdropClick(event: MouseEvent<HTMLDialogElement>) {
    if (dismissible && event.target === event.currentTarget) {
      event.currentTarget.close()
    }
  }

  // ESC는 네이티브 cancel 이벤트를 거쳐 닫힌다.
  function blockCancelWhenBusy(event: SyntheticEvent<HTMLDialogElement>) {
    if (!dismissible) {
      event.preventDefault()
    }
  }

  return (
    <dialog
      ref={dialogRef}
      className={styles.dialog}
      aria-labelledby={titleId}
      aria-describedby={description ? descriptionId : undefined}
      onClose={onClose}
      onCancel={blockCancelWhenBusy}
      onClick={closeOnBackdropClick}
    >
      <div className={styles.content}>
        <header>
          <h2 id={titleId} className={styles.title}>
            {title}
          </h2>
          {description && (
            <p id={descriptionId} className={styles.description}>
              {description}
            </p>
          )}
        </header>
        {children}
      </div>
    </dialog>
  )
}
