package com.example.mobilecourcework

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import java.io.File

class FullScreenViewerFragment : Fragment(R.layout.fragment_full_screen_viewer) {

    private lateinit var file: File

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получаем файл из аргументов
        file = File(requireArguments().getString("file_path")!!)

        // Отображаем файл
        val imageView = view.findViewById<ImageView>(R.id.full_screen_image)
        Glide.with(requireContext())
            .load(file)
            .into(imageView)

        // Кнопка удаления
        view.findViewById<ImageButton>(R.id.delete_button).setOnClickListener {
            deleteFile()
        }

        // Кнопка перехода к созданию нового контента
        view.findViewById<ImageButton>(R.id.create_new_button).setOnClickListener {
            findNavController().navigate(R.id.action_fullScreenViewerFragment_to_photoFragment)
        }
    }

    private fun deleteFile() {
        if (file.exists()) {
            file.delete()
            findNavController().popBackStack() // Возврат к галерее
        }
    }
}
