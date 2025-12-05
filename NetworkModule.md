# NetworkModule - Reusable Network Layer Architecture

## Overview

**Purpose**: Provide a reusable, interface-based network communication layer using Retrofit + OkHttp + Kotlin Serialization that can be used across multiple Android projects.

**Core Principle**: **Public Interface, Private Implementation** - Expose only what clients need, hide all internal complexity.

**Key Features**:
- Type-safe Retrofit service creation
- Configurable OkHttp client (timeouts, interceptors, retry policies)
- Kotlin Serialization support
- Comprehensive error handling with typed errors
- Request/response logging
- Retry mechanisms
- Certificate pinning support
- Easy testing with mock implementations

---

## Architecture Diagram

```
┌─────────────────────────────────────────────────────────────┐
│                     Consumer Modules                        │
│              (LlmModule, ApiModule, etc.)                   │
└──────────────────────────┬──────────────────────────────────┘
                           │ depends on
┌──────────────────────────▼──────────────────────────────────┐
│                  NetworkModule Public API                   │
│  ┌────────────────────────────────────────────────────┐    │
│  │  NetworkClientFactory (interface)                  │    │
│  │  - createService<T>()                              │    │
│  │  - NetworkConfig (data class)                      │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  NetworkError (sealed class)                       │    │
│  │  - NoConnection, Timeout, ServerError, etc.        │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  NetworkResult<T> (sealed class)                   │    │
│  │  - Success<T>, Error                               │    │
│  └────────────────────────────────────────────────────┘    │
└──────────────────────────┬──────────────────────────────────┘
                           │ uses internally
┌──────────────────────────▼──────────────────────────────────┐
│              NetworkModule Internal Implementation          │
│  ┌────────────────────────────────────────────────────┐    │
│  │  NetworkClientFactoryImpl                          │    │
│  │  - Retrofit builder                                │    │
│  │  - OkHttpClient factory                            │    │
│  │  - Interceptor chain management                    │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  Interceptors (internal)                           │    │
│  │  - LoggingInterceptor                              │    │
│  │  - RetryInterceptor                                │    │
│  │  - ErrorMappingInterceptor                         │    │
│  └────────────────────────────────────────────────────┘    │
│  ┌────────────────────────────────────────────────────┐    │
│  │  OkHttp / Retrofit / Kotlin Serialization          │    │
│  └────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
```

---

## Package Structure

```
com.networkmodule/
├── api/                          # PUBLIC API - What consumers use
│   ├── NetworkClientFactory.kt   # Main factory interface
│   ├── NetworkConfig.kt          # Configuration data class
│   ├── NetworkResult.kt          # Result wrapper
│   ├── NetworkError.kt           # Error types
│   └── NetworkLogger.kt          # Logging interface
│
├── internal/                     # INTERNAL - Hidden from consumers
│   ├── factory/
│   │   └── NetworkClientFactoryImpl.kt  # Retrofit/OkHttp setup
│   ├── interceptor/
│   │   ├── LoggingInterceptor.kt
│   │   ├── RetryInterceptor.kt
│   │   └── HeaderInterceptor.kt
│   ├── adapter/
│   │   └── NetworkResultCallAdapter.kt  # Auto-wrap responses
│   └── util/
│       ├── ErrorMapper.kt
│       └── NetworkUtils.kt
│
└── di/                           # Dependency Injection
    └── NetworkModule.kt          # Hilt module
```

---

## Public API Design

### 1. NetworkClientFactory (Core Interface)

```kotlin
package com.networkmodule.api

/**
 * Factory for creating type-safe Retrofit service instances.
 *
 * This is the PRIMARY interface for consuming modules.
 *
 * Usage:
 * ```
 * val factory: NetworkClientFactory = ... // Injected via Hilt
 * val geminiService = factory.createService<GeminiApiService>(
 *     baseUrl = "https://generativelanguage.googleapis.com/",
 *     config = NetworkConfig(
 *         connectTimeoutSeconds = 30,
 *         readTimeoutSeconds = 60,
 *         enableLogging = BuildConfig.DEBUG
 *     )
 * )
 * ```
 */
interface NetworkClientFactory {

    /**
     * Creates a Retrofit service instance for the specified API interface.
     *
     * @param T The Retrofit service interface type
     * @param baseUrl The base URL for this service
     * @param config Optional network configuration (uses defaults if null)
     * @return Configured service instance
     */
    fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        config: NetworkConfig = NetworkConfig.DEFAULT
    ): T

    /**
     * Inline reified version for Kotlin callers (preferred).
     */
    inline fun <reified T> createService(
        baseUrl: String,
        config: NetworkConfig = NetworkConfig.DEFAULT
    ): T = createService(T::class.java, baseUrl, config)
}
```

