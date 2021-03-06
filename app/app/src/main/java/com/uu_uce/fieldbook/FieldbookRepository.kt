package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import com.uu_uce.database.FieldbookDao

/**
 * A communication layer between the ViewModel and the database
 *
 * @property[fieldbookDao] the data access object for the pin table
 * @constructor creates the FieldbookRepository
 */
class FieldbookRepository(private val fieldbookDao: FieldbookDao) {

    val allEntries: LiveData<List<FieldbookEntry>> = fieldbookDao.getAll()

    suspend fun getContent(entryId: Int, action: ((FieldbookEntry) -> Unit)) {
        action(fieldbookDao.getContent(entryId))
    }

    suspend fun insert(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.insert(fieldbookEntry)
    }

    suspend fun delete(fieldbookEntry: FieldbookEntry) {
        fieldbookDao.delete(fieldbookEntry)
    }

    suspend fun deleteAll(){
        fieldbookDao.deleteAll()
    }

    suspend fun search(searchText : String, action : ((List<FieldbookEntry>?) -> Unit)){
        if(searchText.count() > 0){
            action(fieldbookDao.search("%$searchText%"))
        }
        else{
            action(allEntries.value)
        }
    }

    suspend fun update(title: String, content: String, entryId: Int) {
        fieldbookDao.update(title, content, entryId)
    }

    suspend fun getPins(pinIds : List<String>, action: (List<FieldbookEntry>) -> Unit){
        action(fieldbookDao.getPins(pinIds))
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

