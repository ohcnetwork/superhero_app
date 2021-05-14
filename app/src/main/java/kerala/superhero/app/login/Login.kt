package kerala.superhero.app.login

interface Login {
    fun invokeLoginAPI()
    fun validatePhone(): Boolean
    fun validatePassword(): Boolean
    fun navigateToHome(accessToken: String)
    fun validate(): Boolean
}