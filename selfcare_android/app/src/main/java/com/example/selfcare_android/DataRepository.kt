package com.example.selfcare_android
import  android.content.Context
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedWriter
import java.io.File
import java.io.FileNotFoundException
import java.io.FileReader
import java.io.FileWriter
import java.io.PrintWriter

class DataRepository(private val context: Context) {
    private val gson = Gson()
    private val fileName = "my_app_data.json" // 保存するファイル名
    private val file = File(context.filesDir, fileName)

    // データを読み込む関数
    suspend fun loadData(): AppData = withContext(Dispatchers.IO) {
        try {
            FileReader(file).use { reader ->
                return@withContext gson.fromJson(reader, AppData::class.java)
            }
        } catch (e: FileNotFoundException) {
            return@withContext AppData() // 空のデータを返す
        } catch (e: Exception) {
            e.printStackTrace()
            return@withContext AppData() // 何か問題があれば空のデータを返す
        }
    }

    // データを保存する関数
    suspend fun saveData(appData: AppData) = withContext(Dispatchers.IO) {
        try {
            FileWriter(file).use { writer ->
                gson.toJson(appData, writer)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}