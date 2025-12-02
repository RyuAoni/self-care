package com.example.selfcare_android

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
//import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*
import android.provider.MediaStore
import android.app.Activity
import java.io.File

class AlbumActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
//    private lateinit var adapter: PhotoAdapter
    private lateinit var adapter: AlbumAdapter
    private lateinit var emptyText: TextView

//    private val photos = mutableListOf<Photo>()
//    private val PICK_IMAGE_REQUEST = 100
//    private var selectedPhotoPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        // 戻るボタン (IDが buttonClose の場合と backButton の場合があるため、レイアウトに合わせて調整してください)
        // ここでは activity_album.xml に backButton があると仮定します
        val backBtn = findViewById<ImageView>(R.id.backButton) ?: findViewById<ImageView>(R.id.buttonClose)
        backBtn?.setOnClickListener {
            finish()
        }

        recyclerView = findViewById(R.id.photoRecyclerView) // レイアウトのIDを確認してください (例: photoRecyclerView)
        // もしレイアウトが activity_album.xml で ID が photoRecyclerView なら書き換えてください
        // 今回は安全のため findViewById<RecyclerView>(R.id.photoRecyclerView) を優先します
        if (recyclerView == null) {
            recyclerView = findViewById(R.id.photoRecyclerView)
        }

        // 空の時のテキスト（レイアウトにあれば）
        emptyText = findViewById(R.id.emptyText)

//        setupTopBar()
        setupRecyclerView()
        setupBottomNavigation()
//        setupFab()
//        loadPhotos()
    }

