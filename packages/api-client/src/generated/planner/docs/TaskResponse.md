
# TaskResponse


## Properties

Name | Type
------------ | -------------
`taskId` | string
`activityId` | string
`performedAt` | Date
`completionSummary` | string
`startedAt` | Date
`endedAt` | Date
`seriesId` | string
`occurrenceDate` | string
`note` | string
`userId` | string
`title` | string
`taskType` | [TaskType](TaskType.md)
`scheduledDate` | string
`status` | [TaskStatus](TaskStatus.md)
`version` | number

## Example

```typescript
import type { TaskResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "taskId": null,
  "activityId": null,
  "performedAt": null,
  "completionSummary": null,
  "startedAt": null,
  "endedAt": null,
  "seriesId": null,
  "occurrenceDate": null,
  "note": null,
  "userId": null,
  "title": null,
  "taskType": null,
  "scheduledDate": null,
  "status": null,
  "version": null,
} satisfies TaskResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
