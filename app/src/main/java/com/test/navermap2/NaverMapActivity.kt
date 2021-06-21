package com.test.navermap2

import android.content.DialogInterface
import android.content.Intent
import android.graphics.Color
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.annotation.UiThread
import androidx.appcompat.app.AlertDialog
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.naver.maps.geometry.LatLng
import com.naver.maps.map.*
import com.naver.maps.map.overlay.InfoWindow
import com.naver.maps.map.overlay.Marker
import com.naver.maps.map.overlay.OverlayImage
import com.naver.maps.map.util.FusedLocationSource
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.activity_main.btn_navigation
import kotlinx.android.synthetic.main.activity_naver_map.*
import kotlinx.android.synthetic.main.ic_info.*
import java.text.SimpleDateFormat
import java.util.*
import kotlin.reflect.typeOf

class NaverMapActivity : AppCompatActivity(), OnMapReadyCallback {
    private lateinit var locationSource: FusedLocationSource
    private lateinit var naverMap: NaverMap
    val auth = FirebaseAuth.getInstance()
    var backKeyPressedTime : Long = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_naver_map)

        // NaverMap Api 초기화
        locationSource = FusedLocationSource(this, LOCATION_PERMISSION_REQUEST_CODE)

        val fm = supportFragmentManager
        val mapFragment = fm.findFragmentById(R.id.map) as MapFragment?
            ?: MapFragment.newInstance().also {
                fm.beginTransaction().add(R.id.map, it).commit()
            }

        mapFragment.getMapAsync(this)
        initNavigationBar() // 하단바 생성

        // 정보 버튼 클릭시 AlertDialog
        info.setOnClickListener {
            val dlg: AlertDialog.Builder = AlertDialog.Builder(
                this@NaverMapActivity,
                android.R.style.Theme_DeviceDefault_Light_Dialog_NoActionBar_MinWidth
            )
            dlg.setView(R.layout.ic_info)
            dlg.setPositiveButton(
                "확인                                    ",
                DialogInterface.OnClickListener { dialog, which ->

                })

            dlg.show()
        }


    }

    override fun onBackPressed() {
        //1번째 백버튼 클릭
        if(System.currentTimeMillis()>backKeyPressedTime+2000){
            backKeyPressedTime = System.currentTimeMillis();
            Toast.makeText(this, "한번 더 누르시면 앱이 종료됩니다", Toast.LENGTH_SHORT).show();
        }
        //2번째 백버튼 클릭 (종료)
        else{
            finishAffinity()
            System.exit(0)
            android.os.Process.killProcess(android.os.Process.myPid())
        }
    }


    // 하단바 생성 함수
    private fun initNavigationBar() {
        btn_navigation.run {
            setOnNavigationItemSelectedListener {
                when(it.itemId) {
                    R.id.first -> {
                        val intent = Intent(context, MainActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.second -> {
//                        val intent = Intent(context, NaverMapActivity::class.java)
//                        startActivity(intent)
                    }
                    R.id.third -> {
                        val intent = Intent(context, DamageActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            }
            selectedItemId = R.id.second
        }
    }

    // gps 권한 설정 함수
    override fun onRequestPermissionsResult(requestCode: Int,
                                            permissions: Array<String>,
                                            grantResults: IntArray) {
        if (locationSource.onRequestPermissionsResult(requestCode, permissions,
                        grantResults)) {
            if (!locationSource.isActivated) { // 권한 거부됨
                naverMap.locationTrackingMode = LocationTrackingMode.None
            }
            return
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
    }

    @UiThread
    override fun onMapReady(naverMap: NaverMap) {
        // 파이어 베이스
        var firestore: FirebaseFirestore?


        firestore = FirebaseFirestore.getInstance()

        val start: Long = System.currentTimeMillis()

        val date = Date(start)
        val mFormat = SimpleDateFormat("yyyyMMdd_HH:mm:ss", Locale("ko", "KR"))
        val time = mFormat.format(date)

        firestore.collection("images")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if(document.data["latitude"] != null) {
                        // 마커5 속성
                        val marker5 = Marker()
                        marker5.position = LatLng(
                            document.data["latitude"] as Double,
                            document.data["longitude"] as Double
                        )
                        marker5.map = naverMap
                        if (time.toString().substring(0, 8)
                                .toInt() - document.data["timestamp"].toString().substring(0, 8)
                                .toInt() >= 3
                        ) {
                            marker5.icon = OverlayImage.fromResource(R.drawable.ic_marker_green)
                        } else {
                            marker5.icon = OverlayImage.fromResource(R.drawable.ic_marker_yellow)
                        }

                        if (intent.hasExtra("title")) {
                            var title = intent.getStringExtra("title")
                            var latitude = intent.getDoubleExtra("latitude", 35.4540299)
                            var longitude = intent.getDoubleExtra("longitude", 128.8082449)
                            if (title == document.data["title"].toString()) {
                                val infoWindow = InfoWindow()
                                infoWindow.open(marker5)
                                infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(this) {
                                    override fun getText(infoWindow: InfoWindow): CharSequence {
                                        return title as CharSequence
                                    }
                                }
                                // 카메라 위치, 줌 조정
                                val cameraUpdate =
                                    CameraUpdate.scrollTo(LatLng(latitude, longitude))
                                val cameraUpdate1 = CameraUpdate.zoomTo(21.0)
                                naverMap.moveCamera(cameraUpdate)
                                naverMap.moveCamera(cameraUpdate1)
                            }
                        }

                        //마커5 이벤트
                        marker5.setOnClickListener {
                            val intent = Intent(this, DetailActivity::class.java)

                            intent.putExtra("title", document.data["title"].toString())
                            intent.putExtra("timestamp", document.data["timestamp"].toString())
                            intent.putExtra("memo", document.data["memo"].toString())
                            intent.putExtra("imageUrl", document.data["imageUrl"].toString())
                            intent.putExtra("latitude", document.data["latitude"] as Double)
                            intent.putExtra("longitude", document.data["longitude"] as Double)

                            startActivity(intent)

                            // 이벤트 전파
                            false
                        }
                    }
                }
            }
        firestore.collection("damages")
            .get()
            .addOnSuccessListener { result ->
                for (document in result) {
                    if(document.data["latitude"] != null){
                        // 마커6 속성
                        val marker6 = Marker()
                        marker6.position = LatLng(
                            document.data["latitude"] as Double,
                            document.data["longitude"] as Double
                        )
                        marker6.map = naverMap
                        if (time.toString().substring(0, 8)
                                .toInt() - document.data["timestamp"].toString().substring(0, 8)
                                .toInt() >= 3
                        ) {
                            marker6.icon = OverlayImage.fromResource(R.drawable.ic_marker_green)
                        } else {
                            marker6.icon = OverlayImage.fromResource(R.drawable.ic_marker_red)
                        }

                        if (intent.hasExtra("title")) {
                            var title = intent.getStringExtra("title")
                            var latitude = intent.getDoubleExtra("latitude", 35.4540299)
                            var longitude = intent.getDoubleExtra("longitude", 128.8082449)
                            if (title == document.data["title"].toString()) {
                                val infoWindow = InfoWindow()
                                infoWindow.open(marker6)
                                infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(this) {
                                    override fun getText(infoWindow: InfoWindow): CharSequence {
                                        return title
                                    }
                                }
                            }

                            // 카메라 위치, 줌 조정
                            val cameraUpdate = CameraUpdate.scrollTo(LatLng(latitude, longitude))
                            val cameraUpdate1 = CameraUpdate.zoomTo(21.0)
                            naverMap.moveCamera(cameraUpdate)
                            naverMap.moveCamera(cameraUpdate1)
                        }

                        //마커6 이벤트
                        marker6.setOnClickListener {
                            val intent = Intent(this, DamageDetailActivity::class.java)

                            intent.putExtra("title", document.data["title"].toString())
                            intent.putExtra("timestamp", document.data["timestamp"].toString())
                            intent.putExtra("memo", document.data["memo"].toString())
                            intent.putExtra("imageUrl", document.data["imageUrl"].toString())
                            intent.putExtra("latitude", document.data["latitude"] as Double)
                            intent.putExtra("longitude", document.data["longitude"] as Double)

                            startActivity(intent)
                            // 이벤트 전파
                            false
                        }
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.d(TestActivity.TAG, "Error getting documents: ", exception)
            }

        // 카메라 위치, 줌 조정
        val cameraUpdate = CameraUpdate.scrollTo(LatLng(35.4540299, 128.8082449))
        val cameraUpdate1 = CameraUpdate.zoomTo(15.3)
        naverMap.moveCamera(cameraUpdate)
        naverMap.moveCamera(cameraUpdate1)

        // 마커 객체 생성
        val marker1 = Marker()
        val marker2 = Marker()
        val marker3 = Marker()
        val marker4 = Marker()
        // 정보 창 객체 생성
        val infoWindow = InfoWindow()

        // 정보창 속성
        infoWindow.adapter = object : InfoWindow.DefaultTextAdapter(this) {
            override fun getText(infoWindow: InfoWindow): CharSequence {
                return "정보 창 내용"
            }
        }

        //마커1 이벤트
        marker1.setOnClickListener {
            Toast.makeText(this, "마커 1 클릭됨", Toast.LENGTH_SHORT).show()
            infoWindow.open(marker1)
            // 이벤트 전파
            false
        }

        //마커2 이벤트
        marker2.setOnClickListener {
            Toast.makeText(this, "마커 2 클릭됨", Toast.LENGTH_SHORT).show()
            // 이벤트 소비
            true
        }

        //마커3 이벤트
        marker3.setOnClickListener {
            Toast.makeText(this, "마커 3 클릭됨", Toast.LENGTH_SHORT).show()
            // 이벤트 소비
            true
        }

        // 설정 객체 생성
        val uiSettings = naverMap.uiSettings

        // 나침반, 확대축소버튼, 현재위치버튼 추가
        uiSettings.isCompassEnabled = true
        uiSettings.isLocationButtonEnabled = true
        uiSettings.isZoomControlEnabled = true

        // 현재 위치 표시
        this.naverMap = naverMap
        naverMap.locationSource = locationSource
        naverMap.locationTrackingMode = LocationTrackingMode.NoFollow
    }
    // 권한 설정
    companion object {
        val TAG: String = "MyLog"
        private const val LOCATION_PERMISSION_REQUEST_CODE = 1000
    }
}