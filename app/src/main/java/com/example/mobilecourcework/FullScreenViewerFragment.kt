package com.example.mobilecourcework

import android.os.Bundle
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.VideoView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import java.io.File

import android.os.Handler
import android.os.Looper
import android.widget.SeekBar

class FullScreenViewerFragment : Fragment(R.layout.fragment_full_screen_viewer) {

    private lateinit var file: File
    private lateinit var handler: Handler

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        file = File(requireArguments().getString("file_path")!!)
        val imageView = view.findViewById<ImageView>(R.id.full_screen_image)
        val videoView = view.findViewById<VideoView>(R.id.full_screen_video)
        val playPauseButton = view.findViewById<ImageButton>(R.id.play_pause_button)
        val seekBar = view.findViewById<SeekBar>(R.id.video_seek_bar)

        if (file.extension.lowercase() == "mp4") {
            // Настройка для видео
            videoView.visibility = View.VISIBLE
            imageView.visibility = View.GONE
            playPauseButton.visibility = View.VISIBLE
            seekBar.visibility = View.VISIBLE

            videoView.setVideoPath(file.absolutePath)

            videoView.setOnPreparedListener { mediaPlayer ->
                mediaPlayer.isLooping = false // Зацикливание по желанию
                seekBar.max = videoView.duration

                playPauseButton.setOnClickListener {
                    if (videoView.isPlaying) {
                        videoView.pause()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                    } else {
                        videoView.start()
                        playPauseButton.setImageResource(android.R.drawable.ic_media_pause)
                    }
                }

                // Обновление положения ползунка
                handler = Handler(Looper.getMainLooper())
                handler.post(object : Runnable {
                    override fun run() {
                        if (videoView.isPlaying) {
                            seekBar.progress = videoView.currentPosition
                        }
                        handler.postDelayed(this, 500) // Обновление каждые 500 мс
                    }
                })

                // Обработка перемещения ползунка
                seekBar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
                    override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                        if (fromUser) {
                            videoView.seekTo(progress)
                        }
                    }

                    override fun onStartTrackingTouch(seekBar: SeekBar?) {}
                    override fun onStopTrackingTouch(seekBar: SeekBar?) {}
                })
            }

            videoView.setOnCompletionListener {
                playPauseButton.setImageResource(android.R.drawable.ic_media_play)
                seekBar.progress = 0
            }
        } else {
            // Настройка для изображений
            videoView.visibility = View.GONE
            playPauseButton.visibility = View.GONE
            seekBar.visibility = View.GONE
            imageView.visibility = View.VISIBLE

            Glide.with(requireContext())
                .load(file)
                .into(imageView)
        }

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

    override fun onDestroyView() {
        super.onDestroyView()
        if (::handler.isInitialized) {
            handler.removeCallbacksAndMessages(null)
        }
    }
}


