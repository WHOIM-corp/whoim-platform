import { useCallback, useLayoutEffect, useRef, useState } from 'react'
import type { RefObject } from 'react'

export type IndicatorBox = { top: number; left: number; width: number; height: number }

function isSameBox(first: IndicatorBox | null, second: IndicatorBox | null): boolean {
  if (first === null || second === null) {
    return first === second
  }
  return (
    first.top === second.top &&
    first.left === second.left &&
    first.width === second.width &&
    first.height === second.height
  )
}

// 활성 항목의 위치를 컨테이너 기준으로 측정한다. 컨테이너 크기가 바뀌면 다시 잰다.
export function useSlidingIndicator(activeKey: string | null): {
  containerRef: RefObject<HTMLElement | null>
  registerItem: (key: string) => (element: HTMLElement | null) => void
  box: IndicatorBox | null
} {
  const containerRef = useRef<HTMLElement | null>(null)
  const itemsRef = useRef(new Map<string, HTMLElement>())
  const [box, setBox] = useState<IndicatorBox | null>(null)

  const measure = useCallback(() => {
    const container = containerRef.current
    const item = activeKey === null ? undefined : itemsRef.current.get(activeKey)
    let next: IndicatorBox | null = null
    if (container !== null && item !== undefined) {
      const containerRect = container.getBoundingClientRect()
      const itemRect = item.getBoundingClientRect()
      next = {
        top: itemRect.top - containerRect.top - container.clientTop + container.scrollTop,
        left: itemRect.left - containerRect.left - container.clientLeft,
        width: itemRect.width,
        height: itemRect.height,
      }
    }
    setBox((previous) => (isSameBox(previous, next) ? previous : next))
  }, [activeKey])

  useLayoutEffect(() => {
    measure()
    const container = containerRef.current
    if (container === null) {
      return undefined
    }
    const observer = new ResizeObserver(measure)
    observer.observe(container)
    return () => observer.disconnect()
  }, [measure])

  const registerItem = useCallback(
    (key: string) => (element: HTMLElement | null) => {
      if (element === null) {
        itemsRef.current.delete(key)
      } else {
        itemsRef.current.set(key, element)
      }
    },
    [],
  )

  return { containerRef, registerItem, box }
}
