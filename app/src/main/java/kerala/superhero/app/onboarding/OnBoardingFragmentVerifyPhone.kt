package kerala.superhero.app.onboarding

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.EditText
import android.widget.RelativeLayout
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.network.sendOTP
import kerala.superhero.app.utils.hideKeyboard
import kerala.superhero.app.utils.isAValidIndianPhoneNumber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert

class OnBoardingFragmentVerifyPhone : Fragment() {


    private lateinit var phoneContinueButton: Button
    private lateinit var phoneNumberEditText: EditText
    private lateinit var baseLayout: RelativeLayout

    companion object {
        var phoneNumber = ""
        var token = ""
    }


    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.fragment_on_boarding_verify_phone, container, false)
        phoneContinueButton = v.findViewById(R.id.phone_continue_button)
        phoneNumberEditText = v.findViewById(R.id.phone_number)
        baseLayout = v.findViewById(R.id.base_layout)

        phoneContinueButton.setOnClickListener {
            continueSendingOtp()
        }

        phoneNumberEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                continueSendingOtp()
                true
            } else {
                false
            }
        }

        //touching outside editText closes keyboard
        baseLayout.setOnTouchListener { view, event ->
            when (event?.action) {
                MotionEvent.ACTION_DOWN -> hideKeyboard()
            }
            view?.onTouchEvent(event) ?: true
        }

        //closes keyboard when 10 digits are reached
        phoneNumberEditText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
            }

            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {
            }

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.toString().length >= 10)
                    hideKeyboard()
            }
        })

        return v
    }

    private fun continueSendingOtp() {
        phoneNumber = phoneNumberEditText.text.toString().trim()
        if (!phoneNumberEditText.text.isAValidIndianPhoneNumber()) {
            showErrorDialog(getString(R.string.incorrect_phone_number))
            return
        }
        invokeOTPRequest()
    }

    private fun invokeOTPRequest() {
        phoneNumberEditText.isEnabled = false
        val dialog = Dialog(requireActivity())
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)
        dialog.show()
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) { sendOTP(phoneNumber) }
                ) {
                is ResultWrapper.Success -> requireActivity().runOnUiThread {
                    token = response.value.data.token

                    dialog.hide()
                    requireActivity().supportFragmentManager.beginTransaction()
                        .replace(
                            R.id.on_boarding_fragments_container,
                            OnBoardingFragmentVerifyOtp()
                        )
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                else -> requireActivity().runOnUiThread {
                    dialog.hide()
                    phoneNumberEditText.isEnabled = true
                    Toast.makeText(requireActivity(), R.string.unable_to_sent_otp, Toast.LENGTH_SHORT)
                        .show()
                }
            }
        }
    }

    private fun showErrorDialog(errorMessage: String) {
        requireActivity().alert {
            message = errorMessage
            title = getString(R.string.incorrect_input)
            positiveButton(R.string.ok) {
                it.dismiss()
            }
        }.show()
    }

}