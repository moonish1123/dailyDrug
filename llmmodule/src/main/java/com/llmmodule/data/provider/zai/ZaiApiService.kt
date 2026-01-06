package com.llmmodule.data.provider.zai

import com.llmmodule.data.provider.zai.model.ZaiChatCompletionsRequest
import com.llmmodule.data.provider.zai.model.ZaiChatCompletionsResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST

internal interface ZaiApiService {

    @POST("api/paas/v4/chat/completions")
    suspend fun createChatCompletion(
        @Header("Authorization") authorization: String,
        @Body request: ZaiChatCompletionsRequest
    ): Response<ZaiChatCompletionsResponse>

}
