package com.example.mobilecourcework

import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Environment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.Toast
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoFragment : Fragment() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA // Начальная камера

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

        // Обработчик кнопки переключения камеры
        view.findViewById<ImageButton>(R.id.button_switch_camera).setOnClickListener {
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
            imageCapture = ImageCapture.Builder().build()

            // Перезапуск камеры с текущим `cameraSelector`
            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (exc: Exception) {
                Toast.makeText(requireContext(), "Ошибка запуска камеры: ${exc.message}", Toast.LENGTH_SHORT).show()
            }
        }, ContextCompat.getMainExecutor(requireContext()))
    }

    private fun takePhoto() {
        val mediaDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        val photoFile = File(mediaDir, "${System.currentTimeMillis()}.jpg")

        val outputOptions = ImageCapture.OutputFileOptions.Builder(photoFile).build()

        // Эффект вспышки перед сохранением снимка
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
        // Переключение между основной и фронтальной камерой
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
        }
        startCamera() // Перезапуск камеры с новым выбором
    }

    private fun allPermissionsGranted() = REQUIRED_PERMISSIONS.all {
        ContextCompat.checkSelfPermission(requireContext(), it) == PackageManager.PERMISSION_GRANTED
    }

    override fun onDestroy() {
        super.onDestroy()
        cameraExecutor.shutdown()
    }

    companion object {
        private const val REQUEST_CODE_PERMISSIONS = 1001
        private val REQUIRED_PERMISSIONS = arrayOf(android.Manifest.permission.CAMERA)
    }
}

