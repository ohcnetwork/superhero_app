package kerala.superhero.app.onboarding

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import androidx.fragment.app.FragmentTransaction
import kerala.superhero.app.R

class OnBoardingFragmentIntro : Fragment() {

    private lateinit var continueButton: Button

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val v: View = inflater.inflate(R.layout.fragment_on_boarding_intro, container, false)
        continueButton = v.findViewById(R.id.on_boarding_continue_button)

        continueButton.setOnClickListener {
            requireActivity().supportFragmentManager.beginTransaction()
                .replace(R.id.on_boarding_fragments_container, OnBoardingFragmentVerifyPhone())
                .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                .commit()
        }
        return v
    }

}