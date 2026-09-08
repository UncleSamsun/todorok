import { useInfiniteQuery } from '@tanstack/react-query'
import { activity } from '@todorok/api-client'

export function useStudyTemplates(api: activity.TemplateApi, owner: string | null | undefined, includeArchived: boolean, enabled = true) {
  return useRecordTemplates(api, owner, activity.TemplateDomain.Study, activity.TemplateKind.StudyCategory, includeArchived, enabled)
}

export function useRecordTemplates(api: activity.TemplateApi, owner: string | null | undefined, domain: activity.TemplateDomain, kind: activity.TemplateKind, includeArchived: boolean, enabled = true) {
  return useInfiniteQuery({
    queryKey: ['templates', owner, domain, kind, includeArchived ? 'all' : 'active'],
    enabled,
    initialPageParam: undefined as string | undefined,
    queryFn: ({ signal, pageParam }) => api.listTemplates({ domain, kind, includeArchived, cursor: pageParam }, { signal }),
    getNextPageParam: (page) => page.nextCursor || undefined,
  })
}
