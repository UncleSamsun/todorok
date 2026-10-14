
# ListNotificationDeliveries200ResponseInner


## Properties

Name | Type
------------ | -------------
`id` | string
`status` | string
`attempts` | number
`scheduledFor` | Date
`errorCode` | string

## Example

```typescript
import type { ListNotificationDeliveries200ResponseInner } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "id": null,
  "status": null,
  "attempts": null,
  "scheduledFor": null,
  "errorCode": null,
} satisfies ListNotificationDeliveries200ResponseInner

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as ListNotificationDeliveries200ResponseInner
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
