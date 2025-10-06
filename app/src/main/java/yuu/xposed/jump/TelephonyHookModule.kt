package yuu.xposed.jump

import android.R.id.message
import android.content.ContentValues
import android.content.Context
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Binder
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.hjq.device.compat.DeviceOs
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XposedBridge
import de.robv.android.xposed.XposedHelpers
import de.robv.android.xposed.callbacks.XC_LoadPackage
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TelephonyProviderHook: IXposedHookLoadPackage {

    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam) {
        // 1. 只 hook com.android.providers.telephony 这个进程
        if (lpparam.packageName != "com.android.providers.telephony" || !isTargetRom()) return
        val providerClasses = listOf(
            "com.android.providers.telephony.PushMessageProvider", "com.android.providers.telephony.PushShopProvider", "com.android.providers.telephony.ServiceNumberProvider"
        )

        providerClasses.forEach { className ->
            try {
                XposedHelpers.findAndHookMethod(
                    className, lpparam.classLoader, "update", Uri::class.java, ContentValues::class.java, String::class.java, Array<String>::class.java, object: XC_MethodHook() {
                        override fun beforeHookedMethod(param: MethodHookParam) {
                            try {
                                val uri = param.args[0] as Uri
                                val values = param.args[1] as? ContentValues
                                val provider = param.thisObject
                                val context = XposedHelpers.callMethod(provider, "getContext") as Context

                                // 2. 判断调用方 UID 是否是系统应用
                                if (isSystemAppByUid(context)) {
                                    XposedBridge.log("[TelephonyProviderHook] 系统应用调用, 放行: $uri")
                                    return
                                }

                                val callingUid = Binder.getCallingUid()
                                val pm = context.packageManager
                                val pkgs = pm.getPackagesForUid(callingUid)
                                val pkgName = pkgs?.joinToString(",") ?: "unknown"
                                XposedBridge.log("[TelephonyProviderHook] 非系统调用者: $pkgName, uri=$uri, values=$values")
                                // 3. 阻断
                                param.result = 0
                                // 4. 弹toast提示
                                notifyUser(context, pkgName)
                                // 5. 保存日志
                                logAttempt(context, pkgName, uri.toString(), values.toString())
                            } catch (t: Throwable) {
                                XposedBridge.log("[TelephonyProviderHook] Hook error: $t")
                            }
                        }
                    })
                XposedBridge.log("[TelephonyProviderHook] Hooked $className.update")
            } catch (t: Throwable) {
                XposedBridge.log("[TelephonyProviderHook] Hook $className 失败: $t")
            }
        }
    }

    private fun isTargetRom(): Boolean {
        return DeviceOs.isColorOs() || DeviceOs.isRealmeUi() || DeviceOs.isOxygenOs()
    }

    private fun isSystemAppByUid(context: Context): Boolean {
        return try {
            val uid = Binder.getCallingUid()
            val pm = context.packageManager
            val packages = pm.getPackagesForUid(uid) ?: return false
            for (pkg in packages) {
                val appInfo = pm.getApplicationInfo(pkg, 0)
                if ((appInfo.flags and ApplicationInfo.FLAG_SYSTEM) != 0 || (appInfo.flags and ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) {
                    return true
                }
            }
            false
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyProviderHook] isSystemAppByUid error: $e")
            false
        }
    }

    private var lastNotifyTime = 0L
    private val RATE_LIMIT_MS = 5000L

    private fun notifyUser(context: Context, packageName: String) {
        val now = System.currentTimeMillis()
        if (now - lastNotifyTime < RATE_LIMIT_MS) return
        lastNotifyTime = now
        val message = "$packageName 利用短信漏洞"
        Handler(Looper.getMainLooper()).post {
            try {
                Toast.makeText(context, message, Toast.LENGTH_LONG).show()
            } catch (t: Throwable) {
                XposedBridge.log("[TelephonyProviderHook] notifyUser error: $t")
            }
        }
    }

    private fun logAttempt(context: Context, packageName: String, uri: String, values: String) {
        try {
            val externalDir = context.getExternalFilesDir(null)
            // 路径:  /storage/emulated/0/Android/data/com.android.providers.telephony/files/yuu.log
            val logFile = File(externalDir, "yuu.log")
            val formatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            val time = formatter.format(Date())
            FileWriter(logFile, true).use { writer ->
                writer.append(time).append("  ").append(packageName).append(", uri:").append(uri).append(", values:").append(values).append('\n')
            }
            XposedBridge.log("[TelephonyProviderHook] logAttempt file=${logFile.absolutePath}")
        } catch (e: Throwable) {
            XposedBridge.log("[TelephonyProviderHook] logAttempt error: $e")
        }
    }
}
