package sh.siava.AOSPMods

import android.app.AndroidAppHelper
import android.app.Application
import android.content.Context
import android.content.Intent
import android.content.Intent.FLAG_RECEIVER_FOREGROUND
import android.content.pm.PackageManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.TextView
import de.robv.android.xposed.IXposedHookLoadPackage
import de.robv.android.xposed.IXposedHookZygoteInit
import de.robv.android.xposed.XC_MethodHook
import de.robv.android.xposed.XSharedPreferences
import de.robv.android.xposed.XposedHelpers.callMethod
import de.robv.android.xposed.XposedHelpers.callStaticMethod
import de.robv.android.xposed.XposedHelpers.findAndHookMethod
import de.robv.android.xposed.XposedHelpers.findClass
import de.robv.android.xposed.XposedHelpers.getObjectField
import de.robv.android.xposed.callbacks.XC_LoadPackage

class QuickUnlock : IXposedHookZygoteInit, IXposedHookLoadPackage {
    private var mLockPatterUtils: Any? = null
    private var mLockCallback: Any? = null
    private var mKeyguardMonitor: Any? = null
    private val classLoader: ClassLoader? = null
    override fun initZygote(startupParam: IXposedHookZygoteInit.StartupParam?) {
        if (pref?.getBoolean("trick_quickUnlock", false) == true) {
            findAndHookMethod(
                "com.android.keyguard.KeyguardPasswordViewController",
                param.classLoader,
                "onViewAttached",
                object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        pref.reload()
                        mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils")
                        mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback")
                        mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor")
                    }
                })
            findAndHookMethod(
                "com.android.keyguard.KeyguardPinViewController",
                param.classLoader,
                "onViewAttached",
                object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        pref.reload()
                        mLockPatterUtils = getObjectField(param.thisObject, "mLockPatternUtils")
                        mLockCallback = getObjectField(param.thisObject, "mKeyguardSecurityCallback")
                        mKeyguardMonitor = getObjectField(param.thisObject, "mKeyguardUpdateMonitor")
                    }
                })
            findAndHookMethod(
                "com.android.keyguard.KeyguardPasswordView",
                param.classLoader,
                "onFinishInflate",
                object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val passwordEntry: TextView = getObjectField(param.thisObject, "mPasswordEntry") as TextView
                        passwordEntry.addTextChangedListener(object : TextWatcher() {
                            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
                            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
                            override fun afterTextChanged(s: Editable?) {
                                val entry: String = passwordEntry.text.toString()
                                val passwordLength: Int = pref.getInt("passwordLength", -1)
                                if (entry.length == passwordLength) {
                                    val userId = callMethod(mKeyguardMonitor, "getCurrentUser") as Int
                                    val credential: Class<*> =
                                        findClass("com.android.internal.widget.LockscreenCredential", classLoader)
                                    val password: Any = callStaticMethod(credential, "createPassword", entry)
                                    val valid = callMethod(
                                        mLockPatterUtils,
                                        "checkCredential", password, userId, null as Any?
                                    ) as Boolean
                                    if (valid) {
                                        callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0)
                                        callMethod(mLockCallback, "dismiss", true, userId)
                                    }
                                }
                            }
                        })
                    }
                })
            findAndHookMethod("com.android.keyguard.PasswordTextView", param.classLoader, "append",
                Char::class.javaPrimitiveType, object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val entry = getObjectField(param.thisObject, "mText") as String
                        val passwordLength: Int = pref.getInt("passwordLength", -1)
                        if (entry.length == passwordLength) {
                            val userId = callMethod(mKeyguardMonitor, "getCurrentUser") as Int
                            val credential: Class<*> =
                                findClass("com.android.internal.widget.LockscreenCredential", classLoader)
                            val pin: Any = callStaticMethod(credential, "createPin", entry)
                            val valid = callMethod(
                                mLockPatterUtils,
                                "checkCredential", pin, userId, null as Any?
                            ) as Boolean
                            if (valid) {
                                callMethod(mLockCallback, "reportUnlockAttempt", userId, true, 0)
                                callMethod(mLockCallback, "dismiss", true, userId)
                            }
                        }
                    }
                })
        } else if (param.packageName.equals("com.android.settings")) {
            findAndHookMethod("com.android.settings.password.ChooseLockGeneric\$ChooseLockGenericFragment",
                param.classLoader,
                "setUnlockMethod",
                String::class.java,
                object : XC_MethodHook() {
                    protected override fun afterHookedMethod(param: MethodHookParam) {
                        val lock = param.args[0] as String
                        val context: Context? = context
                        if (lock == "unlock_set_off" || lock == "unlock_set_none") {
                            if (context != null) {
                                val password = Intent("sh.siava.AOSPMods.SET_INTEGER")
                                password.setFlags(FLAG_RECEIVER_FOREGROUND)
                                password.setPackage("sh.siava.AOSPMods")
                                password.putExtra("preference", "passwordLength")
                                password.putExtra("value", -1)
                                context.sendBroadcast(password)
                                val unlock = Intent("sh.siava.AOSPMods.SET_BOOLEAN")
                                unlock.setFlags(FLAG_RECEIVER_FOREGROUND)
                                unlock.setPackage("sh.siava.AOSPMods")
                                unlock.putExtra("preference", "trick_quickUnlock")
                                unlock.putExtra("value", false)
                                context.sendBroadcast(unlock)
                            }
                        }
                    }
                })
            findAndHookMethod(
                "com.android.settings.password.ChooseLockPattern\$ChooseLockPatternFragment",
                param.classLoader,
                "startSaveAndFinish",
                object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam?) {
                        val context: Context? = context
                        if (context != null) {
                            val password = Intent("sh.siava.AOSPMods.SET_INTEGER")
                            password.flags = FLAG_RECEIVER_FOREGROUND
                            password.setPackage("sh.siava.AOSPMods")
                            password.putExtra("preference", "passwordLength")
                            password.putExtra("value", -1)
                            context.sendBroadcast(password)
                            val unlock = Intent("sh.siava.AOSPMods.SET_BOOLEAN")
                            unlock.flags = FLAG_RECEIVER_FOREGROUND
                            unlock.setPackage("sh.siava.AOSPMods")
                            unlock.putExtra("preference", "trick_quickUnlock")
                            unlock.putExtra("value", false)
                            context.sendBroadcast(unlock)
                        }
                    }
                })
            findAndHookMethod(
                "com.android.settings.password.ChooseLockPassword\$ChooseLockPasswordFragment",
                param.classLoader,
                "startSaveAndFinish",
                object : XC_MethodHook() {
                    protected override fun beforeHookedMethod(param: MethodHookParam) {
                        val mChosenPassword: Any = getObjectField(param.thisObject, "mChosenPassword")
                        val length = callMethod(mChosenPassword, "size") as Int
                        val context: Context? = context
                        if (context != null) {
                            val password = Intent("sh.siava.AOSPMods.SET_INTEGER")
                            password.flags = FLAG_RECEIVER_FOREGROUND
                            password.setPackage("sh.siava.AOSPMods")
                            password.putExtra("preference", "passwordLength")
                            password.putExtra("value", length)
                            context.sendBroadcast(password)
                        }
                    }
                })
        }
    }

    private val context: Context?
        private get() {
            var context: Context? = null
            val app: Application = AndroidAppHelper.currentApplication()
            try {
                context = app.createPackageContext("sh.siava.AOSPMods", 0)
            } catch (ignored: PackageManager.NameNotFoundException) {
            }
            return context
        }

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpparam: XC_LoadPackage.LoadPackageParam?) {
    }

    companion object {
        private val pref: XSharedPreferences? = null
    }
}
