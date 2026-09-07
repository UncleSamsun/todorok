import { planner } from '@todorok/api-client'
export const groups = [
  [planner.TaskType.General, '할 일'],
  [planner.TaskType.Workout, '운동'],
  [planner.TaskType.Study, '공부'],
  [planner.TaskType.Climbing, '클라이밍'],
] as const
export type Task = planner.TaskResponse
export type Summary = planner.CalendarDaySummary
export const countLabel = (day: string, summary?: Summary) =>
  `${day}, 완료 ${summary?.completedCount ?? 0}개 / 전체 ${summary?.totalCount ?? 0}개`
