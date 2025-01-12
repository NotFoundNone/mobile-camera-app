package com.example.mobilecourcework

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.navigation.findNavController
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.mobilecourcework.databinding.ItemGalleryImageBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class GalleryAdapter(private val mediaFiles: List<File>) :
    RecyclerView.Adapter<GalleryAdapter.ViewHolder>() {

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    class ViewHolder(val binding: ItemGalleryImageBinding) : RecyclerView.ViewHolder(binding.root)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false
        )
        return ViewHolder(binding)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = mediaFiles[position]

        holder.binding.apply {
            // Загрузка изображения или превью видео
            if (file.extension.lowercase() == "mp4") {
                Glide.with(root.context)
                    .asBitmap()
                    .load(file)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imageView)
            } else {
                Glide.with(root.context)
                    .load(file)
                    .placeholder(android.R.drawable.ic_menu_gallery)
                    .into(imageView)
            }

            // Определяем тип файла
            val fileType = when (file.extension.lowercase()) {
                "jpg", "png" -> "Изображение"
                "mp4" -> "Видео"
                else -> "Неизвестный тип"
            }

            // Форматируем дату создания
            val creationDate = dateFormat.format(Date(file.lastModified()))

            // Устанавливаем текст
            mediaType.text = fileType
            mediaDate.text = creationDate

            // Обработка клика для перехода в полноэкранный просмотр
            root.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("file_path", file.absolutePath)
                }
                root.findNavController()
                    .navigate(R.id.action_galleryFragment_to_fullScreenViewerFragment, bundle)
            }
        }
    }

    override fun getItemCount(): Int = mediaFiles.size
}
