package com.yacientv
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import android.content.Context

@CloudstreamPlugin
class YacienTVPlugin: Plugin() {
    override fun load(context: Context) {
        registerMainAPI(YacienTV())
    }
}