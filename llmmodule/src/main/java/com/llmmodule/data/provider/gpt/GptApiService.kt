package com.llmmodule.data.provider.gpt

import com.llmmodule.data.provider.gpt.model.GptChatCompletionsRequest
import com.llmmodule.data.provider.gpt.model.GptChatCompletionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface GptApiService {

    @POST("v1/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Header("Content-Type") contentType: String = "application/json",
        @Body request: GptChatCompletionsRequest
    ): Response<GptChatCompletionsResponse>


}
