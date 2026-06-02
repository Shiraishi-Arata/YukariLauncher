package com.arata.yukarilauncher.feature.mod.modpack.export

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.CheckBox
import android.widget.ImageView
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogExportPathPickerBinding
import com.arata.yukarilauncher.databinding.ItemExportPathPickerBinding
import java.io.File

class ExportPathPickerDialog(
    private val context: Context,
    private val rootDir: File,
    private val title: String,
    private val defaultChecked: Set<String>,
    private val onConfirm: (Set<String>) -> Unit
) {
    private data class Node(
        val path: String,
        val name: String,
        val file: File,
        val depth: Int,
        val isDirectory: Boolean,
        var expanded: Boolean = false,
        var checked: Boolean = false
    )

    private val nodes = mutableListOf<Node>()
    private val visibleNodes = mutableListOf<Node>()

/**
 * showする
 */
    fun show() {
        buildNodes()
        refreshVisibleNodes()

        val binding = DialogExportPathPickerBinding.inflate(LayoutInflater.from(context))
        val adapter = PathAdapter()
        binding.pathList.layoutManager = LinearLayoutManager(context)
        binding.pathList.adapter = adapter

        binding.expandAll.setOnClickListener {
            nodes.filter { it.isDirectory }.forEach { it.expanded = true }
            refreshVisibleNodes()
            adapter.notifyDataSetChanged()
        }
        binding.collapseAll.setOnClickListener {
            nodes.filter { it.isDirectory }.forEach { it.expanded = false }
            refreshVisibleNodes()
            adapter.notifyDataSetChanged()
        }

        AlertDialog.Builder(context, R.style.CustomAlertDialogTheme)
            .setTitle(title)
            .setView(binding.root)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                onConfirm(nodes.filter { it.checked }.map { it.path }.toSet())
            }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

/**
 * buildNodesする
 */
    private fun buildNodes() {
        nodes.clear()
        addChildren(rootDir, 0)
    }

/**
 * addChildrenする
 */
    private fun addChildren(parent: File, depth: Int) {
        parent.listFiles()
            ?.sortedWith(compareBy<File> { !it.isDirectory }.thenBy { it.name.lowercase() })
            ?.forEach { file ->
                val relativePath = rootDir.toPath().relativize(file.toPath()).toString().replace('\\', '/')
                val checked = defaultChecked.any { it == relativePath || relativePath.startsWith("$it/") }
                nodes.add(
                    Node(
                        relativePath,
                        file.name,
                        file,
                        depth,
                        file.isDirectory,
                        expanded = false,
                        checked = checked
                    )
                )
                if (file.isDirectory) addChildren(file, depth + 1)
            }
    }

/**
 * refreshVisibleNodesする
 */
    private fun refreshVisibleNodes() {
        visibleNodes.clear()
        val expandedDirectories = mutableSetOf<String>()
        nodes.forEach { node ->
            if (node.depth == 0) {
                visibleNodes.add(node)
                if (node.isDirectory && node.expanded) expandedDirectories.add(node.path)
                return@forEach
            }

            val parentPath = node.path.substringBeforeLast('/', missingDelimiterValue = "")
            if (parentPath.isNotBlank() && expandedDirectories.any { parentPath == it || parentPath.startsWith("$it/") }) {
                visibleNodes.add(node)
            }

            if (node.isDirectory && node.expanded) expandedDirectories.add(node.path)
        }
    }

    private inner class PathAdapter : RecyclerView.Adapter<PathAdapter.PathViewHolder>() {
        inner class PathViewHolder(val binding: ItemExportPathPickerBinding) : RecyclerView.ViewHolder(binding.root)

/**
 * onCreateViewHolderする
 */
        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PathViewHolder {
            return PathViewHolder(ItemExportPathPickerBinding.inflate(LayoutInflater.from(parent.context), parent, false))
        }

/**
 * getItemCountする
 */
        override fun getItemCount(): Int = visibleNodes.size

/**
 * onBindViewHolderする
 */
        override fun onBindViewHolder(holder: PathViewHolder, position: Int) {
            val node = visibleNodes[position]
            holder.binding.apply {
                val paddingStart = (node.depth * 20) + 8
                root.setPadding(paddingStart, root.paddingTop, root.paddingRight, root.paddingBottom)
                name.text = node.name
                checkBox.isChecked = node.checked

                if (node.isDirectory) {
                    expandIcon.visibility = View.VISIBLE
                    expandIcon.rotation = if (node.expanded) 90f else 0f
                    expandIcon.setOnClickListener {
                        node.expanded = !node.expanded
                        refreshVisibleNodes()
                        notifyDataSetChanged()
                    }
                } else {
                    expandIcon.visibility = View.INVISIBLE
                }

                val checkedListener = View.OnClickListener {
                    val checked = !node.checked
                    applyChecked(node, checked)
                    refreshVisibleNodes()
                    notifyDataSetChanged()
                }
                checkBox.setOnClickListener(checkedListener)
                name.setOnClickListener(checkedListener)
            }
        }
    }

/**
 * applyCheckedする
 */
    private fun applyChecked(target: Node, checked: Boolean) {
        target.checked = checked
        if (target.isDirectory) {
            nodes.filter { it.path.startsWith("${target.path}/") }.forEach { it.checked = checked }
        }
    }
}