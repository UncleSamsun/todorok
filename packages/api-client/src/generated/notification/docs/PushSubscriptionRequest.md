
# PushSubscriptionRequest


## Properties

Name | Type
------------ | -------------
`endpoint` | string
`keys` | [PushSubscriptionRequestKeys](PushSubscriptionRequestKeys.md)

## Example

```typescript
import type { PushSubscriptionRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "endpoint": null,
  "keys": null,
} satisfies PushSubscriptionRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as PushSubscriptionRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
