package com.arata.yukarilauncher.ui.subassembly.customcontrols

import java.io.File

abstract class ControlSelectedListener {
    abstract fun onItemSelected(file: File)
    abstract fun onItemLongClick(file: File)
}
