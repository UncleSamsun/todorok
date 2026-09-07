
# FieldInput

Exactly one non-null value member matching type is required. Blank text or memo is normalized to omitted.

## Properties

Name | Type
------------ | -------------
`fieldId` | string
`type` | [TemplateFieldType](TemplateFieldType.md)
`numberValue` | number
`timeSeconds` | number
`textValue` | string
`checked` | boolean
`memoValue` | string

## Example

```typescript
import type { FieldInput } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "fieldId": null,
  "type": null,
  "numberValue": null,
  "timeSeconds": null,
  "textValue": null,
  "checked": null,
  "memoValue": null,
} satisfies FieldInput

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as FieldInput
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
