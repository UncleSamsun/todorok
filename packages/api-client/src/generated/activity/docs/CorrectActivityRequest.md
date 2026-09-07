
# CorrectActivityRequest


## Properties

Name | Type
------------ | -------------
`expectedVersion` | number
`performedAt` | Date
`startedAt` | Date
`endedAt` | Date
`note` | string
`detail` | [ActivityDetail](ActivityDetail.md)

## Example

```typescript
import type { CorrectActivityRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "expectedVersion": null,
  "performedAt": null,
  "startedAt": null,
  "endedAt": null,
  "note": null,
  "detail": null,
} satisfies CorrectActivityRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CorrectActivityRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
