package com.uu_uce.fieldbook

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.viewModelScope
import com.uu_uce.database.UceRoomDatabase
import kotlinx.coroutines.launch

/**
 * Manages and stores data from the database
 * and makes sure it survives through the entire LifeCycle
 *
 * @param[application] the context for the ViewModel
 * @constructor makes a ViewModel
 */
class FieldbookViewModel(application: Application): AndroidViewModel(application) {

    private val fieldbookRepository: FieldbookRepository

    val allFieldbookEntries: LiveData<List<FieldbookEntry>>

    init {
        val fieldbookDao = UceRoomDatabase.getDatabase(application, viewModelScope).fieldbookDao()
        fieldbookRepository = FieldbookRepository(fieldbookDao)
        allFieldbookEntries = fieldbookRepository.allEntries
    }

    fun getContent (entryId: Int, action : ((FieldbookEntry) -> Unit)) = viewModelScope.launch {
        fieldbookRepository.getContent(entryId, action)
    }

    fun insert (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.insert(fieldbookEntry)
    }

    fun delete (fieldbookEntry: FieldbookEntry) = viewModelScope.launch {
        fieldbookRepository.delete(fieldbookEntry)
    }

    fun deleteAll() = viewModelScope.launch {
        fieldbookRepository.deleteAll()
    }

    fun search(searchText : String, action : ((List<FieldbookEntry>?) -> Unit)) = viewModelScope.launch {
        fieldbookRepository.search(searchText, action)
    }

    fun update(title: String, content: String, entryId: Int) = viewModelScope.launch {
        fieldbookRepository.update(title, content, entryId)
    }

    fun getPins(pinIds : List<String>, action : ((List<FieldbookEntry>) -> (Unit))) = viewModelScope.launch {
        fieldbookRepository.getPins(pinIds, action)
    }
}

/* This program has been developed by students from the bachelor Computer
# Science at Utrecht University within the Software Project course. ©️ Copyright
# Utrecht University (Department of Information and Computing Sciences)*/

