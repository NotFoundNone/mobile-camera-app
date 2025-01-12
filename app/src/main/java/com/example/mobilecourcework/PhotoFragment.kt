package com.example.mobilecourcework

import android.content.ContentValues
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.example.mobilecourcework.databinding.FragmentPhotoBinding
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

// Фрагмент для работы с фото
class PhotoFragment : Fragment() {
    // View Binding для доступа к элементам интерфейса
    private var _binding: FragmentPhotoBinding? = null
    private val binding get() = _binding!!

    // Исполнительный сервис для фоновых операций камеры
    private lateinit var cameraExecutor: ExecutorService

    // Объект для захвата изображений
    private lateinit var imageCapture: ImageCapture

    // Текущий выбор камеры (задняя по умолчанию)
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        // Инициализация View Binding
        _binding = FragmentPhotoBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Инициализация исполнительного сервиса для работы с камерой
        cameraExecutor = Executors.newSingleThreadExecutor()

        // Установка начального состояния переключателя
        binding.toggleGroup.check(binding.buttonPhoto.id)
        binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
        binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))

        // Обработчик переключения между режимами (фото/видео)
        binding.toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    binding.buttonPhoto.id -> {
                        // Режим фото
                        binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        findNavController().navigate(R.id.action_videoFragment_to_photoFragment)
                    }
                    binding.buttonVideo.id -> {
                        // Режим видео
                        binding.buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        binding.buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        findNavController().navigate(R.id.action_photoFragment_to_videoFragment)
                    }
                }
            }
        }

        // Проверяем разрешения и запускаем камеру
        if (allPermissionsGranted()) {
            startCamera()
        } else {
            // Запрос на предоставление необходимых разрешений
            requestPermissions(REQUIRED_PERMISSIONS, REQUEST_CODE_PERMISSIONS)
        }

        // Обработчик для кнопки "Сделать фото"
        binding.buttonAction.setOnClickListener {
            takePhoto()
        }

        // Обработчик для переключения камеры (передняя/задняя)
        binding.buttonSwitchCamera.setOnClickListener {
            toggleCameraSelector()
            startCamera()
        }

        // Обработчик для перехода в галерею
        binding.buttonGallery.setOnClickListener {
            findNavController().navigate(R.id.action_photoFragment_to_galleryFragment)
        }
    }

    // Запуск камеры
    private fun startCamera() {
        val cameraProviderFuture = ProcessCameraProvider.getInstance(requireContext())
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            // Настраиваем превью камеры
            val preview = Preview.Builder().build().also {
                it.setSurfaceProvider(binding.previewView.surfaceProvider)
            }

            // Создаем объект для захвата изображений
            imageCapture = ImageCapture.Builder().build()

            // Отвязываем все предыдущие случаи использования и привязываем новые
            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
                )
            } catch (exc: Exception) {
                // Обработка ошибки при запуске камеры
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    // Захват фотографии
    private fun takePhoto() {
        // Параметры сохранения файла в галерее
        val contentValues = ContentValues().apply {
            put(MediaStore.MediaColumns.DISPLAY_NAME, "${System.currentTimeMillis()}.jpg") // Уникальное имя файла
            put(MediaStore.MediaColumns.MIME_TYPE, "image/jpeg") // MIME-тип файла
            put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_PICTURES) // Путь для сохранения
        }

        val resolver = requireContext().contentResolver
        val outputOptions = ImageCapture.OutputFileOptions.Builder(
            resolver,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            contentValues
        ).build()

        // Сохраняем изображение и обрабатываем результат
        imageCapture.takePicture(
            outputOptions,
            ContextCompat.getMainExecutor(requireContext()),
            object : ImageCapture.OnImageSavedCallback {
                override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                    // Сообщение об успешном сохранении
                    Toast.makeText(requireContext(), "Фото сохранено в галерее", Toast.LENGTH_LONG).show()
                }

                override fun onError(exception: ImageCaptureException) {
                    // Сообщение об ошибке
                    exception.printStackTrace()
                    Toast.makeText(requireContext(), "Ошибка сохранения: ${exception.message}", Toast.LENGTH_SHORT).show()
                }
            }
        )
    }

    // Переключение камеры (передняя/задняя)
    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
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

    // Очищаем View Binding и завершаем работу executor'а
    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001 // Код запроса разрешений
        private val REQUIRED_PERMISSIONS = arrayOf(
            android.Manifest.permission.CAMERA // Разрешение на использование камеры
        )
    }
}
