package com.nilhcem.blenamebadge.ui.main

import android.content.Intent
import android.net.Uri
import com.nilhcem.blenamebadge.data.DrawableInfo
import com.nilhcem.blenamebadge.data.device.model.DataToSend

interface MainTextDrawableNavigator {
    fun selectText()

    fun selectDrawable(selectedItem: DrawableInfo?)

    fun convertTextToDeviceDataModel(): DataToSend

    fun convertBitmapToDeviceDataModel(): DataToSend

    fun setupRecycler()

    fun removeListeners()

    fun getCurrentDate(): String

    fun showSaveFileDialog()

    fun configToJSON(): String

    fun showFileOverrideDialog(fileName: String, jsonString: String)

    fun saveFile(fileName: String, jsonString: String)
}
