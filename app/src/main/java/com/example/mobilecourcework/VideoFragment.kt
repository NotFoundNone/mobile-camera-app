package com.example.mobilecourcework

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.arthenica.mobileffmpeg.Config
import com.arthenica.mobileffmpeg.FFmpeg
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoFragment : Fragment() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isRecording = false
    private var isSwitchingCamera = false
    private val temporaryVideos = mutableListOf<File>()

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)

        val currentFragmentId = findNavController().currentDestination?.id
        if (currentFragmentId == R.id.photoFragment) {
            toggleGroup.check(R.id.button_photo)
        } else if (currentFragmentId == R.id.videoFragment) {
            toggleGroup.check(R.id.button_video)
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_photo -> {
                        if (currentFragmentId != R.id.photoFragment) {
                            findNavController().navigate(R.id.action_videoFragment_to_photoFragment)
                        }
                    }
                    R.id.button_video -> {
                        if (currentFragmentId != R.id.videoFragment) {
                            findNavController().navigate(R.id.action_photoFragment_to_videoFragment)
                        }
                    }
                }
            }
        }

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        setupRecordVideoButton(view)

        view.findViewById<MaterialButton>(R.id.button_gallery).setOnClickListener {
            findNavController().navigate(R.id.action_videoFragment_to_galleryFragment)
        }

        view.findViewById<MaterialButton>(R.id.button_switch_camera).setOnClickListener {
            switchCamera()
        }
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.unbindAll() // Освобождаем предыдущую камеру
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }


    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File(mediaDir, "video_${System.nanoTime()}.mp4")
        temporaryVideos.add(videoFile)

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        currentRecording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        Toast.makeText(requireContext(), "Запись началась", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        Toast.makeText(requireContext(), "Видео сохранено: ${videoFile.absolutePath}", Toast.LENGTH_LONG).show()
                        validateVideos()
                        preprocessVideosBeforeMerge()
                        mergeVideos()
                    }
                }
            }
    }

    private fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
        isRecording = false
    }

    private fun switchCamera() {
        if (isSwitchingCamera) return
        isSwitchingCamera = true

        val wasRecording = isRecording // Сохраняем текущее состояние записи

        if (isRecording) {
            stopRecordingForSwitch() // Останавливаем текущую запись перед переключением
        }

        toggleCameraSelector() // Меняем камеру

        Handler(Looper.getMainLooper()).postDelayed({
            startCamera() // Запускаем новую камеру
            isSwitchingCamera = false

            // Проверяем, не завершена ли запись вручную
            if (wasRecording && isRecording) {
                startRecording() // Возобновляем запись, если она была активна
            }
        }, 500) // Небольшая задержка для стабилизации камеры
    }



    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun stopRecordingForSwitch() {
        try {
            currentRecording?.stop()
            currentRecording = null
            isRecording = false
        } catch (e: Exception) {
            Log.e("VideoFragment", "Ошибка при остановке записи для переключения камеры: ${e.message}")
        }
    }


    private fun validateVideos() {
        temporaryVideos.forEach { video ->
            if (!video.exists() || video.length() == 0L) {
                temporaryVideos.remove(video)
                Toast.makeText(requireContext(), "Поврежденный файл удалён: ${video.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun preprocessVideosBeforeMerge() {
        val processedVideos = mutableListOf<File>()

        temporaryVideos.forEach { video ->
            if (video.exists() && video.length() > 0L) {
                val processedFile = File(video.parent, "processed_${video.name}")
                val command = "-i ${video.absolutePath} -c:v mpeg4 -q:v 2 -r 30 -g 30 -c:a aac -b:a 128k ${processedFile.absolutePath}"

                val result = FFmpeg.execute(command)
                if (result == Config.RETURN_CODE_SUCCESS) {
                    processedVideos.add(processedFile)
                } else {
                    // Логируем ошибки FFmpeg
                    Log.e("FFmpeg", "Ошибка обработки видео: ${video.absolutePath}. Код ошибки: $result")
                    Toast.makeText(requireContext(), "Ошибка обработки видео: ${video.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e("FFmpeg", "Некорректный файл: ${video.absolutePath}")
            }
        }

        temporaryVideos.clear()
        temporaryVideos.addAll(processedVideos)
    }

    private fun mergeVideos() {
        if (temporaryVideos.size <= 1) return

        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFile = File(mediaDir, "merged_${System.currentTimeMillis()}.mp4")

        val listFile = File(mediaDir, "video_list.txt")
        listFile.printWriter().use { out ->
            temporaryVideos.filter { it.exists() }.forEach { video ->
                out.println("file '${video.absolutePath}'")
            }
        }

        // Команда для объединения видео
        val command = "-f concat -safe 0 -i ${listFile.absolutePath} -c:v mpeg4 -q:v 2 -r 30 -c:a aac -b:a 128k ${outputFile.absolutePath}"

        val result = FFmpeg.execute(command)

        if (result == Config.RETURN_CODE_SUCCESS && outputFile.exists() && outputFile.length() > 0L) {
            Toast.makeText(requireContext(), "Видео объединено: ${outputFile.absolutePath}", Toast.LENGTH_LONG).show()

            // Удаляем временные видеофайлы
            temporaryVideos.forEach { it.delete() }
            temporaryVideos.clear()

            // Удаляем список файлов
            listFile.delete()
        } else {
            // Логируем ошибки объединения
            Log.e("FFmpeg", "Ошибка объединения видео. Код ошибки: $result")
            Toast.makeText(requireContext(), "Ошибка объединения видео", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupRecordVideoButton(view: View) {
        val recordButton = view.findViewById<MaterialButton>(R.id.button_record_video)
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordButton.setIconResource(R.drawable.record_video)
            } else {
                startRecording()
                recordButton.setIconResource(R.drawable.stop_recording)
            }
        }
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                startCamera()
            } else {
                Toast.makeText(requireContext(), "Необходимо разрешение для работы", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }
}