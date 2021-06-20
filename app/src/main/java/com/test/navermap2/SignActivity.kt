package com.test.navermap2

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.os.SystemClock
import android.util.Log
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import kotlinx.android.synthetic.main.activity_sign.*

class SignActivity : AppCompatActivity() {
    private lateinit var auth: FirebaseAuth
    var TAG = "SignActivity"
    var isRun = true
    var thread = PassChk()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign)
        // Initialize Firebase Auth
        auth = FirebaseAuth.getInstance()
        thread.start()

        btSign.setOnClickListener {
            signUp()
        }
        btLogin.setOnClickListener {
            var intent = Intent(this,LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    public override fun onStart() {
        super.onStart()
        val currentUser = auth.currentUser
    }

    fun signUp(){
        var email = edtEmail.getText().toString()
        var password = edtPass1.getText().toString()
        var intent = Intent(this,LoginActivity::class.java)

        if (!email.contains("@")&&email.length<6){
            var toast = Toast.makeText(this,"이메일 형식이 맞지 않습니다",Toast.LENGTH_SHORT)
            toast.show()
        }else {
            auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d(TAG, "createUserWithEmail:success")
                        val user = auth.currentUser
                        //updateUI(user)
                        isRun = false
                        var toast = Toast.makeText(this, "회원가입 성공", Toast.LENGTH_SHORT)
                        toast.show()
                        startActivity(intent)
                        finish()
                    } else {
                        // If sign in fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure", task.exception)
                        Toast.makeText(
                            baseContext, "Authentication failed.",
                            Toast.LENGTH_SHORT
                        ).show()
                        //updateUI(null)
                    }
                }
        }
    }
    inner class PassChk:Thread(){
        override fun run() {
            while (isRun){
                SystemClock.sleep(1000)
                var pass1:String = edtPass1.text.toString()
                var pass2:String = edtPass2.text.toString()
                if (pass1.equals(pass2)){
                    runOnUiThread{
                        tvError.setText("")
                        btSign.setEnabled(true)
                    }
                }else{
                    runOnUiThread{
                        tvError.setText("비밀번호가 맞지 않습니다")
                        btSign.setEnabled(false)
                    }
                }
                if (pass1.length<6){
                    runOnUiThread {
                        tvError.setText("비밀번호는 6자 이상이여야 합니다")
                    }
                }
            }
        }
    }
}