package kerala.superhero.app.signup

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.text.InputFilter
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import kerala.superhero.app.R
import kerala.superhero.app.data.*
import kerala.superhero.app.home.HomeActivity
import kerala.superhero.app.login.LoginActivity
import kerala.superhero.app.network.*
import kerala.superhero.app.onboarding.OnBoardingActivity
import kerala.superhero.app.prefs
import kerala.superhero.app.utils.*
import kerala.superhero.app.views.ProgressDialog
import kotlinx.android.synthetic.main.activity_sign_up.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.jetbrains.anko.alert


class SignUpActivity : AppCompatActivity(), SignUp {
    private var progressDialog: ProgressDialog? = null

    private var assetGroupsDialog: SearchableListDialog? = null
    private var assetCategoryDialog: SearchableListDialog? = null
    private var districtsDialog: SearchableListDialog? = null
    private var panchayathsDialog: SearchableListDialog? = null
    private var wardsDialog: SearchableListDialog? = null

    private var selectedAssetGroup: AssetGroup? = null
    private var selectedAssetCategory: AssetCategory? = null


    private var statesDialog: SearchableListDialog? = null

    private var stateId = "empty"

    override fun onDestroy() {
        progressDialog?.dismiss()
        super.onDestroy()
    }

    override fun onRestart() {
        progressDialog?.show()
        fetchAndPopulateLists {
            progressDialog?.hide()
        }
        super.onRestart()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)

        registrationEditText.filters = registrationEditText.filters + InputFilter.AllCaps()

        progressDialog = ProgressDialog(this)

        /**
         * Change in Code - Nitheesh.
         * edited 26 april 21
         */

        if (!prefs.accessToken.isNullOrEmpty()) {
            navigateToHome(prefs.accessToken!!)
        } else if (intent.getStringExtra("phone").isNullOrEmpty()) {
            Intent(this, OnBoardingActivity::class.java).let(::startActivity)
            finish()
        } else
            fetchAndPopulateLists {

            }

        signUpButton.setOnClickListener {
            attemptSignUp()
        }

        signupButtonImage.setOnClickListener {
            attemptSignUp()
        }

        groupTextView.setOnClickListener {
            assetGroupsDialog?.show()
        }

        categoryTextView.setOnClickListener {
            assetCategoryDialog?.show()
        }

        districtTextView.setOnClickListener {
            districtsDialog?.show()
        }

        panchayathTextView.setOnClickListener {
            panchayathsDialog?.show()
        }

        wardTextView.setOnClickListener {
            wardsDialog?.show()
        }

