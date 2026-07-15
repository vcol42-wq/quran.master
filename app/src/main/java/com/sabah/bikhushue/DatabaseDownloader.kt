package com.sabah.bikhushue

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

object DatabaseDownloader {

    private const val TAG = "DatabaseDownloader"
    
    // URLs for the database files hosted on GitHub Releases (v1.0)
    private const val QURAN_DB_URL = "https://github.com/vcol42-wq/quran.master/releases/download/v1.0/quran_factory.db"
    private const val AZKAR_DB_URL = "https://github.com/vcol42-wq/quran.master/releases/download/v1.0/azkar.db"

    // Min expected sizes to consider them fully downloaded (in bytes)
    private const val QURAN_DB_MIN_SIZE = 1_000_000L // 1 MB
    private const val AZKAR_DB_MIN_SIZE = 50_000L    // 50 KB

    var isDownloading = false
        private set

    fun isQuranDbReady(context: Context): Boolean {
        val dbFile = context.getDatabasePath("quran_factory.db")
        return dbFile.exists() && dbFile.length() > QURAN_DB_MIN_SIZE
    }

    fun isAzkarDbReady(context: Context): Boolean {
        val dbFile = context.getDatabasePath("azkar.db")
        return dbFile.exists() && dbFile.length() > AZKAR_DB_MIN_SIZE
    }

    fun checkAndDownloadDatabases(context: Context, onProgress: (String) -> Unit, onComplete: (Boolean) -> Unit) {
        if (isQuranDbReady(context) && isAzkarDbReady(context)) {
            Log.d(TAG, "All databases are already downloaded and ready.")
            onComplete(true)
            return
        }

        if (isDownloading) {
            Log.d(TAG, "Download is already in progress.")
            return
        }

        isDownloading = true
        val mainHandler = Handler(Looper.getMainLooper())
        
        Thread {
            try {
                // Create database directory if it doesn't exist
                val dbDir = context.getDatabasePath("dummy").parentFile
                if (dbDir != null && !dbDir.exists()) {
                    dbDir.mkdirs()
                }

                var success = true

                // Download Quran DB if needed
                if (!isQuranDbReady(context)) {
                    mainHandler.post { onProgress("جاري تنزيل بيانات المصحف (قد يستغرق بعض الوقت)...") }
                    val quranSuccess = downloadFile(
                        QURAN_DB_URL,
                        context.getDatabasePath("quran_factory.db")
                    )
                    if (!quranSuccess) success = false
                }

                // Download Azkar DB if needed
                if (success && !isAzkarDbReady(context)) {
                    mainHandler.post { onProgress("جاري تنزيل بيانات الأذكار...") }
                    val azkarSuccess = downloadFile(
                        AZKAR_DB_URL,
                        context.getDatabasePath("azkar.db")
                    )
                    if (!azkarSuccess) success = false
                }

                mainHandler.post {
                    isDownloading = false
                    onComplete(success)
                }

            } catch (e: Exception) {
                Log.e(TAG, "Error downloading databases", e)
                mainHandler.post {
                    isDownloading = false
                    onComplete(false)
                }
            }
        }.start()
    }

    private fun downloadFile(urlString: String, destFile: File): Boolean {
        var currentUrl = urlString
        var redirectCount = 0
        var connection: HttpURLConnection? = null
        
        try {
            while (redirectCount < 5) {
                val url = URL(currentUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.requestMethod = "GET"
                connection.connectTimeout = 15000
                connection.readTimeout = 30000
                connection.instanceFollowRedirects = false // We handle it manually
                connection.connect()

                val responseCode = connection.responseCode
                if (responseCode == HttpURLConnection.HTTP_MOVED_PERM || 
                    responseCode == HttpURLConnection.HTTP_MOVED_TEMP || 
                    responseCode == HttpURLConnection.HTTP_SEE_OTHER ||
                    responseCode == 307 || responseCode == 308) {
                    val newUrl = connection.getHeaderField("Location")
                    connection.disconnect()
                    if (newUrl == null) return false
                    currentUrl = newUrl
                    redirectCount++
                    continue
                }

                if (responseCode != HttpURLConnection.HTTP_OK) {
                    Log.e(TAG, "Server returned HTTP $responseCode for $currentUrl")
                    return false
                }
                
                break // We got HTTP_OK
            }

            if (connection == null) return false

            // Write to a temporary file first to avoid corrupted state if interrupted
            val tempFile = File(destFile.absolutePath + ".tmp")
            
            val inputStream = connection.inputStream
            val outputStream = FileOutputStream(tempFile)
            val buffer = ByteArray(8192)
            var bytesRead: Int
            
            while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                outputStream.write(buffer, 0, bytesRead)
            }
            
            outputStream.flush()
            outputStream.close()
            inputStream.close()

            // Rename temp file to final file
            if (destFile.exists()) {
                destFile.delete()
            }
            return tempFile.renameTo(destFile)

        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $urlString", e)
            val tempFile = File(destFile.absolutePath + ".tmp")
            if (tempFile.exists()) tempFile.delete()
            return false
        } finally {
            connection?.disconnect()
        }
    }
}
