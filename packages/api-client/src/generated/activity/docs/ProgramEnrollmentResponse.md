
# ProgramEnrollmentResponse


## Properties

Name | Type
------------ | -------------
`enrollmentId` | string
`catalogKey` | string
`catalogVersion` | number
`recommendedWeek` | number
`startWeek` | number
`currentWeek` | number
`currentSession` | number
`status` | string
`target` | [ProgramSessionTarget](ProgramSessionTarget.md)

## Example

```typescript
import type { ProgramEnrollmentResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "enrollmentId": null,
  "catalogKey": null,
  "catalogVersion": null,
  "recommendedWeek": null,
  "startWeek": null,
  "currentWeek": null,
  "currentSession": null,
  "status": null,
  "target": null,
} satisfies ProgramEnrollmentResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ProgramEnrollmentResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
