package pro.eugw.lessoncountdown.activity

import android.graphics.BitmapFactory
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import android.text.method.LinkMovementMethod
import android.widget.ImageView
import android.widget.TextView
import pro.eugw.lessoncountdown.BuildConfig
import pro.eugw.lessoncountdown.R

class AboutActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        title = getString(R.string.about)
        findViewById<ImageView>(R.id.imageViewUltimate).setImageBitmap(BitmapFactory.decodeStream(resources.openRawResource(R.raw.lc_ic_hires)))
        (findViewById<TextView>(R.id.textViewDev)).movementMethod = LinkMovementMethod.getInstance()
        (findViewById<TextView>(R.id.textViewVer)).text = BuildConfig.VERSION_NAME
    }
}
