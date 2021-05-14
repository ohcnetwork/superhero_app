package kerala.superhero.app.login

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import kerala.superhero.app.R
import kerala.superhero.app.data.ResultWrapper
import kerala.superhero.app.home.HomeActivity
import kerala.superhero.app.network.login
import kerala.superhero.app.prefs
import kerala.superhero.app.signup.SignUpActivity
import kerala.superhero.app.utils.handleGenericError
import kerala.superhero.app.utils.handleNetworkError
import kerala.superhero.app.utils.isAValidIndianPhoneNumber
import kerala.superhero.app.views.ProgressDialog
import kotlinx.android.synthetic.main.activity_login.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert

class LoginActivity : AppCompatActivity(), Login {

    private var progressDialog: ProgressDialog? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        progressDialog = ProgressDialog(this)

        loginButton.setOnClickListener {
            attemptLogin()
        }

        loginButtonImage.setOnClickListener {
            attemptLogin()
        }

        signUpLink.setOnClickListener {
            Intent(this, SignUpActivity::class.java).apply {
                putExtra("phone", intent.extras?.getString("phone", ""))
            }.let(::startActivity)
            finishAffinity()
        }

        forgotPasswordLink.setOnClickListener {
            Intent(this, ResetPasswordActivity::class.java).apply {
                putExtra("phone", intent.extras?.getString("phone", ""))
            }.let(::startActivity)
            finishAffinity()
        }

        if (intent.extras?.getString("phone", "").isNullOrEmpty()) {
            resetPasswordPhoneNumberEditText.isEnabled = true
        } else {
            resetPasswordPhoneNumberEditText.isEnabled = false
            resetPasswordPhoneNumberEditText.setText(intent.extras?.getString("phone", ""))
        }

    }

    override fun onDestroy() {
        progressDialog?.dismiss()
        super.onDestroy()
    }

    private fun attemptLogin() {
        if (validate()) {
            showProgress()
            invokeLoginAPI()
        }
    }

    private fun showProgress() {
        progressDialog?.show()
    }

    override fun invokeLoginAPI() {
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) {
                    login(
                        resetPasswordPhoneNumberEditText.text.toString().trim(),
                        resetPasswordEditText.text.toString()
                    )
                }
                ) {
                is ResultWrapper.Success -> navigateToHome(response.value.data.access_token)
                is ResultWrapper.GenericError -> handleGenericError(response, true)
                is ResultWrapper.NetworkError -> handleNetworkError()
            }
            hideProgress()
        }
    }

    private fun hideProgress() {
        runOnUiThread {
            progressDialog?.hide()
        }
    }

    override fun validatePhone(): Boolean {
        val isValid = resetPasswordPhoneNumberEditText.text.isAValidIndianPhoneNumber()
        if (!isValid) {
            showErrorDialog(getString(R.string.incorrect_phone_number))
        }
        return isValid
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

    override fun validatePassword(): Boolean {
        val isValid = resetPasswordEditText.text.isNotEmpty()
        if (!isValid) {
            showErrorDialog(getString(R.string.please_enter_your_password))
        }
        return isValid
    }

    override fun navigateToHome(accessToken: String) {
        prefs.accessToken = accessToken
        hideProgress()
        Intent(this, HomeActivity::class.java).let(::startActivity)
        finish()
    }

    override fun validate(): Boolean {
        return validatePhone() && validatePassword()
    }
}
