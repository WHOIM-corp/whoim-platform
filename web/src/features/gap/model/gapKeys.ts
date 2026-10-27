export const gapKeys = {
  all: ['gaps'] as const,
  list: (packageId: string) => [...gapKeys.all, 'list', packageId] as const,
}
