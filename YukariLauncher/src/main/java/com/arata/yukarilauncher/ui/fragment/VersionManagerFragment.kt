package com.arata.yukarilauncher.ui.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
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
import net.kdt.pojavlaunch.Tools
import java.io.File

class VersionManagerFragment : FragmentWithAnim(R.layout.fragment_version_manager), View.OnClickListener {
    companion object {
        const val TAG: String = "VersionManagerFragment"
    }

    private lateinit var binding: FragmentVersionManagerBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentVersionManagerBinding.inflate(inflater, container, false)
        return binding.root
    }

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

    private fun File.mustExists(): File {
        if (!exists()) mkdirs()
        return this
    }

    private fun swapFilesFragment(lockPath: File, listPath: File) {
        val bundle = Bundle().apply {
            putString(FilesFragment.BUNDLE_LOCK_PATH, lockPath.mustExists().absolutePath)
            putString(FilesFragment.BUNDLE_LIST_PATH, listPath.mustExists().absolutePath)
            putBoolean(FilesFragment.BUNDLE_QUICK_ACCESS_PATHS, false)
        }
        YLTools.swapFragmentWithAnim(this, FilesFragment::class.java, FilesFragment.TAG, bundle)
    }

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

    // ---------- Material 3 Progress Dialog Helper ----------
    private data class ProgressDialogComponents(
        val builder: MaterialAlertDialogBuilder,
        val container: LinearLayout,
        val messageView: TextView,
        val progressBar: LinearProgressIndicator
    )

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

    private fun showMaterialUpdateSelectionDialog(
        activity: android.app.Activity,
        updates: List<ModUpdate>,
        gameDir: File
    ) {
        val labels = updates.map { "${it.modName}: ${it.currentVersion} → ${it.latestVersion}" }.toTypedArray()
        val checked = BooleanArray(updates.size) { true }

        MaterialAlertDialogBuilder(activity)
            .setTitle("Select mods to update")
            .setMultiChoiceItems(labels, checked) { _, which, isChecked ->
                checked[which] = isChecked
            }
            .setPositiveButton("Continue") { _, _ ->
                val selectedUpdates = updates.filterIndexed { index, _ -> checked[index] }
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

                // -------- MOD UPDATE CHECKER ----------
                checkUpdates -> {
                    binding.checkUpdates.isEnabled = false
                    val components = createMaterialProgressDialog("Checking mod updates")
                    val dialog = components.builder.show()
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
                            components.progressBar.progress = if (total == 0) 0 else current * 100 / total
                            components.messageView.text = "Checking ($current/$total)\n$modName"
                        },
                        onComplete = { updates ->
                            dialog.dismiss()
                            binding.checkUpdates.isEnabled = true

                            if (updates.isEmpty()) {
                                Toast.makeText(activity, "All mods are up to date!", Toast.LENGTH_LONG).show()
                            } else {
                                showMaterialUpdateSelectionDialog(activity, updates, gameDir)
                            }
                        },
                        onError = { e ->
                            dialog.dismiss()
                            binding.checkUpdates.isEnabled = true
                            Tools.showError(activity, "Update check failed: ${e.message}", e)
                        }
                    )
                }

                else -> {}
            }
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.BounceInRight))
                .apply(AnimPlayer.Entry(editLayout, Animations.BounceInLeft))
        }
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        binding.apply {
            animPlayer.apply(AnimPlayer.Entry(shortcutsLayout, Animations.FadeOutLeft))
                .apply(AnimPlayer.Entry(editLayout, Animations.FadeOutRight))
        }
    }
}