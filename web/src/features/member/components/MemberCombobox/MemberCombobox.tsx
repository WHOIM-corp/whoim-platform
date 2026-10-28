import { Search } from 'lucide-react'
import { useEffect, useId, useRef, useState } from 'react'
import type { ChangeEvent, KeyboardEvent } from 'react'
import { PersonSlot } from '@/features/handover/components/PersonSlot/PersonSlot.tsx'
import { positionLabels, type Position } from '@/features/auth/model/authTypes.ts'
import { HighlightedText } from '@/features/member/components/MemberCombobox/HighlightedText.tsx'
import { useMemberSearch } from '@/features/member/hooks/useMemberSearch.ts'
import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import styles from '@/features/member/components/MemberCombobox/MemberCombobox.module.scss'

type MemberComboboxProps = {
  position: Position
  value: MemberSearchResult | null
  onChange: (member: MemberSearchResult | null) => void
  ariaLabel: string
  invalid?: boolean
}

export function MemberCombobox({ position, value, onChange, ariaLabel, invalid = false }: MemberComboboxProps) {
  const listboxId = useId()
  const inputRef = useRef<HTMLInputElement>(null)
  const shouldRefocusRef = useRef(false)
  const [keyword, setKeyword] = useState('')
  const [isOpen, setIsOpen] = useState(false)
  const [activeIndex, setActiveIndex] = useState(0)
  const { members, isLoading, error, settledKeyword, isStale } = useMemberSearch(position, keyword, isOpen && value === null)

  // 변경을 누르면 검색칸이 다시 나타난 뒤 그 칸으로 포커스를 옮긴다.
  useEffect(() => {
    if (value === null && shouldRefocusRef.current) {
      shouldRefocusRef.current = false
      inputRef.current?.focus()
    }
  }, [value])

  if (value !== null) {
    return (
      <PersonSlot
        member={value}
        onClear={() => {
          shouldRefocusRef.current = true
          onChange(null)
        }}
      />
    )
  }

  const boundedActiveIndex = Math.min(activeIndex, Math.max(members.length - 1, 0))
  const status = renderStatus()
  const activeOptionId = isOpen && status === null && members.length > 0 ? `${listboxId}-option-${boundedActiveIndex}` : undefined

  function select(member: MemberSearchResult) {
    onChange(member)
    setKeyword('')
    setIsOpen(false)
    setActiveIndex(0)
  }

  function handleChange(event: ChangeEvent<HTMLInputElement>) {
    setKeyword(event.target.value)
    setActiveIndex(0)
    setIsOpen(true)
  }

  function handleKeyDown(event: KeyboardEvent<HTMLInputElement>) {
    if (event.nativeEvent.isComposing) {
      return
    }
    if (event.key === 'Enter') {
      // 폼이 제출되지 않게 항상 막는다. 제출은 Package 만들기 버튼으로 한다.
      event.preventDefault()
      const active = members[boundedActiveIndex]
      if (isOpen && !isStale && active !== undefined) {
        select(active)
      }
    } else if (event.key === 'ArrowDown' || event.key === 'ArrowUp') {
      event.preventDefault()
      if (!isOpen) {
        setIsOpen(true)
        return
      }
      const step = event.key === 'ArrowDown' ? 1 : -1
      if (members.length > 0) {
        setActiveIndex((boundedActiveIndex + step + members.length) % members.length)
      }
    } else if (event.key === 'Escape' && isOpen) {
      event.preventDefault()
      setIsOpen(false)
    }
  }

  function renderStatus(): string | null {
    if (error !== null) {
      return '검색하지 못했습니다'
    }
    if (isLoading) {
      return '검색 중입니다'
    }
    if (members.length === 0) {
      const label = positionLabels[position]
      if (isStale) {
        return '검색 중입니다'
      }
      return settledKeyword === '' ? `${label} 계정이 없습니다` : `"${settledKeyword}"에 맞는 ${label} 계정이 없습니다`
    }
    return null
  }

  function renderOptions() {
    return members.map((member, index) => (
      <li
        key={member.memberId}
        id={`${listboxId}-option-${index}`}
        role="option"
        aria-selected={index === boundedActiveIndex}
        className={styles.option}
        // 입력칸이 blur로 닫히기 전에 선택되도록 mousedown에서 처리한다.
        onMouseDown={(event) => {
          event.preventDefault()
          if (!isStale) {
            select(member)
          }
        }}
        onMouseEnter={() => setActiveIndex(index)}
      >
        <span className={styles.avatar} aria-hidden="true">
          {member.name.charAt(0)}
        </span>
        <span className={styles.identity}>
          <b className={styles.name}>
            <HighlightedText text={member.name} keyword={settledKeyword} />
          </b>
          <small className={styles.detail}>
            <HighlightedText text={`${member.department} · ${member.jobTitle}`} keyword={settledKeyword} />
          </small>
        </span>
        <span className={styles.employeeNumber}>
          <HighlightedText text={member.employeeNumber} keyword={settledKeyword} />
        </span>
      </li>
    ))
  }

  return (
    <div className={styles.combobox}>
      <Search className={styles.icon} size={16} strokeWidth={1.5} aria-hidden="true" />
      <input
        ref={inputRef}
        className={styles.input}
        role="combobox"
        aria-label={ariaLabel}
        aria-expanded={isOpen}
        aria-controls={listboxId}
        aria-activedescendant={activeOptionId}
        aria-autocomplete="list"
        aria-invalid={invalid}
        autoComplete="off"
        placeholder="이름, 부서, 사번으로 검색"
        value={keyword}
        onChange={handleChange}
        onFocus={() => setIsOpen(true)}
        onBlur={() => setIsOpen(false)}
        onKeyDown={handleKeyDown}
      />
      {isOpen && status !== null && (
        <div role="status" className={`${styles.popup} ${styles.message}`}>
          {status}
        </div>
      )}
      <ul
        id={listboxId}
        role="listbox"
        aria-label={ariaLabel}
        aria-busy={isStale}
        className={isStale ? `${styles.popup} ${styles.stale}` : styles.popup}
        hidden={!isOpen || status !== null}
      >
        {isOpen && status === null && renderOptions()}
      </ul>
    </div>
  )
}
