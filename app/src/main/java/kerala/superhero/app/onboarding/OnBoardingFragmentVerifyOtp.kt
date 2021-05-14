package kerala.superhero.app.onboarding

import android.annotation.SuppressLint
import android.app.Dialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentTransaction
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.network.verifyOTP
import kerala.superhero.app.utils.hideKeyboard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert

class OnBoardingFragmentVerifyOtp : Fragment() {

    private lateinit var verifyOtpButton: Button
    private lateinit var otpEditText: EditText
    private lateinit var warningText: TextView
    private lateinit var baseLayout: RelativeLayout

    private lateinit var otp: String

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.fragment_on_boarding_verify_otp, container, false)
        verifyOtpButton = v.findViewById(R.id.verify_otp_button)
        otpEditText = v.findViewById(R.id.otp_edit_text)
        warningText = v.findViewById(R.id.text_three)
        baseLayout = v.findViewById(R.id.base_layout)

        verifyOtpButton.setOnClickListener {
            continueVerifyingOtp()
        }


        otpEditText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                continueVerifyingOtp()
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

        return v
    }

    private fun continueVerifyingOtp() {
        otp = otpEditText.text.toString().trim()
        if (otp.isEmpty()) {
            showErrorDialog(getString(R.string.otp_error))
        } else {
            invokeOTPVerifyRequest()
        }
    }

    private fun invokeOTPVerifyRequest() {
        verifyOtpButton.isEnabled = false
        val dialog = Dialog(requireActivity())
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setContentView(R.layout.progress_dialog)
        dialog.setCancelable(false)
        dialog.show()
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) {
                    verifyOTP(
                        OnBoardingFragmentVerifyPhone.phoneNumber,
                        otp,
                        OnBoardingFragmentVerifyPhone.token
                    )
                }
                ) {
                is ResultWrapper.Success -> requireActivity().runOnUiThread {
                    val successFlag = response.value.meta.success
                    dialog.hide()

                    if (successFlag) {

                        requireActivity().supportFragmentManager.beginTransaction()
                            .replace(
                                R.id.on_boarding_fragments_container,
                                OnBoardingFragmentFinal()
                            )
                            .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                            .commit()
                    } else {
                        verifyOtpButton.isEnabled = true
                        warningText.text = getString(R.string.otp_error)
                        warningText.setTextColor(Color.parseColor("#c70000"))
                    }

                }
                else -> requireActivity().runOnUiThread {
                    verifyOtpButton.isEnabled = true
                    Toast.makeText(requireActivity(), R.string.error, Toast.LENGTH_SHORT)
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