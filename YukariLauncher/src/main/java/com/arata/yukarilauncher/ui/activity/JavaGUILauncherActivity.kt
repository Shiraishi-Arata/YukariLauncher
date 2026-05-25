@file:Suppress("DEPRECATION")

package com.arata.yukarilauncher.ui.activity

import android.annotation.SuppressLint
import android.content.ClipboardManager
import android.graphics.drawable.Animatable
import android.net.Uri
import android.os.Bundle
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.View
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import com.getkeepsafe.taptargetview.TapTargetView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.Tools
import com.arata.yukarilauncher.databinding.ActivityJavaGuiLauncherBinding
import com.arata.yukarilauncher.event.value.JvmExitEvent
import com.arata.yukarilauncher.feature.awt.AWTInputBridge
import com.arata.yukarilauncher.feature.awt.AWTInputEvent
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.log.Logger
import com.arata.yukarilauncher.launch.LaunchArgs
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.ui.subassembly.customcontrols.keyboard.AwtCharSender
import com.arata.yukarilauncher.ui.subassembly.view.FloatingLoggerWindow
import com.arata.yukarilauncher.ui.view.AWTCanvasView
import com.arata.yukarilauncher.ui.view.SingleTapConfirm
import com.arata.yukarilauncher.utils.MathUtils
import com.arata.yukarilauncher.utils.NewbieGuideUtils
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.image.ImageUtils
import com.arata.yukarilauncher.utils.mouse.CursorDrawableUtils
import com.arata.yukarilauncher.utils.path.LibPath
import com.arata.yukarilauncher.utils.path.PathManager
import com.arata.yukarilauncher.utils.runtime.JREUtils
import com.arata.yukarilauncher.utils.runtime.MultiRTUtils
import com.arata.yukarilauncher.utils.runtime.Runtime
import org.apache.commons.io.IOUtils
import org.greenrobot.eventbus.Subscribe
import org.lwjgl.glfw.CallbackBridge
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.ByteBuffer
import java.util.zip.ZipFile

class JavaGUILauncherActivity : BaseActivity(), View.OnTouchListener {

    companion object {
        const val EXTRAS_JRE_NAME = "jre_name"
        const val SUBSCRIBE_JVM_EXIT_EVENT = "subscribe_jvm_exit_event"
        const val FORCE_SHOW_LOG = "force_show_log"

        @JvmStatic
        fun classVersionToJavaVersion(majorVersion: Int): Int {
            return if (majorVersion < 46) 2 else majorVersion - 44
        }
    }

    private lateinit var binding: ActivityJavaGuiLauncherBinding
    private var mGestureDetector: GestureDetector? = null
    private var mIsVirtualMouseEnabled = false
    private var mSubscribeJvmExitEvent = false
    private var floatingLogger: FloatingLoggerWindow? = null
    private var mMouseHotspotX = 0f
    private var mMouseHotspotY = 0f
    private var prevX = 0f
    private var prevY = 0f

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityJavaGuiLauncherBinding.inflate(layoutInflater)
        setContentView(binding.root)

        floatingLogger = FloatingLoggerWindow(this)
        floatingLogger?.hide()
        binding.launcherLoggerView.visibility = View.GONE

        try {
            val latestLogFile = File(PathManager.DIR_GAME_HOME, "latestlog.txt")
            if (!latestLogFile.exists() && !latestLogFile.createNewFile())
                throw IOException("Failed to create a new log file")
            Logger.begin(latestLogFile.absolutePath)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }

        Logger.setLogListener { text ->
            runOnUiThread {
                floatingLogger?.appendLog("$text\n")
            }
        }

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        MainActivity.GLOBAL_CLIPBOARD = getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        binding.awtTouchChar.setCharacterSender(AwtCharSender())

        mGestureDetector = GestureDetector(this, SingleTapConfirm())
        binding.mainTouchpad.isFocusable = false
        binding.mainTouchpad.visibility = View.GONE

        binding.installmodMousePri.setOnTouchListener(this)
        binding.installmodMouseSec.setOnTouchListener(this)
        binding.installmodWindowMoveup.setOnTouchListener(this)
        binding.installmodWindowMovedown.setOnTouchListener(this)
        binding.installmodWindowMoveleft.setOnTouchListener(this)
        binding.installmodWindowMoveright.setOnTouchListener(this)

        binding.mousePointer.setImageDrawable(YLTools.customMouse(this))
        binding.mousePointer.drawable?.let { drawable ->
            drawable.setVisible(true, true)
            if (drawable is Animatable) drawable.start()
        }

