package kerala.superhero.app.home

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import kotlinx.android.synthetic.main.dialog_super_hero.*
import kerala.superhero.app.R

class ThanksDialog(context: Context, val onClose: () -> Unit): Dialog(context) {

    override fun dismiss() {
        super.dismiss()
        onClose()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setCancelable(false)
        setContentView(R.layout.dialog_super_hero)

        okayTextView.setOnClickListener {
            dismiss()
        }

        likeButtonImage.setOnClickListener {
            dismiss()
        }

    }

}