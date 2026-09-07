
# TemplateResponse


## Properties

Name | Type
------------ | -------------
`templateId` | string
`domain` | [TemplateDomain](TemplateDomain.md)
`kind` | [TemplateKind](TemplateKind.md)
`archived` | boolean
`revision` | number
`currentVersion` | [TemplateVersion](TemplateVersion.md)

## Example

```typescript
import type { TemplateResponse } from '@todorok/api-client'

// TODO: Update the object below with actual values
const example = {
  "templateId": null,
  "domain": null,
  "kind": null,
  "archived": null,
  "revision": null,
  "currentVersion": null,
} satisfies TemplateResponse

console.log(example)

// Convert the instance to a JSON string
const exampleJSON: string = JSON.stringify(example)
console.log(exampleJSON)

// Parse the JSON string back to an object
const exampleParsed = JSON.parse(exampleJSON) as TemplateResponse
console.log(exampleParsed)
```

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
