package com.uu_uce.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.uu_uce.fieldbook.FieldbookDao
import com.uu_uce.fieldbook.FieldbookEntry
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.*

@Database(entities = [PinData::class, FieldbookEntry::class], version = 1, exportSchema = false)
abstract class UceRoomDatabase : RoomDatabase() {

    abstract fun pinDao() : PinDao

    abstract fun fieldbookDao() : FieldbookDao

    private class UceDatabaseCallback(private val scope: CoroutineScope) : RoomDatabase.Callback() {
        override fun onOpen(db: SupportSQLiteDatabase) {
            super.onOpen(db)
            INSTANCE?.let { database ->
                scope.launch(Dispatchers.IO) {
                    populatePinTable(database.pinDao())
                    //populateFieldbook(database.fieldbookDao())
                }
            }
        }

        suspend fun populateFieldbook(fieldbookDao: FieldbookDao) {
            val sdf = DateFormat.getDateTimeInstance()
            val currentDate = sdf.format(Date())

            val fieldbook: MutableList<FieldbookEntry> = mutableListOf(
                FieldbookEntry(
                    "31N3149680N46777336E",
                    currentDate,
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"IMAGE\",\"file_name\":\"test.png\"}]",
                    60
                ),
                FieldbookEntry(
                    "31N3133680N46718336E",
                    currentDate,
                    "[{\"tag\":\"TEXT\",\"text\":\"Dit is een faketekst. Alles wat hier staat is slechts om een indruk te geven van het grafische effect van tekst op deze plek. Wat u hier leest is een voorbeeldtekst. Deze wordt later vervangen door de uiteindelijke tekst, die nu nog niet bekend is. De faketekst is dus een tekst die eigenlijk nergens over gaat. Het grappige is, dat mensen deze toch vaak lezen. Zelfs als men weet dat het om een faketekst gaat, lezen ze toch door.\"},{\"tag\":\"VIDEO\", \"file_name\":\"zoo.mp4\", \"thumbnail\":\"zoothumbnail.png\", \"title\":\"zoo video\"}]",
                    60
                )
            )

            fieldbookDao.deleteAllFieldbookEntries()

            for (entry in fieldbook)
                fieldbookDao.insert(entry)
        }
        suspend fun populatePinTable(pinDao: PinDao) {
            pinDao.deleteAllPins()

            val pins : MutableList<PinData> = mutableListOf(
                PinData(0, "31N3149680N46777336E", 1, "TEXT" , "testPin1", "[{\"tag\":\"TEXT\", \"text\":\"test\"}]", 60) ,
                PinData(1, "31N3133680N46718336E", 2, "IMAGE", "testPin2", "[{\"tag\":\"IMAGE\", \"file_name\":\"test.png\"}]", 60),
                PinData(2, "31N3130000N46710000E", 3, "VIDEO", "testPin3", "[{\"tag\":\"VIDEO\", \"file_name\":\"zoo.mp4\", \"thumbnail\":\"zoothumbnail.png\", \"title\":\"zoo video\"}]", 60)
            )

            for (pin in pins) {
                pinDao.insert(pin)
            }
        }
    }

    companion object{
        @Volatile
        private var INSTANCE: UceRoomDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): UceRoomDatabase {
            return INSTANCE ?:
            synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    UceRoomDatabase::class.java,
                    "uce_database"
                )
                    .addCallback(UceDatabaseCallback(scope))
                    .allowMainThreadQueries()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}