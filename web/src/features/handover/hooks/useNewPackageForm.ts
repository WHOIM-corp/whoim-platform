import { useState } from 'react'
import type { FormEvent } from 'react'
import { generatePath, useNavigate } from 'react-router'
import { ROUTE_PATHS } from '@/app/routePaths.ts'
import { useCreatePackage } from '@/features/handover/hooks/useCreatePackage.ts'
import type { MemberSearchResult } from '@/features/member/model/memberTypes.ts'
import type { ApiError } from '@/shared/api/apiError.ts'

export const PACKAGE_NAME_MAX_LENGTH = 100
export const PACKAGE_DESCRIPTION_MAX_LENGTH = 1000

type FieldName = 'name' | 'description' | 'predecessor' | 'successor'
type FieldErrors = Partial<Record<FieldName, string>>

type NewPackageForm = {
  name: string
  description: string
  predecessor: MemberSearchResult | null
  successor: MemberSearchResult | null
  errors: FieldErrors
  // 이름 필드에 붙지 않는 서버 오류만 담는다.
  serverError: ApiError | null
  isPending: boolean
  // 검증 실패마다 늘어난다. 화면이 첫 오류 칸으로 포커스를 옮기는 신호다.
  invalidAttempt: number
  setName: (name: string) => void
  setDescription: (description: string) => void
  setPredecessor: (member: MemberSearchResult | null) => void
  setSuccessor: (member: MemberSearchResult | null) => void
  handleSubmit: (event: FormEvent) => void
}

export function useNewPackageForm(): NewPackageForm {
  const navigate = useNavigate()
  const createPackage = useCreatePackage()
  const [name, setNameValue] = useState('')
  const [description, setDescriptionValue] = useState('')
  const [predecessor, setPredecessorValue] = useState<MemberSearchResult | null>(null)
  const [successor, setSuccessorValue] = useState<MemberSearchResult | null>(null)
  const [errors, setErrors] = useState<FieldErrors>({})
  const [serverError, setServerError] = useState<ApiError | null>(null)
  const [invalidAttempt, setInvalidAttempt] = useState(0)

  function clearError(field: FieldName) {
    setErrors((previous) => ({ ...previous, [field]: undefined }))
  }

  function setName(next: string) {
    setNameValue(next)
    clearError('name')
  }

  function setDescription(next: string) {
    setDescriptionValue(next)
    clearError('description')
  }

  function setPredecessor(member: MemberSearchResult | null) {
    setPredecessorValue(member)
    clearError('predecessor')
  }

  function setSuccessor(member: MemberSearchResult | null) {
    setSuccessorValue(member)
    clearError('successor')
  }

  function validate(): FieldErrors {
    const found: FieldErrors = {}
    if (name.trim() === '') {
      found.name = 'Package 이름을 입력해 주세요.'
    } else if (name.trim().length > PACKAGE_NAME_MAX_LENGTH) {
      found.name = `Package 이름은 ${PACKAGE_NAME_MAX_LENGTH}자 이하로 입력해 주세요.`
    }
    if (description.trim().length > PACKAGE_DESCRIPTION_MAX_LENGTH) {
      found.description = `설명은 ${PACKAGE_DESCRIPTION_MAX_LENGTH}자 이하로 입력해 주세요.`
    }
    if (predecessor === null) {
      found.predecessor = '전임자를 검색해서 선택해 주세요.'
    }
    if (successor === null) {
      found.successor = '후임자를 검색해서 선택해 주세요.'
    }
    return found
  }

  function handleSubmit(event: FormEvent) {
    event.preventDefault()
    setServerError(null)
    const found = validate()
    setErrors(found)
    if (predecessor === null || successor === null || Object.keys(found).length > 0) {
      setInvalidAttempt((previous) => previous + 1)
      return
    }

    const trimmedDescription = description.trim()
    createPackage.submit(
      {
        predecessorId: predecessor.memberId,
        successorId: successor.memberId,
        name: name.trim(),
        description: trimmedDescription === '' ? undefined : trimmedDescription,
      },
      {
        onSuccess: (created) => navigate(generatePath(ROUTE_PATHS.packageDetail, { packageId: created.id })),
        onError: (error) => {
          if (error.errorCode === 'STATE_CONFLICT') {
            setErrors({ name: error.message })
            setInvalidAttempt((previous) => previous + 1)
            return
          }
          setServerError(error)
        },
      },
    )
  }

  return {
    name,
    description,
    predecessor,
    successor,
    errors,
    serverError,
    isPending: createPackage.isPending,
    invalidAttempt,
    setName,
    setDescription,
    setPredecessor,
    setSuccessor,
    handleSubmit,
  }
}
