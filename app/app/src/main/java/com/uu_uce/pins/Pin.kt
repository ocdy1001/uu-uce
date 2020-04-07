package com.uu_uce.pins

import android.app.Activity
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.core.content.res.ResourcesCompat
import com.uu_uce.R
import com.uu_uce.pinDatabase.PinViewModel
import com.uu_uce.mapOverlay.coordToScreen
import com.uu_uce.misc.LogType
import com.uu_uce.misc.Logger
import com.uu_uce.services.UTMCoordinate
import com.uu_uce.shapefiles.p2
import com.uu_uce.shapefiles.p2Zero
import kotlin.math.roundToInt

enum class PinType {
    TEXT,
    VIDEO,
    IMAGE,
    MCQUIZ
}

class Pin(
    val id : Int,
    private var coordinate      : UTMCoordinate,
    /*private var difficulty      : Int,
    private var type            : PinType,*/
    private var title           : String,
    private var content         : PinContent,
    private var image           : Drawable,
    private var status          : Int,              //-1 : recalculating, 0 : locked, 1 : unlocked, 2 : completed
    private var predecessorIds  : List<Int>,
    private var followIds       : List<Int>,
    private val viewModel       : PinViewModel
) {
    init {
        predecessorIds.map { I ->
            if (I == id) error("Pin can not be own predecessor")
        }
    }

    private val pinWidth = 60 // TODO: set this in settings somewhere

    // Calculate pin height to maintain aspect ratio
    private val pinHeight =
        pinWidth * (image.intrinsicHeight.toFloat() / image.intrinsicWidth.toFloat())


    // Initialize variables used in checking for clicks
    var inScreen: Boolean = true
    var boundingBox: Pair<p2, p2> = Pair(p2Zero, p2Zero)

    var popupWindow: PopupWindow? = null

    // Quiz
    private var answered : Array<Boolean>   = Array(content.contentBlocks.count()){true}
    private var totalReward     = 0
    private var questionRewards : Array<Int> = Array(content.contentBlocks.count()){0}


    fun draw(viewport: Pair<p2, p2>, width : Int, height : Int, view: View, canvas: Canvas) {
        val screenLocation: Pair<Float, Float> =
            coordToScreen(coordinate, viewport, view.width, view.height)

        if(screenLocation.first.isNaN() || screenLocation.second.isNaN())
            return //TODO: Should not be called with NaN

        // Calculate pin bounds on canvas
        val minX = (screenLocation.first - pinWidth / 2).roundToInt()
        val minY = (screenLocation.second - pinHeight).roundToInt()
        val maxX = (screenLocation.first + pinWidth / 2).roundToInt()
        val maxY = (screenLocation.second).roundToInt()

        // Check whether pin is unlocked
        if (status == 0) return

        // Check whether pin is out of screen
        if (
            minX > width    ||
            maxX < 0        ||
            minY > height   ||
            maxY < 0
        ) {
            Logger.log(LogType.Event, "Pin", "Pin outside of viewport")
            inScreen = false
            return
        }
        inScreen = true

        // Set boundingbox for pin tapping
        boundingBox =
            Pair(p2(minX.toDouble(), minY.toDouble()), p2(maxX.toDouble(), maxY.toDouble()))

        image.setBounds(minX, minY, maxX, maxY)
        image.draw(canvas)
    }

    // Check if pin should be unlocked
    fun tryUnlock(action : (() -> Unit)){
        if(predecessorIds[0] != -1 && status < 1){
            viewModel.tryUnlock(id, predecessorIds, action)
        }
        else{
            action()
        }
    }

    fun openPinPopupWindow(parentView: View, activity : Activity, onDissmissAction: () -> Unit) {
        val layoutInflater = activity.layoutInflater

        // Build an custom view (to be inflated on top of our current view & build it's popup window)
        val customView = layoutInflater.inflate(R.layout.pin_content_view, null, false)

        popupWindow = PopupWindow(
            customView,
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )

        popupWindow?.setOnDismissListener {
            popupWindow = null

            onDissmissAction()
        }

        // Add the title for the popup window
        val windowTitle = customView.findViewById<TextView>(R.id.popup_window_title)
        windowTitle.text = title

        // Add content to popup window
        val layout: LinearLayout = customView.findViewById(R.id.scrollLayout)

        // Fill layout of popup
        resetQuestions()
        var containsQuiz = false
        for(i in 0 until content.contentBlocks.count()){
            content.contentBlocks[i].generateContent(i, layout, activity, this)
            if(content.contentBlocks[i] is MCContentBlock) containsQuiz = true
        }

        if(containsQuiz && status < 2){
            val finishButton = Button(activity)
            finishButton.text = activity.getString(R.string.finish_text)
            finishButton.setBackgroundColor(ResourcesCompat.getColor(activity.resources, R.color.colorUU, null))
            val buttonLayout = LinearLayout.LayoutParams(
                parentView.width * 2 / 3,
                TableRow.LayoutParams.WRAP_CONTENT
            )
            finishButton.layoutParams = buttonLayout
            finishButton.gravity = Gravity.CENTER_HORIZONTAL
            finishButton.setOnClickListener{
                finishQuiz(activity, parentView)
            }
            layout.addView(finishButton)
        }

        // Open popup
        popupWindow?.showAtLocation(parentView, Gravity.CENTER, 0, 0)

        // Get elements
        val btnClosePopupWindow = customView.findViewById<Button>(R.id.popup_window_close_button)
        val checkBoxCompletePin = customView.findViewById<CheckBox>(R.id.complete_box)

        // Set checkbox to correct state
        if(containsQuiz){
            checkBoxCompletePin.isChecked = (getStatus() == 2)
        }
        else{
            checkBoxCompletePin.visibility = View.INVISIBLE
        }

        // Set onClickListeners
        btnClosePopupWindow.setOnClickListener {
            popupWindow?.dismiss()
        }
    }

    private fun complete() {
        if (status < 2)
            viewModel.completePin(id, followIds)
    }

    fun addQuestion(questionId : Int, reward: Int){
        answered[questionId] = false
        totalReward += reward
    }

    fun answerQuestion(questionId : Int, reward : Int){
        questionRewards[questionId] = reward
        answered[questionId] = true
    }

    private fun resetQuestions(){
        questionRewards.map{0}
        totalReward = 0
        answered.map{true}
    }

    private fun finishQuiz(activity : Activity, parentView: View){
        if(answered.all{b -> b}){
            // All questions answered
            var reward = questionRewards.sum()
            popupWindow?.dismiss()

            var sufficient = false
            if(reward >= 0.55 * totalReward){
                sufficient = true
                complete()
            }

            //Open popup
            val layoutInflater = activity.layoutInflater

            // Build an custom view (to be inflated on top of our current view & build it's popup window)
            val customView = layoutInflater.inflate(R.layout.quiz_complete_popup, null, false)

            popupWindow = PopupWindow(
                customView,
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )

            popupWindow?.setOnDismissListener {
                popupWindow = null
            }

            // Open popup
            popupWindow?.showAtLocation(parentView, Gravity.CENTER, 0, 0)

            // Get elements
            val georgeReaction      = customView.findViewById<ImageView>(R.id.george_reaction)
            val quizResultText      = customView.findViewById<TextView>(R.id.quiz_result_text)
            val completeText        = customView.findViewById<TextView>(R.id.complete_text)
            val btnClosePopupWindow = customView.findViewById<Button>(R.id.close_button)
            val btnOpenQuiz         = customView.findViewById<Button>(R.id.reopen_button)

            // Set content based on result
            if(sufficient){
                georgeReaction.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.happy_george, null))
                quizResultText.text = activity.getString(R.string.quiz_success_head)
                completeText.text   = activity.getString(R.string.quiz_success_body, title, reward, totalReward)
                btnOpenQuiz.text = activity.getString(R.string.reopen_button_success)
            }
            else{
                georgeReaction.setImageDrawable(ResourcesCompat.getDrawable(activity.resources, R.drawable.crying_george, null))
                quizResultText.text = activity.getString(R.string.quiz_fail_head)
                completeText.text   = activity.getString(R.string.quiz_fail_body)
                btnOpenQuiz.text = activity.getString(R.string.reopen_button_fail)
            }

            // Set buttons
            btnClosePopupWindow.setOnClickListener {
                popupWindow?.dismiss()
            }

            btnOpenQuiz.setOnClickListener {
                popupWindow?.dismiss()
                openPinPopupWindow(parentView, activity){}
            }
        }
        else{
            // Questions left unanswered
            Toast.makeText(activity, "Some questions still lack answers", Toast.LENGTH_SHORT).show()
        }
    }

    fun getTitle(): String {
        return title
    }

    fun getContent(): PinContent {
        return content
    }

    fun setStatus(newStatus: Int) {
        status = newStatus
    }

    fun getStatus(): Int {
        return status
    }
}

