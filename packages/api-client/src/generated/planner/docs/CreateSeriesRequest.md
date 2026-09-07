
# CreateSeriesRequest


## Properties

Name | Type
------------ | -------------
`commandId` | string
`templateSelection` | [TemplateSelection](TemplateSelection.md)
`title` | string
`taskType` | [TaskType](TaskType.md)
`startDate` | string
`endDate` | string
`note` | string
`rule` | [RecurrenceRule](RecurrenceRule.md)

## Example

```typescript
import type { CreateSeriesRequest } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "commandId": null,
  "templateSelection": null,
  "title": null,
  "taskType": null,
  "startDate": null,
  "endDate": null,
  "note": null,
  "rule": null,
} satisfies CreateSeriesRequest

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CreateSeriesRequest
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
