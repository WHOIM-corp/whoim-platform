export const handoverKeys = {
  all: ['handover-packages'] as const,
  list: () => [...handoverKeys.all, 'list'] as const,
  detail: (id: string) => [...handoverKeys.all, 'detail', id] as const,
}
