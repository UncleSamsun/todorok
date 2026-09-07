# NoteApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getDailyNote**](NoteApi.md#getdailynote) | **GET** /notes/{date} |  |
| [**updateDailyNote**](NoteApi.md#updatedailynoteoperation) | **PATCH** /notes/{date} |  |



## getDailyNote

> DailyNoteResponse getDailyNote(date)



### Example

```ts
import {
  Configuration,
  NoteApi,
} from '@todorok/api-client';
import type { GetDailyNoteRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NoteApi(config);

  const body = {
    // string
    date: 2013-10-20,
  } satisfies GetDailyNoteRequest;

  try {
    const data = await api.getDailyNote(body);
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
| **date** | `string` |  | [Defaults to `undefined`] |

### Return type

[**DailyNoteResponse**](DailyNoteResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 날짜 메모 (미작성 version은 null) |  -  |
| **400** | 잘못된 요청 |  -  |
| **401** | 인증 실패 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updateDailyNote

> DailyNoteResponse updateDailyNote(date, updateDailyNoteRequest)



### Example

```ts
import {
  Configuration,
  NoteApi,
} from '@todorok/api-client';
import type { UpdateDailyNoteOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new NoteApi(config);

  const body = {
    // string
    date: 2013-10-20,
    // UpdateDailyNoteRequest
    updateDailyNoteRequest: ...,
  } satisfies UpdateDailyNoteOperationRequest;

  try {
    const data = await api.updateDailyNote(body);
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
| **date** | `string` |  | [Defaults to `undefined`] |
| **updateDailyNoteRequest** | [UpdateDailyNoteRequest](UpdateDailyNoteRequest.md) |  | |

### Return type

[**DailyNoteResponse**](DailyNoteResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 저장된 날짜 메모 |  -  |
| **400** | 잘못된 요청 |  -  |
| **401** | 인증 실패 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
