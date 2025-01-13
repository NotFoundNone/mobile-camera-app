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

    // Формат для отображения даты последнего изменения файла
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())

    // Класс ViewHolder хранит ссылки на элементы разметки для одного элемента RecyclerView
    class ViewHolder(val binding: ItemGalleryImageBinding) : RecyclerView.ViewHolder(binding.root)

    // Создаём ViewHolder, который связывает разметку item_gallery_image с кодом
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val binding = ItemGalleryImageBinding.inflate(
            LayoutInflater.from(parent.context), // Получаем LayoutInflater из контекста родительского ViewGroup
            parent, // Родительский ViewGroup, в который будет добавлен элемент
            false // Указываем, что элемент ещё не будет добавляться в родительский ViewGroup
        )
        return ViewHolder(binding) // Возвращаем новый ViewHolder с привязанной разметкой
    }

    // Привязываем данные к элементу интерфейса
    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val file = mediaFiles[position] // Получаем файл из списка по текущей позиции

        holder.binding.apply {
            // Загрузка превью для изображения или видео с использованием Glide
            if (file.extension.lowercase() == "mp4") { // Если файл - это видео
                Glide.with(root.context) // Используем Glide для загрузки изображения
                    .asBitmap() // Загружаем первый кадр видео в виде изображения
                    .load(file) // Указываем путь к файлу
                    .placeholder(android.R.drawable.ic_menu_gallery) // Плейсхолдер, если загрузка занимает время
                    .into(imageView) // Загружаем превью в imageView
            } else { // Если файл - это изображение
                Glide.with(root.context) // Используем Glide для загрузки изображения
                    .load(file) // Указываем путь к файлу
                    .placeholder(android.R.drawable.ic_menu_gallery) // Плейсхолдер для изображения
                    .into(imageView) // Загружаем изображение в imageView
            }

            // Определяем тип файла на основе его расширения
            val fileType = when (file.extension.lowercase()) {
                "jpg", "png" -> "Изображение" // Расширения для изображений
                "mp4" -> "Видео" // Расширение для видео
                else -> "Неизвестный тип" // Для других типов файлов
            }

            // Получаем дату последнего изменения файла и форматируем её
            val creationDate = dateFormat.format(Date(file.lastModified()))

            // Устанавливаем текстовые поля: тип файла и дату создания
            mediaType.text = fileType
            mediaDate.text = creationDate

            // Устанавливаем обработчик клика на элемент списка
            root.setOnClickListener {
                val bundle = Bundle().apply {
                    putString("file_path", file.absolutePath) // Передаём путь к файлу в bundle
                }
                root.findNavController() // Используем NavController для навигации
                    .navigate(R.id.action_galleryFragment_to_fullScreenViewerFragment, bundle) // Переход на экран полноэкранного просмотра
            }
        }
    }

    // Возвращаем количество элементов в списке
    override fun getItemCount(): Int = mediaFiles.size
}
