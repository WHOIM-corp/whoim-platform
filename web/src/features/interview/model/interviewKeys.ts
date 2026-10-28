export const interviewKeys = {
  all: ['interviews'] as const,
  detail: (id: string) => [...interviewKeys.all, 'detail', id] as const,
}
