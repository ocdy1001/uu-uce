package com.uu_uce

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.location.Location
import android.net.Uri
import android.os.Bundle
import android.os.Environment
import android.provider.MediaStore
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.PopupWindow
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import androidx.lifecycle.ViewModelProvider
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.uu_uce.fieldbook.FieldbookAdapter
import com.uu_uce.fieldbook.FieldbookEntry
import com.uu_uce.fieldbook.FieldbookViewModel
import com.uu_uce.pins.BlockTag
import com.uu_uce.services.LocationServices
import com.uu_uce.services.checkPermissions
import com.uu_uce.services.getPermissions
import com.uu_uce.ui.createTopbar
import java.io.File
import java.io.FileOutputStream
import java.text.DateFormat
import java.text.SimpleDateFormat
import java.util.*
import java.util.stream.Stream


class FieldBook : AppCompatActivity() {

    enum class DateTimeFormat{
        FILE_PATH,
        FIELDBOOK_ENTRY
    }

    var permissionsNeeded = listOf(
        Manifest.permission.READ_EXTERNAL_STORAGE,
        Manifest.permission.WRITE_EXTERNAL_STORAGE,
        Manifest.permission.CAMERA
    )

    lateinit var image: ImageView
    lateinit var text: EditText

    lateinit var imageUri: Uri
    lateinit var bitmap: Bitmap

    lateinit var currentPhotoPath: String

