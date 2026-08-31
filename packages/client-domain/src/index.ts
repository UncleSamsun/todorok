export type TaskType = 'GENERAL' | 'WORKOUT' | 'STUDY' | 'CLIMBING'

export type TaskStatus = 'PLANNED' | 'COMPLETED' | 'SKIPPED'

export interface TaskSummary {
  id: string
  title: string
  scheduledDate: string
  type: TaskType
  status: TaskStatus
}
