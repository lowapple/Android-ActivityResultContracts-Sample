package io.lowapple.android.app.sample

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.widget.Button
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContentProviderCompat.requireContext
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.google.android.material.floatingactionbutton.FloatingActionButton
import java.io.File
import kotlin.coroutines.CoroutineContext

class MainActivity : AppCompatActivity() {
    private lateinit var imageView: ImageView

    // 이미지 촬영
    private lateinit var takePictureButton: Button
    private lateinit var takePicture: ActivityResultLauncher<Uri>

    // 이미지 선택
    private lateinit var choosePictureButton: Button
    private lateinit var choosePicture: ActivityResultLauncher<String>

    private var currentImageUri: Uri? = null

    // 권한
    private val requestPermissionLauncher =
        registerForActivityResult(
            ActivityResultContracts.RequestPermission()
        ) { isGranted: Boolean ->
            if (isGranted) {
                // TODO Granted 처리
            } else {
                // TODO Denied 처리
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        imageView = findViewById(R.id.image_view)
        takePictureButton = findViewById(R.id.take_picture_button)
        choosePictureButton = findViewById(R.id.choose_button)

        // 사진 촬영 버튼 리스너 등록
        takePictureButton.setOnClickListener {
            // 권한 체크
            if (ContextCompat.checkSelfPermission(
                    applicationContext,
                    Manifest.permission.CAMERA
                ) == PackageManager.PERMISSION_DENIED
            ) {
                requestPermissionLauncher.launch(Manifest.permission.CAMERA)
            }
            // 사진 촬영
            else {
                val imageFile = createFileInFiles(applicationContext, "/images")
                currentImageUri =
                    FileProvider.getUriForFile(
                        this,
                        applicationContext.packageName + ".provider",
                        imageFile
                    )
                if (currentImageUri != null)
                    takePicture.launch(currentImageUri)
            }
        }
        // 사진촬영 Activity Launcher
        takePicture =
            registerForActivityResult(ActivityResultContracts.TakePicture()) { isTakePicture ->
                if (isTakePicture) {
                    // 이미지 업데이트
                    imageView.setImageBitmap(
                        loadBitmapFromUri(currentImageUri!!)
                    )
                    Toast.makeText(this, "사진을 찍었습니다", Toast.LENGTH_SHORT).show()
                } else {
                    Toast.makeText(this, "사진을 찍어주세요", Toast.LENGTH_SHORT).show()
                }
                currentImageUri = null
            }
        // 사진 선택
        choosePictureButton.setOnClickListener {
            choosePicture.launch("image/*")
        }
        // 사전 선택 Activity Launcher
        choosePicture = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
            if (uri != null) {
                imageView.setImageBitmap(
                    loadBitmapFromUri(uri)
                )
                Toast.makeText(this, "사진을 불러왔습니다", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "불러오기를 취소했습니다", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun loadBitmapFromUri(uri: Uri) = runCatching {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            ImageDecoder.decodeBitmap(
                ImageDecoder.createSource(
                    contentResolver,
                    uri
                )
            )
        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, uri)
        }
    }.getOrElse {
        val `is` = applicationContext.contentResolver.openInputStream(uri)
        BitmapFactory.decodeStream(`is`)
    }

    private fun createFileInFiles(context: Context, path: String, prefix: String? = null): File {
        val timeStamp: String = System.currentTimeMillis().toString()
        val fileName =
            "${path}/${if (prefix != null && prefix.isBlank()) prefix else ""}_${timeStamp}.jpeg"
        val file = File(getAppFilesDir(context), fileName)
        if (file.parentFile?.exists() == false) {
            file.parentFile?.mkdirs()
        }
        if (!file.exists()) {
            file.createNewFile()
        }
        return file
    }

    private fun getAppFilesDir(context: Context): File? {
        val file = context.filesDir
        if (file != null && !file.exists()) {
            file.mkdirs()
        }
        return file
    }
}