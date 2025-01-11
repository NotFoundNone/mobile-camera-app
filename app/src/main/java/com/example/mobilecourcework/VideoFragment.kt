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
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
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

    // LOG: Тег для логирования
    companion object {
        private const val TAG = "VideoFragmentLogs"
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA,
            android.Manifest.permission.RECORD_AUDIO
        )
    }

    // ====== Поля ======
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null

    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
    private var isRecording = false
    private var isSwitchingCamera = false

    private val temporaryVideos = mutableListOf<File>()

    private lateinit var recordingTimerLayout: LinearLayout
    private lateinit var recordingTimer: TextView
    private lateinit var recordingIndicator: ImageView
    private lateinit var timerHandler: Handler
    private var startTime: Long = 0

    // ====== Жизненный цикл фрагмента ======
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View {
        Log.d(TAG, "onCreateView() called") // LOG
        return inflater.inflate(R.layout.fragment_video, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated() called") // LOG

        cameraExecutor = Executors.newSingleThreadExecutor()
        previewView = view.findViewById(R.id.previewView)

        // Настройка кнопок переключения фрагментов Фото / Видео
        setupToggleButtons(view)

        // Проверка разрешений
        if (allPermissionsGranted()) {
            Log.d(TAG, "Permissions granted, startCamera()") // LOG
            startCamera()
        } else {
            Log.d(TAG, "Permissions not granted, requestPermissions()") // LOG
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Кнопка галереи
        view.findViewById<MaterialButton>(R.id.button_gallery).setOnClickListener {
            Log.d(TAG, "button_gallery clicked") // LOG
            findNavController().navigate(R.id.action_videoFragment_to_galleryFragment)
        }

        // Кнопка переключения камеры
        view.findViewById<MaterialButton>(R.id.button_switch_camera).setOnClickListener {
            Log.d(TAG, "button_switch_camera clicked") // LOG

            switchCamera()
        }

        // Подготовка UI-элементов для таймера
        recordingTimerLayout = view.findViewById(R.id.recording_timer_layout)
        recordingTimer = view.findViewById(R.id.recording_timer)
        recordingIndicator = view.findViewById(R.id.recording_indicator)
        timerHandler = Handler(Looper.getMainLooper())

        // Кнопка записи
        setupRecordVideoButton(view)
    }

    override fun onDestroy() {
        Log.d(TAG, "onDestroy() called") // LOG
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    // ====== Настройка Toggle-кнопок (Фото / Видео) ======
    private fun setupToggleButtons(view: View) {
        Log.d(TAG, "setupToggleButtons() called") // LOG
        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val buttonPhoto = view.findViewById<MaterialButton>(R.id.button_photo)
        val buttonVideo = view.findViewById<MaterialButton>(R.id.button_video)
        val currentFragmentId = findNavController().currentDestination?.id
        Log.d(TAG, "currentFragmentId = $currentFragmentId") // LOG

        // Начальное состояние
        if (currentFragmentId == R.id.photoFragment) {
            toggleGroup.check(R.id.button_photo)
            buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            toggleGroup.check(R.id.button_video)
            buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_photo -> {
                        Log.d(TAG, "Toggle -> Photo clicked") // LOG
                        buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        if (currentFragmentId != R.id.photoFragment) {
                            findNavController().navigate(R.id.action_videoFragment_to_photoFragment)
                        }
                    }
                    R.id.button_video -> {
                        Log.d(TAG, "Toggle -> Video clicked") // LOG
                        buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        if (currentFragmentId != R.id.videoFragment) {
                            findNavController().navigate(R.id.action_photoFragment_to_videoFragment)
                        }
                    }
                }
            }
        }
    }

    // ====== Инициализация и запуск камеры ======
    private fun startCamera() {
        Log.d(TAG, "startCamera() called with cameraSelector=$cameraSelector") // LOG
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()
            Log.d(TAG, "cameraProviderFuture returned cameraProvider=$cameraProvider") // LOG

            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(previewView.surfaceProvider)
            }

            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
                Log.d(TAG, "startCamera() - camera bound successfully") // LOG
            } catch (exc: Exception) {
                Log.e(TAG, "Camera binding failed: ${exc.message}", exc) // LOG
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // ====== Кнопка записи (запуск и остановка) ======
    private fun setupRecordVideoButton(view: View) {
        Log.d(TAG, "setupRecordVideoButton() - setting OnClickListener for recordButton") // LOG
        val recordButton = view.findViewById<MaterialButton>(R.id.button_record_video)
        recordButton.setOnClickListener {
            Log.d(TAG, "recordButton clicked. isRecording=$isRecording") // LOG
            if (isRecording) {
                Log.d(TAG, "-> stopRecording()") // LOG
                stopRecording()
                recordButton.setIconResource(R.drawable.record_video)
            } else {
                Log.d(TAG, "-> startRecording()") // LOG
                startRecording()
                recordButton.setIconResource(R.drawable.stop_recording)
            }
            Log.d(TAG, "recordButton click finished. isRecording=$isRecording") // LOG
        }
    }

    @SuppressLint("MissingPermission")
    private fun startRecording() {
        Log.d(TAG, "startRecording() called. isRecording=$isRecording") // LOG
        isRecording = true

        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val videoFile = File(mediaDir, "video_${System.nanoTime()}.mp4")
        temporaryVideos.add(videoFile)
        Log.d(TAG, "startRecording() - new videoFile=${videoFile.absolutePath}") // LOG

        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        currentRecording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                withAudioEnabled()
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                when (event) {
                    is VideoRecordEvent.Start -> {
                        // LOG:
                        Log.d(TAG, "VideoRecordEvent.Start. isRecording=$isRecording. file=${videoFile.absolutePath}")
                        Toast.makeText(requireContext(), "Запись началась", Toast.LENGTH_SHORT).show()
                    }
                    is VideoRecordEvent.Finalize -> {
                        Log.d(TAG, "VideoRecordEvent.Finalize. isRecording=$isRecording. file=${videoFile.absolutePath}")
                        isRecording = false
                        stopTimer()
                        recordingTimerLayout.visibility = View.GONE
                        Toast.makeText(requireContext(), "Видео сохранено:\n${videoFile.absolutePath}", Toast.LENGTH_LONG).show()
                        validateVideos()
                        preprocessVideosBeforeMerge()
                        mergeVideos()
                    }
                    // Дополнительные состояния можно логировать, если нужны:
                    is VideoRecordEvent.Status -> {
                        Log.d(TAG, "VideoRecordEvent.Status: $event") // LOG
                    }
                    is VideoRecordEvent.Pause -> {
                        Log.d(TAG, "VideoRecordEvent.Pause: $event") // LOG
                    }
                    is VideoRecordEvent.Resume -> {
                        Log.d(TAG, "VideoRecordEvent.Resume: $event") // LOG
                    }
                }
            }

        // UI таймер
        recordingTimerLayout.visibility = View.VISIBLE
        startTimer()
        updateRecordButtonUI(true)

        Log.d(TAG, "startRecording() done. isRecording=$isRecording") // LOG
    }

    private fun stopRecording() {
        Log.d(TAG, "stopRecording() called. isRecording=$isRecording") // LOG
        currentRecording?.stop()
        currentRecording = null
        isRecording = false

        stopTimer()
        recordingTimerLayout.visibility = View.GONE
        updateRecordButtonUI(false)
        Log.d(TAG, "stopRecording() done. isRecording=$isRecording") // LOG
    }

    // ====== Переключение камеры ======
    private fun switchCamera() {
        Log.d(TAG, "switchCamera() called. isSwitchingCamera=$isSwitchingCamera. isRecording=$isRecording") // LOG
        if (isSwitchingCamera) return
        isSwitchingCamera = true

        val wasRecording = isRecording
        if (wasRecording) {
            Log.d(TAG, "switchCamera() - we were recording, calling stopRecordingForSwitch()") // LOG
            stopRecordingForSwitch()
            updateRecordButtonUI(false)
        }

        toggleCameraSelector()

        Handler(Looper.getMainLooper()).postDelayed({
            Log.d(TAG, "switchCamera() - postDelayed startCamera()") // LOG
            startCamera()
            isSwitchingCamera = false

            if (wasRecording) {
                Log.d(TAG, "switchCamera() - wasRecording=true, start new recording") // LOG
                startRecording()
                updateRecordButtonUI(true)
            }
        }, 500)
    }

    private fun toggleCameraSelector() {
        Log.d(TAG, "toggleCameraSelector() called with current=$cameraSelector") // LOG
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        Log.d(TAG, "toggleCameraSelector() -> new=$cameraSelector") // LOG
    }

    private fun stopRecordingForSwitch() {
        Log.d(TAG, "stopRecordingForSwitch() called. isRecording=$isRecording") // LOG
        try {
            currentRecording?.stop()
            currentRecording = null
            isRecording = false
            stopTimer()
            recordingTimerLayout.visibility = View.GONE
        } catch (e: Exception) {
            Log.e(TAG, "Ошибка при остановке записи для переключения камеры: ${e.message}", e) // LOG
        }
        Log.d(TAG, "stopRecordingForSwitch() done. isRecording=$isRecording") // LOG
    }

    // ====== Подготовка и склеивание видеофайлов ======
    private fun validateVideos() {
        Log.d(TAG, "validateVideos() called. temporaryVideos.size=${temporaryVideos.size}") // LOG
        // Удаляем повреждённые или пустые файлы
        temporaryVideos.forEach { video ->
            if (!video.exists() || video.length() == 0L) {
                Log.d(TAG, "validateVideos() - removing damaged file: ${video.absolutePath}") // LOG
                temporaryVideos.remove(video)
                Toast.makeText(requireContext(), "Поврежденный файл удалён:\n${video.absolutePath}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun preprocessVideosBeforeMerge() {
        Log.d(TAG, "preprocessVideosBeforeMerge() called. temporaryVideos.size=${temporaryVideos.size}") // LOG
        val processedVideos = mutableListOf<File>()

        temporaryVideos.forEach { video ->
            if (video.exists() && video.length() > 0L) {
                val processedFile = File(video.parent, "processed_${video.name}")
                // LOG:
                Log.d(TAG, "FFmpeg command for file=${video.name}: -i ${video.absolutePath} -c:v mpeg4 -q:v 2 -r 30 -g 30 -c:a aac -b:a 128k ${processedFile.absolutePath}")

                val command = "-i ${video.absolutePath} -c:v mpeg4 -q:v 2 -r 30 -g 30 -c:a aac -b:a 128k ${processedFile.absolutePath}"
                val result = FFmpeg.execute(command)
                if (result == Config.RETURN_CODE_SUCCESS) {
                    Log.d(TAG, "FFmpeg success for file: ${video.name}") // LOG
                    processedVideos.add(processedFile)
                } else {
                    Log.e(TAG, "Ошибка обработки видео: ${video.absolutePath}, code=$result") // LOG
                    Toast.makeText(requireContext(), "Ошибка обработки видео:\n${video.absolutePath}", Toast.LENGTH_SHORT).show()
                }
            } else {
                Log.e(TAG, "Некорректный файл: ${video.absolutePath}") // LOG
            }
        }

        temporaryVideos.clear()
        temporaryVideos.addAll(processedVideos)
    }

    private fun mergeVideos() {
        Log.d(TAG, "mergeVideos() called. temporaryVideos.size=${temporaryVideos.size}") // LOG
        if (temporaryVideos.size <= 1) {
            Log.d(TAG, "mergeVideos() - no need to merge, count <= 1") // LOG
            return
        }

        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_MOVIES)
        val outputFile = File(mediaDir, "merged_${System.currentTimeMillis()}.mp4")
        val listFile = File(mediaDir, "video_list.txt")

        listFile.printWriter().use { out ->
            temporaryVideos.filter { it.exists() }.forEach { video ->
                out.println("file '${video.absolutePath}'")
            }
        }
        Log.d(TAG, "mergeVideos() - created listFile: ${listFile.absolutePath}") // LOG

        val command = "-f concat -safe 0 -i ${listFile.absolutePath} -c:v mpeg4 -q:v 2 -r 30 -c:a aac -b:a 128k ${outputFile.absolutePath}"
        Log.d(TAG, "mergeVideos() - FFmpeg command: $command") // LOG

        val result = FFmpeg.execute(command)
        if (result == Config.RETURN_CODE_SUCCESS && outputFile.exists() && outputFile.length() > 0L) {
            Log.d(TAG, "mergeVideos() - merged result saved: ${outputFile.absolutePath}") // LOG
            Toast.makeText(requireContext(), "Видео объединено:\n${outputFile.absolutePath}", Toast.LENGTH_LONG).show()

            // Удаляем временные видеофайлы
            temporaryVideos.forEach {
                Log.d(TAG, "mergeVideos() - deleting temp file: ${it.absolutePath}") // LOG
                it.delete()
            }
            temporaryVideos.clear()

            // Удаляем список
            listFile.delete()
        } else {
            Log.e(TAG, "Ошибка объединения видео. Код ошибки: $result") // LOG
            Toast.makeText(requireContext(), "Ошибка объединения видео", Toast.LENGTH_SHORT).show()
        }
    }

    // ====== Вспомогательные методы (UI, таймер, разрешения) ======
    private fun updateRecordButtonUI(isRecording: Boolean) {
        Log.d(TAG, "updateRecordButtonUI($isRecording) called.") // LOG
        val recordButton = view?.findViewById<MaterialButton>(R.id.button_record_video)
        if (isRecording) {
            recordButton?.setIconResource(R.drawable.stop_recording)
        } else {
            recordButton?.setIconResource(R.drawable.record_video)
        }
    }

    private fun startTimer() {
        Log.d(TAG, "startTimer() called") // LOG
        startTime = System.currentTimeMillis()
        timerHandler.post(object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / (1000 * 60)) % 60
                recordingTimer.text = String.format("%02d:%02d", minutes, seconds)
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    private fun stopTimer() {
        Log.d(TAG, "stopTimer() called") // LOG
        timerHandler.removeCallbacksAndMessages(null)
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        Log.d(TAG, "onRequestPermissionsResult() called") // LOG
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE_PERMISSIONS) {
            if (allPermissionsGranted()) {
                Log.d(TAG, "All permissions granted, startCamera()") // LOG
                startCamera()
            } else {
                Log.d(TAG, "Permissions not granted by the user") // LOG
                Toast.makeText(requireContext(), "Необходимо разрешение для работы", Toast.LENGTH_SHORT).show()
            }
        }
    }
}
