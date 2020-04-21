package com.uu_uce.instrumentedTests

import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.RecyclerView
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition
import androidx.test.espresso.matcher.RootMatchers.isPlatformPopup
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.rule.ActivityTestRule
import com.uu_uce.AllPins
import com.uu_uce.R
import com.uu_uce.allpins.PinData
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.childAtPosition
import com.uu_uce.clickChildViewWithId
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.Thread.sleep


@RunWith(AndroidJUnit4::class)
@LargeTest
class AllPinsTests {
    private lateinit var pinViewModel: PinViewModel
    private lateinit var sharedPref : SharedPreferences

    @get:Rule
    var activityRule: ActivityTestRule<AllPins>
            = ActivityTestRule(AllPins::class.java)

    @Before
    fun initDatabase(){

        // Populate test database
        pinViewModel = ViewModelProvider(activityRule.activity).get(PinViewModel::class.java)
        val pinList: MutableList<PinData> = mutableListOf()
        pinList.add(
            PinData(
                0,
                "31N46777336N3149680E",
                1,
                "TEXT",
                "A",
                "[{\"tag\":\"TEXT\", \"text\":\"test\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                1,
                "31N46718336N3133680E",
                2,
                "IMAGE",
                "B",
                "[{\"tag\":\"IMAGE\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/images/test.png\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                2,
                "31N46710000N3130000E",
                3,
                "VIDEO",
                "C",
                "[{\"tag\":\"VIDEO\", \"file_path\":\"file:///data/data/com.uu_uce/files/pin_content/videos/zoo.mp4\", \"thumbnail\":\"file:///data/data/com.uu_uce/files/pin_content/videos/thumbnails/zoothumbnail.png\", \"title\":\"zoo video\"}]",
                1,
                "-1",
                "-1"
            )
        )
        pinList.add(
            PinData(
                3,
                "31N46715335N3134680E",
                3,
                "MCQUIZ",
                "D",
                "[{\"tag\":\"TEXT\", \"text\":\"Press right or also right\"}, {\"tag\":\"MCQUIZ\", \"mc_correct_option\" : \"Right\", \"mc_incorrect_option\" : \"Wrong\" , \"mc_correct_option\" : \"Also right\", \"mc_incorrect_option\" : \"Also wrong\", \"reward\" : 50}]",
                1,
                "-1",
                "-1"
            )
        )

        // Set sorting by type
        sharedPref = activityRule.activity.getPreferences(Context.MODE_PRIVATE)
        with(sharedPref.edit()) {
            putInt("selectedOption", 4)
            apply()
        }

        pinViewModel.setPins(pinList)
    }

    @Test
    fun loadAllPins(){
        // Check if allpins successfully loaded
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndClosePinClose(){
        // Open first pin
        onView(withId(R.id.allpins_recyclerview)).perform(
                actionOnItemAtPosition<RecyclerView.ViewHolder>(
                    0, clickChildViewWithId(R.id.open_button)
                )
            )

        // Check if pin successfully opened
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Close pin content with close button
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if pin closed successfully
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndClosePinBack(){
        // Open first pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                0, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.scrollLayout))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Close pin content with back button
        pressBack()

        // Check if pin closed successfully
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndCloseVideoCross(){
        // Open video pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                3, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_player))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using close button
        onView(withId(R.id.close_video_player))
            .inRoot(not(isPlatformPopup()))
            .perform(click())

        // Check if the player was closed successfully
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun openAndCloseVideoBack(){
        // Open video pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                3, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Open video player
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if video player opened successfully
        onView(withId(R.id.video_player))
            .inRoot(not(isPlatformPopup()))
            .check(matches(isDisplayed()))

        // Close video player using back button
        pressBack()

        // Check if the player was closed successfully
        onView(withId(R.id.start_video_button))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceSuccess(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select correct answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Right")
            )
        )
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Click finish button
        onView(withId(R.id.finish_quiz_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check to see if popup was correct
        onView(withId(R.id.quiz_result_text))
            .inRoot(isPlatformPopup())
            .check(matches(withText(R.string.quiz_success_head)))

        // Reopen popup
        onView(withId(R.id.reopen_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if pin is completed
        onView(withId(R.id.complete_box))
            .inRoot(isPlatformPopup())
            .check(matches(isChecked()))
    }

    @Test
    fun multipleChoiceFail(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select wrong answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Wrong")
            )
        )
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Click finish button
        onView(withId(R.id.finish_quiz_button))
            .inRoot(isPlatformPopup())
            .perform(scrollTo(), click())

        // Check to see if popup was correct
        onView(withId(R.id.quiz_result_text))
            .inRoot(isPlatformPopup())
            .check(matches(withText(R.string.quiz_fail_head)))

        // Reopen popup
        onView(withId(R.id.reopen_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check if pin is completed
        onView(withId(R.id.complete_box))
            .inRoot(isPlatformPopup())
            .check(matches(not(isChecked())))
    }

    @Test
    fun multipleChoiceCloseWarning(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Select an answer
        onView(
            allOf(
                isDescendantOfA(withId(R.id.multiple_choice_table)),
                withText("Also right")
            ))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that warning pops up when progress is made
        onView(withText("Closing Pin"))
            .check(matches(isDisplayed()))

        // Stay in pin
        onView(withText("No"))
            .perform(click())

        //Check to see that pin didn't close
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Close pin
        onView(withText("Yes"))
            .perform(click())

        // Check if pin closed
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun multipleChoiceCloseNoWarning(){
        // Open multiple choice pin
        onView(withId(R.id.allpins_recyclerview)).perform(
            actionOnItemAtPosition<RecyclerView.ViewHolder>(
                1, clickChildViewWithId(R.id.open_button)
            )
        )

        // Check if pin successfully opened
        onView(withId(R.id.multiple_choice_table))
            .inRoot(isPlatformPopup())
            .check(matches(isDisplayed()))

        onView(withId(R.id.allpins_recyclerview))
            .check(matches(not(hasFocus())))

        // Attempt to close pin
        onView(withId(R.id.popup_window_close_button))
            .inRoot(isPlatformPopup())
            .perform(click())

        // Check to see that warning doesn't pops up when no progress is made
        onView(withText("Closing Pin"))
            .check(doesNotExist())

        sleep(100)

        // Check if pin closed
        onView(withId(R.id.allpins_recyclerview))
            .check(matches(isDisplayed()))
    }

    @Test
    fun pinSorting(){
        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by title
        onView(withText("Title a-z"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("A")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by title reversed
        onView(withText("Title z-a"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("D")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by difficulty
        onView(withText("Difficulty easy-hard"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("A")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by difficulty reversed
        onView(withText("Difficulty hard-easy"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("D")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by type
        onView(withText("Type a-z"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("B")))

        // Open sorting popup
        onView(withId(R.id.fab))
            .perform(click())

        // Sort by type reversed
        onView(withText("Type z-a"))
            .perform(click())

        // Check if sorting was successful
        onView(
            allOf(
                isDescendantOfA(childAtPosition(withId(R.id.allpins_recyclerview), 0)),
                withId(R.id.allpins_recyclerview_item_title)
            )
        ).check(matches(withText("C")))
    }
}