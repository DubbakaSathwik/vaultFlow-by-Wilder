package com.example.data.ocr

import android.graphics.Bitmap
import android.util.Log
import com.example.domain.model.ExtractedReceipt
import com.example.BuildConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@Serializable
data class GenerateContentRequest(
    val contents: List<Content>,
    val generationConfig: GenerationConfig? = null
)

@Serializable
data class Content(
    val parts: List<Part>,
    val role: String? = null
)

@Serializable
data class Part(
    val text: String? = null,
    val inlineData: Blob? = null
)

@Serializable
data class Blob(
    val mimeType: String,
    val data: String
)

@Serializable
data class GenerationConfig(
    val responseMimeType: String? = null
)

@Serializable
data class GenerateContentResponse(
    val candidates: List<Candidate>? = null
)

@Serializable
data class Candidate(
    val content: ContentResponse? = null
)

@Serializable
data class ContentResponse(
    val parts: List<PartResponse>? = null
)

@Serializable
data class PartResponse(
    val text: String? = null
)

@Serializable
data class GeminiExtractionResult(
    val isMultipleTransactions: Boolean,
    val transactions: List<ExtractedTransactionJson>
)

@Serializable
data class ExtractedTransactionJson(
    val amount: Double? = null,
    val merchantName: String? = null,
    val dateStr: String? = null,
    val timeStr: String? = null,
    val paymentApp: String? = null,
    val bankName: String? = null,
    val last4Digits: String? = null,
    val upiId: String? = null,
    val transactionId: String? = null,
    val referenceNumber: String? = null,
    val accountHolder: String? = null,
    val paymentStatus: String? = null
)

@Serializable
data class ChatMessage(
    val role: String, // "user" or "model"
    val content: String,
    val timestamp: Long = System.currentTimeMillis()
)

object GeminiService {
    private const val TAG = "GeminiService"
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(60, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private val mediaTypeJson = "application/json".toMediaType()

    private fun getApiKey(): String {
        return BuildConfig.GEMINI_API_KEY
    }

    suspend fun extractReceiptWithGemini(bitmap: Bitmap): GeminiExtractionResult? = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            Log.e(TAG, "Gemini API Key is missing or default placeholder")
            return@withContext null
        }

        try {
            // 1. Base64 Encode Bitmap
            val outputStream = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, outputStream)
            val byteArray = outputStream.toByteArray()
            val base64Data = android.util.Base64.encodeToString(byteArray, android.util.Base64.NO_WRAP)

            // 2. Build Part prompt
            val promptText = """
                You are an advanced financial receipt parsing assistant. Extract ALL transaction details from the provided payment receipt, success screen, or transaction history screenshot.
                
                Analyze the image and return a JSON object. You MUST determine if this image represents a single receipt/success screen or if it is a transaction history screenshot (which contains a list of multiple transactions).
                
                Return a JSON object matching this schema:
                {
                  "isMultipleTransactions": true/false,
                  "transactions": [
                    {
                      "amount": 120.50,
                      "merchantName": "McDonald's",
                      "dateStr": "12 Jul 2026",
                      "timeStr": "02:30 PM",
                      "paymentApp": "Google Pay",
                      "bankName": "HDFC Bank",
                      "last4Digits": "1234",
                      "upiId": "mcd@okaxis",
                      "transactionId": "TXN123456",
                      "referenceNumber": "123456789012",
                      "accountHolder": "McDonald's Store",
                      "paymentStatus": "Successful"
                    }
                  ]
                }
                
                Important Guidelines:
                - If the image contains a list of multiple transactions (like a history page), extract ALL visible transaction entries in the 'transactions' array and set 'isMultipleTransactions' to true.
                - If the image is a single receipt, return exactly ONE entry in the 'transactions' array and set 'isMultipleTransactions' to false.
                - Ensure numeric amount is extracted correctly without currency symbols.
                - If any fields are not found, use null or empty strings. DO NOT make up data.
            """.trimIndent()

            val requestObj = GenerateContentRequest(
                contents = listOf(
                    Content(
                        parts = listOf(
                            Part(text = promptText),
                            Part(inlineData = Blob(mimeType = "image/jpeg", data = base64Data))
                        )
                    )
                ),
                generationConfig = GenerationConfig(responseMimeType = "application/json")
            )

