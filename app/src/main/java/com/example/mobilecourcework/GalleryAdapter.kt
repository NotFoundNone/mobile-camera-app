package com.example.mobilecourcework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryAdapter(private val mediaFiles: List<File>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.image_view)
        val mediaType: TextView = itemView.findViewById(R.id.media_type)
        val mediaDate: TextView = itemView.findViewById(R.id.media_date)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(R.layout.item_gallery_image, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = mediaFiles[position]

        // Загрузка изображения или превью видео
        Glide.with(holder.itemView.context)
            .load(file)
            .placeholder(android.R.drawable.ic_menu_gallery)
            .into(holder.imageView)

        // Определяем тип файла
        val fileType = when (file.extension.lowercase()) {
            "jpg", "png" -> "Изображение"
            "mp4" -> "Видео"
            else -> "Неизвестный тип"
        }

        // Форматируем дату создания
        val creationDate = dateFormat.format(Date(file.lastModified()))

        // Устанавливаем текст
        holder.mediaType.text = fileType
        holder.mediaDate.text = creationDate

        // Обработка клика для перехода в полноэкранный просмотр
        holder.itemView.setOnClickListener {
            val bundle = Bundle().apply {
                putString("file_path", file.absolutePath)
            }
            holder.itemView.findNavController()
                .navigate(R.id.action_galleryFragment_to_fullScreenViewerFragment, bundle)
        }
    }

    override fun getItemCount(): Int = mediaFiles.size
}

