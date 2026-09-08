
# ClimbingDetail


## Properties

Name | Type
------------ | -------------
`durationSeconds` | number
`rounds` | [Array&lt;ClimbingRound&gt;](ClimbingRound.md)
`fields` | [Array&lt;FieldInput&gt;](FieldInput.md)

## Example

```typescript
import type { ClimbingDetail } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "durationSeconds": null,
  "rounds": null,
  "fields": null,
} satisfies ClimbingDetail

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ClimbingDetail
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
