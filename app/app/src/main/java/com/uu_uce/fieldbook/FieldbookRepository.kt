package com.uu_uce.fieldbook

import androidx.lifecycle.LiveData
import com.uu_uce.database.FieldbookDao

class FieldbookRepository(private val fieldbookDao: FieldbookDao) {

    val allEntries: LiveData<List<FieldbookEntry>> = fieldbookDao.getAll()

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
}