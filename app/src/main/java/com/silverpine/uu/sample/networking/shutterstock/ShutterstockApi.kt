package com.silverpine.uu.sample.networking.shutterstock

import androidx.annotation.Keep
import com.silverpine.uu.core.UUError
import com.silverpine.uu.core.UUResult
import com.silverpine.uu.core.UUResultBlock
import com.silverpine.uu.networking.UUHttpLoggingMode
import com.silverpine.uu.networking.UUHttpMethod
import com.silverpine.uu.networking.UURemoteApi
import com.silverpine.uu.networking.UUTypedHttpRequest
import com.silverpine.uu.networking.authorization.UUBasicAuthorizationProvider
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ShutterstockApi : UURemoteApi()
{
    private val baseUrl = "https://api.shutterstock.com/v2"

    var username: String = ""
        set(value)
        {
            field = value
            updateAuthProvider()
        }

    var password: String = ""
        set(value)
        {
            field = value
            updateAuthProvider()
        }

    private fun updateAuthProvider()
    {
        if (username.isNotBlank() && password.isNotBlank())
        {
            defaultAuthorizationProvider = UUBasicAuthorizationProvider(username, password)
        }
    }

    /**
     * Search Shutterstock images by keyword.
     */
    fun searchImages(
        query: String,
        page: Int = 1,
        perPage: Int = 20,
        completion: UUResultBlock<ShutterstockSearchResponse>
    ) {
        val request = UUTypedHttpRequest(
            url = "$baseUrl/images/search",
            query =
                hashMapOf("query" to query,
                    "page" to page.toString(),
                    "per_page" to perPage.toString(),
                    "image_type" to "photo"),
            successClass = ShutterstockSearchResponse::class.java,
            errorClass = ShutterstockErrorResponse::class.java
        ).apply {
            method = UUHttpMethod.GET
            loggingMode = UUHttpLoggingMode.Verbose
        }

        execute(request) { response ->
            val parsed = response.parsedResponse as? ShutterstockSearchResponse
            if (parsed != null) {
                completion(UUResult.success(parsed))
            } else {
                completion(
                    UUResult.failure(
                        response.error ?: UUError(
                            code = -1,
                            domain = "ShutterstockApi"
                        )
                    )
                )
            }
        }
    }

}

@Keep
@Serializable
data class ShutterstockSearchResponse(
    val data: List<ShutterstockImage> = emptyList(),
    val page: Int = 0,

    @SerialName("per_page")
    val perPage: Int = 0,

    @SerialName("total_count")
    val totalCount: Int = 0
)

@Keep
@Serializable
data class ShutterstockImage(
    val id: String,
    val description: String? = null,
    val assets: ShutterstockAssets? = null
)

@Keep
@Serializable
data class ShutterstockAssets(
    val preview: ShutterstockAsset? = null,

    @SerialName("preview_1000")
    val preview1000: ShutterstockAsset? = null
)

@Keep
@Serializable
data class ShutterstockAsset(
    val url: String,
    val width: Int,
    val height: Int
)

@Keep
@Serializable
data class ShutterstockErrorResponse(
    val message: String? = null,
    val code: String? = null
)