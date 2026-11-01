package com.hiregrid.app.core.network

import android.content.Context
import com.hiregrid.app.core.datastore.SessionManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object RetrofitClient {

    // Using 127.0.0.1 bridges directly to your PC over USB when 'adb reverse tcp:8000 tcp:8000' is active
    var baseUrl: String = "http://127.0.0.1:8000/api/v1/"
        set(value) {
            field = if (value.endsWith("/")) value else "$value/"
            apiServiceInstance = null
        }

    @Volatile
    private var cachedToken: String? = null

    @Volatile
    private var apiServiceInstance: ApiService? = null

    fun initSessionSync(sessionManager: SessionManager, scope: CoroutineScope = CoroutineScope(Dispatchers.IO)) {
        scope.launch(Dispatchers.IO) {
            sessionManager.accessToken.collect { token ->
                cachedToken = token
            }
        }
    }

    fun getApiService(context: Context): ApiService {
        return apiServiceInstance ?: synchronized(this) {
            val loggingInterceptor = HttpLoggingInterceptor().apply {
                level = HttpLoggingInterceptor.Level.BODY
            }

            val authInterceptor = AuthInterceptor { cachedToken }

            val okHttpClient = OkHttpClient.Builder()
                .connectTimeout(30, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .writeTimeout(30, TimeUnit.SECONDS)
                .addInterceptor(authInterceptor)
                .addInterceptor(loggingInterceptor)
                .build()

            val retrofit = Retrofit.Builder()
                .baseUrl(baseUrl)
                .client(okHttpClient)
                .addConverterFactory(GsonConverterFactory.create())
                .build()

            val instance = retrofit.create(ApiService::class.java)
            apiServiceInstance = instance
            instance
        }
    }
}
