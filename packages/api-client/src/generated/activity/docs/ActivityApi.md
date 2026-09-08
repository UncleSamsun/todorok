# ActivityApi

All URIs are relative to */api/activity/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**correctActivity**](ActivityApi.md#correctactivityoperation) | **PATCH** /activities/{activityId} |  |
| [**createActivity**](ActivityApi.md#createactivityoperation) | **POST** /activities |  |
| [**enrollProgram**](ActivityApi.md#enrollprogramoperation) | **POST** /program-enrollments |  |
| [**getActivity**](ActivityApi.md#getactivity) | **GET** /activities/{activityId} |  |
| [**getMonthlyActivitySummary**](ActivityApi.md#getmonthlyactivitysummary) | **GET** /activities/summary |  |
| [**getProgramEnrollment**](ActivityApi.md#getprogramenrollment) | **GET** /program-enrollments/{enrollmentId} |  |
| [**listActivities**](ActivityApi.md#listactivities) | **GET** /activities |  |
| [**listProgramEnrollments**](ActivityApi.md#listprogramenrollments) | **GET** /program-enrollments |  |
| [**listPrograms**](ActivityApi.md#listprograms) | **GET** /programs |  |
| [**voidActivity**](ActivityApi.md#voidactivityoperation) | **POST** /activities/{activityId}/void |  |



## correctActivity

> ActivityResponse correctActivity(activityId, correctActivityRequest)



Replace editable values using the original templateSnapshot definition. Status, task, type, detailFormat and template identity/version/snapshot are immutable. Legacy Study JSON is preserved by the server. Omitted note or interval clears them. Invalidate all cached activity lists and planner days/months for this user after saving and after synchronization; the prior performed date is returned.

### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { CorrectActivityOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string
    activityId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // CorrectActivityRequest
    correctActivityRequest: ...,
  } satisfies CorrectActivityOperationRequest;

  try {
    const data = await api.correctActivity(body);
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
| **activityId** | `string` |  | [Defaults to `undefined`] |
| **correctActivityRequest** | [CorrectActivityRequest](CorrectActivityRequest.md) |  | |

### Return type

[**ActivityResponse**](ActivityResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Corrected record with incremented revision |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |
| **413** | Template management body exceeds 1 MiB |  -  |
| **415** | Compressed template management and activity record bodies are not supported |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## createActivity

> ActivityResponse createActivity(createActivityRequest)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { CreateActivityOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // CreateActivityRequest
    createActivityRequest: ...,
  } satisfies CreateActivityOperationRequest;

  try {
    const data = await api.createActivity(body);
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
| **createActivityRequest** | [CreateActivityRequest](CreateActivityRequest.md) |  | |

### Return type

[**ActivityResponse**](ActivityResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Activity 생성 |  -  |
| **400** | 잘못된 요청 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |
| **413** | Template management body exceeds 1 MiB |  -  |
| **415** | Compressed template management and activity record bodies are not supported |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## enrollProgram

> ProgramEnrollmentResponse enrollProgram(enrollProgramRequest)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { EnrollProgramOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // EnrollProgramRequest
    enrollProgramRequest: ...,
  } satisfies EnrollProgramOperationRequest;

  try {
    const data = await api.enrollProgram(body);
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
| **enrollProgramRequest** | [EnrollProgramRequest](EnrollProgramRequest.md) |  | |

### Return type

[**ProgramEnrollmentResponse**](ProgramEnrollmentResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | Enrollment pinned to one catalog version with its first target session. |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getActivity

> ActivityResponse getActivity(activityId)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { GetActivityRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string
    activityId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetActivityRequest;

  try {
    const data = await api.getActivity(body);
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
| **activityId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**ActivityResponse**](ActivityResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Activity 상세 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getMonthlyActivitySummary

> MonthlyActivitySummaryResponse getMonthlyActivitySummary(month, activityType)



Read-only aggregate for one owner\&#39;s performed activities in a Seoul calendar month.

### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { GetMonthlyActivitySummaryRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string
    month: month_example,
    // ActivityType
    activityType: ...,
  } satisfies GetMonthlyActivitySummaryRequest;

  try {
    const data = await api.getMonthlyActivitySummary(body);
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
| **month** | `string` |  | [Defaults to `undefined`] |
| **activityType** | `ActivityType` |  | [Defaults to `undefined`] [Enum: WORKOUT, STUDY, CLIMBING] |

### Return type

[**MonthlyActivitySummaryResponse**](MonthlyActivitySummaryResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Monthly activity aggregate |  -  |
| **400** | 잘못된 요청 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getProgramEnrollment

> ProgramEnrollmentResponse getProgramEnrollment(enrollmentId)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { GetProgramEnrollmentRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string
    enrollmentId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetProgramEnrollmentRequest;

  try {
    const data = await api.getProgramEnrollment(body);
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
| **enrollmentId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**ProgramEnrollmentResponse**](ProgramEnrollmentResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Current enrollment progress and target session. |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listActivities

> ActivityPageResponse listActivities(date, cursor, limit)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { ListActivitiesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string (optional)
    date: 2013-10-20,
    // string (optional)
    cursor: cursor_example,
    // number (optional)
    limit: 56,
  } satisfies ListActivitiesRequest;

  try {
    const data = await api.listActivities(body);
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
| **date** | `string` |  | [Optional] [Defaults to `undefined`] |
| **cursor** | `string` |  | [Optional] [Defaults to `undefined`] |
| **limit** | `number` |  | [Optional] [Defaults to `20`] |

### Return type

[**ActivityPageResponse**](ActivityPageResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Activity 목록 |  -  |
| **400** | 잘못된 요청 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listProgramEnrollments

> Array&lt;ProgramEnrollmentResponse&gt; listProgramEnrollments()



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { ListProgramEnrollmentsRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  try {
    const data = await api.listProgramEnrollments();
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

[**Array&lt;ProgramEnrollmentResponse&gt;**](ProgramEnrollmentResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Current owner\&#39;s program enrollments, newest first. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listPrograms

> Array&lt;ProgramCatalogSummary&gt; listPrograms()



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { ListProgramsRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  try {
    const data = await api.listPrograms();
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

[**Array&lt;ProgramCatalogSummary&gt;**](ProgramCatalogSummary.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Imported immutable program catalog summaries. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## voidActivity

> ActivityResponse voidActivity(activityId, voidActivityRequest)



### Example

```ts
import {
  Configuration,
  ActivityApi,
} from '@todorok/api-client';
import type { VoidActivityOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new ActivityApi(config);

  const body = {
    // string
    activityId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // VoidActivityRequest
    voidActivityRequest: ...,
  } satisfies VoidActivityOperationRequest;

  try {
    const data = await api.voidActivity(body);
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
| **activityId** | `string` |  | [Defaults to `undefined`] |
| **voidActivityRequest** | [VoidActivityRequest](VoidActivityRequest.md) |  | |

### Return type

[**ActivityResponse**](ActivityResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Activity 무효화 |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 또는 상태 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
