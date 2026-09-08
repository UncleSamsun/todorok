
# EnrollProgramRequest


## Properties

Name | Type
------------ | -------------
`commandId` | string
`catalogKey` | string
`catalogVersion` | number
`initialTestValue` | number
`startWeek` | number

## Example

```typescript
import type { EnrollProgramRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "commandId": null,
  "catalogKey": null,
  "catalogVersion": null,
  "initialTestValue": null,
  "startWeek": null,
} satisfies EnrollProgramRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as EnrollProgramRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
