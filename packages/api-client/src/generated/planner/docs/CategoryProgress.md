
# CategoryProgress


## Properties

Name | Type
------------ | -------------
`taskType` | [TaskType](TaskType.md)
`totalCount` | number
`completedCount` | number

## Example

```typescript
import type { CategoryProgress } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "taskType": null,
  "totalCount": null,
  "completedCount": null,
} satisfies CategoryProgress

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as CategoryProgress
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


