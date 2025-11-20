package com.example.selfcare_android

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

// メイン画面（トップ画面）
class MainActivity : AppCompatActivity() {

    // 歩数センサーの権限リクエスト用コード
    private val REQUEST_CODE_ACTIVITY_RECOGNITION = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // activity_main.xml を読み込む
        setContentView(R.layout.activity_main)

        // 歩数センサーの権限確認とリクエスト (Android 10以上の場合)
        checkAndRequestPermissions()

        // 「日記入力画面へ」ボタン（ID: button_start_diary）を取得
        val startDiaryButton: Button = findViewById(R.id.button_start_diary)

        // ボタンがクリックされたときの処理を設定
        startDiaryButton.setOnClickListener {
            // DiaryInputActivityに遷移するためのIntentを作成し、起動
            val intent = Intent(this, DiaryInputActivity::class.java)
            startActivity(intent)
        }
    }

    // 権限の確認とリクエストを行う関数
    private fun checkAndRequestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.ACTIVITY_RECOGNITION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // 権限がない場合、リクエストする
                ActivityCompat.requestPermissions(
                    this,
                    arrayOf(Manifest.permission.ACTIVITY_RECOGNITION),
                    REQUEST_CODE_ACTIVITY_RECOGNITION
                )
            }
        }
    }

    // 権限リクエストの結果を受け取るコールバック
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_ACTIVITY_RECOGNITION) {
            if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                // 許可された場合
            } else {
                Toast.makeText(this, "歩数を取得するには権限が必要です", Toast.LENGTH_SHORT).show()
            }
        }
    }
}