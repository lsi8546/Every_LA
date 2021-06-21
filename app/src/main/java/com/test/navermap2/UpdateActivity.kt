package com.test.navermap2

import android.Manifest
import android.app.Activity
import android.content.ContentValues
import android.content.DialogInterface
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
import androidx.appcompat.app.AlertDialog
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.bumptech.glide.Glide
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.tasks.Task
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.UploadTask
import kotlinx.android.synthetic.main.activity_create.*
import kotlinx.android.synthetic.main.activity_create.backbtn
import kotlinx.android.synthetic.main.activity_create.btn_register
import kotlinx.android.synthetic.main.activity_create.btncreate
import kotlinx.android.synthetic.main.activity_create.editTextTextPersonName
import kotlinx.android.synthetic.main.activity_create.edit_memo
import kotlinx.android.synthetic.main.activity_create.imageView4
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_update.*
import kotlinx.android.synthetic.main.activity_update.btn_update
import kotlinx.android.synthetic.main.activity_update_demage.*
import java.io.FileOutputStream
import java.io.IOException
import java.text.SimpleDateFormat
import java.util.*
import kotlin.concurrent.thread

class UpdateActivity : AppCompatActivity() {
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

    var title: String? = null
    var memo: String? = null
    var imageUrl: String? = null

    private var isClickable = true
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_update)

        // Initiate
        storage = FirebaseStorage.getInstance()
        firestore = FirebaseFirestore.getInstance()

        title = intent.getStringExtra("title").toString()
        memo = intent.getStringExtra("memo").toString()
        imageUrl = intent.getStringExtra("imageUrl").toString()

        editTextTextPersonName.setText(title)
        edit_memo.setText(memo)
        Glide.with(this).load(imageUrl.toString()).into(imageView4)

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
        // 등록하기 버튼
        btn_update.setOnClickListener{
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            requestLocation()
            contentUpload()
            startActivity(intent)

        }
    }
    fun showProgress(show : Boolean){
        if (show){
            progressBar1.visibility = View.VISIBLE
        } else{
            progressBar1.visibility = View.GONE
        }
    }
    fun init(){
        showProgress(false)
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
            // AlertDialog
//            val dlg: AlertDialog.Builder = AlertDialog.Builder(this@UpdateActivity,  android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth)
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

    // 카메라, 갤러리 실행후 이미지 저장 함수
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?){
        super.onActivityResult(requestCode, resultCode, data)
        if(resultCode == Activity.RESULT_OK){
            when(requestCode){
                FLAG_REQ_CAMERA->{
                    if (photoUri != null) {
                        val bitmap = loadBitmapFromMediaStoreBy(photoUri!!)
                        imageView4.setImageBitmap(bitmap)
                        btn_update.setOnClickListener{
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
                                requestLocation()
                                contentUpload()
                                it.postDelayed({
                                    isClickable = true
                                }, 4000)
                            }
                        }
                    }
                }

                FLAG_REQ_STORAGE->{
                    photoUri = data?.data
                    imageView4.setImageURI(photoUri)
                    // 이미지 주소가 디비에 자장되어야할 부분
                    btn_update.setOnClickListener{
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

        var pasttimestamp = intent.getStringExtra("timestamp")

        val db = Firebase.firestore

        //Promise method
        if(photoUri!=null) {
            storageRef?.putFile(photoUri!!)
                ?.continueWithTask { task: Task<UploadTask.TaskSnapshot> ->
                    return@continueWithTask storageRef.downloadUrl
                }?.addOnSuccessListener { uri ->
                    val newdb2 = db.collection("images").document(pasttimestamp.toString())
                    newdb2.update("imageUrl", uri.toString())
                    if(editTextTextPersonName.text.toString()!=null){
                        newdb2.update("title", editTextTextPersonName.text.toString())
                    }
                    if(edit_memo.text.toString()!=null){
                        newdb2.update("memo", edit_memo.text.toString())
                    }
                    val intent = Intent(this, MainActivity::class.java)
                    startActivity(intent)

                    setResult(Activity.RESULT_OK)

                    finish()
            }
        }else {
            val newdb2 = db.collection("images").document(pasttimestamp.toString())
            newdb2.update("title", editTextTextPersonName.text.toString())
            newdb2.update("memo", edit_memo.text.toString())
        }
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