### 2. NetworkConfig (Configuration)

```kotlin
package com.networkmodule.api

import kotlinx.serialization.Serializable

/**
 * Configuration for network client behavior.
 *
 * Immutable configuration object following builder pattern.
 */
@Serializable
data class NetworkConfig(
    val connectTimeoutSeconds: Long = 30,
    val readTimeoutSeconds: Long = 60,
    val writeTimeoutSeconds: Long = 60,
    val enableLogging: Boolean = false,
    val retryOnConnectionFailure: Boolean = true,
    val maxRetries: Int = 3,
    val retryDelayMillis: Long = 1000,
    val customHeaders: Map<String, String> = emptyMap(),
    val certificatePinning: CertificatePinningConfig? = null,
    val cacheConfig: CacheConfig? = null
) {
    companion object {
        val DEFAULT = NetworkConfig()

        val STREAMING = NetworkConfig(
            readTimeoutSeconds = 300,  // 5 minutes for streaming
            writeTimeoutSeconds = 300
        )

        val FAST_FAIL = NetworkConfig(
            connectTimeoutSeconds = 10,
            readTimeoutSeconds = 15,
            maxRetries = 1
        )
    }
}

/**
 * Certificate pinning configuration (optional security feature).
 */
@Serializable
data class CertificatePinningConfig(
    val hostname: String,
    val pins: List<String>  // SHA-256 hashes
)

/**
 * HTTP cache configuration.
 */
@Serializable
data class CacheConfig(
    val maxSizeMB: Int = 10,
    val maxAgeSeconds: Long = 300
)
```

### 3. NetworkResult (Type-safe Result)

```kotlin
package com.networkmodule.api

/**
 * Type-safe wrapper for network operations.
 *
 * Prevents throwing exceptions, forces explicit error handling.
 *
 * Usage:
 * ```
 * when (val result = repository.fetchData()) {
 *     is NetworkResult.Success -> handleSuccess(result.data)
 *     is NetworkResult.Error -> handleError(result.error)
 * }
 * ```
 */
sealed class NetworkResult<out T> {

    /**
     * Successful response with data.
     */
    data class Success<T>(
        val data: T,
        val statusCode: Int = 200,
        val headers: Map<String, String> = emptyMap()
    ) : NetworkResult<T>()

    /**
     * Failed response with structured error.
     */
    data class Error(
        val error: NetworkError,
        val statusCode: Int? = null,
        val rawResponse: String? = null
    ) : NetworkResult<Nothing>()

    // Helper methods
    fun isSuccess(): Boolean = this is Success
    fun isError(): Boolean = this is Error

    fun getOrNull(): T? = when (this) {
        is Success -> data
        is Error -> null
    }

    fun getOrThrow(): T = when (this) {
        is Success -> data
        is Error -> throw error.toException()
    }

    inline fun <R> map(transform: (T) -> R): NetworkResult<R> = when (this) {
        is Success -> Success(transform(data), statusCode, headers)
        is Error -> this
    }

    inline fun onSuccess(action: (T) -> Unit): NetworkResult<T> {
        if (this is Success) action(data)
        return this
    }

    inline fun onError(action: (NetworkError) -> Unit): NetworkResult<T> {
        if (this is Error) action(error)
        return this
    }
}

// Extension for suspend functions
suspend fun <T> NetworkResult<T>.suspendOnSuccess(
    action: suspend (T) -> Unit
): NetworkResult<T> {
    if (this is NetworkResult.Success) action(data)
    return this
}
```

### 4. NetworkError (Typed Errors)

