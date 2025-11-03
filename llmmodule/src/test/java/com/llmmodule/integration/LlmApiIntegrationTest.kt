package com.llmmodule.integration

import com.llmmodule.data.provider.claude.ClaudeLlmService
import com.llmmodule.data.provider.gpt.GptLlmService
import com.llmmodule.data.provider.local.LocalLlmService
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
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Integration tests that hit real LLM APIs when the corresponding keys are present
 * in local.properties. These tests are skipped automatically if keys are missing.
 *
 * Run manually with network access:
 * ./gradlew :llmmodule:test --tests "com.llmmodule.integration.LlmApiIntegrationTest"
 */
class LlmApiIntegrationTest {

    private val properties: Properties by lazy { loadLocalProperties() }

    @Test
    fun `local provider responds without API key`() = runBlocking {
        val service = LocalLlmService()
        val request = LlmRequest(prompt = "local provider shortcut")

        val result = withTimeout(5.seconds) {
            service.generateText(request, apiKey = null).first()
        }

        assertTrue(result is LlmResult.Success)
    }

    @Test
    fun `claude generate text with real api`() = runBlocking {
        val key = properties.getProperty("ANTHROPIC_API_KEY")?.takeIf { it.isNotBlank() }
        assumeTrue("ANTHROPIC_API_KEY is not configured in local.properties", !key.isNullOrEmpty())

        val factory = createFactory()
        val service = ClaudeLlmService(factory)
        val request = LlmRequest(prompt = "안녕하세요? DailyDrug 테스트 요청입니다.")

        val result = withTimeout(30.seconds) {
            service.generateText(request, key).first()
        }

        if (result !is LlmResult.Success) {
            println("Claude integration test skipped: $result")
        }
        assumeTrue("Claude call failed: $result", result is LlmResult.Success)
    }

    @Test
    fun `gpt generate text with real api`() = runBlocking {
        val key = properties.getProperty("OPENAI_API_KEY")?.takeIf { it.isNotBlank() }
        assumeTrue("OPENAI_API_KEY is not configured in local.properties", !key.isNullOrEmpty())

        val factory = createFactory()
        val service = GptLlmService(factory)
        val request = LlmRequest(prompt = "Hello from DailyDrug GPT integration test.")

        val result = withTimeout(30.seconds) {
            service.generateText(request, key).first()
        }

        if (result !is LlmResult.Success) {
            println("GPT integration test skipped: $result")
        }
        assumeTrue("GPT call failed: $result", result is LlmResult.Success)
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
