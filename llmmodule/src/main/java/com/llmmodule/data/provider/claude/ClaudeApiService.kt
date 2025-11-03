package com.llmmodule.data.provider.claude

import com.llmmodule.data.provider.claude.model.ClaudeMessagesRequest
import com.llmmodule.data.provider.claude.model.ClaudeMessagesResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface ClaudeApiService {

    @POST("v1/messages")
    suspend fun createMessage(
        @Header("x-api-key") apiKey: String,
        @Header("anthropic-version") version: String,
        @Body request: ClaudeMessagesRequest
    ): Response<ClaudeMessagesResponse>
}
