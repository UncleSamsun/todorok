
# TemplatePageResponse


## Properties

Name | Type
------------ | -------------
`items` | [Array&lt;TemplateResponse&gt;](TemplateResponse.md)
`nextCursor` | string

## Example

```typescript
import type { TemplatePageResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "items": null,
  "nextCursor": null,
} satisfies TemplatePageResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TemplatePageResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
