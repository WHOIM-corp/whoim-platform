import { getInterview } from '@/features/interview/api/interviewApi.ts'
import { interviewKeys } from '@/features/interview/model/interviewKeys.ts'
import { useApiQuery } from '@/shared/api/useApiQuery.ts'

export function useInterview(id: string) {
  return useApiQuery({ queryKey: interviewKeys.detail(id), queryFn: () => getInterview(id) })
}
