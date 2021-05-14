package kerala.superhero.app.onboarding

import android.os.Bundle
import android.os.Handler
import androidx.appcompat.app.AppCompatActivity
import kerala.superhero.app.R

class OnBoardingActivity : AppCompatActivity() {


    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_on_boarding)

        supportFragmentManager.beginTransaction()
            .add(R.id.on_boarding_fragments_container, OnBoardingFragmentIntro())
            .commit()

    }


    override fun onBackPressed() {
        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }
        this.doubleBackToExitPressedOnce = true


        Handler().postDelayed(Runnable { doubleBackToExitPressedOnce = false }, 2000)
    }
}
