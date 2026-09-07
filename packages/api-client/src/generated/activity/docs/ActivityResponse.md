
# ActivityResponse


## Properties

Name | Type
------------ | -------------
`activityId` | string
`commandId` | string
`taskId` | string
`userId` | string
`activityType` | [ActivityType](ActivityType.md)
`performedAt` | Date
`startedAt` | Date
`endedAt` | Date
`detail` | [ActivityDetail](ActivityDetail.md)
`status` | [ActivityStatus](ActivityStatus.md)
`version` | number
`note` | string
`syncState` | [ActivitySyncState](ActivitySyncState.md)
`syncReason` | string
`previousPerformedAt` | Date

## Example

```typescript
import type { ActivityResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "activityId": null,
  "commandId": null,
  "taskId": null,
  "userId": null,
  "activityType": null,
  "performedAt": null,
  "startedAt": null,
  "endedAt": null,
  "detail": null,
  "status": null,
  "version": null,
  "note": null,
  "syncState": null,
  "syncReason": null,
  "previousPerformedAt": null,
} satisfies ActivityResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActivityResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
