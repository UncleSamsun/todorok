
# SeriesResponse


## Properties

Name | Type
------------ | -------------
`seriesId` | string
`userId` | string
`title` | string
`taskType` | [TaskType](TaskType.md)
`startDate` | string
`endDate` | string
`note` | string
`rule` | [RecurrenceRule](RecurrenceRule.md)
`archived` | boolean
`version` | number

## Example

```typescript
import type { SeriesResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "seriesId": null,
  "userId": null,
  "title": null,
  "taskType": null,
  "startDate": null,
  "endDate": null,
  "note": null,
  "rule": null,
  "archived": null,
  "version": null,
} satisfies SeriesResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as SeriesResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
