package kerala.superhero.app.signup

interface SignUp {
    fun invokeSignUpAPI()
    fun validatePhone(): Boolean
    fun validateEmail(): Boolean
    fun validatePassword(): Boolean
    fun validateAllSelections(): Boolean
    fun validateRegistration(): Boolean
    fun navigateToHome(accessToken: String)
    fun validate(): Boolean
}