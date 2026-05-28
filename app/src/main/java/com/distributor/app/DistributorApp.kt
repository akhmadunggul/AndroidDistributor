package com.distributor.app

import android.app.Application
import android.content.Context
import com.distributor.app.utils.LocaleHelper

class DistributorApp : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}
