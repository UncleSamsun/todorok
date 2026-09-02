
# DayDetailResponse


## Properties

Name | Type
------------ | -------------
`date` | Date
`tasks` | [Array&lt;TaskResponse&gt;](TaskResponse.md)

## Example

```typescript
import type { DayDetailResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "date": null,
  "tasks": null,
} satisfies DayDetailResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as DayDetailResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


