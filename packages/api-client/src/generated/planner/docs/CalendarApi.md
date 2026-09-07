# CalendarApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getCalendarSummary**](CalendarApi.md#getcalendarsummary) | **GET** /calendar |  |
| [**getDayDetail**](CalendarApi.md#getdaydetail) | **GET** /calendar/{date} |  |



## getCalendarSummary

> CalendarSummaryResponse getCalendarSummary(from, to)



시작일과 종료일을 포함한 최대 42일의 달력 요약을 반환한다.

### Example

```ts
import {
  Configuration,
  CalendarApi,
} from '@todorok/api-client';
import type { GetCalendarSummaryRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CalendarApi(config);

  const body = {
    // string
    from: 2013-10-20,
    // string | from부터 최대 42일 범위의 종료일
    to: 2013-10-20,
  } satisfies GetCalendarSummaryRequest;

  try {
    const data = await api.getCalendarSummary(body);
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
| **from** | `string` |  | [Defaults to `undefined`] |
| **to** | `string` | from부터 최대 42일 범위의 종료일 | [Defaults to `undefined`] |

### Return type

[**CalendarSummaryResponse**](CalendarSummaryResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 달력 요약 |  -  |
| **400** | 날짜 범위가 42일을 초과하거나 잘못된 요청 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getDayDetail

> DayDetailResponse getDayDetail(date)



### Example

```ts
import {
  Configuration,
  CalendarApi,
} from '@todorok/api-client';
import type { GetDayDetailRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new CalendarApi(config);

  const body = {
    // string
    date: 2013-10-20,
  } satisfies GetDayDetailRequest;

  try {
    const data = await api.getDayDetail(body);
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

[**DayDetailResponse**](DayDetailResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 선택 날짜 상세 |  -  |
| **404** | 대상을 찾을 수 없음 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)