```kotlin
package com.networkmodule.api

/**
 * Comprehensive network error types.
 *
 * Covers all failure scenarios with actionable information.
 */
sealed class NetworkError {

    abstract val message: String
    abstract val cause: Throwable?

    /**
     * No internet connection available.
     */
    data class NoConnection(
        override val message: String = "No internet connection",
        override val cause: Throwable? = null
    ) : NetworkError()

    /**
     * Request timeout (connection or read/write).
     */
    data class Timeout(
        override val message: String = "Request timed out",
        override val cause: Throwable? = null,
        val timeoutType: TimeoutType = TimeoutType.READ
    ) : NetworkError() {
        enum class TimeoutType { CONNECT, READ, WRITE }
    }

    /**
     * Client-side error (4xx status codes).
     */
    data class ClientError(
        override val message: String,
        override val cause: Throwable? = null,
        val statusCode: Int,
        val errorBody: String? = null
    ) : NetworkError() {

        fun isUnauthorized() = statusCode == 401
        fun isForbidden() = statusCode == 403
        fun isNotFound() = statusCode == 404
        fun isRateLimited() = statusCode == 429
    }

    /**
     * Server-side error (5xx status codes).
     */
    data class ServerError(
        override val message: String = "Server error",
        override val cause: Throwable? = null,
        val statusCode: Int,
        val errorBody: String? = null
    ) : NetworkError()

    /**
     * Response parsing/serialization error.
     */
    data class ParseError(
        override val message: String = "Failed to parse response",
        override val cause: Throwable? = null,
        val rawResponse: String? = null
    ) : NetworkError()

    /**
     * SSL/TLS certificate error.
     */
    data class SslError(
        override val message: String = "SSL certificate validation failed",
        override val cause: Throwable? = null
    ) : NetworkError()

    /**
     * Unknown/unexpected error.
     */
    data class Unknown(
        override val message: String = "Unknown error occurred",
        override val cause: Throwable? = null
    ) : NetworkError()

    /**
     * Converts error to throwable for legacy code.
     */
    fun toException(): Exception = when (this) {
        is NoConnection -> java.net.UnknownHostException(message)
        is Timeout -> java.net.SocketTimeoutException(message)
        is ClientError -> HttpException(statusCode, message, cause)
        is ServerError -> HttpException(statusCode, message, cause)
        is ParseError -> kotlinx.serialization.SerializationException(message, cause)
        is SslError -> javax.net.ssl.SSLException(message, cause)
        is Unknown -> RuntimeException(message, cause)
    }
}

/**
 * Custom HTTP exception.
 */
class HttpException(
    val statusCode: Int,
    override val message: String,
    override val cause: Throwable? = null
) : Exception(message, cause)
```

### 5. NetworkLogger (Optional Interface)

```kotlin
package com.networkmodule.api

/**
 * Interface for custom logging implementations.
 *
 * Allows consumers to integrate with their logging framework.
 */
interface NetworkLogger {
    fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?)
    fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long)
    fun logError(url: String, error: NetworkError)

    companion object {
        /**
         * Default Android Logcat implementation.
         */
        val DEFAULT: NetworkLogger = object : NetworkLogger {
            override fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?) {
                android.util.Log.d("Network", "→ $method $url")
                if (!body.isNullOrBlank()) {
                    android.util.Log.v("Network", "Body: $body")
                }
            }

            override fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long) {
                android.util.Log.d("Network", "← $statusCode $url (${durationMs}ms)")
            }

            override fun logError(url: String, error: NetworkError) {
                android.util.Log.e("Network", "✗ $url: ${error.message}", error.cause)
            }
        }

        /**
         * No-op logger for production.
         */
        val NONE: NetworkLogger = object : NetworkLogger {
            override fun logRequest(url: String, method: String, headers: Map<String, String>, body: String?) {}
            override fun logResponse(url: String, statusCode: Int, body: String?, durationMs: Long) {}
            override fun logError(url: String, error: NetworkError) {}
        }
    }
}
```

---

## Hilt Integration

```kotlin
package com.networkmodule.di

import android.content.Context
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkLogger
import com.networkmodule.internal.factory.NetworkClientFactoryImpl
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideNetworkClientFactory(
        @ApplicationContext context: Context,
        logger: NetworkLogger
    ): NetworkClientFactory {
        return NetworkClientFactoryImpl(
            cacheDir = context.cacheDir,
            defaultLogger = logger
        )
    }

    @Provides
    @Singleton
    fun provideNetworkLogger(): NetworkLogger {
        return if (BuildConfig.DEBUG) {
            NetworkLogger.DEFAULT
        } else {
            NetworkLogger.NONE
        }
    }
}
```

