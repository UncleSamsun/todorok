import { useInfiniteQuery } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'

export function useStudyTemplates(api: activity.TemplateApi, owner: string | null | undefined, includeArchived: boolean, enabled = true) {
  return useInfiniteQuery({
    queryKey: ['templates', owner, 'STUDY', 'STUDY_CATEGORY', includeArchived ? 'all' : 'active'],
    enabled,
    initialPageParam: undefined as string | undefined,
    queryFn: ({ signal, pageParam }) => api.listTemplates({ domain: activity.TemplateDomain.Study, kind: activity.TemplateKind.StudyCategory, includeArchived, cursor: pageParam }, { signal }),
    getNextPageParam: (page) => page.nextCursor || undefined,
  })
}