//    private fun setupTopBar() {
//        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
//            finish()
//        }
//    }

    override fun onResume() {
        super.onResume()
        loadPhotos()
    }

    private fun setupRecyclerView() {
//        recyclerView = findViewById(R.id.photoRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

//        adapter = PhotoAdapter(photos) { position ->
//            selectedPhotoPosition = position
//            openImagePicker()
//        }
        adapter = AlbumAdapter(emptyList()) { entry ->
            // 写真をタップしたら、その日記の詳細画面へ飛ぶ
            val intent = Intent(this, DiaryDetailActivity::class.java)

            // 日付文字列 (yyyy/MM/dd) を分解して渡す
            try {
                // 日付フォーマットに合わせてパース
                // DiaryGenerateActivityで保存しているフォーマットは "yyyy/MM/dd"
                val parts = entry.date.split("/")
                if (parts.size == 3) {
                    intent.putExtra("year", parts[0].toInt())
                    intent.putExtra("month", parts[1].toInt() - 1) // Calendarの月は0始まり
                    intent.putExtra("day", parts[2].toInt())
                    startActivity(intent)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        recyclerView.adapter = adapter
    }

    private fun loadPhotos() {
        // 1. 日記データをロード
        val appData = JsonDataManager.load(this)

        // 2. 画像パスを持っていて、かつ実際にファイルが存在する日記だけを抽出
        val photoEntries = appData.diaries.filter {
            !it.imagePath.isNullOrEmpty()
        }.sortedByDescending { it.date } // 新しい順

        Log.d("AlbumActivity", "Found ${photoEntries.size} photos")

        // 3. 表示更新
        if (photoEntries.isEmpty()) {
            recyclerView.visibility = View.GONE
            emptyText.visibility = View.VISIBLE
        } else {
            recyclerView.visibility = View.VISIBLE
            emptyText.visibility = View.GONE
            adapter.updateList(photoEntries)
        }
    }

    private fun setupBottomNavigation() {
        val bottomNav = findViewById<BottomNavigationView>(R.id.bottom_navigation)

        // 初期選択を解除する
        bottomNav.menu.setGroupCheckable(0, true, false)
        for (i in 0 until bottomNav.menu.size()) {
            bottomNav.menu.getItem(i).isChecked = false
        }
        bottomNav.menu.setGroupCheckable(0, true, true)

        bottomNav.setOnItemSelectedListener { item ->
            when (item.itemId) {
                R.id.nav_stats -> {
                    val intent = Intent(this, EmotionAnalysisActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_calendar -> {
                    val intent = Intent(this, CalendarActivity::class.java)
                    startActivity(intent)
                    true
                }
                R.id.nav_profile -> {
                    val intent = Intent(this, SettingsActivity::class.java)
                    startActivity(intent)
                    true
                }
                else -> false
            }
        }
    }

//    private fun setupFab() {
//        findViewById<FloatingActionButton>(R.id.fabAddPhoto).setOnClickListener {
//            addNewPhoto()
//        }
//    }

//    private fun addNewPhoto() {
//        val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
//        val newPhoto = Photo(
//            id = UUID.randomUUID().toString(),
//            uri = null,
//            date = currentDate
//        )
//        photos.add(newPhoto)
//        adapter.notifyItemInserted(photos.size - 1)
//        savePhotos()
//
//        // 新しく追加した写真を選択して画像ピッカーを開く
//        selectedPhotoPosition = photos.size - 1
//        openImagePicker()
//    }

//    private fun openImagePicker() {
//        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
//        startActivityForResult(intent, PICK_IMAGE_REQUEST)
//    }

//    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
//        super.onActivityResult(requestCode, resultCode, data)
//
//        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
//            val imageUri = data.data
//            if (imageUri != null && selectedPhotoPosition >= 0 && selectedPhotoPosition < photos.size) {
//                // URIのパーミッションを永続化
//                try {
//                    contentResolver.takePersistableUriPermission(
//                        imageUri,
//                        Intent.FLAG_GRANT_READ_URI_PERMISSION
//                    )
//                } catch (e: Exception) {
//                    // パーミッション取得失敗時のハンドリング
//                }
//
//                // 写真情報を更新
//                photos[selectedPhotoPosition].uri = imageUri.toString()
//                adapter.notifyItemChanged(selectedPhotoPosition)
//                savePhotos()
//            }
//        }
//    }

//    private fun savePhotos() {
//        val prefs = getSharedPreferences("AlbumPhotos", MODE_PRIVATE)
//        val editor = prefs.edit()
//
//        // 既存のデータをクリア
//        editor.clear()
//
//        // 写真の数を保存
//        editor.putInt("photo_count", photos.size)
//
//        // 各写真のデータを保存
//        photos.forEachIndexed { index, photo ->
//            editor.putString("photo_${index}_id", photo.id)
//            editor.putString("photo_${index}_uri", photo.uri)
//            editor.putString("photo_${index}_date", photo.date)
//        }
//
//        editor.apply()
//    }

//    private fun loadPhotos() {
//        val prefs = getSharedPreferences("AlbumPhotos", MODE_PRIVATE)
//        val photoCount = prefs.getInt("photo_count", 0)
//
//        photos.clear()
//
//        for (i in 0 until photoCount) {
//            val id = prefs.getString("photo_${i}_id", UUID.randomUUID().toString()) ?: continue
//            val uri = prefs.getString("photo_${i}_uri", null)
//            val date = prefs.getString("photo_${i}_date", "") ?: ""
//
//            photos.add(Photo(id, uri, date))
//        }
//
//        adapter.notifyDataSetChanged()
//    }

//    fun deletePhoto(position: Int) {
//        if (position >= 0 && position < photos.size) {
//            photos.removeAt(position)
//            adapter.notifyItemRemoved(position)
//            savePhotos()
//        }
//    }
}

// アルバム用アダプター
class AlbumAdapter(
    private var entries: List<DiaryEntry>,
    private val onItemClick: (DiaryEntry) -> Unit
) : RecyclerView.Adapter<AlbumAdapter.ViewHolder>() {

    fun updateList(newEntries: List<DiaryEntry>) {
        entries = newEntries
        notifyDataSetChanged()
    }

    class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        // item_photo.xml のIDに合わせて取得
        val imageView: ImageView = view.findViewById(R.id.photoImage) // または ivPhoto
        val dateText: TextView = view.findViewById(R.id.photoDate)   // または tvDate
//        val cardView: CardView = view.findViewById(R.id.photoCard)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val entry = entries[position]

        // 日付
        holder.dateText.text = entry.date

        // ★修正: ファイルが存在するか確認し、なければデフォルト画像を表示
        if (entry.imagePath != null) {
            val file = File(entry.imagePath)
            if (file.exists()) {
                holder.imageView.setImageURI(Uri.fromFile(file))
            } else {
                // ファイルが見つからない場合はデフォルト画像
                holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
            }
        } else {
            holder.imageView.setImageResource(android.R.drawable.ic_menu_gallery)
        }

        // クリック時
        holder.itemView.setOnClickListener {
            onItemClick(entry)
        }

        // 削除ボタンの処理（必要であれば実装）
        holder.deleteButton.setOnClickListener {
            // ここで削除処理を呼ぶことも可能です
        }
    }

    override fun getItemCount() = entries.size
}

//data class Photo(
//    val id: String,
//    var uri: String?,
//    var date: String
//)

//class PhotoAdapter(
//    private val photos: MutableList<Photo>,
//    private val onPhotoClick: (Int) -> Unit
//) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

//    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
//        val cardView: CardView = view.findViewById(R.id.photoCard)
//        val imageView: ImageView = view.findViewById(R.id.photoImage)
//        val dateText: TextView = view.findViewById(R.id.photoDate)
//        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
//    }

//    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
//        val view = LayoutInflater.from(parent.context)
//            .inflate(R.layout.item_photo, parent, false)
//        return PhotoViewHolder(view)
//    }
//
//    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
//        val photo = photos[position]
//
//        // 日付を表示
//        holder.dateText.text = photo.date
//
//        // 写真を表示
//        if (photo.uri != null) {
//            try {
//                holder.imageView.setImageURI(Uri.parse(photo.uri))
//                holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
//            } catch (e: Exception) {
//                holder.imageView.setImageResource(R.drawable.ic_launcher_background)
//            }
//        } else {
//            holder.imageView.setImageResource(R.drawable.ic_launcher_background)
//        }
//
//        // カードクリックで写真選択
//        holder.cardView.setOnClickListener {
//            onPhotoClick(holder.adapterPosition)
//        }
//
//        // 削除ボタン
//        holder.deleteButton.setOnClickListener {
//            val activity = holder.itemView.context as? AlbumActivity
//            activity?.deletePhoto(holder.adapterPosition)
//        }
//    }

//    override fun getItemCount() = photos.size
//}