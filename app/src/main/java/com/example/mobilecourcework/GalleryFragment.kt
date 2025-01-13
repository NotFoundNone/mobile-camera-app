package com.example.mobilecourcework

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import com.example.mobilecourcework.databinding.FragmentGalleryBinding
import java.io.File

class GalleryFragment : Fragment() {

    // Переменная для View Binding
    private var _binding: FragmentGalleryBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация View Binding для доступа к элементам разметки
        _binding = FragmentGalleryBinding.inflate(inflater, container, false)
        return binding.root // Возвращаем корневой элемент разметки
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Устанавливаем GridLayoutManager для отображения галереи в виде сетки с 3 столбцами
        binding.recyclerViewGallery.layoutManager = GridLayoutManager(context, 3)

        // Устанавливаем обработчик на кнопку "Назад", чтобы вернуться к предыдущему фрагменту
        binding.buttonBack.setOnClickListener {
            findNavController().navigate(R.id.action_galleryFragment_to_photoFragment)
        }

        // Получаем список изображений и видео, а затем передаём его в адаптер RecyclerView
        val mediaFiles = getMediaFiles()
        val adapter = GalleryAdapter(mediaFiles) // Создаём адаптер с файлами
        binding.recyclerViewGallery.adapter = adapter // Привязываем адаптер к RecyclerView
    }

    // Метод для получения списка изображений и видео
    private fun getMediaFiles(): List<File> {
        val mediaFiles = mutableListOf<File>() // Создаём список для хранения файлов
        val contentResolver: ContentResolver = requireContext().contentResolver // Получаем ContentResolver для работы с MediaStore

        // --- Получение изображений ---
        val imageCursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, // URI для доступа к изображениям
            arrayOf(MediaStore.Images.Media.DATA), // Проекция: возвращаем только путь к файлу
            null, // Условие выборки (в данном случае без фильтрации)
            null, // Параметры для условия выборки
            "${MediaStore.Images.Media.DATE_ADDED} DESC" // Сортировка: последние добавленные файлы первыми
        )

        imageCursor?.use { // Безопасное использование курсора
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Images.Media.DATA) // Получаем индекс столбца пути
            while (it.moveToNext()) { // Перебираем строки результата
                val imagePath = it.getString(dataColumn) // Извлекаем путь к файлу
                mediaFiles.add(File(imagePath)) // Добавляем файл в список
            }
        }

        // --- Получение видео ---
        val videoCursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI, // URI для доступа к видео
            arrayOf(MediaStore.Video.Media.DATA), // Проекция: возвращаем только путь к файлу
            null, // Условие выборки
            null, // Параметры для условия выборки
            "${MediaStore.Video.Media.DATE_ADDED} DESC" // Сортировка: последние добавленные файлы первыми
        )

        videoCursor?.use { // Безопасное использование курсора
            val dataColumn = it.getColumnIndexOrThrow(MediaStore.Video.Media.DATA) // Получаем индекс столбца пути
            while (it.moveToNext()) { // Перебираем строки результата
                val videoPath = it.getString(dataColumn) // Извлекаем путь к файлу
                mediaFiles.add(File(videoPath)) // Добавляем файл в список
            }
        }

        // Логируем найденные файлы для отладки
        mediaFiles.forEach {
            Log.d("GalleryFragment", "Found file: ${it.name} | Path: ${it.absolutePath}")
        }

        return mediaFiles // Возвращаем список найденных файлов
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null // Освобождаем ресурсы View Binding
    }
}
