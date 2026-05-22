package com.gabrielformento.neuronioamil

import android.content.Intent
import android.media.MediaPlayer
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.animation.AlphaAnimation
import android.widget.ImageView
import androidx.appcompat.app.AppCompatActivity

class SplashActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_splash)

        val imgSplash = findViewById<ImageView>(R.id.imgSplash)

        val idAbertura1 = resources.getIdentifier("abertura1", "drawable", packageName)
        if (idAbertura1 != 0) {
            imgSplash.setImageResource(idAbertura1)
        }

        fadeIn(imgSplash)

        val idSom = resources.getIdentifier("abertura1", "raw", packageName)
        if (idSom != 0) {
            val mp = MediaPlayer.create(this, idSom)
            mp.start()
        }

        Handler(Looper.getMainLooper()).postDelayed({
            val idAbertura2 = resources.getIdentifier("abertura2", "drawable", packageName)
            if (idAbertura2 != 0) {
                imgSplash.setImageResource(idAbertura2)
            }
        }, 3000)

        Handler(Looper.getMainLooper()).postDelayed({
            startActivity(Intent(this, MainActivity::class.java))
            finish()
        }, 6000)
    }

    private fun fadeIn(view: View) {
        val anim = AlphaAnimation(0f, 1f)
        anim.duration = 2000
        view.visibility = View.VISIBLE
        view.startAnimation(anim)
    }
}