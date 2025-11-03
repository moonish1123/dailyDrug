package com.llmmodule.integration

import com.llmmodule.data.provider.claude.ClaudeLlmService
import com.llmmodule.data.provider.gpt.GptLlmService
import com.llmmodule.domain.model.LlmRequest
import com.llmmodule.domain.model.LlmResult
import com.networkmodule.api.NetworkClientFactory
import com.networkmodule.api.NetworkLogger
import java.io.File
import java.nio.file.Files
import java.util.Properties
import kotlin.time.Duration.Companion.seconds
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.serialization.json.Json
import org.junit.Assume.assumeTrue
import org.junit.Test

class KoreanHistorySummaryTest {

    private val properties: Properties by lazy { loadLocalProperties() }

    @Test
    fun `summarize Korean modern history with claude and gpt`() = runBlocking {
        val claudeApiKey = properties.getProperty("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }
        val gptApiKey = properties.getProperty("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }

        assumeTrue("ANTHROPIC_API_KEY is not configured in local.properties", !claudeApiKey.isNullOrEmpty())
        assumeTrue("OPENAI_API_KEY is not configured in local.properties", !gptApiKey.isNullOrEmpty())

        val factory = createFactory()
        val claudeService = ClaudeLlmService(factory)
        val gptService = GptLlmService(factory)

        val prompt = "대한민국 근대사를 100자로 요약해줘"

        println("--- Claude API 호출 ---")
        val claudeRequest = LlmRequest(prompt = prompt)
        val claudeResult = withTimeout(30.seconds) {
            claudeService.generateText(claudeRequest, claudeApiKey).first()
        }

        if (claudeResult is LlmResult.Success) {
            println("Claude 응답: ${claudeResult.data.text}")
        } else {
            println("Claude 호출 실패: $claudeResult")
        }

        println("\n--- GPT API 호출 ---")
        val gptRequest = LlmRequest(prompt = prompt)
        val gptResult = withTimeout(30.seconds) {
            gptService.generateText(gptRequest, gptApiKey).first()
        }

        if (gptResult is LlmResult.Success) {
            println("GPT 응답: ${gptResult.data.text}")
        } else {
            println("GPT 호출 실패: $gptResult")
        }
    }

    private fun loadLocalProperties(): Properties {
        val candidates = sequenceOf(
            File("local.properties"),
            File("../local.properties"),
            File("../../local.properties")
        )
        val props = Properties()
        val file = candidates.firstOrNull { it.exists() }
        if (file != null) {
            file.inputStream().use(props::load)
        }
        return props
    }

    private fun createFactory(): NetworkClientFactory {
        val cacheDir = Files.createTempDirectory("llm-network-cache").toFile().apply {
            deleteOnExit()
        }
        val implClass = Class.forName("com.networkmodule.internal.factory.NetworkClientFactoryImpl")
        val constructor = implClass.getConstructor(
            File::class.java,
            NetworkLogger::class.java,
            Json::class.java
        )
        val json = Json {
            ignoreUnknownKeys = true
            isLenient = true
        }
        val instance = constructor.newInstance(cacheDir, NetworkLogger.NONE, json)
        return instance as NetworkClientFactory
    }
}
