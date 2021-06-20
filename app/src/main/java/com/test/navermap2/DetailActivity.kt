package com.test.navermap2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_detail.*
import kotlinx.android.synthetic.main.activity_detail.backbtn

class DetailActivity : AppCompatActivity() {
    var firestore : FirebaseFirestore? = null
    // Access a Cloud Firestore instance from your Activity
    val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_detail)

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
                val intent = Intent(this, UpdateActivity::class.java)
                intent.putExtra("timestamp", timestamp)
                intent.putExtra("title", title)
                intent.putExtra("memo", memo)
                intent.putExtra("imageUrl", imageUrl)
                startActivity(intent)
            }

            // 삭제하기 버튼
            delete_btn.setOnClickListener {
                db.collection("images").document(timestamp.toString())
                    .delete()
                finish()
            }

            // 뒤로가기 버튼
            backbtn.setOnClickListener {
                val intent = Intent(this, MainActivity::class.java)
                startActivity(intent)
            }

            // 지도 버튼
            mapbtn.setOnClickListener {
                if (latitude != 0.0) {
                    val intent = Intent(this, NaverMapActivity::class.java)
                    intent.putExtra("title", title)
                    intent.putExtra("latitude", latitude)
                    intent.putExtra("longitude", longitude)
                    startActivity(intent)
                }
            }
        }
    }



