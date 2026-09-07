# TemplateApi

All URIs are relative to */api/activity/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**archiveTemplate**](TemplateApi.md#archivetemplateoperation) | **POST** /templates/{templateId}/archive |  |
| [**createTemplate**](TemplateApi.md#createtemplateoperation) | **POST** /templates |  |
| [**createTemplateVersion**](TemplateApi.md#createtemplateversionoperation) | **POST** /templates/{templateId}/versions |  |
| [**getTaskRecordTemplate**](TemplateApi.md#gettaskrecordtemplate) | **GET** /tasks/{taskId}/record-template |  |
| [**getTemplate**](TemplateApi.md#gettemplate) | **GET** /templates/{templateId} |  |
| [**getTemplateVersion**](TemplateApi.md#gettemplateversion) | **GET** /templates/{templateId}/versions/{templateVersion} |  |
| [**listTemplates**](TemplateApi.md#listtemplates) | **GET** /templates |  |



## archiveTemplate

> TemplateResponse archiveTemplate(templateId, archiveTemplateRequest)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { ArchiveTemplateOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // string
    templateId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // ArchiveTemplateRequest
    archiveTemplateRequest: ...,
  } satisfies ArchiveTemplateOperationRequest;

  try {
    const data = await api.archiveTemplate(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **templateId** | `string` |  | [Defaults to `undefined`] |
| **archiveTemplateRequest** | [ArchiveTemplateRequest](ArchiveTemplateRequest.md) |  | |

### Return type

[**TemplateResponse**](TemplateResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Archived template retaining every immutable version |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |
| **413** | Template management body exceeds 1 MiB |  -  |
| **415** | Compressed template management and activity record bodies are not supported |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createTemplate

> TemplateResponse createTemplate(createTemplateRequest)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { CreateTemplateOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // CreateTemplateRequest
    createTemplateRequest: ...,
  } satisfies CreateTemplateOperationRequest;

  try {
    const data = await api.createTemplate(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **createTemplateRequest** | [CreateTemplateRequest](CreateTemplateRequest.md) |  | |

### Return type

[**TemplateResponse**](TemplateResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Created immutable template version 1 |  -  |
| **400** | 잘못된 요청 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |
| **413** | Template management body exceeds 1 MiB |  -  |
| **415** | Compressed template management and activity record bodies are not supported |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createTemplateVersion

> TemplateResponse createTemplateVersion(templateId, createTemplateVersionRequest)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { CreateTemplateVersionOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // string
    templateId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CreateTemplateVersionRequest
    createTemplateVersionRequest: ...,
  } satisfies CreateTemplateVersionOperationRequest;

  try {
    const data = await api.createTemplateVersion(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **templateId** | `string` |  | [Defaults to `undefined`] |
| **createTemplateVersionRequest** | [CreateTemplateVersionRequest](CreateTemplateVersionRequest.md) |  | |

### Return type

[**TemplateResponse**](TemplateResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Template with its newly appended current version |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |
| **413** | Template management body exceeds 1 MiB |  -  |
| **415** | Compressed template management and activity record bodies are not supported |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTaskRecordTemplate

> TaskRecordTemplateResponse getTaskRecordTemplate(taskId)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { GetTaskRecordTemplateRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetTaskRecordTemplateRequest;

  try {
    const data = await api.getTaskRecordTemplate(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **taskId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**TaskRecordTemplateResponse**](TaskRecordTemplateResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Current complete definition, or linked&#x3D;false for a task without a binding. Read-only. |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTemplate

> TemplateResponse getTemplate(templateId)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { GetTemplateRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // string
    templateId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetTemplateRequest;

  try {
    const data = await api.getTemplate(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **templateId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**TemplateResponse**](TemplateResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Template identity with its complete current immutable version |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTemplateVersion

> TemplateVersion getTemplateVersion(templateId, templateVersion)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { GetTemplateVersionRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // string
    templateId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // number
    templateVersion: 789,
  } satisfies GetTemplateVersionRequest;

  try {
    const data = await api.getTemplateVersion(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **templateId** | `string` |  | [Defaults to `undefined`] |
| **templateVersion** | `number` |  | [Defaults to `undefined`] |

### Return type

[**TemplateVersion**](TemplateVersion.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Complete immutable historical version |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listTemplates

> TemplatePageResponse listTemplates(domain, kind, includeArchived, cursor, limit)



### Example

```ts
import {
  Configuration,
  TemplateApi,
} from '@todorok/api-client';
import type { ListTemplatesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TemplateApi(config);

  const body = {
    // TemplateDomain (optional)
    domain: ...,
    // TemplateKind (optional)
    kind: ...,
    // boolean (optional)
    includeArchived: true,
    // string (optional)
    cursor: cursor_example,
    // number (optional)
    limit: 56,
  } satisfies ListTemplatesRequest;

  try {
    const data = await api.listTemplates(body);
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters


| Name | Type | Description  | Notes |
|------------- | ------------- | ------------- | -------------|
| **domain** | `TemplateDomain` |  | [Optional] [Defaults to `undefined`] [Enum: STUDY, WORKOUT, CLIMBING] |
| **kind** | `TemplateKind` |  | [Optional] [Defaults to `undefined`] [Enum: STUDY_CATEGORY, FREE_WORKOUT, FREE_HANGBOARD, CLIMBING_SESSION] |
| **includeArchived** | `boolean` |  | [Optional] [Defaults to `false`] |
| **cursor** | `string` |  | [Optional] [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `20`] |

### Return type

[**TemplatePageResponse**](TemplatePageResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Owner-scoped keyset page ordered by creation time and identity descending |  -  |
| **400** | 잘못된 요청 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
