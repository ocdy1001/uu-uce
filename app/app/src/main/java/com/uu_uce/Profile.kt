package com.uu_uce

import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.preference.PreferenceManager
import com.uu_uce.profile.ProfileAchievements
import com.uu_uce.profile.ProfileBadges
import com.uu_uce.profile.ProfileStatistics
import com.uu_uce.ui.createTopbar
import kotlinx.android.synthetic.main.activity_profile.*

/**
 * An activity in which the user can see their personal information as well as unlockables.
 * @property[selectedOptionText] the text view corresponding to the currently selected fragment.
 * @property[selectedOptionBar] the bar under the text corresponding to the currently selected fragment.
 * @property[sharedPref] the shared preferences where the settings are stored.
 * @constructor a Profile Activity.
 */
class Profile : AppCompatActivity() {
    private lateinit var selectedOptionText : TextView
    private lateinit var selectedOptionBar : View
    private lateinit var sharedPref : SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = PreferenceManager.getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if(darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        } else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_profile)

        createTopbar(this, "Profile")

        user_name_text.text = sharedPref.getString("com.uu_uce.USERNAME", getString(R.string.profile_user_name))
        organization_name.text = sharedPref.getString("com.uu_uce.ORGNAME", getString(R.string.profile_organization_name))

        selectedOptionText = badges_button_text
        selectedOptionBar = badges_button_bar
        openFragment(ProfileBadges())

        score_text.text = sharedPref.getInt("com.uu_uce.USER_POINTS", 0).toString()

        badges_button.setOnClickListener{
            if(selectedOptionBar != badges_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                badges_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                badges_button_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.HighBlue))

                selectedOptionText = badges_button_text
                selectedOptionBar  = badges_button_bar

                openFragment(ProfileBadges())
            }
        }

        achievements_button.setOnClickListener{
            if(selectedOptionBar != achievements_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                achievements_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                achievements_button_bar.setBackgroundColor(ContextCompat.getColor(this, R.color.HighBlue))

                selectedOptionText = achievements_button_text
                selectedOptionBar  = achievements_button_bar

                openFragment(ProfileAchievements())
            }
        }

        statistics_button.setOnClickListener{
            if(selectedOptionBar != statistics_button_bar){
                selectedOptionText.setTextColor(ContextCompat.getColor(this, R.color.TextGrey))
                selectedOptionBar.setBackgroundColor(ContextCompat.getColor(this, R.color.TextGrey))

                statistics_button_text.setTextColor(ContextCompat.getColor(this, R.color.HighBlue))
                statistics_button_bar.setBackgroundColor(
                    ContextCompat.getColor(
                        this,
                        R.color.HighBlue
                    )
                )

                selectedOptionText = statistics_button_text
                selectedOptionBar = statistics_button_bar

                openFragment(ProfileStatistics())
            }
        }
    }

    /**
     * Opens a fragment in the fragment holder.
     * @param[fragment] the fragment to be opened.
     */
    private fun openFragment(fragment: Fragment) {
        val transaction = supportFragmentManager.beginTransaction()
        transaction.replace(R.id.profile_container, fragment)
        transaction.addToBackStack(null)
        transaction.commit()
    }

    override fun onBackPressed() {
        finish()
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