        stateTextView.setOnClickListener {
            statesDialog?.show()
        }

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
            resetPasswordPhoneNumberEditText.setText(intent.extras?.getString("phone", ""))
        }


    }

    private fun fetchAndPopulateLists(callback: () -> Unit) {
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) { getAllAssetGroups() }
            ) {
                is ResultWrapper.Success -> {
                    runOnUiThread {
                        // Setting asset groups and category
                        setUpAssetGroupsAndCategory(response.value.data)
                    }

                    /**
                     * Modified to show only State - Nitheesh Ag
                     * 24/08/2020
                     */
//                    fetchDistricts()
                    fetchStates()
                    runOnUiThread {
                        callback()
                    }
                }
                is ResultWrapper.GenericError -> {
                    handleGenericError(response, false) {
                        runOnUiThread {
                            fetchAndPopulateLists(callback)
                        }
                    }
                }
                is ResultWrapper.NetworkError -> {
                    handleNetworkError {
                        runOnUiThread {
                            fetchAndPopulateLists(callback)
                        }
                    }
                }
            }
        }
    }

    private suspend fun fetchStates() {
        when (
            val response =
                withContext(Dispatchers.Default) { getAllStates() }
        ) {
            is ResultWrapper.Success -> {
                runOnUiThread {
                    setUpAssetStates(response.value.data)
                }
            }
            is ResultWrapper.GenericError -> handleGenericError(response, false) {
                GlobalScope.launch { fetchStates() }
            }

            is ResultWrapper.NetworkError -> handleNetworkError() {
                GlobalScope.launch { fetchStates() }
            }
        }

    }

    private fun setUpAssetStates(states: List<States>) {
        statesDialog = SearchableListDialog(this@SignUpActivity, states.map {
            SearchableListDialog.SearchableListItem(it.id, it.state)
        }) {
            stateTextView.text = it.name
            stateId = it.id.toString()
            if (stateId == "ec42d1ff-db14-4299-8db7-a4acd9977783") {
                districtTextView.isVisible = true
                panchayathTextView.isVisible = true
                wardTextView.isVisible = true
                fetchDistricts {
                    progressDialog?.hide()
                }
            } else {
                districtTextView.isVisible = false
                panchayathTextView.isVisible = false
                wardTextView.isVisible = false
                districtTextView.text = ""
                panchayathTextView.text = ""
                wardTextView.text = ""
            }
        }
    }

    private fun fetchDistricts(callback: () -> Unit) {
        progressDialog?.show()
        GlobalScope.launch {
            when (
                val districtResponse =
                    withContext(Dispatchers.Default) { getAllDistricts() }
            ) {
                is ResultWrapper.Success -> {
                    runOnUiThread {
                        // Setting districts
                        setUpAssetDistricts(districtResponse.value.data)
                        callback()
                    }
                }
                is ResultWrapper.GenericError -> handleGenericError(districtResponse, false) {
                    runOnUiThread {
                        fetchDistricts {
                            progressDialog?.hide()
                        }
                    }
                }
                is ResultWrapper.NetworkError -> handleNetworkError() {
                    runOnUiThread {
                        fetchDistricts {
                            progressDialog?.hide()
                        }
                    }
                }
            }
        }
    }

    private fun setUpAssetDistricts(districts: List<District>) {
        districtsDialog = SearchableListDialog(this@SignUpActivity, districts.map {
            SearchableListDialog.SearchableListItem(it.code, it.name)
        }) {
            districtTextView.text = it.name
            panchayathTextView.isEnabled = true
            panchayathTextView.text = ""
            setUpPanchayaths(districts.find { item -> item.name == it.name })
        }
    }

    private fun setUpPanchayaths(selectedDistrict: District?) {
        progressDialog?.show()
        GlobalScope.launch {
            when (val response = withContext(Dispatchers.Default) {
                getAllPanchayathsInDistrict(
                    selectedDistrict?.name ?: ""
                )
            }) {
                is ResultWrapper.Success -> runOnUiThread {
                    val panchayathsList = response.value.data
                    panchayathsDialog =
                        SearchableListDialog(this@SignUpActivity, panchayathsList.map {
                            SearchableListDialog.SearchableListItem(it.name, it.name)
                        }) {
                            panchayathTextView.text = it.name
                            wardTextView.isEnabled = true
                            wardTextView.text = ""
                            setUpWards(panchayathsList.find { item -> item.name == it.name })
                        }
                }
                is ResultWrapper.GenericError -> runOnUiThread {
                    districtTextView.text = ""
                    panchayathTextView.isEnabled = false
                    panchayathTextView.text = ""
                    handleGenericError(response)
                }
                is ResultWrapper.NetworkError -> runOnUiThread {
                    districtTextView.text = ""
                    panchayathTextView.isEnabled = false
                    panchayathTextView.text = ""
                    handleNetworkError()
                }
            }
            runOnUiThread {
                progressDialog?.dismiss()
            }
        }
    }


    private fun setUpWards(selectedPanchayath: Panchayath?) {
        selectedPanchayath?.run {
            val wards = (1..ward_count).map {
                SearchableListDialog.SearchableListItem(it, it.toString())
            }
            wardsDialog = SearchableListDialog(this@SignUpActivity, wards.map {
                SearchableListDialog.SearchableListItem(it.name, it.name)
            }) {
                wardTextView.text = it.name
            }
        }
    }


    private fun setUpAssetGroupsAndCategory(assetGroups: List<AssetGroup>) {
        assetGroupsDialog = SearchableListDialog(this@SignUpActivity, assetGroups.map {
            SearchableListDialog.SearchableListItem(it.id, it.title)
        }) {
            groupTextView.text = it.name
            categoryTextView.isEnabled = true
            categoryTextView.text = ""
            selectedAssetGroup = assetGroups.find { item -> item.id == it.id }
            selectedAssetCategory = null
            val categoryList = selectedAssetGroup?.categoryList ?: emptyList()
            assetCategoryDialog =
                SearchableListDialog(this@SignUpActivity, categoryList.map { assetGroup ->
                    SearchableListDialog.SearchableListItem(assetGroup.id, assetGroup.title)
                }) { selectedCategory ->
                    categoryTextView.text = selectedCategory.name
                    selectedAssetCategory =
                        categoryList.find { item -> item.id == selectedCategory.id }
                }
        }
    }

    private fun attemptSignUp() {
        if (validate()) {
            showProgress()
            invokeSignUpAPI()
        }
    }

    private fun showProgress() {
        progressDialog?.show()
    }

    private fun hideProgress() {
        runOnUiThread {
            progressDialog?.hide()
        }
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

    /**
     * Modified to add state - Nitheesh Ag
     * 12/10/2020
     */
    override fun invokeSignUpAPI() {
        GlobalScope.launch {
            when (
                val response = withContext(Dispatchers.Default) {
                    signUp(
                        nameEditText.text.toString().trim(),
                        resetPasswordPhoneNumberEditText.text.toString().trim(),
                        emailEditText.text.toString().trim(),
                        resetPasswordEditText.text.toString(),
                        registrationEditText.text.toString().trim(),
                        selectedAssetCategory?.id ?: 1,
                        selectedAssetGroup?.id ?: 1,
                        stateId,
                        districtTextView.text.toString().trim(),
                        panchayathTextView.text.toString().trim(),
                        wardTextView.text.toString().trim(),
                        secondaryPhoneNumberEditText.text.toString().trim()
                    )
                }
            ) {
                is ResultWrapper.Success -> navigateToHome(response.value.data.access_token)
                is ResultWrapper.GenericError -> handleGenericError(response)
                is ResultWrapper.NetworkError -> handleNetworkError()
            }
            hideProgress()
        }
    }

//    override fun invokeSignUpAPI() {
//        GlobalScope.launch {
//            when (
//                val response = withContext(Dispatchers.Default) {
//                    signUp(
//                        nameEditText.text.toString().trim(),
//                        resetPasswordPhoneNumberEditText.text.toString().trim(),
//                        emailEditText.text.toString().trim(),
//                        resetPasswordEditText.text.toString(),
//                        registrationEditText.text.toString().trim(),
//                        selectedAssetCategory?.id ?: 1,
//                        selectedAssetGroup?.id ?: 1,
//                        stateId,
//                        secondaryPhoneNumberEditText.text.toString().trim()
//                    )
//                }
//                ) {
//                is ResultWrapper.Success -> navigateToHome(response.value.data.access_token)
//                is ResultWrapper.GenericError -> handleGenericError(response)
//                is ResultWrapper.NetworkError -> handleNetworkError()
//            }
//            hideProgress()
//        }
//    }

    override fun validatePhone(): Boolean {
        val isValid = resetPasswordPhoneNumberEditText.text.isAValidIndianPhoneNumber()
        if (!isValid) {
            showErrorDialog(getString(R.string.incorrect_phone_number))
        }
        return isValid
    }

    override fun validateEmail(): Boolean {
        val isValid =
            android.util.Patterns.EMAIL_ADDRESS.matcher(emailEditText.text.trim()).matches()
        if (!isValid) {
            showErrorDialog(getString(R.string.incorrect_email_address))
        }
        return isValid
    }

    override fun validatePassword(): Boolean {
        val isCharacterValid = resetPasswordEditText.text.toString().length > 7
        if (!isCharacterValid) {
            showErrorDialog(getString(R.string.password_length))
        }
        return isCharacterValid
    }

    override fun validateAllSelections(): Boolean {
        if (selectedAssetGroup == null) {
            showErrorDialog(getString(R.string.please_select_asset_group))
            return false
        }
        if (selectedAssetCategory == null) {
            showErrorDialog(getString(R.string.please_select_asset_category))
            return false
        }
        if (stateTextView.text.isNullOrEmpty() || stateId == "empty") {
            showErrorDialog(getString(R.string.please_select_state))
            return false
        }
        if (stateId == "ec42d1ff-db14-4299-8db7-a4acd9977783") {
            if (districtTextView.text.isNullOrEmpty()) {
                showErrorDialog(getString(R.string.please_select_district))
                return false
            }
            if (panchayathTextView.text.isNullOrEmpty()) {
                showErrorDialog(getString(R.string.please_select_panchayath))
                return false
            }
            if (wardTextView.text.isNullOrEmpty()) {
                showErrorDialog(getString(R.string.please_select_ward))
                return false
            }

        }
        return true
    }

    override fun validateRegistration(): Boolean {
        val isRegistrationValid = registrationEditText.text.toString().all {
            it.isLetterOrDigit()
        }
        if (!isRegistrationValid) {
            showErrorDialog(getString(R.string.alphanumeric_only))
        }
        return isRegistrationValid
    }

    override fun navigateToHome(accessToken: String) {
        prefs.accessToken = accessToken
        hideProgress()
        Intent(this, HomeActivity::class.java).let(::startActivity)
        finishAffinity()
    }

    override fun validate(): Boolean {
        return validatePhone() && validateEmail() && validatePassword() && validateRegistration() && validateAllSelections()
    }
}
