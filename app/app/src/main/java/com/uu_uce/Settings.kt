package com.uu_uce

import android.app.AlertDialog
import android.content.SharedPreferences
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.ViewModelProvider
import androidx.preference.PreferenceManager.getDefaultSharedPreferences
import com.uu_uce.allpins.PinViewModel
import com.uu_uce.allpins.parsePins
import com.uu_uce.pins.PinContent
import com.uu_uce.pins.VideoContentBlock
import com.uu_uce.services.*
import com.uu_uce.ui.createTopbar
import com.uu_uce.views.pinsUpdated
import kotlinx.android.synthetic.main.activity_settings.*
import java.io.File
import kotlin.math.max
import kotlin.math.min

// Default settings
const val defaultPinSize = 60
const val defaultUnlockRange = 100
var needsRestart = false
const val mapsFolderName = "Maps"
const val contentFolderName = "PinContent"
const val legendName = "legend.png"
const val mergedPinBackground = 5
const val mergedPinIcon = "MERGEDPIN"

/**
 * An activity where the user can change the look of the app, change its workings or manage the storage used by the app.
 * @property[minPinSize] the minimum size a pin can be set to.
 * @property[maxPinSize] the maximum size a pin can be set to.
 * @property[minRange] the minimum range to which the completeDistance can be set.
 * @property[maxRange] the maximum range to which the completeDistance can be set.
 * @property[mapsDir] the name of the folder that the maps will be saved to.
 * @property[contentDir] the name of the folder that the pin content will be saved to.
 * @property[sharedPref] the shared preferences where the settings are stored.
 * @property[pinViewModel] the ViewModel throught which the pin database is accessed.
 * @constructor a Settings Activity.
 */
class Settings : AppCompatActivity() {
    // private variables
    private val minPinSize = 10
    private val maxPinSize = 200
    private val minRange = 10
    private val maxRange = 200

    private lateinit var mapsDir: String
    private lateinit var contentDir: String

    private lateinit var sharedPref: SharedPreferences
    private lateinit var pinViewModel: PinViewModel

    private var updating = false
    private var downloadingMaps = false
    private var downloadingContent = false

