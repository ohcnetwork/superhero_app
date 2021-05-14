package kerala.superhero.app.login

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.view.View
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.network.resetPassword
import kerala.superhero.app.network.sendOTP
import kerala.superhero.app.utils.handleGenericError
import kerala.superhero.app.utils.handleNetworkError
import kerala.superhero.app.utils.isAValidIndianPhoneNumber
import kerala.superhero.app.views.ProgressDialog
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.android.synthetic.main.activity_reset_passowrd.*
import kotlinx.android.synthetic.main.activity_reset_passowrd.resetPasswordEditText
import kotlinx.android.synthetic.main.activity_reset_passowrd.resetPasswordPhoneNumberEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert
import org.jetbrains.anko.toast

class ResetPasswordActivity : AppCompatActivity() {

    private var token = ""
    private var phone = ""

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_reset_passowrd)

        progressDialog = ProgressDialog(this)

        phone = intent.extras?.getString("phone", "") ?: ""

        loginLink.setOnClickListener {
            Intent(this, LoginActivity::class.java).apply {
                putExtra("phone", intent.extras?.getString("phone", ""))
            }.let(::startActivity)
            finishAffinity()
        }



        if (intent.extras?.getString("phone", "").isNullOrEmpty()) {
            resetPasswordPhoneNumberEditText.isEnabled = true
        } else {
            resetPasswordPhoneNumberEditText.isEnabled = false
            resetPasswordPhoneNumberEditText.setText(phone)
            invokeOTPRequest()
            getOtp.visibility = View.GONE
        }


        resetPasswordButton.setOnClickListener {
            if (validate()) {


                changePassword()
            }
        }

        resetPasswordButtonImage.setOnClickListener {
            if (validate()) {
                changePassword()
            }
        }

        getOtp.setOnClickListener {
            attemptToSendOtp()

        }

    }

    private fun attemptToSendOtp() {
        phone = resetPasswordPhoneNumberEditText.text.toString()
        val isValid = resetPasswordPhoneNumberEditText.text.isAValidIndianPhoneNumber()
        if (!isValid) {
            showErrorDialog(getString(R.string.incorrect_phone_number))
        } else {
            invokeOTPRequest()
        }
    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        super.onDestroy()
    }

    private fun invokeOTPRequest() {
        progressDialog?.show()
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) { sendOTP(phone) }
                ) {
                is ResultWrapper.Success -> runOnUiThread {
                    progressDialog?.dismiss()
                    toast(getString(R.string.otp_sent))
                    token = response.value.data.token
                }
                else -> runOnUiThread {
                    progressDialog?.dismiss()
                    toast(getString(R.string.unable_to_sent_otp))
                    loginLink.callOnClick()
                }
            }
        }
    }

    private fun changePassword() {
        progressDialog?.show()
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) {
                    resetPassword(
                        phone,
                        resetConfirmPasswordEditText.text.toString(),
                        otpEditText.text.toString(),
                        token
                    )
                }
                ) {
                is ResultWrapper.Success -> runOnUiThread {
                    alert {
                        message = response.value.message
                        positiveButton(R.string.ok) {
                            loginLink.callOnClick()
                        }
                        isCancelable = false
                    }.show()
                }
                is ResultWrapper.GenericError -> handleGenericError(response)
                is ResultWrapper.NetworkError -> handleNetworkError()
            }
            runOnUiThread {
                progressDialog?.dismiss()
            }
        }
    }

    private fun validatePassword(): Boolean {
        val isCharacterValid = resetPasswordEditText.text.toString().length > 7
        if (!isCharacterValid) {
            showErrorDialog(getString(R.string.password_length))
        }
        return isCharacterValid
    }

    private fun validateConfirmPassword(): Boolean {
        val passwordsMatch =
            resetPasswordEditText.text.toString() == resetConfirmPasswordEditText.text.toString()
        if (!passwordsMatch) {
            showErrorDialog(getString(R.string.passwords_do_not_match))
        }
        return passwordsMatch
    }

    private fun validate(): Boolean {
        return !otpEditText.text.isNullOrEmpty() && token.isNotEmpty() && validatePassword() && validateConfirmPassword() && resetPasswordPhoneNumberEditText.text.isAValidIndianPhoneNumber()
    }

    private fun showErrorDialog(errorMessage: String) {
        alert {
            message = errorMessage
            title = getString(R.string.incorrect_input)
            positiveButton(R.string.ok) {
                it.dismiss()
            }
        }.show()
    }
}