    lateinit var fieldbookViewModel: FieldbookViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_field_book)

        val recyclerView = findViewById<RecyclerView>(R.id.fieldbook_recyclerview)
        val parent = findViewById<View>(R.id.fieldbook_layout)
        val addButton = findViewById<FloatingActionButton>(R.id.fieldbook_fab)
        createTopbar(this, "my fieldbook")

        val fieldbookAdapter = FieldbookAdapter(this)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = fieldbookAdapter

        fieldbookViewModel = ViewModelProvider(this).get(FieldbookViewModel::class.java)
        fieldbookViewModel.allFieldbookEntries.observe(this, androidx.lifecycle.Observer {
            fieldbookAdapter.setFieldbook(it)
        })

        addButton.setOnClickListener{
            openFieldbookAdderPopup(parent)
        }
    }

    private fun openFieldbookAdderPopup(parent: View) {
        val customView = layoutInflater.inflate(R.layout.add_fieldbook_popup, null, false)
        val popupWindow = PopupWindow(customView, ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)

        popupWindow.showAtLocation(parent, Gravity.CENTER, 0, 0)

        // makes sure the keyboard appears whenever we want to add text
        popupWindow.isFocusable = true
        popupWindow.update()

        checkPermissions(this,permissionsNeeded + LocationServices.permissionsNeeded).let {
            if (it.count()!=0)
                getPermissions(this, this,it)
        }

        text = customView.findViewById(R.id.addText)

        image = customView.findViewById(R.id.addImage)

        image.setOnClickListener {
            selectImage(this)
        }

        val closePopup = customView.findViewById<Button>(R.id.close_fieldbook_popup)
        closePopup.setOnClickListener{
            val sdf = DateFormat.getDateTimeInstance()

            saveFieldbookEntry(
                text.text.toString(),
                bitmap,
                getCurrentDateTime(DateTimeFormat.FIELDBOOK_ENTRY),
                LocationServices.lastKnownLocation)
            popupWindow.dismiss()
        }
    }

    private fun selectImage(context: Context) {
        val options = arrayOf("Take Photo", "Choose from gallery", " Cancel")
        val dialog = AlertDialog.Builder(context)
        dialog.setTitle("Upload an image")

        dialog.setItems(options) { dialogInterface, which ->

            when (which) {
                0 -> {
                    val takePictureIntent = Intent(MediaStore.ACTION_IMAGE_CAPTURE)
                    /*
                    val photoFile: File? = try {
                        createImageFile()
                    } catch (ex: IOException) {
                        null
                    }
                    if (photoFile != null) {
                        val photoUri: Uri = FileProvider.getUriForFile(
                            this,
                            "com.uu-uce.fileprovider",
                            photoFile
                        )
                        intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri)
                    }
                     */
                    startActivityForResult(takePictureIntent, 0)
                }
                1 -> startActivityForResult(Intent(Intent.ACTION_PICK,
                    MediaStore.Images.Media.EXTERNAL_CONTENT_URI), 1)
                2 -> dialogInterface.dismiss()
            }
        }
        dialog.show()
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?
    ) {
        super.onActivityResult(requestCode, resultCode, data)
        if (resultCode == Activity.RESULT_OK && data != null) {

            //TODO: this is just a thumbnail... get full size picture
            when (requestCode) {
                0 -> {
                    bitmap = data.extras?.get("data") as Bitmap
                    image.setImageBitmap(bitmap) }
                1 -> {
                    imageUri = data.data!!
                    image.setImageURI(imageUri) }
            }
        }
    }

    private fun createImageFile(): File {
        // Create an image file name
        val timeStamp: String = getCurrentDateTime(DateTimeFormat.FILE_PATH)
        val storageDir: File? = getExternalFilesDir(Environment.DIRECTORY_PICTURES)
        return File.createTempFile(
            "IMG_${timeStamp}_", /* prefix */
            ".jpg", /* suffix */
            storageDir /* directory */
        ).also {
            // Save a file: path for use with ACTION_VIEW intents
            currentPhotoPath = it.absolutePath
        }
    }

    private fun getCurrentDateTime(dtf: DateTimeFormat): String {
        val pattern: String = when(dtf) {
            DateTimeFormat.FILE_PATH -> "yyyMMdd_HHmmss"
            DateTimeFormat.FIELDBOOK_ENTRY -> "dd-MM-yyyy HH:mm"
        }

        return SimpleDateFormat(
            pattern,
            Locale("nl_NL")).format(Date()
        )
    }

    private fun saveFieldbookEntry(
        text: String,
        image: Bitmap,
        currentDate: String,
        location: Location
    ) {
        val content = listOf(
            Pair(
                BlockTag.TEXT,
                text
            ),
            Pair(
                BlockTag.IMAGE,
                saveBitmapToLocation(image)
            )
        )

        val entry = FieldbookEntry( //TODO: use UTM location
            location.toString(),
            currentDate,
            buildJSONContent(content).also{ jsonString ->
                // added for debugging purposes
                val root = "data/data/com.uu_uce/files/fieldbook"
                val myDir: File = File("$root/Content").also{
                    it.mkdirs()
                }
                val fileName = "TestContent.txt"
                val file = File(myDir,fileName)
                file.writeText(jsonString)
            }
        ).also{
            fieldbookViewModel.insert(it)
        }
    }

    private fun saveBitmapToLocation(image: Bitmap): String {
        val root = "data/data/com.uu_uce/files/fieldbook"
        val myDir: File = File("$root/Pictures").also{
            it.mkdirs()
        }
        val fileName = "IMG_${getCurrentDateTime(DateTimeFormat.FILE_PATH)}.png"
        val file = File(myDir,fileName)
        FileOutputStream(file).also{
            bitmap.compress(Bitmap.CompressFormat.PNG,100,it)
        }.apply{
            flush()
            close()
        }
        return file.toUri().toString()
    }

    private fun buildJSONContent(contentList: List<Pair<BlockTag,String>>): String {
        return  "[" +
                    "{" +
                        "\"tag\":\"${contentList.first().first}\"," +
                        "\"text\":\"${contentList.first().second}\"" +
                    "}," +
                    "{" +
                        "\"tag\":\"${contentList.last().first}\"," +
                        "\"file_name\":\"${contentList.last().second}\"" +
                    "}" +
                "]"
    }
}