
# TemplateVersion


## Properties

Name | Type
------------ | -------------
`templateId` | string
`templateVersion` | number
`name` | string
`fields` | [Array&lt;FieldDefinition&gt;](FieldDefinition.md)

## Example

```typescript
import type { TemplateVersion } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "templateId": null,
  "templateVersion": null,
  "name": null,
  "fields": null,
} satisfies TemplateVersion

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TemplateVersion
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
