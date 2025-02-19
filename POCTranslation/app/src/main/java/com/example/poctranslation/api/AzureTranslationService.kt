package com.example.poctranslation.api

import okhttp3.RequestBody
import okhttp3.ResponseBody
import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query

interface AzureTranslationService {
    @Headers(
        "Ocp-Apim-Subscription-Key: 3dInCSUAqFyS0VPpgkWPw7Xd4yV56tlWnAwHhl2IlC3UGdWbkuvkJQQJ99BBACGhslBXJ3w3AAAbACOGhy2G",
        "Ocp-Apim-Subscription-Region: centralindia",
        "Content-Type: application/json"
    )
    @POST("translate")
    fun translateText(
        @Query("api-version") apiVersion: String = "3.0",
        @Query("from") fromLang: String,
        @Query("to") toLang: String,
        @Body requestBody: RequestBody
    ): Call<List<TranslationResponse>>
}

// Response Model
data class TranslationResponse(
    val translations: List<Translation>
)

data class Translation(
    val text: String,
    val to: String
)