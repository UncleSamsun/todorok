
# ActivityResponse


## Properties

Name | Type
------------ | -------------
`activityId` | string
`taskId` | string
`userId` | string
`activityType` | [ActivityType](ActivityType.md)
`performedAt` | Date
`detail` | { [key: string]: any; }
`status` | [ActivityStatus](ActivityStatus.md)
`version` | number

## Example

```typescript
import type { ActivityResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "activityId": null,
  "taskId": null,
  "userId": null,
  "activityType": null,
  "performedAt": null,
  "detail": null,
  "status": null,
  "version": null,
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


