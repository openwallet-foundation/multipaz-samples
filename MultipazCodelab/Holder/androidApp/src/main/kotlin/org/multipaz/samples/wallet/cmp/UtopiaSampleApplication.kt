package org.multipaz.samples.wallet.cmp

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.multipaz.samples.wallet.cmp.di.initKoin

class UtopiaSampleApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        initKoin {
            androidContext(this@UtopiaSampleApplication)
            androidLogger()
            modules()
        }
    }
}
