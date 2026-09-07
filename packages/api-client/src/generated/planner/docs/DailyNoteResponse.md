
# DailyNoteResponse


## Properties

Name | Type
------------ | -------------
`date` | string
`content` | string
`version` | number

## Example

```typescript
import type { DailyNoteResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "date": null,
  "content": null,
  "version": null,
} satisfies DailyNoteResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as DailyNoteResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
