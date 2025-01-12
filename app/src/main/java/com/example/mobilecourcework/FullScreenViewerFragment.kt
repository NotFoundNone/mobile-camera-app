package com.example.mobilecourcework

import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.example.mobilecourcework.databinding.FragmentFullScreenViewerBinding
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class FullScreenViewerFragment : Fragment() {

    private var _binding: FragmentFullScreenViewerBinding? = null
    private val binding get() = _binding!!

    private lateinit var file: File
    private lateinit var handler: Handler

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        _binding = FragmentFullScreenViewerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Получение файла из аргументов
        file = File(requireArguments().getString("file_path")!!)

        if (file.extension.lowercase() == "mp4") {
            setupVideoViewer()
        } else {
            setupImageViewer()
        }

        // Кнопка удаления
        binding.deleteButton.setOnClickListener {
            deleteFile()
        }

        // Кнопка перехода к созданию нового контента
        binding.createNewButton.setOnClickListener {
            findNavController().navigate(R.id.action_fullScreenViewerFragment_to_photoFragment)
        }
    }

    private fun setupVideoViewer() {
        // Настройка для видео
        with(binding) {
            fullScreenVideo.visibility = View.VISIBLE
            fullScreenImage.visibility = View.GONE
            playPauseButton.visibility = View.VISIBLE
            videoSeekBar.visibility = View.VISIBLE

            fullScreenVideo.setVideoPath(file.absolutePath)

            fullScreenVideo.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false
                videoSeekBar.max = fullScreenVideo.duration

                playPauseButton.setOnClickListener {
                    if (fullScreenVideo.isPlaying) {
                        fullScreenVideo.pause()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        fullScreenVideo.start()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                    }
                }

                // Обновление положения ползунка
                handler = Handler(Looper.getMainLooper())
                handler.post(object : Runnable {
                    override fun run() {
                        if (fullScreenVideo.isPlaying) {
                            videoSeekBar.progress = fullScreenVideo.currentPosition
                        }
                        handler.postDelayed(this, 500)
                    }
                })

                // Обработка перемещения ползунка
                videoSeekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            fullScreenVideo.seekTo(progress)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            fullScreenVideo.setOnCompletionListener {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                videoSeekBar.progress = 0
            }
        }
    }

    private fun setupImageViewer() {
        // Настройка для изображений
        with(binding) {
            fullScreenVideo.visibility = View.GONE
            playPauseButton.visibility = View.GONE
            videoSeekBar.visibility = View.GONE
            fullScreenImage.visibility = View.VISIBLE

            Glide.with(requireContext())
                .load(file)
                .into(fullScreenImage)
        }
    }

    private fun deleteFile() {
        if (file.exists()) {
            file.delete()
            findNavController().popBackStack()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
    }
}
