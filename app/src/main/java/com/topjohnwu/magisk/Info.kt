package com.topjohnwu.magisk

import com.github.pwittchen.reactivenetwork.library.rx2.ReactiveNetwork
import com.topjohnwu.magisk.extensions.get
import com.topjohnwu.magisk.extensions.subscribeK
import com.topjohnwu.magisk.model.entity.UpdateInfo
import com.topjohnwu.magisk.utils.CachedValue
import com.topjohnwu.magisk.utils.KObservableField
import com.topjohnwu.superuser.Shell
import com.topjohnwu.superuser.ShellUtils

val isRunningAsStub get() = Info.stub != null

object Info {

    val envRef = CachedValue { loadState() }

    val env by envRef              // Local
    var remote = UpdateInfo()      // Remote
    var stub: DynAPK.Data? = null  // Stub

    var keepVerity = false
    var keepEnc = false
    var recovery = false

    val isConnected by lazy {
        KObservableField(false).also { field ->
            ReactiveNetwork.observeNetworkConnectivity(get())
                .subscribeK {
                    field.value = it.available()
                }
        }
    }

    private fun loadState() = runCatching {
        val str = ShellUtils.fastCmd("magisk -v").split(":".toRegex())[0]
        val code = ShellUtils.fastCmd("magisk -V").toInt()
        val hide = Shell.su("magiskhide --status").exec().isSuccess
        Env(code, str, hide)
    }.getOrElse { Env() }

    class Env(
        code: Int = -1,
        val magiskVersionString: String = "",
        hide: Boolean = false
    ) {
        val magiskHide get() = Config.magiskHide
        val magiskVersionCode = when (code) {
            in Int.MIN_VALUE..Const.Version.MIN_VERCODE -> -1
            else -> code
        }
        val unsupported = code > 0 && code < Const.Version.MIN_VERCODE

        init {
            Config.magiskHide = hide
        }
    }
}
