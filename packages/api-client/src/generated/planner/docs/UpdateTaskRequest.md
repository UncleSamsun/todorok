
# UpdateTaskRequest


## Properties

Name | Type
------------ | -------------
`scheduledTime` | string
`reminderEnabled` | boolean
`note` | string
`title` | string
`scheduledDate` | string
`version` | number

## Example

```typescript
import type { UpdateTaskRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "scheduledTime": null,
  "reminderEnabled": null,
  "note": null,
  "title": null,
  "scheduledDate": null,
  "version": null,
} satisfies UpdateTaskRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as UpdateTaskRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