            val requestBodyJson = json.encodeToString(requestObj)
            
            val request = Request.Builder()
                .url("${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    Log.e(TAG, "Request failed with code: ${response.code}, body: ${response.body?.string()}")
                    return@withContext null
                }

                val responseBody = response.body?.string() ?: return@withContext null
                Log.d(TAG, "Gemini Raw Response: $responseBody")

                val geminiResponse = json.decodeFromString<GenerateContentResponse>(responseBody)
                val jsonText = geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: return@withContext null

                Log.d(TAG, "Gemini Extracted JSON: $jsonText")
                return@withContext json.decodeFromString<GeminiExtractionResult>(jsonText)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in extractReceiptWithGemini", e)
            return@withContext null
        }
    }

    suspend fun askGemini(
        question: String,
        contextLedger: String,
        history: List<ChatMessage> = emptyList()
    ): String = withContext(Dispatchers.IO) {
        val apiKey = getApiKey()
        if (apiKey.isEmpty() || apiKey == "MY_GEMINI_API_KEY") {
            return@withContext "API Key is not configured in the Secrets panel. Please check your setup."
        }

        try {
            val currentDateTime = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val systemPrompt = """
                You are VaultFlow's Advanced Conversational AI Financial Co-Pilot, an expert financial analyst and budget advisor.
                
                You have direct, real-time access to the user's financial ledger (transactions, accounts, savings goals, and debts) which is compiled below.
                Use this ledger context strictly and truthfully to answer any of the user's questions.
                
                Guidelines:
                1. Answer questions clearly, objectively, and accurately.
                2. If the user asks about specific transactions (e.g., "How much did I pay to XYZ friend this month?"), analyze the ledger data, calculate the exact total, list the matching transactions with dates, and provide a direct answer.
                3. If there is no ledger data matching their request, state that clearly rather than making up information.
                4. Provide helpful financial tips or recommendations when requested, keeping their actual budget and saving goals in mind.
                5. The current date/time is provided below. Use it to determine temporal relations (like "this week", "last month", "yesterday").
                6. Keep your tone professional, friendly, supportive, and clear. Avoid engineering jargon or internal file path details. Do not use markdown headers larger than h3. Keep answers concise and scannable.
                
                Current Date & Time:
                $currentDateTime
                
                --- USER FINANCIAL LEDGER CONTEXT ---
                $contextLedger
                -------------------------------------
            """.trimIndent()

            val contents = mutableListOf<Content>()
            
            val systemContent = Content(
                role = "user",
                parts = listOf(Part(text = "System: $systemPrompt"))
            )
            val ackContent = Content(
                role = "model",
                parts = listOf(Part(text = "Understood. I will act as VaultFlow's Conversational AI Financial Co-Pilot and use your provided ledger context to answer your questions accurately and objectively."))
            )
            
            contents.add(systemContent)
            contents.add(ackContent)

            // Add history
            history.forEach { msg ->
                contents.add(
                    Content(
                        role = if (msg.role == "user") "user" else "model",
                        parts = listOf(Part(text = msg.content))
                    )
                )
            }

            // Add current question
            contents.add(
                Content(
                    role = "user",
                    parts = listOf(Part(text = question))
                )
            )

            val requestObj = GenerateContentRequest(contents = contents)
            val requestBodyJson = json.encodeToString(requestObj)
            
            val request = Request.Builder()
                .url("${BASE_URL}gemini-3.5-flash:generateContent?key=$apiKey")
                .post(requestBodyJson.toRequestBody(mediaTypeJson))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext "Sorry, the AI Assistant service encountered an error (HTTP ${response.code}). Please verify your network and Gemini API key."
                }

                val responseBody = response.body?.string() ?: return@withContext "Empty response received from the model."
                val geminiResponse = json.decodeFromString<GenerateContentResponse>(responseBody)
                return@withContext geminiResponse.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                    ?: "No response content generated."
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error in askGemini", e)
            return@withContext "Error communicating with AI Assistant: ${e.localizedMessage ?: e.message}"
        }
    }
}