---

## Testing Support

### 1. Fake Implementation for Testing

```kotlin
package com.networkmodule.testing

import com.networkmodule.api.*

/**
 * Fake implementation for unit tests.
 *
 * Allows complete control over responses without network calls.
 */
class FakeNetworkClientFactory : NetworkClientFactory {

    private val serviceMap = mutableMapOf<Class<*>, Any>()

    override fun <T> createService(
        serviceClass: Class<T>,
        baseUrl: String,
        config: NetworkConfig
    ): T {
        @Suppress("UNCHECKED_CAST")
        return serviceMap[serviceClass] as? T
            ?: throw IllegalStateException("No fake service registered for ${serviceClass.simpleName}")
    }

    fun <T> registerFakeService(serviceClass: Class<T>, fakeImpl: T) {
        serviceMap[serviceClass] = fakeImpl as Any
    }

    inline fun <reified T> registerFakeService(fakeImpl: T) {
        registerFakeService(T::class.java, fakeImpl)
    }
}
```

### 2. Test Utilities

```kotlin
package com.networkmodule.testing

import com.networkmodule.api.NetworkError
import com.networkmodule.api.NetworkResult

/**
 * Helper functions for testing.
 */
object NetworkTestUtils {

    fun <T> successResult(data: T, statusCode: Int = 200): NetworkResult<T> {
        return NetworkResult.Success(data, statusCode)
    }

    fun <T> errorResult(error: NetworkError, statusCode: Int? = null): NetworkResult<T> {
        return NetworkResult.Error(error, statusCode)
    }

    fun noConnectionError(): NetworkError = NetworkError.NoConnection()

    fun timeoutError(): NetworkError = NetworkError.Timeout()

    fun unauthorizedError(): NetworkError = NetworkError.ClientError(
        message = "Unauthorized",
        statusCode = 401
    )

    fun serverError(): NetworkError = NetworkError.ServerError(
        message = "Internal Server Error",
        statusCode = 500
    )
}
```

---

## Usage Examples

### Example 1: Creating a Service (Consumer Code)

```kotlin
// In LlmModule or any consumer module
@Module
@InstallIn(SingletonComponent::class)
object LlmNetworkModule {

    @Provides
    @Singleton
    @Named("gemini")
    fun provideGeminiService(
        factory: NetworkClientFactory
    ): GeminiApiService {
        return factory.createService(
            baseUrl = "https://generativelanguage.googleapis.com/",
            config = NetworkConfig(
                readTimeoutSeconds = 60,
                enableLogging = BuildConfig.DEBUG,
                maxRetries = 3
            )
        )
    }

    @Provides
    @Singleton
    @Named("gpt")
    fun provideGptService(
        factory: NetworkClientFactory
    ): GptApiService {
        return factory.createService(
            baseUrl = "https://api.openai.com/",
            config = NetworkConfig.STREAMING  // Use preset
        )
    }
}
```

### Example 2: Defining Service Interface

```kotlin
// In LlmModule
interface GeminiApiService {

    @POST("v1/models/gemini-pro:generateContent")
    suspend fun generateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): NetworkResult<GenerateContentResponse>

    @POST("v1/models/gemini-pro:streamGenerateContent")
    suspend fun streamGenerateContent(
        @Header("x-goog-api-key") apiKey: String,
        @Body request: GenerateContentRequest
    ): NetworkResult<GenerateContentStreamResponse>
}
```

### Example 3: Using in Repository

```kotlin
// In LlmModule data layer
class GeminiRepositoryImpl @Inject constructor(
    @Named("gemini") private val apiService: GeminiApiService,
    private val apiKeyProvider: ApiKeyProvider
) : LlmRepository {

    override suspend fun generateText(prompt: String): Result<String> {
        val apiKey = apiKeyProvider.getGeminiApiKey()
            ?: return Result.failure(IllegalStateException("Gemini API key not configured"))

        val request = GenerateContentRequest(
            contents = listOf(Content(parts = listOf(Part(text = prompt))))
        )

        return when (val result = apiService.generateContent(apiKey, request)) {
            is NetworkResult.Success -> {
                val text = result.data.candidates.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null) {
                    Result.success(text)
                } else {
                    Result.failure(IllegalStateException("Empty response"))
                }
            }
            is NetworkResult.Error -> {
                Result.failure(result.error.toException())
            }
        }
    }
}
```

