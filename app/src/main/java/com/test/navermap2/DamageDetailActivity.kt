package com.test.navermap2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_demage_detail.*
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_detail.backbtn
import kotlinx.android.synthetic.main.activity_detail.created_memo
import kotlinx.android.synthetic.main.activity_detail.created_time
import kotlinx.android.synthetic.main.activity_detail.delete_btn
import kotlinx.android.synthetic.main.activity_detail.imageView7
import kotlinx.android.synthetic.main.activity_detail.mapbtn
import kotlinx.android.synthetic.main.activity_detail.textView11
import kotlinx.android.synthetic.main.activity_detail.update_btn

class DamageDetailActivity : AppCompatActivity() {
    var firestore : FirebaseFirestore? = null
    // Access a Cloud Firestore instance from your Activity
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_demage_detail)

        // 파이어스토어 인스턴스 초기화
        firestore = FirebaseFirestore.getInstance()

        var title = intent.getStringExtra("title")
        var timestamp = intent.getStringExtra("timestamp")
        var memo = intent.getStringExtra("memo")
        var imageUrl = intent.getStringExtra("imageUrl")
        var latitude = intent.getDoubleExtra("latitude", 0.0)
        var longitude = intent.getDoubleExtra("longitude", 0.0)

        textView11.text = title
        created_time.text = timestamp
        created_memo.text = memo
        Glide.with(this).load(imageUrl.toString()).into(imageView7)


        // 수정하기 버튼
        update_btn.setOnClickListener {
            var intent = Intent(this, DamageUpdateActivity::class.java)
            intent.putExtra("timestamp", timestamp)
            intent.putExtra("title", title)
            intent.putExtra("memo", memo)
            intent.putExtra("imageUrl", imageUrl)
            startActivity(intent)
            finish()
        }

        // 삭제하기 버튼
        delete_btn.setOnClickListener {
            db.collection("damages").document(timestamp.toString())
                .delete()
            finish()
        }

        // 뒤로가기 버튼
        backbtn.setOnClickListener {
            val intent = Intent(this, DamageActivity::class.java)
            startActivity(intent)
        }

        // 지도 버튼
        mapbtn.setOnClickListener {
            if(latitude != 0.0) {
                val intent = Intent(this, NaverMapActivity::class.java)
                intent.putExtra("title", title)
                intent.putExtra("latitude", latitude)
                intent.putExtra("longitude", longitude)
                startActivity(intent)
            }
        }

        // 훼손 버튼
        crush_btn.setOnClickListener {
            val intent = Intent(this, WebView::class.java)
            startActivity(intent)
        }

    }
}