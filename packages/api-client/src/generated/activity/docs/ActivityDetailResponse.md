
# ActivityDetailResponse


## Properties

Name | Type
------------ | -------------
`workout` | [WorkoutDetail](WorkoutDetail.md)
`study` | [StudyDetailResponse](StudyDetailResponse.md)
`climbing` | [ClimbingDetail](ClimbingDetail.md)

## Example

```typescript
import type { ActivityDetailResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "workout": null,
  "study": null,
  "climbing": null,
} satisfies ActivityDetailResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActivityDetailResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
