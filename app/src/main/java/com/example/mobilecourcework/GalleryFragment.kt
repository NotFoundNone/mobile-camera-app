package com.example.mobilecourcework

import android.content.ContentResolver
import android.database.Cursor
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.io.File

class GalleryFragment : Fragment() {

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_gallery, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val recyclerView = view.findViewById<RecyclerView>(R.id.recycler_view_gallery)
        recyclerView.layoutManager = GridLayoutManager(context, 3) // 3 столбца для изображений

        view.findViewById<ImageButton>(R.id.button_back).setOnClickListener {
            findNavController().navigate(R.id.action_galleryFragment_to_photoFragment)
        }

        val imageFiles = getImageFiles()
        val adapter = GalleryAdapter(imageFiles)
        recyclerView.adapter = adapter
    }

    private fun getImageFiles(): List<File> {
        val imageFiles = mutableListOf<File>()
        val contentResolver: ContentResolver = requireContext().contentResolver

        // Указание типов файлов: изображения
        val imageProjection = arrayOf(
            MediaStore.Images.Media.DATA,
            MediaStore.Images.Media.DISPLAY_NAME
        )

        // Запрос изображений из MediaStore
        val imageCursor: Cursor? = contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            imageProjection,
            null,
            null,
            "${MediaStore.Images.Media.DATE_ADDED} DESC" // Последние добавленные изображения
        )

        imageCursor?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA)
            while (cursor.moveToNext()) {
                val imagePath = cursor.getString(dataColumn)
                imageFiles.add(File(imagePath))
            }
        }

        // Указание типов файлов: видео
        val videoProjection = arrayOf(
            MediaStore.Video.Media.DATA,
            MediaStore.Video.Media.DISPLAY_NAME
        )

        // Запрос видео из MediaStore
        val videoCursor: Cursor? = contentResolver.query(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            videoProjection,
            null,
            null,
            "${MediaStore.Video.Media.DATE_ADDED} DESC" // Последние добавленные видео
        )

        videoCursor?.use { cursor ->
            val dataColumn = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATA)
            while (cursor.moveToNext()) {
                val videoPath = cursor.getString(dataColumn)
                imageFiles.add(File(videoPath))
            }
        }

        imageFiles.forEach {
            Log.d("GalleryFragment", "Found file: ${it.name} | Path: ${it.absolutePath}")
        }

        return imageFiles
    }
}
