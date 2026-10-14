# PushApi

All URIs are relative to */api/notification/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**deletePushSubscription**](PushApi.md#deletepushsubscriptionoperation) | **DELETE** /push/subscriptions |  |
| [**getPushConfig**](PushApi.md#getpushconfig) | **GET** /push/config |  |
| [**listNotificationDeliveries**](PushApi.md#listnotificationdeliveries) | **GET** /deliveries |  |
| [**savePushSubscription**](PushApi.md#savepushsubscription) | **POST** /push/subscriptions |  |



## deletePushSubscription

> deletePushSubscription(deletePushSubscriptionRequest)



### Example

```ts
import {
  Configuration,
  PushApi,
} from '@todorok/api-client';
import type { DeletePushSubscriptionOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PushApi(config);

  const body = {
    // DeletePushSubscriptionRequest
    deletePushSubscriptionRequest: ...,
  } satisfies DeletePushSubscriptionOperationRequest;

  try {
    const data = await api.deletePushSubscription(body);
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
| **deletePushSubscriptionRequest** | [DeletePushSubscriptionRequest](DeletePushSubscriptionRequest.md) |  | |

### Return type

`void` (Empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: Not defined


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | Deleted only if owned by the authenticated account. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## getPushConfig

> GetPushConfig200Response getPushConfig()



### Example

```ts
import {
  Configuration,
  PushApi,
} from '@todorok/api-client';
import type { GetPushConfigRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PushApi(config);

  try {
    const data = await api.getPushConfig();
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

[**GetPushConfig200Response**](GetPushConfig200Response.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Public VAPID key, empty when push is unavailable. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## listNotificationDeliveries

> Array&lt;ListNotificationDeliveries200ResponseInner&gt; listNotificationDeliveries()



### Example

```ts
import {
  Configuration,
  PushApi,
} from '@todorok/api-client';
import type { ListNotificationDeliveriesRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PushApi(config);

  try {
    const data = await api.listNotificationDeliveries();
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

[**Array&lt;ListNotificationDeliveries200ResponseInner&gt;**](ListNotificationDeliveries200ResponseInner.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Latest 50 deliveries for the authenticated owner. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## savePushSubscription

> SavePushSubscription200Response savePushSubscription(pushSubscriptionRequest)



### Example

```ts
import {
  Configuration,
  PushApi,
} from '@todorok/api-client';
import type { SavePushSubscriptionRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new PushApi(config);

  const body = {
    // PushSubscriptionRequest
    pushSubscriptionRequest: ...,
  } satisfies SavePushSubscriptionRequest;

  try {
    const data = await api.savePushSubscription(body);
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
| **pushSubscriptionRequest** | [PushSubscriptionRequest](PushSubscriptionRequest.md) |  | |

### Return type

[**SavePushSubscription200Response**](SavePushSubscription200Response.md)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | Idempotent registration for this owner. Never returns subscription secrets. |  -  |
| **400** | Shared problem response. |  -  |
| **401** | Shared problem response. |  -  |
| **409** | Shared problem response. |  -  |
| **503** | Shared problem response. |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
