
# StudyDetail


## Properties

Name | Type
------------ | -------------
`subject` | string
`durationMinutes` | number
`values` | { [key: string]: any; }
`snapshot` | { [key: string]: any; }

## Example

```typescript
import type { StudyDetail } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "subject": null,
  "durationMinutes": null,
  "values": null,
  "snapshot": null,
} satisfies StudyDetail

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as StudyDetail
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
