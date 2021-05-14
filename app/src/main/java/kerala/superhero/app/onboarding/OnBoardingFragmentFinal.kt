package kerala.superhero.app.onboarding

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import kerala.superhero.app.R
import kerala.superhero.app.signup.SignUpActivity
import kerala.superhero.app.utils.checkPermissionsAndDo

class OnBoardingFragmentFinal : Fragment() {

    private lateinit var agreeButton: Button
    private val debugTag = "OnBoardingDebug"

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.fragment_on_boarding_final, container, false)
        agreeButton = v.findViewById(R.id.agree_button)
        agreeButton.setOnClickListener {
            requireActivity().checkPermissionsAndDo(
                listOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                getString(R.string.permission_required)
            ) {
                continueToSignUp()
            }

        }
        return v
    }

    private fun continueToSignUp() {
        Log.d(debugTag, " ${OnBoardingFragmentVerifyPhone.phoneNumber}")

        Intent(requireActivity(), SignUpActivity::class.java).apply {
            putExtra("phone", OnBoardingFragmentVerifyPhone.phoneNumber)
            this.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }.let(::startActivity)
    }

}