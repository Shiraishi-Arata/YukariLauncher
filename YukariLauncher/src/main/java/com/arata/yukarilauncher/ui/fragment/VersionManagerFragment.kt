package com.arata.yukarilauncher.ui.fragment

import android.graphics.BitmapFactory
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentVersionManagerBinding
import com.arata.yukarilauncher.feature.download.platform.update.ModUpdate
import com.arata.yukarilauncher.feature.download.platform.update.ModUpdateManager
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.feature.version.NoVersionException
import com.arata.yukarilauncher.feature.version.Version
import com.arata.yukarilauncher.feature.version.VersionsManager
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.dialog.TipDialog
import com.arata.yukarilauncher.utils.YLTools
import com.arata.yukarilauncher.utils.file.FileDeletionHandler
import com.google.android.material.dialog.MaterialAlertDialogBuilder
import com.google.android.material.progressindicator.LinearProgressIndicator
import com.arata.yukarilauncher.Tools
import org.json.JSONObject
import java.io.File
import java.util.zip.ZipFile

/**
 * バージョン管理フラグメント
 */
class VersionManagerFragment : FragmentWithAnim(R.layout.fragment_version_manager), View.OnClickListener {
    companion object {
        const val TAG: String = "VersionManagerFragment"
        private const val ICON_SCAN_ENTRY_LIMIT = 256
        private const val ICON_SCAN_MAX_SIZE_BYTES = 512 * 1024L
    }

