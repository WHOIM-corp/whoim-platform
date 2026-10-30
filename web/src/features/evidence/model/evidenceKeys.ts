export const evidenceKeys = {
  all: ['evidence-documents'] as const,
  list: (packageId: string) => [...evidenceKeys.all, 'list', packageId] as const,
}
