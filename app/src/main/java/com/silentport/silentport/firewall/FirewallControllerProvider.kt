package com.silentport.silentport.firewall

import android.content.Context
import java.util.concurrent.atomic.AtomicReference

object FirewallControllerProvider {

    fun get(context: Context): FirewallController {
        val app = context.applicationContext as com.silentport.silentport.SilentPortApp
        return app.container.firewallController
    }
}

