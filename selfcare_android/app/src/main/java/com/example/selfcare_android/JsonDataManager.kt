package com.example.selfcare_android

import android.content.Context
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import java.io.File
import java.io.IOException

/**
 * 内部ストレージへのJSON保存・読み込みを管理するシングルトン
 */
object JsonDataManager {
    private const val FILENAME = "app_data.json"
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * データを保存する
     * @param context アプリケーションコンテキスト
     * @param appData 保存するデータオブジェクト
     */
    fun save(context: Context, appData: AppData) {
        try {
            val jsonString = gson.toJson(appData)
            // MODE_PRIVATEで他のアプリからはアクセス不可にする
            context.openFileOutput(FILENAME, Context.MODE_PRIVATE).use { output ->
                output.write(jsonString.toByteArray())
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }

    /**
     * データを読み込む
     * @param context アプリケーションコンテキスト
     * @return 保存されたAppData。ファイルがない場合は新規作成して返す。
     */
    fun load(context: Context): AppData {
        val file = File(context.filesDir, FILENAME)
        if (!file.exists()) {
            // ファイルがまだない場合は空のデータを作成
            return AppData()
        }

        return try {
            context.openFileInput(FILENAME).bufferedReader().use { reader ->
                val jsonString = reader.readText()
                if (jsonString.isBlank()) {
                    AppData()
                } else {
                    gson.fromJson(jsonString, AppData::class.java)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
            AppData()
        } catch (e: Exception) {
            // JSON形式不正などの場合
            e.printStackTrace()
            AppData()
        }
    }
}