    private lateinit var binding: FragmentVersionManagerBinding

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVersionManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        val fragment = this
        binding.apply {
            shortcutsMods.setOnClickListener(fragment)
            gamePath.setOnClickListener(fragment)
            resourcePath.setOnClickListener(fragment)
            worldPath.setOnClickListener(fragment)
            shaderPath.setOnClickListener(fragment)
            screenshotPath.setOnClickListener(fragment)
            logsPath.setOnClickListener(fragment)
            crashReportPath.setOnClickListener(fragment)
            versionSettings.setOnClickListener(fragment)
            versionRename.setOnClickListener(fragment)
            versionCopy.setOnClickListener(fragment)
            versionDelete.setOnClickListener(fragment)
            checkUpdates.setOnClickListener(fragment)
        }
    }

    /**
     * 存在しない場合はディレクトリを作成する
     */
    private fun File.mustExists(): File {
        if (!exists()) mkdirs()
        return this
    }

    /**
     * ファイル管理フラグメントに遷移する
     */
    private fun swapFilesFragment(lockPath: File, listPath: File) {
        val bundle = Bundle().apply {
            putString(FilesFragment.BUNDLE_LOCK_PATH, lockPath.mustExists().absolutePath)
            putString(FilesFragment.BUNDLE_LIST_PATH, listPath.mustExists().absolutePath)
            putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
        }
        YLTools.swapFragmentWithAnim(this, FilesFragment::class.java, FilesFragment.TAG, bundle)
    }

    /**
     * バージョンからMinecraftゲームバージョンを取得する
     */
    private fun getGameVersion(version: Version): String {
        val versionInfo = version.getVersionInfo()
        if (versionInfo != null && versionInfo.minecraftVersion.isNotBlank()) {
            return versionInfo.minecraftVersion
        }
        val rawName = version.getVersionName()
        return rawName
            .replace(" Fabric", "")
            .replace(" Forge", "")
            .replace(" NeoForge", "")
            .trim()
    }

    /**
     * バージョンにインストールされているModローダーを取得する
     */
    private fun getSelectedLoader(version: Version): String? {
        val loaderName = version.getVersionInfo()
            ?.loaderInfo
            ?.firstOrNull()
            ?.name
            ?.trim()
            ?.lowercase()

        return when (loaderName) {
            "fabric" -> "fabric"
            "quilt" -> "quilt"
            "forge" -> "forge"
            "neoforge" -> "neoforge"
            else -> null
        }
    }

    /**
     * Mod JARファイルからアイコンを抽出する
     */
    private fun loadModIcon(context: android.content.Context, modFile: File?): Drawable? {
        if (modFile == null || !modFile.isFile || !modFile.name.endsWith(".jar")) {
            return ContextCompat.getDrawable(context, R.drawable.ic_file)
        }
        return try {
            ZipFile(modFile).use { zip ->
                val metadataIconPath = getIconPathFromMetadata(zip)
                if (metadataIconPath != null) {
                    val entry = zip.getEntry(metadataIconPath)
                    if (entry != null && !entry.isDirectory && entry.size <= ICON_SCAN_MAX_SIZE_BYTES) {
                        zip.getInputStream(entry).use { input ->
                            BitmapFactory.decodeStream(input)?.let { bitmap ->
                                return BitmapDrawable(context.resources, bitmap)
                            }
                        }
                    }
                }

                val entry = zip.entries().asSequence()
                    .take(ICON_SCAN_ENTRY_LIMIT)
                    .filter { !it.isDirectory && it.name.endsWith(".png", ignoreCase = true) }
                    .filter { it.size in 1..ICON_SCAN_MAX_SIZE_BYTES }
                    .minByOrNull { scoreIconEntry(it.name) }
                    ?: return null

                zip.getInputStream(entry).use { input ->
                    BitmapFactory.decodeStream(input)?.let { bitmap ->
                        BitmapDrawable(context.resources, bitmap)
                    }
                }
            }
        } catch (_: Exception) {
            null
        } ?: ContextCompat.getDrawable(context, R.drawable.ic_file)
    }

    /**
     * メタデータからアイコンパスを取得する
     */
    private fun getIconPathFromMetadata(zip: ZipFile): String? {
        val fabricEntry = zip.getEntry("fabric.mod.json")
        if (fabricEntry != null) {
            zip.getInputStream(fabricEntry).use { input ->
                val jsonString = input.bufferedReader().readText()
                try {
                    val json = JSONObject(jsonString)
                    val iconPath = json.optString("icon", null)
                    if (!iconPath.isNullOrEmpty()) {
                        return normalizeIconPath(iconPath)
                    }
                } catch (_: Exception) { }
            }
        }

        fun parseTomlIconPath(entryName: String): String? {
            val entry = zip.getEntry(entryName) ?: return null
            zip.getInputStream(entry).use { input ->
                val content = input.bufferedReader().readText()
                val pattern = java.util.regex.Pattern.compile("logoFile\\s*=\\s*\"([^\"]+)\"")
                val matcher = pattern.matcher(content)
                if (matcher.find()) {
                    return normalizeIconPath(matcher.group(1))
                }
            }
            return null
        }

        parseTomlIconPath("META-INF/mods.toml")?.let { return it }
        parseTomlIconPath("META-INF/neoforge.mods.toml")?.let { return it }
        return null
    }

    /**
     * アイコンパスを正規化する
     */
    private fun normalizeIconPath(path: String): String = path.trimStart('/')

    /**
     * アイコンファイル名のスコアを計算する
     */
    private fun scoreIconEntry(name: String): Int {
        val lower = name.lowercase()
        return when {
            lower.endsWith("assets/icon.png") -> 0
            lower.endsWith("icon.png") -> 1
            lower.endsWith("logo.png") -> 2
            lower.endsWith("pack.png") -> 3
            else -> 10
        }
    }

    /**
     * Mod更新選択用のアダプター
     */
    private inner class ModUpdateSelectionAdapter(
        private val context: android.content.Context,
        private val updates: List<ModUpdate>,
        private val checkedStates: BooleanArray
    ) : RecyclerView.Adapter<ModUpdateSelectionAdapter.ViewHolder>() {

        inner class ViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
            val icon: ImageView = itemView.findViewById(R.id.mod_icon)
            val name: TextView = itemView.findViewById(R.id.mod_name)
            val versionInfo: TextView = itemView.findViewById(R.id.mod_version_info)
            val checkbox: CheckBox = itemView.findViewById(R.id.mod_checkbox)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_mod_update_selection, parent, false)
            return ViewHolder(view)
        }

        override fun onBindViewHolder(holder: ViewHolder, position: Int) {
            val update = updates[position]
            holder.name.text = update.modName
            holder.versionInfo.text = "${update.currentVersion} → ${update.latestVersion}"
            holder.checkbox.isChecked = checkedStates[position]

            val modFile = update.originalFile
            Thread {
                val drawable = loadModIcon(context, modFile)
                (holder.itemView.context as android.app.Activity).runOnUiThread {
                    holder.icon.setImageDrawable(drawable)
                }
            }.start()

            holder.checkbox.setOnCheckedChangeListener { _, isChecked ->
                checkedStates[position] = isChecked
            }
        }

        override fun getItemCount(): Int = updates.size
    }

    /**
     * プログレスダイアログのコンポーネント
     */
    private data class ProgressDialogComponents(
        val builder: MaterialAlertDialogBuilder,
        val container: LinearLayout,
        val messageView: TextView,
        val progressBar: LinearProgressIndicator
    )

    /**
     * Material3スタイルのプログレスダイアログを作成する
     */
    private fun createMaterialProgressDialog(title: String): ProgressDialogComponents {
        val context = requireContext()
        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            val padding = (16 * resources.displayMetrics.density).toInt()
            setPadding(padding, padding, padding, padding)
        }
        val messageView = TextView(context).apply {
            textSize = 14f
            setTextAppearance(com.google.android.material.R.style.TextAppearance_Material3_BodyMedium)
        }
        val progressBar = LinearProgressIndicator(context).apply {
            isIndeterminate = false
            max = 100
            progress = 0
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ).apply {
                topMargin = (8 * resources.displayMetrics.density).toInt()
            }
        }
        container.addView(messageView)
        container.addView(progressBar)

        val builder = MaterialAlertDialogBuilder(context)
            .setTitle(title)
            .setView(container)
            .setCancelable(false)

        return ProgressDialogComponents(builder, container, messageView, progressBar)
    }

    /**
     * 更新選択ダイアログを表示する
     */
    private fun showMaterialUpdateSelectionDialog(
        activity: android.app.Activity,
        updates: List<ModUpdate>,
        gameDir: File
    ) {
        if (updates.isEmpty()) {
            Toast.makeText(activity, "No mods available for update.", Toast.LENGTH_SHORT).show()
            return
        }

        val checkedStates = BooleanArray(updates.size) { true }

        val dialogView = LayoutInflater.from(activity).inflate(R.layout.dialog_mod_update_selection, null)
        val recyclerView = dialogView.findViewById<RecyclerView>(R.id.mod_updates_recycler)
        recyclerView.layoutManager = LinearLayoutManager(activity)
        val adapter = ModUpdateSelectionAdapter(activity, updates, checkedStates)
        recyclerView.adapter = adapter

        MaterialAlertDialogBuilder(activity)
            .setTitle("Select mods to update")
            .setView(dialogView)
            .setPositiveButton("Continue") { _, _ ->
                val selectedUpdates = updates.filterIndexed { index, _ -> checkedStates[index] }
                if (selectedUpdates.isEmpty()) {
                    Toast.makeText(activity, "No mods selected for update.", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val summary = selectedUpdates.joinToString(separator = "\n") {
                    "${it.modName} → ${it.latestVersion}"
                }

                MaterialAlertDialogBuilder(activity)
                    .setTitle("Confirm updates")
                    .setMessage("Update ${selectedUpdates.size} selected mods?\n\n$summary")
                    .setPositiveButton("Update") { _, _ ->
                        val components = createMaterialProgressDialog("Updating mods")
                        val dialog = components.builder.show()
                        components.messageView.text = "Preparing downloads..."

                        ModUpdateManager.applyUpdates(
                            context = activity,
                            updates = selectedUpdates,
                            gameDir = gameDir,
                            onProgress = { current, total, fileName, percent ->
                                val overallPercent = (((current - 1) * 100) + percent) / total
                                components.progressBar.progress = overallPercent
                                components.messageView.text = "Downloading ($current/$total)\n$fileName ($percent%)"
                            },
                            onComplete = {
                                dialog.dismiss()
                                Toast.makeText(activity, "Selected updates completed.", Toast.LENGTH_LONG).show()
                            },
                            onError = { e ->
                                dialog.dismiss()
                                Tools.showError(activity, "Update failed: ${e.message}", e)
                            }
                        )
                    }
                    .setNegativeButton("Cancel", null)
                    .show()
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * クリックイベントを処理します。
     */
    override fun onClick(v: View) {
        val activity = requireActivity()
        val version = VersionsManager.getCurrentVersion() ?: run {
            Tools.showError(activity, getString(R.string.version_manager_no_installed_version), NoVersionException("No installed version"))
            return
        }
        val gameDir = version.getGameDir()

        binding.apply {
            when (v) {
                shortcutsMods -> {
                    val bundle = Bundle().apply {
                        putString(ModsFragment.BUNDLE_ROOT_PATH, File(gameDir, "mods").mustExists().absolutePath)
                    }
                    YLTools.swapFragmentWithAnim(this@VersionManagerFragment, ModsFragment::class.java, ModsFragment.TAG, bundle)
                }
                gamePath -> swapFilesFragment(gameDir, gameDir)
                resourcePath -> swapFilesFragment(gameDir, File(gameDir, "resourcepacks"))
                worldPath -> swapFilesFragment(gameDir, File(gameDir, "saves"))
                shaderPath -> swapFilesFragment(gameDir, File(gameDir, "shaderpacks"))
                screenshotPath -> swapFilesFragment(gameDir, File(gameDir, "screenshots"))
                logsPath -> swapFilesFragment(gameDir, File(gameDir, "logs"))
                crashReportPath -> swapFilesFragment(gameDir, File(gameDir, "crash-reports"))

                versionSettings -> YLTools.swapFragmentWithAnim(this@VersionManagerFragment, VersionConfigFragment::class.java, VersionConfigFragment.TAG, null)
                versionRename -> VersionsManager.openRenameDialog(activity, version) {
                    Tools.backToMainMenu(activity)
                }
                versionCopy -> VersionsManager.openCopyDialog(activity, version)
                versionDelete -> {
                    TipDialog.Builder(activity)
                        .setTitle(R.string.generic_warning)
                        .setMessage(activity.getString(R.string.version_manager_delete_tip, version.getVersionName()))
                        .setWarning()
                        .setConfirmClickListener {
                            FileDeletionHandler(
                                activity,
                                listOf(version.getVersionPath()),
                                Task.runTask {
                                    VersionsManager.refresh("VersionManagerFragment:versionDelete")
                                }.ended(TaskExecutors.getAndroidUI()) {
                                    Tools.backToMainMenu(activity)
                                }
                            ).start()
                        }
                        .showDialog()
                }

                checkUpdates -> {
                    binding.checkUpdates.isEnabled = false
                    val components = createMaterialProgressDialog("Checking mod updates")
                    var isCancelled = false
                    var progressDialog: android.app.Dialog? = null

                    val builder = components.builder
                    builder.setNegativeButton("Cancel") { _, _ ->
                        isCancelled = true
                        progressDialog?.dismiss()
                    }
                    progressDialog = builder.show()

                    components.messageView.text = "Scanning mods..."

                    val modsDir = File(gameDir, "mods").apply { if (!exists()) mkdirs() }
                    val minecraftVersion = getGameVersion(version)
                    val selectedLoader = getSelectedLoader(version)
                    Logging.i("ModUpdate", "Using game version: $minecraftVersion, selected loader: ${selectedLoader ?: "none"}")

                    ModUpdateManager.checkUpdates(
                        context = activity,
                        modsDir = modsDir,
                        minecraftVersion = minecraftVersion,
                        selectedLoader = selectedLoader,
                        onProgress = { current, total, modName ->
                            if (isCancelled) return@checkUpdates
                            components.progressBar.progress = if (total == 0) 0 else current * 100 / total
                            components.messageView.text = "Checking ($current/$total)\n$modName"
                        },
                        onComplete = { updates ->
                            if (progressDialog?.isShowing == true) {
                                progressDialog?.dismiss()
                            }
                            binding.checkUpdates.isEnabled = true
                            if (isCancelled) {
                                Toast.makeText(activity, "Update check cancelled.", Toast.LENGTH_SHORT).show()
                                return@checkUpdates
                            }
                            if (updates.isEmpty()) {
                                Toast.makeText(activity, "All mods are up to date!", Toast.LENGTH_LONG).show()
                            } else {
                                showMaterialUpdateSelectionDialog(activity, updates, gameDir)
                            }
                        },
                        onError = { e ->
                            if (progressDialog?.isShowing == true) {
                                progressDialog?.dismiss()
                            }
                            binding.checkUpdates.isEnabled = true
                            if (!isCancelled) {
                                Tools.showError(activity, "Update check failed: ${e.message}", e)
                            } else {
                                Toast.makeText(activity, "Update check cancelled.", Toast.LENGTH_SHORT).show()
                            }
                        }
                    )
                }

                else -> {}
            }
        }
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(editLayout, Animations.BounceInLeft))
        }
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(editLayout, Animations.FadeOutRight))
        }
    }
}