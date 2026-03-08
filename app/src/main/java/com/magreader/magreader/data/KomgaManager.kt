package com.magreader.magreader.data

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import com.google.gson.GsonBuilder
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException

class KomgaManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("komga_prefs", Context.MODE_PRIVATE)

    var serverUrl: String?
        get() = prefs.getString("server_url", null)
        private set(value) = prefs.edit().putString("server_url", value).apply()

    var username: String?
        get() = prefs.getString("username", null)
        private set(value) = prefs.edit().putString("username", value).apply()

    var password: String?
        get() = prefs.getString("password", null)
        private set(value) = prefs.edit().putString("password", value).apply()

    private var _api: KomgaApi? = null

    val api: KomgaApi?
        get() {
            if (_api == null) {
                val url = serverUrl ?: return null
                val user = username ?: return null
                val pass = password ?: return null
                _api = createApi(url, user, pass)
            }
            return _api
        }

    fun login(url: String, user: String, pass: String) {
        prefs.edit()
            .putString("server_url", url)
            .putString("username", user)
            .putString("password", pass)
            .commit() // Synchronous save to ensure immediate availability
        _api = createApi(url, user, pass)
    }

    fun logout() {
        prefs.edit().clear().apply()
        _api = null
    }

    private fun createApi(url: String, user: String, pass: String): KomgaApi {
        val logging = HttpLoggingInterceptor { message ->
            Log.d("KomgaNetwork", message)
        }.apply {
            level = HttpLoggingInterceptor.Level.BASIC
        }

        val client = OkHttpClient.Builder()
            .addInterceptor { chain ->
                val authHeader = Credentials.basic(user, pass)
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", authHeader)
                    .addHeader("Accept", "application/json")
                    .build()

                Log.d("KomgaManager", "--> Sending Request")
                Log.d("KomgaManager", "URL: ${request.url}")
                Log.d("KomgaManager", "User: $user")

                val response = chain.proceed(request)

                Log.d("KomgaManager", "<-- Received Response")
                Log.d("KomgaManager", "Code: ${response.code}")
                
                if (response.code == 403) {
                    Log.e("KomgaManager", "403 Forbidden. The credentials were accepted but the server refused access. Check user roles in Komga.")
                }

                val contentType = response.header("Content-Type")
                if (response.isSuccessful && contentType != null && contentType.contains("text/html", ignoreCase = true)) {
                    throw IOException("Server returned HTML instead of JSON. Try removing '/komga' or other suffixes from your URL.")
                }
                
                response
            }
            .addInterceptor(logging)
            .build()

        val gson = GsonBuilder()
            .setLenient()
            .create()

        val retrofit = Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()

        return retrofit.create(KomgaApi::class.java)
    }

    fun refreshApi() {
        val url = serverUrl
        val user = username
        val pass = password
        if (url != null && user != null && pass != null) {
            _api = createApi(url, user, pass)
        }
    }
}
