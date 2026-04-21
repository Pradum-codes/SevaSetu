package com.example.sevasetu.data.remote.cloudinary

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okio.BufferedSink
import okio.source
import org.json.JSONObject
import java.util.concurrent.TimeUnit

object CloudinaryConfig {
    const val CLOUD_NAME = "dsax7zaig"
    const val UPLOAD_PRESET = "seva-setu"
    const val FOLDER = "sevasetu/issues"

    val uploadUrl: String
        get() = "https://api.cloudinary.com/v1_1/$CLOUD_NAME/image/upload"

    fun isConfigured(): Boolean {
        return CLOUD_NAME.isNotBlank() &&
            UPLOAD_PRESET.isNotBlank() &&
            !CLOUD_NAME.startsWith("YOUR_") &&
            !UPLOAD_PRESET.startsWith("YOUR_")
    }
}

class CloudinaryUploader(
    private val context: Context
) {
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    suspend fun uploadImages(imageUris: List<Uri>): Result<List<String>> {
        return runCatching {
            if (!CloudinaryConfig.isConfigured()) {
                throw IllegalStateException(
                    "Cloudinary is not configured. Set cloudinary.cloud_name and cloudinary.upload_preset in local.properties."
                )
            }

            imageUris.mapIndexed { index, uri ->
                uploadSingleImage(uri, index)
            }
        }
    }

    private suspend fun uploadSingleImage(uri: Uri, index: Int): String {
        return withContext(Dispatchers.IO) {
            val fileName = "issue_${System.currentTimeMillis()}_$index.jpg"
            val mimeType = context.contentResolver.getType(uri) ?: "image/jpeg"
            val requestBody = object : RequestBody() {
                override fun contentType() = mimeType.toMediaTypeOrNull()

                override fun writeTo(sink: BufferedSink) {
                    val inputStream = context.contentResolver.openInputStream(uri)
                        ?: throw IllegalStateException("Unable to open selected image")
                    inputStream.use { input ->
                        sink.writeAll(input.source())
                    }
                }
            }

            val multipartBody = okhttp3.MultipartBody.Builder()
                .setType(okhttp3.MultipartBody.FORM)
                .addFormDataPart("file", fileName, requestBody)
                .addFormDataPart("upload_preset", CloudinaryConfig.UPLOAD_PRESET)
                .addFormDataPart("folder", CloudinaryConfig.FOLDER)
                .build()

            val request = Request.Builder()
                .url(CloudinaryConfig.uploadUrl)
                .post(multipartBody)
                .build()

            client.newCall(request).execute().use { response ->
                val rawBody = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val message = parseCloudinaryError(rawBody)
                    throw IllegalStateException(message ?: "Cloudinary upload failed (${response.code})")
                }

                val secureUrl = JSONObject(rawBody)
                    .optString("secure_url")
                    .takeIf { it.isNotBlank() }
                    ?: JSONObject(rawBody).optString("url").takeIf { it.isNotBlank() }

                secureUrl ?: throw IllegalStateException("Cloudinary upload succeeded but returned no URL")
            }
        }
    }

    private fun parseCloudinaryError(rawBody: String): String? {
        if (rawBody.isBlank()) return null
        return runCatching {
            val json = JSONObject(rawBody)
            when {
                json.has("error") -> {
                    val error = json.optJSONObject("error")
                    error?.optString("message")?.takeIf { it.isNotBlank() }
                        ?: json.optString("error")
                }
                json.has("message") -> json.optString("message")
                else -> null
            }
        }.getOrNull()
    }
}


