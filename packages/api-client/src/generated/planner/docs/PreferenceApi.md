# PreferenceApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**getPreferences**](PreferenceApi.md#getpreferences) | **GET** /preferences |  |
| [**updatePreferences**](PreferenceApi.md#updatepreferences) | **PUT** /preferences |  |



## getPreferences

> UserPreferencesResponse getPreferences()



### Example

```ts
import {
  Configuration,
  PreferenceApi,
} from '@todorok/api-client';
import type { GetPreferencesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PreferenceApi(config);

  try {
    const data = await api.getPreferences();
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

[**UserPreferencesResponse**](UserPreferencesResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Authenticated account preferences. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## updatePreferences

> UserPreferencesResponse updatePreferences(updateUserPreferencesRequest)



### Example

```ts
import {
  Configuration,
  PreferenceApi,
} from '@todorok/api-client';
import type { UpdatePreferencesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PreferenceApi(config);

  const body = {
    // UpdateUserPreferencesRequest
    updateUserPreferencesRequest: ...,
  } satisfies UpdatePreferencesRequest;

  try {
    const data = await api.updatePreferences(body);
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
| **updateUserPreferencesRequest** | [UpdateUserPreferencesRequest](UpdateUserPreferencesRequest.md) |  | |

### Return type

[**UserPreferencesResponse**](UserPreferencesResponse.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Updated preferences. |  -  |
| **400** | 잘못된 요청 |  -  |
| **409** | 버전 충돌 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
