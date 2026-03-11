package com.magreader.magreader.data

import android.content.Context
import android.content.SharedPreferences
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Credentials
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.scalars.ScalarsConverterFactory
import java.io.File
import java.io.FileOutputStream

class OpdsManager(private val context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("opds_prefs", Context.MODE_PRIVATE)
    private val offlinePrefs: SharedPreferences = context.getSharedPreferences("offline_metadata", Context.MODE_PRIVATE)
    private val parser = OpdsParser()
    private val gson = Gson()

    var opdsUrl: String?
        get() = prefs.getString("opds_url", null)
        set(value) = prefs.edit().putString("opds_url", value).apply()

    var username: String?
        get() = prefs.getString("username", null)
        set(value) = prefs.edit().putString("username", value).apply()

    var password: String?
        get() = prefs.getString("password", null)
        set(value) = prefs.edit().putString("password", value).apply()

    var dataLocation: String?
        get() = prefs.getString("data_location", context.filesDir.absolutePath)
        set(value) = prefs.edit().putString("data_location", value).apply()

    var gridSpanCount: Int
        get() = prefs.getInt("grid_span_count", 3)
        set(value) = prefs.edit().putInt("grid_span_count", value).apply()

    private val baseDir: File
        get() = dataLocation?.let { File(it) } ?: context.filesDir

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
            parser.parse(xml, url)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }

    suspend fun downloadBook(entry: OpdsEntry): File? {
        val api = api ?: return null
        val url = entry.acquisitionUrl ?: return null
        try {
            val response = api.downloadFile(url)
            if (!baseDir.exists()) baseDir.mkdirs()
            val file = File(baseDir, "${entry.id.hashCode()}.pdf")
            response.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
            
            // Save thumbnail if possible
            if (entry.thumbnailUrl != null) {
                downloadThumbnail(entry.id, entry.thumbnailUrl)
            }

            // Save metadata for offline view
            saveOfflineMetadata(entry)
            
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    private suspend fun downloadThumbnail(entryId: String, url: String) {
        val api = api ?: return
        try {
            val response = api.downloadFile(url)
            if (!baseDir.exists()) baseDir.mkdirs()
            val file = File(baseDir, "${entryId.hashCode()}.thumb")
            response.byteStream().use { input ->
                FileOutputStream(file).use { output ->
                    input.copyTo(output)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    private fun saveOfflineMetadata(entry: OpdsEntry) {
        val json = gson.toJson(entry)
        offlinePrefs.edit().putString(entry.id, json).apply()
    }

    fun deleteBook(entryId: String) {
        val file = File(baseDir, "${entryId.hashCode()}.pdf")
        if (file.exists()) {
            file.delete()
        }
        val thumbFile = File(baseDir, "${entryId.hashCode()}.thumb")
        if (thumbFile.exists()) {
            thumbFile.delete()
        }
        offlinePrefs.edit().remove(entryId).apply()
    }

    fun getOfflineBooks(): List<OpdsEntry> {
        val allMetadata = offlinePrefs.all
        val entries = mutableListOf<OpdsEntry>()
        for (value in allMetadata.values) {
            if (value is String) {
                val entry = gson.fromJson(value, OpdsEntry::class.java)
                // Check if file still exists
                val file = File(baseDir, "${entry.id.hashCode()}.pdf")
                if (file.exists()) {
                    // Update entry to point to local thumbnail
                    val thumbFile = File(baseDir, "${entry.id.hashCode()}.thumb")
                    val updatedEntry = if (thumbFile.exists()) {
                        entry.copy(thumbnailUrl = "file://${thumbFile.absolutePath}", acquisitionUrl = "file://${file.absolutePath}")
                    } else {
                        entry.copy(acquisitionUrl = "file://${file.absolutePath}")
                    }
                    entries.add(updatedEntry)
                }
            }
        }
        return entries
    }
    
    fun getLocalFile(entryId: String): File? {
        val file = File(baseDir, "${entryId.hashCode()}.pdf")
        return if (file.exists()) file else null
    }
}