        binding.mousePointer.post {
            val params = binding.mousePointer.layoutParams
            val drawable = binding.mousePointer.drawable ?: return@post
            val mousescale = ImageUtils.resizeWithRatio(
                drawable.intrinsicWidth, drawable.intrinsicHeight,
                AllSettings.mouseScale.getValue()
            )
            params.width = (mousescale.width * 0.5).toInt()
            params.height = (mousescale.height * 0.5).toInt()
            val hotspot = CursorDrawableUtils.getScaledHotspot(drawable, params.width, params.height)
            mMouseHotspotX = hotspot[0].toFloat()
            mMouseHotspotY = hotspot[1].toFloat()
        }

        binding.mainTouchpad.setOnTouchListener { _, event ->
            val action = event.actionMasked
            val x = event.x
            val y = event.y
            var mouseX = binding.mousePointer.x + mMouseHotspotX
            var mouseY = binding.mousePointer.y + mMouseHotspotY

            if (mGestureDetector?.onTouchEvent(event) == true) {
                sendScaledMousePosition(mouseX, mouseY)
                AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
            } else {
                if (action == MotionEvent.ACTION_MOVE) {
                    mouseX = maxOf(0f, minOf(CallbackBridge.physicalWidth.toFloat(), mouseX + x - prevX))
                    mouseY = maxOf(0f, minOf(CallbackBridge.physicalHeight.toFloat(), mouseY + y - prevY))
                    placeMouseAt(mouseX, mouseY)
                    sendScaledMousePosition(mouseX, mouseY)
                }
            }

            prevY = y
            prevX = x
            true
        }.also {
            prevX = 0f
            prevY = 0f
        }

        binding.textureView.setOnTouchListener { _, event ->
            val x = event.x
            val y = event.y
            if (mGestureDetector?.onTouchEvent(event) == true) {
                sendScaledMousePosition(x + binding.textureView.x, y)
                AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK)
                return@setOnTouchListener true
            }

