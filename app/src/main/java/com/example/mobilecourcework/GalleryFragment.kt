package com.example.mobilecourcework

import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

        val imageFiles = getImageFiles()
        val adapter = GalleryAdapter(imageFiles)
        recyclerView.adapter = adapter
    }

    private fun getImageFiles(): List<File> {
        // Получаем ту же папку, в которую камера сохраняет фотографии
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return mediaDir?.listFiles()?.filter {
            it.isFile && it.extension in listOf("jpg", "png")
        } ?: emptyList()
    }
}
