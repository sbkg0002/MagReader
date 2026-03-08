package com.magreader.magreader.data

import android.content.Context
import android.content.SharedPreferences
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.FileOutputStream

class OpdsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("opds_prefs", Context.MODE_PRIVATE)
    private val parser = OpdsParser()

    var opdsUrl: String?
        get() = prefs.getString("opds_url", null)
        set(value) = prefs.edit().putString("opds_url", value).apply()

    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit().putString("password", value).apply()

    private var _api: OpdsApi? = null

    val api: OpdsApi?
        get() {
            if (_api == null) {
                _api = createApi()
            }
            return _api
        }

    fun logout() {
        prefs.edit().clear().apply()
        _api = null
    }

    private fun createApi(): OpdsApi? {
        val url = opdsUrl ?: return null
        
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.HEADERS
        }

        val clientBuilder = OkHttpClient.Builder()
            .addInterceptor(logging)

        val user = username
        val pass = password
        if (!user.isNullOrEmpty() && !pass.isNullOrEmpty()) {
            clientBuilder.addInterceptor { chain ->
                val request = chain.request().newBuilder()
                    .addHeader("Authorization", Credentials.basic(user, pass))
                    .build()
                chain.proceed(request)
            }
        }

        val retrofit = Retrofit.Builder()
            .baseUrl(if (url.endsWith("/")) url else "$url/")
            .client(clientBuilder.build())
            .addConverterFactory(ScalarsConverterFactory.create())
            .build()

        return retrofit.create(OpdsApi::class.java)
    }

    fun refreshApi() {
        _api = createApi()
    }

    suspend fun getFeed(url: String): OpdsFeed? {
        val api = api ?: return null
        return try {
            val xml = api.getFeed(url)
            parser.parse(xml)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadBook(bookId: String, url: String): File? {
        val api = api ?: return null
        try {
            val response = api.downloadFile(url)
            val file = File(context.filesDir, "$bookId.pdf")
            response.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }
}
