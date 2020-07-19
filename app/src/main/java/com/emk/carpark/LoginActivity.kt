@file:Suppress("DEPRECATION")

package com.emk.carpark

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import kotlinx.android.synthetic.main.activity_login.*
import android.R.id.edit
import android.app.ProgressDialog
import android.content.Context
import android.content.SharedPreferences
import android.widget.Toast


class LoginActivity : AppCompatActivity(), View.OnClickListener {
    companion object {
        const val TAG = "LoginActivity"
    }
    private lateinit var progressBar: ProgressDialog
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        btnLogin.setOnClickListener(this)

        val pref = applicationContext.getSharedPreferences("CarPark", Context.MODE_PRIVATE)
        val currentPlate = pref.getString("plate", "")

        if (currentPlate!!.isNotEmpty()) {
            startMain()
        }
    }

    override fun onClick(v: View?) {

        progressBar = ProgressDialog(this)
        progressBar.setCancelable(false)
        progressBar.setMessage("Giriş Yapılıyor...")
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressBar.progress = 0
        progressBar.max = 100
        progressBar.show()

        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .whereEqualTo("plate", etUsername.text.toString())
            .whereEqualTo("password", etPassword.text.toString())
            .get()
            .addOnSuccessListener { documents ->
                if (documents.size() == 1) {
                    val pref = applicationContext.getSharedPreferences("CarPark", Context.MODE_PRIVATE)
                    pref.edit().putString("plate", etUsername.text.toString()).apply()
                    startMain()
                    Toast.makeText(this, "Giriş Başarılı", Toast.LENGTH_SHORT).show()
                    progressBar.dismiss()

                } else
                {
                    Toast.makeText(this, "Kullanıcı adı ya da şifre hatalı.", Toast.LENGTH_SHORT).show()
                    progressBar.dismiss()
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
                Toast.makeText(this, "Kullanıcı adı/Şifre hatalı.", Toast.LENGTH_SHORT).show()
                progressBar.dismiss()
            }
    }

    fun Register(v: View?)
    {

        progressBar = ProgressDialog(this)
        progressBar.setCancelable(false)
        progressBar.setMessage("Kayıt İşlemi Tamamlanıyor...")
        progressBar.setProgressStyle(ProgressDialog.STYLE_SPINNER)
        progressBar.progress = 0
        progressBar.max = 100
        progressBar.show()


        val db = FirebaseFirestore.getInstance()
        val user = hashMapOf(
            "plate" to etUsername.text.toString(),
            "password" to etPassword.text.toString()
        )

        db.collection("users")
            .document(etUsername.text.toString())
            .set(user)
            .addOnSuccessListener {
                Log.d(TAG, "DocumentSnapshot added")
                progressBar.dismiss()
                Toast.makeText(this, "Kayıt işlemi başarılı! \nLütfen Giriş Yapınız.", Toast.LENGTH_SHORT).show()
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
                Toast.makeText(this, "Kayıt İşlemi Başarısız. Tekrar Deneyiniz.", Toast.LENGTH_SHORT).show()
                progressBar.dismiss()
            }

    }

    private fun startMain() {
        startActivity(Intent(this, MainActivity::class.java))
    }
}