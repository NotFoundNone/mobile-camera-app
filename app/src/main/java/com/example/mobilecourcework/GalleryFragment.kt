package com.example.mobilecourcework

import android.os.Bundle
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
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
        val pictureDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val videoDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)

        val pictureFiles = pictureDir?.listFiles()?.filter {
            it.isFile && it.extension.lowercase() in listOf("jpg", "png")
        } ?: emptyList()

        val videoFiles = videoDir?.listFiles()?.filter {
            it.isFile && it.extension.lowercase() == "mp4"
        } ?: emptyList()

        val allFiles = pictureFiles + videoFiles

        allFiles.forEach {
            Log.d("GalleryFragment", "Found file: ${it.name} | Path: ${it.absolutePath}")
        }

        return allFiles
    }


}
