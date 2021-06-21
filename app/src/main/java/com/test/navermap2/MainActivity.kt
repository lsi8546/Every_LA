package com.test.navermap2

import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.android.synthetic.main.activity_main.*
import kotlinx.android.synthetic.main.item_recycler.*
import kotlinx.android.synthetic.main.item_recycler.view.*

class MainActivity : AppCompatActivity() {
    var firestore: FirebaseFirestore? = null
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 파이어스토어 인스턴스 초기화
        firestore = FirebaseFirestore.getInstance()

        recyclerView.adapter = RecyclerViewAdapter()
        recyclerView.layoutManager = GridLayoutManager(this, 2)

        // 검색 옵션 변수
        var searchOption = "name"

        // 스피너 옵션에 따른 동작
        spinner.onItemSelectedListener = object: AdapterView.OnItemSelectedListener {
            override fun onNothingSelected(parent: AdapterView<*>?) {
            }

            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                when (spinner.getItemAtPosition(position)) {
                    "제목" -> {
                        searchOption = "title"
                    }
                    "메모" -> {
                        searchOption = "memo"
                    }
                }
            }
        }

        // 검색 옵션에 따라 검색
        searchBtn.setOnClickListener {
            (recyclerView.adapter as RecyclerViewAdapter).search(searchWord.text.toString(), searchOption)
        }


        // 기본 툴바 비활성화
        setSupportActionBar(toolbar)
        supportActionBar?.setDisplayShowTitleEnabled(false)

        // 추가 버튼
        plusbtn.setOnClickListener {
            val intent = Intent(this, CreateActivity::class.java)
            startActivity(intent)
        }

        // 하단바 활성화
        initNavigationBar()
    }

    // FireStore 데이터 가져오기
    inner class RecyclerViewAdapter : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
        // ContentDTO 클래스 ArrayList 생성성
        var ContentDTOs: ArrayList<ContentDTO> = arrayListOf()

        init {  // ContentDTO의 모델을 불러온 뒤 ContentDTOs으로 변환해 ArrayList에 담음
            firestore?.collection("images")?.orderBy("timestamp", Query.Direction.DESCENDING)
                ?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                    // ArrayList 비워줌
                    ContentDTOs.clear()

                    for (snapshot in querySnapshot!!.documents) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        ContentDTOs.add(item!!)
                    }
                    notifyDataSetChanged()
                }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
            var view =
                LayoutInflater.from(parent.context).inflate(R.layout.item_recycler, parent, false)
            return ViewHolder(view).apply{
                itemView.setOnClickListener {
                    val curPos: Int = adapterPosition
                    val content: ContentDTO = ContentDTOs.get(curPos)
                    val intent2 = Intent(parent.context, DetailActivity::class.java)

                    intent2.putExtra("title", content.title)
                    intent2.putExtra("timestamp", content.timestamp)
                    intent2.putExtra("memo", content.memo)
                    intent2.putExtra("imageUrl", content.imageUrl)
                    intent2.putExtra("latitude", content.latitude)
                    intent2.putExtra("longitude", content.longitude)

                    startActivity(intent2)

                }
            }
        }

        inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        }

        override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
            var viewHolder = (holder as ViewHolder).itemView

            Glide.with(holder.itemView.context).load(ContentDTOs!![position].imageUrl)
                .into(viewHolder.image_recycler)
            viewHolder.title_recycler.text = ContentDTOs[position].title
            viewHolder.time_recycler.text = ContentDTOs[position].timestamp.toString()


        }

        override fun getItemCount(): Int {
            return ContentDTOs.size
        }

        // 파이어스토어에서 데이터를 불러와서 검색어가 있는지 판단
        fun search(searchWord : String, option : String) {
            firestore?.collection("images")?.addSnapshotListener { querySnapshot, firebaseFirestoreException ->
                // ArrayList 비워줌
                ContentDTOs.clear()

                for (snapshot in querySnapshot!!.documents) {
                    if (snapshot.getString(option)!!.contains(searchWord)) {
                        var item = snapshot.toObject(ContentDTO::class.java)
                        ContentDTOs.add(item!!)
                    }
                }
                notifyDataSetChanged()
            }
        }

    }

    // 하단바 활성화 함수
    private fun initNavigationBar() {
        btn_navigation.run {
            setOnNavigationItemSelectedListener {
                when (it.itemId) {
                    R.id.first -> { //홈 버튼
//                            val intent = Intent(context, MainActivity::class.java)
//                            startActivity(intent)
                    }
                    R.id.second -> { // 캠퍼스 맵 버튼
                        val intent = Intent(context, NaverMapActivity::class.java)
                        startActivity(intent)
                    }
                    R.id.third -> { // 훼손 버튼
                        val intent = Intent(context, DamageActivity::class.java)
                        startActivity(intent)
                    }
                }
                true
            }
            selectedItemId = R.id.first
        }
    }
}





