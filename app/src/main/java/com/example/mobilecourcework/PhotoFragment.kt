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
import com.google.android.material.button.MaterialButton
import com.google.android.material.button.MaterialButtonToggleGroup
import java.io.File
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class PhotoFragment : Fragment() {
    private lateinit var cameraExecutor: ExecutorService
    private lateinit var previewView: PreviewView
    private lateinit var imageCapture: ImageCapture
    private var cameraSelector: CameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

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

        val toggleGroup = view.findViewById<MaterialButtonToggleGroup>(R.id.toggleGroup)
        val buttonPhoto = view.findViewById<MaterialButton>(R.id.button_photo)
        val buttonVideo = view.findViewById<MaterialButton>(R.id.button_video)

        // Устанавливаем начальное состояние
        val currentFragmentId = findNavController().currentDestination?.id
        if (currentFragmentId == R.id.photoFragment) {
            toggleGroup.check(R.id.button_photo)
            buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        } else if (currentFragmentId == R.id.videoFragment) {
            toggleGroup.check(R.id.button_video)
            buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
            buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
        }

        // Обработчик переключения режимов
        toggleGroup.addOnButtonCheckedListener { _, checkedId, isChecked ->
            if (isChecked) {
                when (checkedId) {
                    R.id.button_photo -> {
                        buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                        if (currentFragmentId != R.id.photoFragment) {
                            findNavController().navigate(R.id.action_videoFragment_to_photoFragment)
                        }
                    }
                    R.id.button_video -> {
                        buttonVideo.setTextColor(ContextCompat.getColor(requireContext(), R.color.orange))
                        buttonPhoto.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
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

        view.findViewById<MaterialButton>(R.id.button_action).setOnClickListener {
            takePhoto()
        }

        view.findViewById<MaterialButton>(R.id.button_switch_camera).setOnClickListener {
            toggleCameraSelector()
            startCamera()
        }

        view.findViewById<MaterialButton>(R.id.button_gallery).setOnClickListener {
            findNavController().navigate(R.id.action_photoFragment_to_galleryFragment)
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

            cameraProvider.unbindAll()
            try {
                cameraProvider.bindToLifecycle(
                    this, cameraSelector, preview, imageCapture
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

    private fun toggleCameraSelector() {
        cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
            CameraSelector.DEFAULT_FRONT_CAMERA
        } else {
            CameraSelector.DEFAULT_BACK_CAMERA
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
            android.Manifest.permission.CAMERA
        )
    }
}

