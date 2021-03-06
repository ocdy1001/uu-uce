package com.uu_uce.allpins

import android.app.Activity
import android.content.SharedPreferences
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.os.Build
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.core.content.res.ResourcesCompat
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import androidx.preference.PreferenceManager
import androidx.recyclerview.widget.RecyclerView
import com.mikhaellopez.circleview.CircleView
import com.uu_uce.R
import com.uu_uce.misc.Logger

/**
 * The adapter manages the RecyclerView
 *
 * @property[activity] the associated activity
 * @constructor makes an PinListAdapter object
 */
class PinListAdapter internal constructor(
    private val activity: Activity
)
    : RecyclerView.Adapter<PinListAdapter.PinViewHolder>()
{
    private val resource = activity.resources
    private val inflater: LayoutInflater = LayoutInflater.from(activity)
    private var pinDataList = emptyList<PinData>()
    private val pinViewModel: PinViewModel = ViewModelProvider(activity as ViewModelStoreOwner).get(PinViewModel::class.java)
    private lateinit var sharedPref : SharedPreferences

    var view: View? = null

    /**
     * Represents one item of the RecyclerView
     *
     * @property[itemView] the surrounding/entire View of one RecyclerView item
     * @constructor makes an PinViewHolder object
     */
    inner class PinViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView){
        val parentView : View = itemView
        val fullView : LinearLayout = itemView.findViewById(R.id.recyclerview_item)
        val pinTitle: TextView = itemView.findViewById(R.id.allpins_recyclerview_item_title)
        val pinCoord: TextView = itemView.findViewById(R.id.pin_coordinates)
        val pinType: ImageView = itemView.findViewById(R.id.type_image)
        val pinDiffC: CircleView = itemView.findViewById(R.id.diff)
        val pinStatus: ImageView = itemView.findViewById(R.id.checkBox)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PinViewHolder {
        val itemView = inflater.inflate(R.layout.allpins_recyclerview_item, parent, false)
        sharedPref = PreferenceManager.getDefaultSharedPreferences(activity)
        return PinViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: PinViewHolder, position: Int) {
        val current = pinDataList[position]
        holder.pinTitle.text = current.title
        holder.pinCoord.text = current.location

        // Set completed marker
        if(current.status == 2){
            holder.pinStatus.visibility = View.VISIBLE
        }
        else{
            holder.pinStatus.visibility = View.GONE
        }

        holder.fullView.setOnClickListener{
            val pinConverter = PinConversion(activity)
            val pin = pinConverter.pinDataToPin(
                current,
                pinViewModel
            )
            pin.content.parent = pin
            pin.openContent(view ?: holder.parentView, activity)
        }

        when(current.difficulty){
            1       -> holder.pinDiffC.circleColor = Color.parseColor("#00B222")
            2       -> holder.pinDiffC.circleColor = Color.parseColor("#FF862F")
            3       -> holder.pinDiffC.circleColor = Color.parseColor("#EC1A3D")
            else    -> holder.pinDiffC.circleColor = Color.parseColor("#686868")
        }

        val drawable = when(current.type){
            "TEXT"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_text, null) ?: error ("Image not found")
            "IMAGE"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_image, null) ?: error ("Image not found")
            "VIDEO"     -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_video, null) ?: error ("Image not found")
            "MCQUIZ"    -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quiz, null) ?: error ("Image not found")
            "TASK"      -> ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null) ?: error ("Image not found")
            else        -> {
                Logger.error("PinlistAdapter", "Unknown type")
                ResourcesCompat.getDrawable(resource, R.drawable.ic_symbol_quest, null) ?: error ("Image not found")
            }
        }

        val color =
            if(sharedPref.getBoolean("com.uu_uce.DARKMODE", false))
                ResourcesCompat.getColor(activity.resources, R.color.BestWhite, null)
            else
                ResourcesCompat.getColor(activity.resources, R.color.TextDarkGrey, null)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            drawable.colorFilter = BlendModeColorFilter(color, BlendMode.SRC_ATOP)
        } else {
            // Older versions will use depricated function
            @Suppress("DEPRECATION")
            drawable.setColorFilter(color, PorterDuff.Mode.SRC_ATOP)
        }

        holder.pinType.setImageDrawable(drawable)
    }

    /**
     * Update pins in memory from database.
     * @param[newPinData] list of new PinData which should be loaded in memory.
     * @param[viewModel] the viewModel from which the database can be accessed.
     */
    internal fun setPins(newPinData: List<PinData>, viewModel: PinViewModel) {
        val tempPins: MutableList<PinData> = mutableListOf()
        // Update pins from new data
        for (newPin in newPinData) {
            if (newPin.status > 0) {
                // Pin is not unlocked yet
                tempPins.add(newPin)
            } else if (newPin.status == -1) {
                // Pin needs recalculation
                val predecessorIds = newPin.predecessorIds.split(',')
                if (predecessorIds[0] != "") {
                    viewModel.tryUnlock(newPin.pinId, predecessorIds) {}
                }
            }
        }
        pinDataList = tempPins
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int {
        return pinDataList.size
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

