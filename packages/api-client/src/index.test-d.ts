import type { activity, planner } from './index'

declare const plannerConfiguration: planner.Configuration
declare const activityConfiguration: activity.Configuration

export type ContractConfigurations = [
  typeof plannerConfiguration,
  typeof activityConfiguration,
]