            when (event.actionMasked) {
                MotionEvent.ACTION_MOVE -> sendScaledMousePosition(x + binding.textureView.x, y)
            }
            true
        }

        try {
            placeMouseAt(CallbackBridge.physicalWidth / 2f, CallbackBridge.physicalHeight / 2f)
            val extras = intent.extras ?: run { finish(); return }
            mSubscribeJvmExitEvent = extras.getBoolean(SUBSCRIBE_JVM_EXIT_EVENT, false)
            if (extras.getBoolean(FORCE_SHOW_LOG, false)) {
                floatingLogger?.show()
                showLogFloodWarning()
            }

            val javaArgs = extras.getString("javaArgs")
            val resourceUri = extras.getParcelable<Uri>("modUri")
            val jreName = extras.getString(EXTRAS_JRE_NAME, null)
            if (extras.getBoolean("openLogOutput", false)) openLogOutput(null)
            if (javaArgs != null) {
                startModInstaller(null, javaArgs, jreName)
            } else if (resourceUri != null) {
                val dialog = YLTools.showTaskRunningDialog(this, getString(R.string.multirt_progress_caching))
                Task.runTask {
                    startModInstallerWithUri(resourceUri, jreName)
                    null
                }.ended { dialog.dismiss() }.execute()
            }
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }

        onBackPressedDispatcher.addCallback(object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                if (floatingLogger != null && floatingLogger!!.isVisible) {
                    floatingLogger!!.hide()
                    return
                }
                forceClose()
            }
        })
    }

    override fun onDestroy() {
        super.onDestroy()
        Logger.setLogListener(null)
        floatingLogger?.hide()
        floatingLogger = null
    }

    @Subscribe
    fun event(event: JvmExitEvent) {
        if (mSubscribeJvmExitEvent) {
            YLTools.killProcess()
        }
    }

    private fun showLogFloodWarning() {
        if (NewbieGuideUtils.showOnlyOne("LogFloodWarning")) return
        TapTargetView.showFor(
            this,
            NewbieGuideUtils.getSimpleTarget(
                this, binding.launcherLoggerView.binding.clearLog,
                getString(R.string.version_install_log_flood_warning)
            )
        )
    }

    private fun startModInstallerWithUri(uri: Uri, jreName: String?) {
        try {
            val cacheFile = File(cacheDir, "mod-installer-temp")
            val contentStream = contentResolver.openInputStream(uri)
                ?: throw IOException("Failed to open content stream")
            FileOutputStream(cacheFile).use { fos -> IOUtils.copy(contentStream, fos) }
            contentStream.close()
            startModInstaller(cacheFile, null, jreName)
        } catch (e: IOException) {
            Tools.showError(this, e, true)
        }
    }

    fun selectRuntime(modFile: File): Runtime? {
        val javaVersion = getJavaVersion(modFile)
        if (javaVersion == -1) {
            finalErrorDialog(getString(R.string.execute_jar_failed_to_read_file))
            return null
        }
        val nearestRuntime = MultiRTUtils.getNearestJreName(javaVersion)
        if (nearestRuntime == null) {
            finalErrorDialog(getString(R.string.multirt_nocompatiblert, javaVersion))
            return null
        }
        val selectedRuntime = MultiRTUtils.forceReread(nearestRuntime)
        val selectedJavaVersion = maxOf(javaVersion, selectedRuntime.javaVersion)
        if (selectedJavaVersion > 17) {
            finalErrorDialog(getString(R.string.execute_jar_incompatible_runtime, selectedJavaVersion))
            return null
        }
        return selectedRuntime
    }

    private fun findModPath(argList: List<String>): File? {
        val argsSize = argList.size
        for (i in 0 until argsSize) {
            if (argList[i] != "-jar") continue
            val pathIndex = i + 1
            if (pathIndex >= argsSize) return null
            return File(argList[pathIndex])
        }
        return null
    }

    private fun splitPreservingQuotes(str: String): List<String> {
        val result = mutableListOf<String>()
        val currentPart = StringBuilder()
        var inQuotes = false

        for (i in str.indices) {
            val c = str[i]
            if (c == '"' && (i == 0 || str[i - 1] != '\\')) {
                inQuotes = !inQuotes
            } else if (c.isWhitespace() && !inQuotes) {
                if (currentPart.isNotEmpty()) {
                    result.add(currentPart.toString())
                    currentPart.clear()
                }
            } else {
                currentPart.append(c)
            }
        }

        if (currentPart.isNotEmpty()) {
            result.add(currentPart.toString())
        }

        return result
    }

    private fun startModInstaller(modFile: File?, javaArgs: String?, jreName: String?) {
        Thread({
            val argList = javaArgs?.let { splitPreservingQuotes(it) }
            var selectedMod = modFile
            if (selectedMod == null && argList != null) {
                selectedMod = findModPath(argList)
            }
            val selectedRuntime: Runtime = when {
                jreName != null -> MultiRTUtils.forceReread(jreName)
                selectedMod == null -> MultiRTUtils.forceReread(AllSettings.defaultRuntime.getValue())
                else -> selectRuntime(selectedMod) ?: return@Thread
            }
            launchJavaRuntime(selectedRuntime, modFile, argList)
        }, "JREMainThread").start()
    }

    private fun finalErrorDialog(msg: CharSequence) {
        runOnUiThread {
            TipDialog.Builder(this)
                .setTitle(R.string.generic_error)
                .setMessage(msg.toString())
                .setWarning()
                .setCenterMessage(false)
                .setConfirmClickListener { finish() }
                .setCancelable(false)
                .showDialog()
        }
    }

    override fun onResume() {
        super.onResume()
        window.decorView.systemUiVisibility = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
    }

    @SuppressLint("NonConstantResourceId")
    override fun onTouch(v: View, e: MotionEvent): Boolean {
        val isDown = when (e.actionMasked) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> true
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> false
            else -> return false
        }

        when (v.id) {
            R.id.installmod_mouse_pri -> AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON1_DOWN_MASK, isDown)
            R.id.installmod_mouse_sec -> AWTInputBridge.sendMousePress(AWTInputEvent.BUTTON3_DOWN_MASK, isDown)
        }
        if (isDown) {
            when (v.id) {
                R.id.installmod_window_moveup -> AWTInputBridge.nativeMoveWindow(0, -10)
                R.id.installmod_window_movedown -> AWTInputBridge.nativeMoveWindow(0, 10)
                R.id.installmod_window_moveleft -> AWTInputBridge.nativeMoveWindow(-10, 0)
                R.id.installmod_window_moveright -> AWTInputBridge.nativeMoveWindow(10, 0)
            }
        }
        return true
    }

    fun placeMouseAt(x: Float, y: Float) {
        binding.mousePointer.x = x - mMouseHotspotX
        binding.mousePointer.y = y - mMouseHotspotY
    }

    fun sendScaledMousePosition(x: Float, y: Float) {
        val clampedX = androidx.core.math.MathUtils.clamp(
            x, binding.textureView.x,
            binding.textureView.x + binding.textureView.width
        )
        val clampedY = androidx.core.math.MathUtils.clamp(
            y, binding.textureView.y,
            binding.textureView.y + binding.textureView.height
        )

        AWTInputBridge.sendMousePos(
            MathUtils.map(clampedX, binding.textureView.x,
                binding.textureView.x + binding.textureView.width,
                0f, AWTCanvasView.AWT_CANVAS_WIDTH.toFloat()).toInt(),
            MathUtils.map(clampedY, binding.textureView.y,
                binding.textureView.y + binding.textureView.height,
                0f, AWTCanvasView.AWT_CANVAS_HEIGHT.toFloat()).toInt()
        )
    }

    fun forceClose(v: View) = forceClose()

    fun forceClose() = YLTools.dialogForceClose(this)

    fun openLogOutput(v: View?) = floatingLogger?.toggle()

    fun toggleVirtualMouse(v: View) {
        mIsVirtualMouseEnabled = !mIsVirtualMouseEnabled
        binding.mainTouchpad.visibility = if (mIsVirtualMouseEnabled) View.VISIBLE else View.GONE
        Toast.makeText(
            this,
            if (mIsVirtualMouseEnabled) R.string.control_mouseon else R.string.control_mouseoff,
            Toast.LENGTH_SHORT
        ).show()
    }

    fun launchJavaRuntime(runtime: Runtime, modFile: File?, javaArgs: List<String>?) {
        JREUtils.redirectAndPrintJRELog()
        try {
            val javaArgList = mutableListOf<String>()
            javaArgList.addAll(LaunchArgs.getCacioJavaArgs(runtime.javaVersion == 8))
            if (javaArgs != null) javaArgList.addAll(javaArgs)
            if (modFile != null) {
                javaArgList.add("-jar")
                javaArgList.add(modFile.absolutePath)
            }

            val disableSecurityManager = intent.getBooleanExtra("disableSecurityManager", false)

            if (AllSettings.javaSandbox.getValue() && !disableSecurityManager) {
                javaArgList.reverse()
                javaArgList.add("-Xbootclasspath/a:${LibPath.PRO_GRADE.absolutePath}")
                javaArgList.add("-Djava.security.manager=net.sourceforge.prograde.sm.ProGradeJSM")
                javaArgList.add("-Djava.security.policy=${LibPath.JAVA_SANDBOX_POLICY.absolutePath}")
                javaArgList.reverse()
            }

            Logger.appendToLog("Info: Java arguments: ${javaArgList.joinToString(", ")}")

            JREUtils.launchWithUtils(this, runtime, null, javaArgList, AllSettings.javaArgs.getValue())
        } catch (th: Throwable) {
            Tools.showError(this, th, true)
        }
    }

    fun toggleKeyboard(view: View) = binding.awtTouchChar.switchKeyboardState()

    fun performCopy(view: View) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_C)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }

    fun performPaste(view: View) {
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 1)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_V)
        AWTInputBridge.sendKey(' ', AWTInputEvent.VK_CONTROL, 0)
    }

    fun getJavaVersion(modFile: File): Int {
        try {
            ZipFile(modFile).use { zipFile ->
                val manifest = zipFile.getEntry("META-INF/MANIFEST.MF") ?: return -1
                val manifestString = Tools.read(zipFile.getInputStream(manifest))
                val mainClass = Tools.extractUntilCharacter(manifestString, "Main-Class:", '\n') ?: return -1
                val mainClassPath = mainClass.trim().replace('.', '/') + ".class"
                val mainClassFile = zipFile.getEntry(mainClassPath) ?: return -1
                val classStream = zipFile.getInputStream(mainClassFile)
                val bytesWeNeed = ByteArray(8)
                val readCount = classStream.read(bytesWeNeed)
                classStream.close()
                if (readCount < 8) return -1

                val byteBuffer = ByteBuffer.wrap(bytesWeNeed)
                if (byteBuffer.int != 0xCAFEBABE.toInt()) return -1
                val majorVersion = byteBuffer.short.toInt()
                Logging.i("JavaGUILauncher", "$majorVersion,${byteBuffer.short}")
                return classVersionToJavaVersion(majorVersion)
            }
        } catch (e: Exception) {
            Logging.e("JavaVersion", "Exception thrown", e)
            return -1
        }
    }
}
