
# FieldDefinition


## Properties

Name | Type
------------ | -------------
`fieldId` | string
`name` | string
`type` | [TemplateFieldType](TemplateFieldType.md)
`unit` | string
`position` | number

## Example

```typescript
import type { FieldDefinition } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "fieldId": null,
  "name": null,
  "type": null,
  "unit": null,
  "position": null,
} satisfies FieldDefinition

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FieldDefinition
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