---

## Implementation Phases

### Phase 1: Core Infrastructure (2-3 days)
1. Create package structure
2. Define public API interfaces (NetworkClientFactory, NetworkConfig, NetworkResult, NetworkError)
3. Implement NetworkClientFactoryImpl with basic Retrofit/OkHttp setup
4. Write unit tests for error mapping

### Phase 2: Advanced Features (2-3 days)
5. Implement interceptors (logging, retry, headers)
6. Add NetworkResultCallAdapter for auto-wrapping
7. Implement certificate pinning support
8. Add caching support

### Phase 3: Testing & Documentation (1-2 days)
9. Create FakeNetworkClientFactory for testing
10. Write comprehensive unit tests
11. Add KDoc documentation
12. Create usage examples

### Phase 4: Integration (1 day)
13. Test with LlmModule
14. Performance testing
15. Final polish

---

## Technical Challenges & Solutions

### Challenge 1: Streaming Responses
**Problem**: LLMs like GPT/Gemini support streaming responses, but Retrofit doesn't natively support SSE (Server-Sent Events).

**Solution**:
- For non-streaming: Use NetworkResult<T> as shown
- For streaming: Provide separate StreamingNetworkClient using OkHttp directly with Flow
```kotlin
interface StreamingNetworkClient {
    fun <T> streamRequest(
        request: Request,
        deserializer: (String) -> T
    ): Flow<NetworkResult<T>>
}
```

### Challenge 2: Different Error Formats
**Problem**: Different APIs return errors in different formats (JSON, plain text, HTML).

**Solution**: ErrorMappingInterceptor tries to parse JSON error bodies, falls back to raw string.

### Challenge 3: API Key Security
**Problem**: API keys should not be logged or stored in NetworkModule.

**Solution**: NetworkModule is agnostic to auth - consumers pass keys via headers or interceptors.

---

## Dependencies (build.gradle.kts)

```kotlin
dependencies {
    // Networking
    implementation("com.squareup.retrofit2:retrofit:2.9.0")
    implementation("com.squareup.okhttp3:okhttp:4.12.0")
    implementation("com.squareup.okhttp3:logging-interceptor:4.12.0")

    // Kotlin Serialization
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.2")
    implementation("com.jakewharton.retrofit:retrofit2-kotlinx-serialization-converter:1.0.0")

    // Hilt
    implementation("com.google.dagger:hilt-android:2.50")
    ksp("com.google.dagger:hilt-compiler:2.50")

    // Testing
    testImplementation("junit:junit:4.13.2")
    testImplementation("io.mockk:mockk:1.13.8")
    testImplementation("org.jetbrains.kotlinx:kotlinx-coroutines-test:1.7.3")
    testImplementation("com.squareup.okhttp3:mockwebserver:4.12.0")
}
```

---

## Security Considerations

1. **Never log sensitive headers** (Authorization, API keys) - LoggingInterceptor filters these
2. **Use HTTPS only** - Enforce in production builds
3. **Certificate pinning** for critical services
4. **Timeout enforcement** to prevent hanging connections
5. **Request size limits** to prevent memory issues

---

## Future Enhancements

1. **GraphQL Support**: Add Apollo client integration
2. **WebSocket Support**: For real-time communication
3. **Metrics Collection**: Track request latency, success rates
4. **Circuit Breaker**: Fail fast when service is down
5. **Request Deduplication**: Prevent duplicate concurrent requests

---

## Summary

**What NetworkModule Provides**:
- Interface-based network client creation
- Type-safe error handling with NetworkResult
- Configurable timeouts, retries, logging
- Easy testing with fake implementations
- Zero Android-specific dependencies in domain layer

**What It Does NOT Provide**:
- Business logic (that's in consumer modules)
- Authentication (consumers handle via headers/interceptors)
- Data models (defined by consumers)
- Database caching (use Room in consumer modules)

**Key Design Decisions**:
1. Public interface, private implementation - complete encapsulation
2. NetworkResult<T> instead of throwing exceptions - explicit error handling
3. Configuration over convention - flexible, not opinionated
4. Hilt integration but not Hilt-dependent - can use any DI framework
5. Testing-first design - easy mocking without network calls