    override fun onCreate(savedInstanceState: Bundle?) {
        sharedPref = getDefaultSharedPreferences(this)
        val darkMode = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        // Set desired theme
        if (darkMode) setTheme(R.style.DarkTheme)

        // Set statusbar text color
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !darkMode) {
            window.decorView.systemUiVisibility =
                View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR//  set status text dark
        } else if(!darkMode){
            window.statusBarColor = Color.BLACK// set status background white
        }

        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)

        mapsDir = getExternalFilesDir(null)?.path + File.separator + mapsFolderName
        contentDir = getExternalFilesDir(null)?.path + File.separator + contentFolderName

        pinViewModel = ViewModelProvider(this).get(PinViewModel::class.java)

        createTopbar(this, "Settings")

        // PinSize
        val curSize = sharedPref.getInt("com.uu_uce.PIN_SIZE", defaultPinSize)
        pinsize_seekbar.max = maxPinSize - minPinSize
        pinsize_seekbar.progress = curSize - minPinSize
        pinsize_numberview.setText(curSize.toString())

        pinsize_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                pinsize_numberview.setText((seekBar.progress + minPinSize).toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val pinSize = seekBar.progress + minPinSize
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.PIN_SIZE", pinSize)
                    apply()
                }
            }
        })

        pinsize_numberview.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val progress = min(max(pinsize_numberview.text.toString().toInt(), minPinSize), maxPinSize)
                pinsize_seekbar.progress = progress - minPinSize
                pinsize_numberview.setText(progress.toString())
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.PIN_SIZE", progress)
                    apply()
                }
                return@OnKeyListener true
            }
            false
        })

        // Unlock range
        val curRange = sharedPref.getInt("com.uu_uce.UNLOCKRANGE", defaultUnlockRange)
        unlockrange_seekbar.max = maxRange - minRange
        unlockrange_seekbar.progress = curRange - minRange
        unlockrange_numberview.setText(curRange.toString())

        unlockrange_seekbar.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar, i: Int, b: Boolean) {
                // Display the pinSize
                unlockrange_numberview.setText((seekBar.progress + minRange).toString())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar) {
            }

            override fun onStopTrackingTouch(seekBar: SeekBar) {
                val range = seekBar.progress + minRange
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.UNLOCKRANGE", range)
                    apply()
                }
            }
        })

        unlockrange_numberview.setOnKeyListener(View.OnKeyListener { _, keyCode, event ->
            if (keyCode == KeyEvent.KEYCODE_ENTER && event.action == KeyEvent.ACTION_UP) {
                //Perform Code
                val progress = min(max(unlockrange_numberview.text.toString().toInt(), minRange), maxRange)
                unlockrange_seekbar.progress = progress - minRange
                unlockrange_numberview.setText(progress.toString())
                with(sharedPref.edit()) {
                    putInt("com.uu_uce.UNLOCKRANGE", progress)
                    apply()
                }
                return@OnKeyListener true
            }
            false
        })

        // Network downloading
        val curNetworkDownloading = sharedPref.getBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
        networkdownload_switch.isChecked = curNetworkDownloading
        networkdownload_switch.setOnClickListener {
            toggleNetworkDownloads()
        }

        // Darkmode switch
        darktheme_switch.isChecked = sharedPref.getBoolean("com.uu_uce.DARKMODE", false)
        darktheme_switch.setOnClickListener {
            toggleDarkMode()
        }

        // Download maps
        download_maps_button.setOnClickListener {
            if (downloadingMaps) return@setOnClickListener

            if (!File(getExternalFilesDir(null)?.path + File.separator + mapsFolderName).exists()) {
                downloadingMaps = true
                queryServer("map", this) { mapid -> downloadMaps(mapid) }
            } else {
                AlertDialog.Builder(this, R.style.AlertDialogStyle)
                    .setIcon(R.drawable.ic_sprite_question)
                    .setTitle(getString(R.string.settings_redownload_map_head))
                    .setMessage(getString(R.string.settings_redownload_map_body))
                    .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                        downloadingMaps = true
                        queryServer("map", this) { mapid -> downloadMaps(mapid) }
                    }
                    .setNegativeButton(getString(R.string.negative_button_text), null)
                    .show()
            }
        }

        // Maps storage
        delete_maps_button.visibility =
            if (File(mapsDir).exists()) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        maps_storage_size.text = writableSize(dirSize(File(mapsDir)))

        delete_maps_button.setOnClickListener {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle(getString(R.string.settings_delete_maps_warning_head))
                .setMessage(getString(R.string.settings_delete_maps_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    File(mapsDir).deleteRecursively()
                    needsReload.setValue(true)
                    delete_maps_button.visibility = View.INVISIBLE
                    maps_storage_size.text = writableSize(dirSize(File(mapsDir)))
                    Toast.makeText(
                        this,
                        getString(R.string.settings_map_deleted_text),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton(getString(R.string.negative_button_text), null)
                .show()
        }

        // Download pin content
        download_content_button.setOnClickListener {
            if (!downloadingContent) {
                downloadContent()
            }
        }

        // Content storage
        delete_content_button.visibility =
            if (File(contentDir).exists()) {
                View.VISIBLE
            } else {
                View.INVISIBLE
            }

        content_storage_size.text = writableSize(dirSize(File(contentDir)))

        delete_content_button.setOnClickListener {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle(getString(R.string.settings_delete_content_warning_head))
                .setMessage(getString(R.string.settings_delete_content_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    File(contentDir).deleteRecursively()
                    delete_content_button.visibility = View.INVISIBLE
                    content_storage_size.text = writableSize(dirSize(File(contentDir)))
                    Toast.makeText(
                        this,
                        getString(R.string.settings_content_deleted_text),
                        Toast.LENGTH_LONG
                    ).show()
                }
                .setNegativeButton(getString(R.string.negative_button_text), null)
                .show()
        }

        // Download pins
        download_pins_button.setOnClickListener {
            if (!updating) {
                queryServer("pin", this) { s -> updateDatabase(s) }
            }
        }
    }

    /**
     * Toggles the permission to download over mobile data.
     */
    private fun toggleNetworkDownloads() {
        if (networkdownload_switch.isChecked) {
            AlertDialog.Builder(this, R.style.AlertDialogStyle)
                .setIcon(R.drawable.ic_sprite_warning)
                .setTitle(getString(R.string.settings_mobiledata_warning_head))
                .setMessage(getString(R.string.settings_mobiledata_warning_body))
                .setPositiveButton(getString(R.string.positive_button_text)) { _, _ ->
                    with(sharedPref.edit()) {
                        putBoolean(
                            "com.uu_uce.NETWORK_DOWNLOADING",
                            true
                        )
                        apply()
                    }
                }
                .setNegativeButton(getString(R.string.negative_button_text)) { _, _ ->
                    networkdownload_switch.isChecked = false
                }
                .show()
        } else {
            with(sharedPref.edit()) {
                putBoolean("com.uu_uce.NETWORK_DOWNLOADING", false)
                apply()
            }
        }
    }

    /**
     * Toggles darkmode on or off.
     */
    private fun toggleDarkMode() {
        with(sharedPref.edit()) {
            putBoolean("com.uu_uce.DARKMODE", darktheme_switch.isChecked)
            apply()
        }

        needsRestart = !needsRestart

        // Restart activity
        val intent = intent
        finish()
        startActivity(intent)
    }

    /**
     * Downloads the maps zip and unpacks it.
     * @param[mapid] string containing the map id.
     */
    private fun downloadMaps(mapid: String) {
        if (mapid == "") {
            runOnUiThread {
                maps_downloading_progress.visibility = View.INVISIBLE
                downloadingMaps = false
                Toast.makeText(
                    this,
                    getString(R.string.settings_queryfail),
                    Toast.LENGTH_LONG
                ).show()
            }
            return
        }

        val maps = listOf(getExternalFilesDir(null)?.path + File.separator + mapid)
        runOnUiThread {
            maps_downloading_progress.visibility = View.VISIBLE
        }
        updateFiles(
            maps,
            this,
            { success ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.settings_zip_download_complete),
                            Toast.LENGTH_LONG
                        )
                            .show()
                    }
                    val result = unpackZip(maps.first()) { progress ->
                        runOnUiThread {
                            maps_downloading_progress.progress = progress
                        }
                    }
                    runOnUiThread {
                        if (result) Toast.makeText(
                            this,
                            getString(R.string.settings_zip_unpacked),
                            Toast.LENGTH_LONG
                        ).show()
                        else Toast.makeText(
                            this,
                            getString(R.string.settings_zip_not_unpacked),
                            Toast.LENGTH_LONG
                        ).show()
                        maps_downloading_progress.visibility = View.INVISIBLE
                        needsReload.setValue(true)
                        delete_maps_button.visibility = View.VISIBLE
                        maps_storage_size.text = writableSize(dirSize(File(mapsDir)))
                    }

                    downloadingMaps = false
                } else {
                    runOnUiThread {
                        maps_downloading_progress.visibility = View.INVISIBLE
                        Toast.makeText(this, getString(R.string.download_failed), Toast.LENGTH_LONG)
                            .show()
                    }
                    downloadingMaps = false
                }
            },
            { progress -> runOnUiThread { maps_downloading_progress.progress = progress } }
        )
    }

    /**
     * Downloads all media that is referenced in the database.
     */
    private fun downloadContent() {
        downloadingContent = true
        val list = mutableListOf<String>()
        val missingThumbnails = mutableListOf<String>()

        pinViewModel.getContent(list) {
            val pathList = mutableListOf<String>()

            for (data in list) {
                for (block in PinContent(data, this, false).contentBlocks) {
                    val filePaths = block.getFilePath()
                    pathList.addAll(filePaths)
                    if (block is VideoContentBlock && filePaths.count() == 1) {
                        missingThumbnails.addAll(filePaths)
                    }
                }
            }

            content_downloading_progress.visibility = View.VISIBLE

            updateFiles(
                pathList,
                this,
                { success ->
                    if (success) {
                        runOnUiThread {
                            Toast.makeText(
                                this,
                                getString(R.string.settings_download_complete),
                                Toast.LENGTH_LONG
                            ).show()
                            content_downloading_progress.visibility = View.INVISIBLE
                            content_storage_size.text = writableSize(dirSize(File(contentDir)))
                            delete_content_button.visibility = View.VISIBLE
                        }

                    } else {
                        runOnUiThread {
                            content_downloading_progress.visibility = View.INVISIBLE
                            Toast.makeText(
                                this,
                                getString(R.string.download_failed),
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                    downloadingContent = false
                },
                { progress ->
                    runOnUiThread { content_downloading_progress.progress = progress }
                }
            )
        }
    }

    /**
     * Downloads the newest database json and inserts its data into the database.
     */
    private fun updateDatabase(pinDatabaseFile: String) {
        updating = true
        if (pinDatabaseFile == "") {
            runOnUiThread {
                pins_downloading_progress.visibility = View.INVISIBLE
                Toast.makeText(
                    this,
                    getString(R.string.settings_queryfail),
                    Toast.LENGTH_LONG
                ).show()
            }
            updating = false
            return
        }

        pins_downloading_progress.visibility = View.VISIBLE
        updateFiles(
            listOf(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile),
            this,
            { success ->
                if (success) {
                    runOnUiThread {
                        Toast.makeText(
                            this,
                            getString(R.string.settings_pins_downloaded),
                            Toast.LENGTH_LONG
                        ).show()
                        pins_downloading_progress.visibility = View.INVISIBLE
                        pinViewModel.updatePins(parsePins(File(getExternalFilesDir(null)?.path + File.separator + pinDatabaseFile))) {
                            pinsUpdated.setValue(true)
                            updating = false
                        }
                    }
                } else {
                    runOnUiThread {
                        pins_downloading_progress.visibility = View.INVISIBLE
                        updating = false
                        Toast.makeText(
                            this,
                            getString(R.string.download_failed),
                            Toast.LENGTH_LONG
                        ).show()
                    }
                }
            },
            { progress ->
                runOnUiThread { pins_downloading_progress.progress = progress }
            }
        )
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

