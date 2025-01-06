package com.example.mobilecourcework

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
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
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoFragment : Fragment() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isRecording = false
    private val temporaryVideos = mutableListOf<File>() // Список временных видеофайлов

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return inflater.inflate(R.layout.fragment_photo, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        previewView = view.findViewById(R.id.previewView)
        cameraExecutor = Executors.newSingleThreadExecutor()

        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        view.findViewById<ImageButton>(R.id.button_take_photo).setOnClickListener {
            takePhoto()
        }

        view.findViewById<ImageButton>(R.id.button_gallery).setOnClickListener {
            findNavController().navigate(R.id.action_photoFragment_to_galleryFragment)
        }

        view.findViewById<ImageButton>(R.id.button_switch_camera).setOnClickListener {
            switchCamera()
        }

        setupRecordVideoButton(view)
    }

    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            imageCapture = ImageCapture.Builder().build()

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture, videoCapture
                )
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File(mediaDir, "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        triggerFlashEffect()

        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    Toast.makeText(requireContext(), "Фото сохранено: ${photoFile.absolutePath}", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    exception.printStackTrace()
                }
            }
        )
    }

    private fun triggerFlashEffect() {
        val flashView = View(requireContext()).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            setBackgroundColor(ContextCompat.getColor(requireContext(), android.R.color.white))
        }

        (previewView.parent as ViewGroup).addView(flashView)

        flashView.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction { (previewView.parent as ViewGroup).removeView(flashView) }
            .start()
    }

    private fun switchCamera() {
        if (isRecording) {
            stopRecordingForSwitch() // Останавливаем запись
            toggleCameraSelector()  // Переключаем камеру
            startCamera()           // Перезапускаем камеру
            startRecordingAfterSwitch() // Перезапускаем запись
        } else {
            toggleCameraSelector() // Просто переключаем камеру
            startCamera()
        }
    }

    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    private fun stopRecordingForSwitch() {
        if (isRecording && currentRecording != null) {
            currentRecording?.stop()
            currentRecording = null
            isRecording = false
        } else {
            Toast.makeText(requireContext(), "Попытка остановить неактивную запись", Toast.LENGTH_SHORT).show()
        }
    }


    @SuppressLint("MissingPermission")
    private fun startRecordingAfterSwitch() {
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File(mediaDir, "${System.currentTimeMillis()}.mp4")

        if (!videoFile.exists()) {
            videoFile.createNewFile()
        }

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        currentRecording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                if (allPermissionsGranted()) {
                    withAudioEnabled()
                }
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        isRecording = true
                        temporaryVideos.add(videoFile) // Добавляем файл только после успешного старта записи
                        Toast.makeText(requireContext(), "Запись началась", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        isRecording = false
                        Toast.makeText(requireContext(), "Видео сохранено временно: ${videoFile.absolutePath}", Toast.LENGTH_LONG).show()
                    }
                }
            }
    }

    private fun preprocessVideosBeforeMerge() {
        val processedVideos = mutableListOf<File>()

        temporaryVideos.forEach { video ->
            val processedFile = File(video.parent, "processed_${video.name}")
            val command = "-i ${video.absolutePath} -c:v mpeg4 -c:a aac -strict experimental ${processedFile.absolutePath}"

            val result = FFmpeg.execute(command)
            if (result == Config.RETURN_CODE_SUCCESS) {
                processedVideos.add(processedFile)
            } else {
                Toast.makeText(requireContext(), "Ошибка обработки видео: ${video.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        }

        temporaryVideos.clear()
        temporaryVideos.addAll(processedVideos)
    }

    private fun mergeVideos() {
        if (temporaryVideos.size <= 1) return // Нечего объединять

        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFile = File(mediaDir, "merged_${System.currentTimeMillis()}.mp4")

        val listFile = File(mediaDir, "video_list.txt")
        listFile.printWriter().use { out ->
            temporaryVideos.filter { it.exists() }.forEach { video ->
                out.println("file '${video.absolutePath}'")
            }
        }

        if (!listFile.exists() || listFile.readText().isEmpty()) {
            Toast.makeText(requireContext(), "Список файлов для объединения пуст", Toast.LENGTH_SHORT).show()
            return
        }

        val command = "-f concat -safe 0 -i ${listFile.absolutePath} -c copy ${outputFile.absolutePath}"

        val result = FFmpeg.execute(command)
        if (result == Config.RETURN_CODE_SUCCESS) {
            Toast.makeText(requireContext(), "Видео объединено: ${outputFile.absolutePath}", Toast.LENGTH_LONG).show()
            temporaryVideos.forEach { it.delete() }
            temporaryVideos.clear()
        } else {
            Toast.makeText(requireContext(), "Ошибка объединения видео", Toast.LENGTH_SHORT).show()
        }
    }


    private fun setupRecordVideoButton(view: View) {
        val recordButton = view.findViewById<ImageButton>(R.id.button_record_video)
        recordButton.setOnClickListener {
            if (isRecording) {
                stopRecording()
                recordButton.setImageResource(R.drawable.record_video)
            } else {
                startRecording()
                recordButton.setImageResource(R.drawable.stop_recording)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File(mediaDir, "${System.currentTimeMillis()}.mp4")
        temporaryVideos.add(videoFile)

        val outputOptions = FileOutputOptions.Builder(videoFile).build()

        currentRecording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                if (allPermissionsGranted()) {
                    withAudioEnabled()
                }
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
