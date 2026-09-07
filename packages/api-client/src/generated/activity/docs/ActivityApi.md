# ActivityApi

All URIs are relative to */api/activity/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**createActivity**](ActivityApi.md#createactivityoperation) | **POST** /activities |  |
| [**getActivity**](ActivityApi.md#getactivity) | **GET** /activities/{activityId} |  |
| [**listActivities**](ActivityApi.md#listactivities) | **GET** /activities |  |
| [**voidActivity**](ActivityApi.md#voidactivityoperation) | **POST** /activities/{activityId}/void |  |



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

