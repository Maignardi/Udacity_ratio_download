package com.udacity

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.udacity.databinding.ActivityDetailBinding

class DetailActivity : AppCompatActivity() {

    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.toolbar)

        val fileName = intent.getStringExtra("FILE_NAME") ?: "Unknown file"
        val status = intent.getStringExtra("STATUS") ?: "Unknown status"

        binding.contentDetail.fileName.text = fileName
        binding.contentDetail.status.text = status

        overridePendingTransition(R.anim.slide_in_right, R.anim.slide_out_left)
        pressOkButton()
    }

    override fun onResume() {
        super.onResume()
        binding.contentDetail.detailsMotionLayout.transitionToEnd()
    }

    private fun pressOkButton() {
        binding.btnOk.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
            overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
        }
    }

    override fun onBackPressed() {
        super.onBackPressed()
        overridePendingTransition(R.anim.slide_in_left, R.anim.slide_out_right)
    }
}
