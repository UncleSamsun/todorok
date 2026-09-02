
# ActivityPageResponse


## Properties

Name | Type
------------ | -------------
`items` | [Array&lt;ActivityResponse&gt;](ActivityResponse.md)
`nextCursor` | string

## Example

```typescript
import type { ActivityPageResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "items": null,
  "nextCursor": null,
} satisfies ActivityPageResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ActivityPageResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


