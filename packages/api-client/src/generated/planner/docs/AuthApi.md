# AuthApi

All URIs are relative to */api/planner/v1*

| Method | HTTP request | Description |
|------------- | ------------- | -------------|
| [**login**](AuthApi.md#loginoperation) | **POST** /auth/login |  |
| [**logout**](AuthApi.md#logout) | **POST** /auth/logout |  |
| [**refreshSession**](AuthApi.md#refreshsession) | **POST** /auth/refresh |  |



## login

> SessionResponse login(loginRequest)



허용 목록에 있는 Origin 헤더가 필수다. 성공 시 todorok_refresh cookie를 발급한다. cookie는 HttpOnly, Secure, SameSite&#x3D;Lax, Path&#x3D;/api/planner/v1/auth이며 유효기간은 30일이다. local profile의 HTTP 개발에서만 Secure&#x3D;false를 명시적으로 허용한다. 계정당 1분에 5회, 서버가 관측한 원격 주소당 30회까지 로그인 요청을 허용한다.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@todorok/api-client';
import type { LoginOperationRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const api = new AuthApi();

  const body = {
    // LoginRequest
    loginRequest: ...,
  } satisfies LoginOperationRequest;

  try {
    const data = await api.login(body);
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
| **loginRequest** | [LoginRequest](LoginRequest.md) |  | |

### Return type

[**SessionResponse**](SessionResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: `application/json`
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 세션 발급 성공 |  * Set-Cookie - 회전형 refresh cookie. HttpOnly; Secure; SameSite&#x3D;Lax; Path&#x3D;/api/planner/v1/auth; Max-Age&#x3D;2592000 <br>  * Cache-Control -  <br>  |
| **400** | 잘못된 요청 |  -  |
| **401** | 인증 실패 |  -  |
| **403** | 허용되지 않은 Origin 또는 접근 거절 |  -  |
| **429** | 인증 요청 제한 초과 |  * Retry-After - 재시도 전에 기다릴 초 <br>  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## logout

> logout()



bearer access token과 허용된 Origin이 필수다. 현재 사용자의 todorok_refresh cookie에 해당하는 family를 폐기하고 발급과 동일한 cookie 속성으로 삭제한다. 다른 사용자 family는 변경하지 않는다.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@todorok/api-client';
import type { LogoutRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const config = new Configuration({
    // Configure HTTP bearer authorization: BearerAuth
    accessToken: "YOUR BEARER TOKEN",
  });
  const api = new AuthApi(config);

  try {
    const data = await api.logout();
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

`void` (Empty response body)

### Authorization

[BearerAuth](../README.md#BearerAuth)

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **204** | 로그아웃 성공 |  * Set-Cookie - todorok_refresh&#x3D;; Path&#x3D;/api/planner/v1/auth; Max-Age&#x3D;0; Secure; HttpOnly; SameSite&#x3D;Lax <br>  |
| **401** | 인증 실패 |  -  |
| **403** | 허용되지 않은 Origin 또는 접근 거절 |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)


## refreshSession

> SessionResponse refreshSession()



허용된 Origin 헤더와 todorok_refresh cookie가 필수다. 성공할 때마다 cookie를 회전한다. 재사용하면 session family 전체를 폐기한다. 실패 시 같은 cookie 속성으로 삭제한다.

### Example

```ts
import {
  Configuration,
  AuthApi,
} from '@todorok/api-client';
import type { RefreshSessionRequest } from '@todorok/api-client';

async function example() {
  console.log("🚀 Testing @todorok/api-client SDK...");
  const api = new AuthApi();

  try {
    const data = await api.refreshSession();
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

[**SessionResponse**](SessionResponse.md)

### Authorization

No authorization required

### HTTP request headers

- **Content-Type**: Not defined
- **Accept**: `application/json`, `application/problem+json`


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
| **200** | 세션 발급 성공 |  * Set-Cookie - 회전형 refresh cookie. HttpOnly; Secure; SameSite&#x3D;Lax; Path&#x3D;/api/planner/v1/auth; Max-Age&#x3D;2592000 <br>  * Cache-Control -  <br>  |
| **401** | 인증 실패 |  -  |
| **403** | 허용되지 않은 Origin 또는 접근 거절 |  -  |
| **429** | 인증 요청 제한 초과 |  * Retry-After - 재시도 전에 기다릴 초 <br>  |

[[Back to top]](#) [[Back to API list]](../README.md#api-endpoints) [[Back to Model list]](../README.md#models) [[Back to README]](../README.md)
