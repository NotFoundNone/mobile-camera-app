package com.example.mobilecourcework

import android.annotation.SuppressLint
import android.content.pm.PackageManager
import android.media.MediaScannerConnection
import android.os.Bundle
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.video.*
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobilecourcework.databinding.FragmentVideoBinding
import java.io.File
import java.io.IOException
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class VideoFragment : Fragment() {

    // ViewBinding для доступа к элементам интерфейса
    private var _binding: FragmentVideoBinding? = null
    private val binding get() = _binding!!

    // Executor для работы с камерой в фоновом потоке
    private lateinit var cameraExecutor: ExecutorService

    // Объект для записи видео
    private lateinit var videoCapture: VideoCapture<Recorder>
    private var currentRecording: Recording? = null

    // Выбор камеры (задняя по умолчанию)
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    // Флаг, указывающий, идет ли запись
    private var isRecording = false

    // Флаг, предотвращающий многократное переключение камеры
    private var isSwitchingCamera = false

    // Handler для управления таймером записи
    private lateinit var timerHandler: Handler
    private var startTime: Long = 0

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        // Инициализация ViewBinding
        _binding = FragmentVideoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация Executor и Handler
        cameraExecutor = Executors.newSingleThreadExecutor()
        timerHandler = Handler(Looper.getMainLooper())

        // Устанавливаем кнопки переключения режимов
        setupToggleButtons()

        // Проверяем разрешения и запускаем камеру
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Обработчик для кнопки открытия галереи
        binding.buttonGallery.setOnClickListener {
            findNavController().navigate(R.id.action_videoFragment_to_galleryFragment)
        }

        // Обработчик для переключения камеры
        binding.buttonSwitchCamera.setOnClickListener {
            if (!isRecording) {
                switchCamera()
            } else {
                Toast.makeText(requireContext(), "Нельзя переключать камеру во время записи", Toast.LENGTH_SHORT).show()
            }
        }

        // Устанавливаем обработчик для кнопки записи
        setupRecordVideoButton()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Очищаем ViewBinding и останавливаем Executor
        _binding = null
        cameraExecutor.shutdown()
    }

    // Настройка переключения между режимами (фото/видео)
    private fun setupToggleButtons() {
        val toggleGroup = binding.toggleGroup
        val currentFragmentId = findNavController().currentDestination?.id

        // Устанавливаем начальное состояние кнопок
        if (currentFragmentId == R.id.photoFragment) {
            toggleGroup.check(R.id.button_photo)
            binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else {
            toggleGroup.check(R.id.button_video)
            binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        // Обработчик переключения кнопок
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_photo -> {
                        // Переключение на режим фото
                        binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        if (currentFragmentId != R.id.photoFragment) {
                            findNavController().navigate(R.id.action_videoFragment_to_photoFragment)
                        }
                    }
                    R.id.button_video -> {
                        // Переключение на режим видео
                        binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        if (currentFragmentId != R.id.videoFragment) {
                            findNavController().navigate(R.id.action_photoFragment_to_videoFragment)
                        }
                    }
                }
            }
        }
    }

    // Запуск камеры
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Настраиваем предварительный просмотр
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Настройка записи видео
            val recorder = Recorder.Builder()
                .setQualitySelector(QualitySelector.from(Quality.HIGHEST))
                .build()

            videoCapture = VideoCapture.withOutput(recorder)

            // Отвязываем старые случаи использования и привязываем новые
            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, videoCapture
                )
            } catch (exc: Exception) {
                // Обработка ошибок при запуске камеры
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Настройка кнопки записи видео
    private fun setupRecordVideoButton() {
        binding.buttonRecordVideo.setOnClickListener {
            if (isRecording) {
                // Остановка записи
                stopRecording()
                binding.buttonRecordVideo.setIconResource(R.drawable.record_video)
            } else {
                // Начало записи
                startRecording()
                binding.buttonRecordVideo.setIconResource(R.drawable.stop_recording)
            }
        }
    }

    // Начало записи видео
    @SuppressLint("MissingPermission")
    private fun startRecording() {
        isRecording = true

        // Создаем файл для видео
        val videoFile = createVideoFile()

        // Настройка параметров записи
        val outputOptions = FileOutputOptions.Builder(videoFile).build()
        currentRecording = videoCapture.output
            .prepareRecording(requireContext(), outputOptions)
            .apply {
                withAudioEnabled() // Включаем запись звука
            }
            .start(ContextCompat.getMainExecutor(requireContext())) { event ->
                handleRecordingEvent(event, videoFile)
            }

        // Отображение таймера записи
        binding.recordingTimerLayout.visibility = View.VISIBLE
        startTimer()
        updateRecordButtonUI(true)
    }

    // Обработка событий записи видео
    private fun handleRecordingEvent(event: VideoRecordEvent, videoFile: File) {
        when (event) {
            is VideoRecordEvent.Start -> {
                // Запись началась
                Toast.makeText(requireContext(), "Запись началась", Toast.LENGTH_SHORT).show()
            }
            is VideoRecordEvent.Finalize -> {
                // Запись завершена
                isRecording = false
                stopTimer()
                binding.recordingTimerLayout.visibility = View.GONE

                if (!event.hasError()) {
                    // Добавляем видео в галерею
                    MediaScannerConnection.scanFile(
                        requireContext(),
                        arrayOf(videoFile.absolutePath),
                        arrayOf("video/mp4"),
                        null
                    )
                } else {
                    Toast.makeText(requireContext(), "Ошибка записи видео", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Создание файла для записи видео
    private fun createVideoFile(): File {
        val externalMoviesDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES)
        if (!externalMoviesDir.exists() && !externalMoviesDir.mkdirs()) {
            throw IOException("Не удалось создать директорию для записи видео")
        }
        return File(externalMoviesDir, "video_${System.nanoTime()}.mp4")
    }

    // Остановка записи
    private fun stopRecording() {
        currentRecording?.stop()
        currentRecording = null
        isRecording = false

        stopTimer()
        binding.recordingTimerLayout.visibility = View.GONE
        updateRecordButtonUI(false)
    }

    // Переключение между камерами (передняя/задняя)
    private fun switchCamera() {
        if (isSwitchingCamera) return
        isSwitchingCamera = true

        toggleCameraSelector()

        Handler(Looper.getMainLooper()).postDelayed({
            startCamera()
            isSwitchingCamera = false
        }, 500)
    }

    // Переключение текущей камеры
    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
    }

    // Обновление иконки кнопки записи
    private fun updateRecordButtonUI(isRecording: Boolean) {
        if (isRecording) {
            binding.buttonRecordVideo.setIconResource(R.drawable.stop_recording)
        } else {
            binding.buttonRecordVideo.setIconResource(R.drawable.record_video)
        }
    }

    // Запуск таймера записи
    private fun startTimer() {
        startTime = System.currentTimeMillis()
        timerHandler.post(object : Runnable {
            override fun run() {
                val elapsedTime = System.currentTimeMillis() - startTime
                val seconds = (elapsedTime / 1000) % 60
                val minutes = (elapsedTime / (1000 * 60)) % 60
                binding.recordingTimer.text = String.format("%02d:%02d", minutes, seconds)
                timerHandler.postDelayed(this, 1000)
            }
        })
    }

    // Остановка таймера записи
    private fun stopTimer() {
        timerHandler.removeCallbacksAndMessages(null)
    }

    // Проверяем, предоставлены ли все необходимые разрешения
    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    // Обработка результата запроса разрешений
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

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001 // Код запроса разрешений
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA, // Разрешение на камеру
            android.Manifest.permission.RECORD_AUDIO // Разрешение на запись звука
        )
    }
}
