package com.test.navermap2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.provider.MediaStore
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_naver_map.*
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class CreateActivity : AppCompatActivity() {
    // 카메라, 갤러리 접근 권한
    val CAMERA_PERMISSION = arrayOf(Manifest.permission.CAMERA)
    val STORAGE_PERMISSION = arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE)

    val FLAG_PERM_CAMERA = 98
    val FLAG_PERM_STORAGE = 99
    val FLAG_REQ_CAMERA = 101
    val FLAG_REQ_STORAGE = 102
    var PICK_IMAGE_FROM_ALBUM = 0

    var storage: FirebaseStorage? = null
    var photoUri: Uri? = null
    var firestore: FirebaseFirestore? = null

    var latitude: Double? = null
    var longitude: Double? = null
    var locationClient: FusedLocationProviderClient? = null

    private var isClickable = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        // Initiate
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        // 뒤로가기 버튼
        backbtn.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        // 권한 승인될 경우 동작
        if(checkPermission(STORAGE_PERMISSION, FLAG_PERM_STORAGE)){
            setViews()
        }

        init()
//        // 등록하기 버튼
//        btn_register.setOnClickListener{
//            val intent = Intent(this, MainActivity::class.java)
//            startActivity(intent)
//            requestLocation()
//            contentUpload()
//        }
    }

    //좌표값 불러오는 함수
    private fun requestLocation() {
        locationClient = LocationServices.getFusedLocationProviderClient(this)

        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            return
        }
        locationClient?.lastLocation
            ?.addOnSuccessListener { location ->
                if (location != null) {
//                    var t1 = Toast.makeText(this, "최근 위치 : ${location.latitude}, ${location.longitude}", Toast.LENGTH_LONG)
//                    t1.show()
                    latitude = location.latitude
                    longitude = location.longitude
                }
            }



    }

    // 권한 확인 함수
    fun checkPermission(permissions: Array<out String>, flag: Int): Boolean {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            for(permission in permissions){
                if(ContextCompat.checkSelfPermission(this, permission)!= PackageManager.PERMISSION_GRANTED){
                    ActivityCompat.requestPermissions(this, permissions, flag)
                    return false
                }
            }
        }
        return true
    }

    // 권한 확인 후 함수
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray){
        when(requestCode){
            FLAG_PERM_STORAGE ->{
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "저장소 권한을 승인해야하지만 앱을 사용 할 수 있습니다.", Toast.LENGTH_LONG).show()
                        finish()
                        return
                    }
                }
                setViews()
            }
            FLAG_PERM_CAMERA->{
                for(grant in grantResults){
                    if(grant != PackageManager.PERMISSION_GRANTED){
                        Toast.makeText(this, "카메라 권한을 승인해야하지만 카메라를 사용할 수 있습니다", Toast.LENGTH_LONG).show()
                        return
                    }
                }
                openCamera()
            }
        }
    }

    // 이미지 추가 버튼 누를시 동작
    fun setViews() {
        btncreate.setOnClickListener{
//            // AlertDialog
//            val dlg: AlertDialog.Builder = AlertDialog.Builder(this@CreateActivity,  android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth)
//            dlg.setTitle("선택") //제목
//            dlg.setPositiveButton("카메라", DialogInterface.OnClickListener { dialog, which ->
//                openCamera()
//            })
//            dlg.setNegativeButton("갤러리", DialogInterface.OnClickListener { dialog, which ->
//                openGallery()
//            })
//            dlg.show()
            openCamera()
        }
    }

    fun createImageUri(filename: String, mimeType: String) : Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)
        return contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
    }

    private fun dispatchTakePictureIntent() {
        // 카메라 인텐트 생성
        val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
        createImageUri(newFileName(), "image/jpg")?.let { uri ->
            photoUri = uri
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
            startActivityForResult(takePictureIntent, FLAG_REQ_CAMERA)
        }
    }

    // 카메라 실행
    fun openCamera() {
        if (checkPermission(CAMERA_PERMISSION, FLAG_PERM_CAMERA)) {
            dispatchTakePictureIntent()
        }
    }

    // 갤러리 실행
    fun openGallery(){
        val intent = Intent(Intent.ACTION_PICK)
        intent.type = MediaStore.Images.Media.CONTENT_TYPE
        startActivityForResult(intent, FLAG_REQ_STORAGE)
    }

    fun loadBitmapFromMediaStoreBy(photoUri: Uri): Bitmap? {
        var image: Bitmap? = null
        try {
            image = if (Build.VERSION.SDK_INT > 27) { // Api 버전별 이미지 처리
                val source: ImageDecoder.Source =
                    ImageDecoder.createSource(this.contentResolver, photoUri)
                ImageDecoder.decodeBitmap(source)
            } else {
                MediaStore.Images.Media.getBitmap(this.contentResolver, photoUri)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return image
    }
    fun showProgress(show : Boolean){
        if (show){
            progressBar.visibility = View.VISIBLE
        } else{
            progressBar.visibility = View.GONE
        }
    }
    private fun init(){
        showProgress(false)
    }
    // 카메라, 갤러리 실행후 이미지 저장 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                FLAG_REQ_CAMERA->{
                    if (photoUri != null) {
                        val bitmap = loadBitmapFromMediaStoreBy(photoUri!!)
                        imageView4.setImageBitmap(bitmap)
                        btn_register.setOnClickListener{
                            showProgress(true)
                            thread(start=true) {
                                Thread.sleep(4000)
                                runOnUiThread {
                                    showProgress(false)
                                }
                            }
                            if (isClickable) {
                                isClickable = false
//                                val intent = Intent(this, MainActivity::class.java)
//                                startActivity(intent)
                                it.postDelayed({
                                    isClickable = true
                                }, 4000)
                            }
                            requestLocation()
                            contentUpload()
                        }
                    }
                }

                FLAG_REQ_STORAGE->{
                    photoUri = data?.data
                    imageView4.setImageURI(photoUri)
                    // 이미지 주소가 디비에 자장되어야할 부분
                    btn_register.setOnClickListener{
                        contentUpload()
                        requestLocation()
                    }

                }
            }
        }
    }

    // Firebase
    fun contentUpload() {
        //Make filename
        var timestamp = SimpleDateFormat("yyyyMMdd_HHmmss").format(Date())
        var imageFileName = "IMAGE_" + timestamp + "_.png"

        var storageRef = storage?.reference?.child("images")?.child(imageFileName)

//        // FileUpload
//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener{
//            Toast.makeText(this, "성공", Toast.LENGTH_LONG).show()
//        }

        //Promise method
        storageRef?.putFile(photoUri!!)?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
            return@continueWithTask storageRef.downloadUrl
        }?.addOnSuccessListener { uri ->
            var contentDTO = ContentDTO()

            //Insert downloadUrl of image
            contentDTO.imageUrl = uri.toString()

            //Insert explain of content
            contentDTO.title = editTextTextPersonName.text.toString()

            //Insert timestamp
            val start = System.currentTimeMillis()

            val date = Date(start)
            val mFormat = SimpleDateFormat("yyyyMMdd_HH:mm:ss")
            val time = mFormat.format(date)
            contentDTO.timestamp = time

            //Insert memo
            contentDTO.memo = edit_memo.text.toString()

            contentDTO.latitude = latitude

            contentDTO.longitude = longitude

            firestore?.collection("images")?.document(time)?.set(contentDTO)

            setResult(Activity.RESULT_OK)

            finish()
        }

