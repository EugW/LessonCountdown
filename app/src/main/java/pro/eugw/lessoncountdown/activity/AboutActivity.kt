package pro.eugw.lessoncountdown.activity

import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import kotlinx.android.synthetic.main.activity_about.*
import pro.eugw.lessoncountdown.BaseActivity
import pro.eugw.lessoncountdown.BuildConfig
import pro.eugw.lessoncountdown.R

class AboutActivity : BaseActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_about)
        imageViewUltimate.setImageBitmap(BitmapFactory.decodeStream(resources.openRawResource(R.raw.lc_ic_hires)))
        textViewDev.movementMethod = LinkMovementMethod.getInstance()
        textViewVer.text = BuildConfig.VERSION_NAME
    }
}
