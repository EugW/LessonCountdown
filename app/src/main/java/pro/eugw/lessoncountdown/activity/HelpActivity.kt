package pro.eugw.lessoncountdown.activity

import android.app.Activity
import android.content.Context
import android.graphics.BitmapFactory
import android.os.Bundle
import android.text.method.LinkMovementMethod
import kotlinx.android.synthetic.main.activity_help.*
import pro.eugw.lessoncountdown.BuildConfig
import pro.eugw.lessoncountdown.R
import pro.eugw.lessoncountdown.util.APP_PREFERENCES
import pro.eugw.lessoncountdown.util.DARK_THEME

class HelpActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setTheme(if (getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE).getBoolean(DARK_THEME, false)) R.style.AppTheme_Dark else R.style.AppTheme)
        setContentView(R.layout.activity_help)
        imageViewUltimate.setImageBitmap(BitmapFactory.decodeStream(resources.openRawResource(R.raw.lc_ic_hires)))
        textViewDev.movementMethod = LinkMovementMethod.getInstance()
        textViewVKGroup.movementMethod = LinkMovementMethod.getInstance()
        textViewVer.text = BuildConfig.VERSION_NAME
    }
}