//        //Callback method
//        storageRef?.putFile(photoUri!!)?.addOnSuccessListener {
//            storageRef.downloadUrl.addOnSuccessListener { uri ->
//                var contentDTO = ContentDTO()
//                //Insert downloadUrl of image
//                contentDTO.imageUrl = uri.toString()
//                //Insert explain of content
//                contentDTO.explain = editTextTextPersonName.text.toString()
//                //Insert timestamp
//                contentDTO.timestamp = System.currentTimeMillis()
//                firestore?.collection("images")?.document()?.set(contentDTO)
//                setResult(Activity.RESULT_OK)
//                finish()
//            }
//        }
    }

    // 이미지 저장 함수
    fun saveImageFile(filename: String, mimeType: String, bitmap: Bitmap) : Uri? {
        var values = ContentValues()
        values.put(MediaStore.Images.Media.DISPLAY_NAME, filename)
        values.put(MediaStore.Images.Media.MIME_TYPE, mimeType)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
            values.put(MediaStore.Images.Media.IS_PENDING, 1)
        }

        val uri = contentResolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values)
        try{
            if(uri != null){
                var descriptor = contentResolver.openFileDescriptor(uri, "w")
                if(descriptor != null){
                    val fos = FileOutputStream(descriptor.fileDescriptor)
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos)
                    fos.close()
                    if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q){
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        contentResolver.update(uri, values, null, null)
                    }
                }
            }
        }catch(e:java.lang.Exception){
            Log.e("File", "error=${e.localizedMessage}")
        }
        return uri
    }

    // 저장될 이미지 파일이름 지정 함수
    fun newFileName() : String{
        val sdf = SimpleDateFormat("yyyyMMdd_HHmmss")
        val filename = sdf.format(System.currentTimeMillis())

        return "$filename.jpg"
    }
}