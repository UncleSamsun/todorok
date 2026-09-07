# SeriesApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**archiveSeries**](SeriesApi.md#archiveseries) | **POST** /series/{seriesId}/archive |  |
| [**createSeries**](SeriesApi.md#createseriesoperation) | **POST** /series |  |
| [**getSeries**](SeriesApi.md#getseries) | **GET** /series/{seriesId} |  |
| [**updateSeries**](SeriesApi.md#updateseriesoperation) | **PATCH** /series/{seriesId} |  |



## archiveSeries

> SeriesResponse archiveSeries(seriesId, versionCommand)



### Example

```ts
import {
  Configuration,
  SeriesApi,
} from '@todorok/api-client';
import type { ArchiveSeriesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SeriesApi(config);

  const body = {
    // string
    seriesId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // VersionCommand
    versionCommand: ...,
  } satisfies ArchiveSeriesRequest;

  try {
    const data = await api.archiveSeries(body);
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
| **seriesId** | `string` |  | [Defaults to `undefined`] |
| **versionCommand** | [VersionCommand](VersionCommand.md) |  | |

### Return type

[**SeriesResponse**](SeriesResponse.md)

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


## createSeries

> SeriesResponse createSeries(createSeriesRequest)



### Example

```ts
import {
  Configuration,
  SeriesApi,
} from '@todorok/api-client';
import type { CreateSeriesOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SeriesApi(config);

  const body = {
    // CreateSeriesRequest
    createSeriesRequest: ...,
  } satisfies CreateSeriesOperationRequest;

  try {
    const data = await api.createSeries(body);
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
| **createSeriesRequest** | [CreateSeriesRequest](CreateSeriesRequest.md) |  | |

### Return type

[**SeriesResponse**](SeriesResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **201** | 반복 일정 처리 성공 |  -  |
| **400** | 잘못된 요청 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getSeries

> SeriesResponse getSeries(seriesId)



### Example

```ts
import {
  Configuration,
  SeriesApi,
} from '@todorok/api-client';
import type { GetSeriesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SeriesApi(config);

  const body = {
    // string
    seriesId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
  } satisfies GetSeriesRequest;

  try {
    const data = await api.getSeries(body);
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
| **seriesId** | `string` |  | [Defaults to `undefined`] |

### Return type

[**SeriesResponse**](SeriesResponse.md)

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


## updateSeries

> SeriesResponse updateSeries(seriesId, updateSeriesRequest)



### Example

```ts
import {
  Configuration,
  SeriesApi,
} from '@todorok/api-client';
import type { UpdateSeriesOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new SeriesApi(config);

  const body = {
    // string
    seriesId: 38400000-8cf0-11bd-b23e-10b96e4ef00d,
    // UpdateSeriesRequest
    updateSeriesRequest: ...,
  } satisfies UpdateSeriesOperationRequest;

  try {
    const data = await api.updateSeries(body);
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
| **seriesId** | `string` |  | [Defaults to `undefined`] |
| **updateSeriesRequest** | [UpdateSeriesRequest](UpdateSeriesRequest.md) |  | |

### Return type

[**SeriesResponse**](SeriesResponse.md)

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
