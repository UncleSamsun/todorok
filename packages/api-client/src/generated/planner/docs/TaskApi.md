# TaskApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**completeTask**](TaskApi.md#completetask) | **POST** /tasks/{taskId}/complete |  |
| [**createTask**](TaskApi.md#createtaskoperation) | **POST** /tasks |  |
| [**deleteTask**](TaskApi.md#deletetask) | **DELETE** /tasks/{taskId} |  |
| [**getTask**](TaskApi.md#gettask) | **GET** /tasks/{taskId} |  |
| [**reopenTask**](TaskApi.md#reopentask) | **POST** /tasks/{taskId}/reopen |  |
| [**rolloverTasks**](TaskApi.md#rollovertasks) | **POST** /tasks/rollover |  |
| [**skipTask**](TaskApi.md#skiptask) | **POST** /tasks/{taskId}/skip |  |
| [**updateTask**](TaskApi.md#updatetaskoperation) | **PATCH** /tasks/{taskId} |  |



## completeTask

> TaskResponse completeTask(taskId, versionCommand)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { CompleteTaskRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // VersionCommand
    versionCommand: ...,
  } satisfies CompleteTaskRequest;

  try {
    const data = await api.completeTask(body);
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
| **versionCommand** | [VersionCommand](VersionCommand.md) |  | |

### Return type

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task 완료 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createTask

> TaskResponse createTask(createTaskRequest)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { CreateTaskOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // CreateTaskRequest
    createTaskRequest: ...,
  } satisfies CreateTaskOperationRequest;

  try {
    const data = await api.createTask(body);
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
| **createTaskRequest** | [CreateTaskRequest](CreateTaskRequest.md) |  | |

### Return type

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Task 생성 |  -  |
| **400** | 잘못된 요청 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## deleteTask

> deleteTask(taskId, version)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { DeleteTaskRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // number
    version: 789,
  } satisfies DeleteTaskRequest;

  try {
    const data = await api.deleteTask(body);
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
| **version** | `number` |  | [Defaults to `undefined`] |

### Return type

`void` (Empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Task 삭제 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getTask

> TaskResponse getTask(taskId)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { GetTaskRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetTaskRequest;

  try {
    const data = await api.getTask(body);
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

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task 상세 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## reopenTask

> TaskResponse reopenTask(taskId, versionCommand)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { ReopenTaskRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // VersionCommand
    versionCommand: ...,
  } satisfies ReopenTaskRequest;

  try {
    const data = await api.reopenTask(body);
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
| **versionCommand** | [VersionCommand](VersionCommand.md) |  | |

### Return type

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task 재개 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## rolloverTasks

> RolloverResponse rolloverTasks()



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { RolloverTasksRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  try {
    const data = await api.rolloverTasks();
    console.log(data);
  } catch (error) {
    console.error(error);
  }
}

// Run the test
example().catch(console.error);
```

### Parameters

This endpoint does not need any parameter.

### Return type

[**RolloverResponse**](RolloverResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 반복 일정 처리 성공 |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## skipTask

> TaskResponse skipTask(taskId, versionCommand)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { SkipTaskRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // VersionCommand
    versionCommand: ...,
  } satisfies SkipTaskRequest;

  try {
    const data = await api.skipTask(body);
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
| **versionCommand** | [VersionCommand](VersionCommand.md) |  | |

### Return type

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 반복 일정 처리 성공 |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateTask

> TaskResponse updateTask(taskId, updateTaskRequest)



### Example

```ts
import {
  Configuration,
  TaskApi,
} from '@todorok/api-client';
import type { UpdateTaskOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new TaskApi(config);

  const body = {
    // string
    taskId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // UpdateTaskRequest
    updateTaskRequest: ...,
  } satisfies UpdateTaskOperationRequest;

  try {
    const data = await api.updateTask(body);
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
| **updateTaskRequest** | [UpdateTaskRequest](UpdateTaskRequest.md) |  | |

### Return type

[**TaskResponse**](TaskResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Task 수정 |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

