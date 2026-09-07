
# TaskRecordTemplateResponse


## Properties

Name | Type
------------ | -------------
`linked` | boolean
`templateLink` | [TemplateLink](TemplateLink.md)
`template` | [TemplateResponse](TemplateResponse.md)

## Example

```typescript
import type { TaskRecordTemplateResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "linked": null,
  "templateLink": null,
  "template": null,
} satisfies TaskRecordTemplateResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TaskRecordTemplateResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
