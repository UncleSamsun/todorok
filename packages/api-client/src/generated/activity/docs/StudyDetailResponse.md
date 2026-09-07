
# StudyDetailResponse


## Properties

Name | Type
------------ | -------------
`subject` | string
`durationMinutes` | number
`fields` | [Array&lt;FieldInput&gt;](FieldInput.md)

## Example

```typescript
import type { StudyDetailResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "subject": null,
  "durationMinutes": null,
  "fields": null,
} satisfies StudyDetailResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as StudyDetailResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
