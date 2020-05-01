package com.uu_uce.profile

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.uu_uce.R

class ProfileAchievements : Fragment() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_profile_achievements, container, false)
    }

    companion object {
        @JvmStatic
        fun newInstance() =
            ProfileAchievements().apply {
                arguments = Bundle().apply {

                }
            }
    }
}
