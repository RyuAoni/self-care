package com.example.selfcare_android

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.text.SimpleDateFormat
import java.util.*

class AlbumActivity : AppCompatActivity() {

    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: PhotoAdapter
    private val photos = mutableListOf<Photo>()
    private val PICK_IMAGE_REQUEST = 100
    private var selectedPhotoPosition = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_album)

        setupTopBar()
        setupRecyclerView()
        setupBottomNavigation()
        setupFab()
        loadPhotos()
    }

    private fun setupTopBar() {
        findViewById<ImageView>(R.id.buttonClose).setOnClickListener {
            finish()
        }
    }

    private fun setupRecyclerView() {
        recyclerView = findViewById(R.id.photoRecyclerView)
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        adapter = PhotoAdapter(photos) { position ->
            selectedPhotoPosition = position
            openImagePicker()
        }
        recyclerView.adapter = adapter
    }

    private fun setupBottomNavigation() {
        findViewById<ImageView>(R.id.navStats).setOnClickListener {
            Toast.makeText(this, "統計", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navCalendar).setOnClickListener {
            Toast.makeText(this, "カレンダー", Toast.LENGTH_SHORT).show()
        }

        findViewById<ImageView>(R.id.navProfile).setOnClickListener {
            finish()
        }
    }

    private fun setupFab() {
        findViewById<FloatingActionButton>(R.id.fabAddPhoto).setOnClickListener {
            addNewPhoto()
        }
    }

    private fun addNewPhoto() {
        val currentDate = SimpleDateFormat("yyyy/MM/dd", Locale.getDefault()).format(Date())
        val newPhoto = Photo(
            id = UUID.randomUUID().toString(),
            uri = null,
            date = currentDate
        )
        photos.add(newPhoto)
        adapter.notifyItemInserted(photos.size - 1)
        savePhotos()

        // 新しく追加した写真を選択して画像ピッカーを開く
        selectedPhotoPosition = photos.size - 1
        openImagePicker()
    }

    private fun openImagePicker() {
        val intent = Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        startActivityForResult(intent, PICK_IMAGE_REQUEST)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == PICK_IMAGE_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
            val imageUri = data.data
            if (imageUri != null && selectedPhotoPosition >= 0 && selectedPhotoPosition < photos.size) {
                // URIのパーミッションを永続化
                try {
                    contentResolver.takePersistableUriPermission(
                        imageUri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (e: Exception) {
                    // パーミッション取得失敗時のハンドリング
                }

                // 写真情報を更新
                photos[selectedPhotoPosition].uri = imageUri.toString()
                adapter.notifyItemChanged(selectedPhotoPosition)
                savePhotos()
            }
        }
    }

    private fun savePhotos() {
        val prefs = getSharedPreferences("AlbumPhotos", MODE_PRIVATE)
        val editor = prefs.edit()

        // 既存のデータをクリア
        editor.clear()

        // 写真の数を保存
        editor.putInt("photo_count", photos.size)

        // 各写真のデータを保存
        photos.forEachIndexed { index, photo ->
            editor.putString("photo_${index}_id", photo.id)
            editor.putString("photo_${index}_uri", photo.uri)
            editor.putString("photo_${index}_date", photo.date)
        }

        editor.apply()
    }

    private fun loadPhotos() {
        val prefs = getSharedPreferences("AlbumPhotos", MODE_PRIVATE)
        val photoCount = prefs.getInt("photo_count", 0)

        photos.clear()

        for (i in 0 until photoCount) {
            val id = prefs.getString("photo_${i}_id", UUID.randomUUID().toString()) ?: continue
            val uri = prefs.getString("photo_${i}_uri", null)
            val date = prefs.getString("photo_${i}_date", "") ?: ""

            photos.add(Photo(id, uri, date))
        }

        adapter.notifyDataSetChanged()
    }

    fun deletePhoto(position: Int) {
        if (position >= 0 && position < photos.size) {
            photos.removeAt(position)
            adapter.notifyItemRemoved(position)
            savePhotos()
        }
    }
}

data class Photo(
    val id: String,
    var uri: String?,
    var date: String
)

class PhotoAdapter(
    private val photos: MutableList<Photo>,
    private val onPhotoClick: (Int) -> Unit
) : RecyclerView.Adapter<PhotoAdapter.PhotoViewHolder>() {

    class PhotoViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val cardView: CardView = view.findViewById(R.id.photoCard)
        val imageView: ImageView = view.findViewById(R.id.photoImage)
        val dateText: TextView = view.findViewById(R.id.photoDate)
        val deleteButton: ImageView = view.findViewById(R.id.deleteButton)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PhotoViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_photo, parent, false)
        return PhotoViewHolder(view)
    }

    override fun onBindViewHolder(holder: PhotoViewHolder, position: Int) {
        val photo = photos[position]

        // 日付を表示
        holder.dateText.text = photo.date

        // 写真を表示
        if (photo.uri != null) {
            try {
                holder.imageView.setImageURI(Uri.parse(photo.uri))
                holder.imageView.scaleType = ImageView.ScaleType.CENTER_CROP
            } catch (e: Exception) {
                holder.imageView.setImageResource(R.drawable.ic_launcher_background)
            }
        } else {
            holder.imageView.setImageResource(R.drawable.ic_launcher_background)
        }

        // カードクリックで写真選択
        holder.cardView.setOnClickListener {
            onPhotoClick(holder.adapterPosition)
        }

        // 削除ボタン
        holder.deleteButton.setOnClickListener {
            val activity = holder.itemView.context as? AlbumActivity
            activity?.deletePhoto(holder.adapterPosition)
        }
    }

    override fun getItemCount() = photos.size
}