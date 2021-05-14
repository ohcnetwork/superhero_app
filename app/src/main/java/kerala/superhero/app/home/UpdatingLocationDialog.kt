package kerala.superhero.app.home

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.Window
import kotlinx.android.synthetic.main.dialog_updating_location.*
import kerala.superhero.app.R

class UpdatingLocationDialog(context: Context): Dialog(context) {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(false)
        setContentView(R.layout.dialog_updating_location)
        loaderImageView.setGifImageResource(R.drawable.circle)
    }

}