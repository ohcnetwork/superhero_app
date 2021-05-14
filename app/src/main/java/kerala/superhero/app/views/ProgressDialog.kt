package kerala.superhero.app.views

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Window
import android.widget.TextView
import kerala.superhero.app.R
import kotlinx.android.synthetic.main.progress_dialog.*
import java.lang.IllegalStateException

class ProgressDialog(
    context: Context
) : Dialog(context) {
    var messageTv: TextView? = null

    var message: String = context.getString(R.string.please_wait)
        set(value) {
            try {
                messageTv?.text = message
            } catch (e: IllegalStateException) {
                // This exception is ignored
            }
            field = value
        }

    var cancellable: Boolean = false
        set(value) {
            setCancelable(value)
            field = value
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        setCancelable(cancellable)
        window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        setContentView(R.layout.progress_dialog)
        messageTv = findViewById(R.id.messageTextView)
        messageTv?.text = message
    }

}