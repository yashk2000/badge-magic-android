package com.nilhcem.blenamebadge.ui.main

import com.nilhcem.blenamebadge.data.fragments.ConfigInfo

interface MainSavedNavigator {
    fun setupRecycler()

    fun showLoadAlert(item: ConfigInfo)

    fun setPreview(badgeJSON: String)

    fun setPreviewNull()
}
