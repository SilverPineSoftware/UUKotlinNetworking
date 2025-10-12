package com.silverpine.uu.sample.networking

import androidx.annotation.Keep
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.core.UUResultBlock
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UUHttpSession
import com.silverpine.uu.networking.UUHttpUri
import com.silverpine.uu.networking.UUJsonBody
import com.silverpine.uu.networking.UURemoteApi
import com.silverpine.uu.networking.UUTypedHttpRequest
import com.silverpine.uu.networking.authorization.UUTokenAuthorizationProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class OpenAiApi(sdkKey: String): UURemoteApi(UUHttpSession())
{
    private val baseUrl = "https://api.openai.com/v1"

    init
    {
        defaultAuthorizationProvider = UUTokenAuthorizationProvider(sdkKey)
    }

    fun askSomething(
        input: String,
        completion: UUResultBlock<String>
    )
    {
        val body = OpenAiQuestion(model = "gpt-4.1-mini", input = input)
        val request = buildRequest()
        request.body = UUJsonBody(body)

        executeAuthorizedRequest(request)
        { response ->

            val textResponse = (response.parsedResponse as? OpenAiResponse)?.let()
            {
                it.output.firstOrNull()?.content?.firstOrNull()?.text
            }

            if (textResponse != null)
            {
                completion(UUResult.success(textResponse))
            }
            else
            {
                completion(UUResult.failure(UUError(code = -1, domain = "OpenAiApi")))
            }
        }
    }

    private fun buildRequest(): UUTypedHttpRequest<OpenAiResponse, OpenAiErrorResponse>
    {
        val uri = UUHttpUri("$baseUrl/responses")
        val request = UUTypedHttpRequest<OpenAiResponse, OpenAiErrorResponse>(uri, OpenAiResponse::class.java, OpenAiErrorResponse::class.java)
        request.method = UUHttpMethod.POST
        return request
    }
}

@Keep
@Serializable
data class OpenAiQuestion(
    val model: String,
    val input: String
)

@Keep
@Serializable
data class OpenAiResponse(
    var id: String,

    @SerialName("object")
    var obj: String,

    @SerialName("created_at")
    var createdAt: Long,

    var status: String,

    var model: String,

    var output: List<OpenAiOutput>
)

@Keep
@Serializable
data class OpenAiOutput(
    var id: String,
    var type: String,
    var status: String,
    var content: List<OpenAiOutputContent>
)

@Keep
@Serializable
data class OpenAiOutputContent(
    var type: String = "",
    var text: String = ""
)

/*
{
    "id": "resp_08fafad70c99ec760068ea77a3e9208192902a50c0c29ce479",
    "object": "response",
    "created_at": 1760196516,
    "status": "completed",
    "background": false,
    "billing": {
    "payer": "developer"
},
    "error": null,
    "incomplete_details": null,
    "instructions": null,
    "max_output_tokens": null,
    "max_tool_calls": null,
    "model": "gpt-4.1-mini-2025-04-14",
    "output": [
    {
        "id": "msg_08fafad70c99ec760068ea77a494508192b8a0c07279b56743",
        "type": "message",
        "status": "completed",
        "content": [
        {
            "type": "output_text",
            "annotations": [],
            "logprobs": [],
            "text": "\"Breakfast is the most important meal, not because of health, but because it\u2019s an honest excuse to eat bacon before noon.\""
        }
        ],
        "role": "assistant"
    }
    ],
    "parallel_tool_calls": true,
    "previous_response_id": null,
    "prompt_cache_key": null,
    "reasoning": {
    "effort": null,
    "summary": null
},
    "safety_identifier": null,
    "service_tier": "default",
    "store": true,
    "temperature": 1.0,
    "text": {
    "format": {
    "type": "text"
},
    "verbosity": "medium"
},
    "tool_choice": "auto",
    "tools": [],
    "top_logprobs": 0,
    "top_p": 1.0,
    "truncation": "disabled",
    "usage": {
    "input_tokens": 18,
    "input_tokens_details": {
    "cached_tokens": 0
},
    "output_tokens": 27,
    "output_tokens_details": {
    "reasoning_tokens": 0
},
    "total_tokens": 45
},
    "user": null,
    "metadata": {}
}
*/




@Keep
@Serializable
data class OpenAiErrorResponse(
    val error: OpenAiError
)

@Keep
@Serializable
data class OpenAiError(
    val message: String = "",
    val type: String = "",
    val param: String? = null,
    val code: String? = null
)

/*
{
    "error": {
    "message": "Missing bearer or basic authentication in header",
    "type": "invalid_request_error",
    "param": null,
    "code": null
}
}*/