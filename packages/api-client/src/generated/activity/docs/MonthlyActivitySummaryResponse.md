
# MonthlyActivitySummaryResponse


## Properties

Name | Type
------------ | -------------
`month` | string
`activityType` | [ActivityType](ActivityType.md)
`completedCount` | number
`durationSeconds` | number

## Example

```typescript
import type { MonthlyActivitySummaryResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "month": null,
  "activityType": null,
  "completedCount": null,
  "durationSeconds": null,
} satisfies MonthlyActivitySummaryResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as MonthlyActivitySummaryResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
