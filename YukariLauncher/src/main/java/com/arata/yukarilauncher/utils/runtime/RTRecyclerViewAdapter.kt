package com.arata.yukarilauncher.utils.runtime

import android.annotation.SuppressLint
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageButton
import android.widget.TextView
import androidx.annotation.NonNull
import androidx.recyclerview.widget.RecyclerView
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.setting.AllSettings
import com.arata.yukarilauncher.task.Task
import com.arata.yukarilauncher.ui.dialog.SelectRuntimeDialog
import com.arata.yukarilauncher.utils.platform.Architecture
import com.arata.yukarilauncher.Tools
import java.util.*

/** ランタイム一覧を表示するRecyclerViewアダプター。選択モードと編集モードをサポートする。 */
class RTRecyclerViewAdapter(
    mData: List<Runtime>,
    private val mSelectedListener: RuntimeSelectedListener? = null,
    private val mDialog: SelectRuntimeDialog? = null
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {
    private val mData: MutableList<Runtime> = mData.toMutableList()

    /** アダプターのモード（選択/編集）。 */
    private val mType: Int
    /** 編集中フラグ。trueの場合は削除ボタンが表示される。 */
    var isEditing: Boolean = false

    init {
        mType = if (mDialog != null) TYPE_MODE_SELECT else TYPE_MODE_EDIT
    }

    /**
     * ビューホルダーを生成する。
     * @param parent 親ビューグループ
     * @param viewType ビュータイプ
     * @return 対応するビューホルダー
     */
    override fun onCreateViewHolder(@NonNull parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_MODE_SELECT -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_select_multirt_runtime, parent, false)
                RTSelectViewHolder(view)
            }
            else -> {
                val view = LayoutInflater.from(parent.context).inflate(R.layout.item_multirt_runtime, parent, false)
                RTEditViewHolder(view)
            }
        }
    }

    /**
     * ビューホルダーにデータをバインドする。
     * @param holder ビューホルダー
     * @param position 位置
     */
    override fun onBindViewHolder(@NonNull holder: RecyclerView.ViewHolder, position: Int) {
        if (getItemViewType(position) == TYPE_MODE_EDIT) {
            (holder as RTEditViewHolder).bindRuntime(mData[position], position)
        } else {
            (holder as RTSelectViewHolder).bindRuntime(mData[position])
        }
    }

    /** アイテム総数を返す。 @return アイテム数 */
    override fun getItemCount(): Int = mData.size

    /** ランタイムがデフォルト設定されているか判定する。 @param rt ランタイム @return デフォルトの場合は true */
    fun isDefaultRuntime(rt: Runtime): Boolean = AllSettings.defaultRuntime.getValue() == rt.name

    /** @param position 位置 @return ビュータイプ */
    override fun getItemViewType(position: Int): Int = mType

    /** ランタイムをデフォルトに設定する。 @param rt ランタイム */
    fun setDefault(rt: Runtime) {
        AllSettings.defaultRuntime.put(rt.name).save()
        notifyDataSetChanged()
    }

    /**
     * ランタイム名から表示用Javaバージョン名を生成する。
     * @param runtime ランタイム
     * @return 整形されたバージョン名
     */
    private fun getJavaVersionName(runtime: Runtime): String {
        return runtime.name.replace(".tar.xz", "")
            .replace("-", " ")
    }

    /** ランタイム選択モード用のビューホルダー。 */
    inner class RTSelectViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** メインビュー。 */
        val mainView: View = itemView
        /** コンテキスト。 */
        val mContext: Context = itemView.context
        /** Javaバージョン表示TextView。 */
        val mJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version)
        /** 完全なJavaバージョン表示TextView。 */
        val mFullJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version_full)
        /** ランチャー提供ランタイム表示TextView。 */
        val mProvidedByLauncherTextView: TextView = itemView.findViewById(R.id.multirt_provided_by_launcher)

        /**
         * ランタイムデータをビューにバインドする。
         * @param runtime ランタイム
         */
        fun bindRuntime(runtime: Runtime) {
            if (runtime.name != "auto") {
                mProvidedByLauncherTextView.visibility = if (runtime.isProvidedByLauncher) View.VISIBLE else View.GONE

                if (runtime.versionString != null && runtime.arch != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                    mJavaVersionTextView.text = getJavaVersionName(runtime)
                    mFullJavaVersionTextView.text = runtime.versionString
                    mainView.setOnClickListener { selectRuntime(runtime.name) }
                    return
                }

                if (runtime.versionString == null) {
                    mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt)
                } else {
                    mFullJavaVersionTextView.text = mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch)
                }
                mJavaVersionTextView.text = runtime.name
                mFullJavaVersionTextView.setTextColor(Color.RED)
            } else {
                mJavaVersionTextView.setText(R.string.install_auto_select)
                mFullJavaVersionTextView.visibility = View.GONE
                mainView.setOnClickListener { selectRuntime(null) }
            }
        }

        /**
         * ランタイムを選択してリスナーに通知する。
         * @param jreName 選択したJRE名
         */
        private fun selectRuntime(jreName: String?) {
            mSelectedListener?.onSelected(jreName)
            mDialog?.dismiss()
        }
    }

    /** ランタイム編集モード用のビューホルダー。 */
    inner class RTEditViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        /** Javaバージョン表示TextView。 */
        val mJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version)
        /** 完全なJavaバージョン表示TextView。 */
        val mFullJavaVersionTextView: TextView = itemView.findViewById(R.id.multirt_view_java_version_full)
        /** ランチャー提供ランタイム表示TextView。 */
        val mProvidedByLauncherTextView: TextView = itemView.findViewById(R.id.multirt_provided_by_launcher)
        /** デフォルトのテキストカラー。 */
        val mDefaultColors: ColorStateList = mFullJavaVersionTextView.textColors
        /** デフォルト設定ボタン。 */
        val mSetDefaultButton: Button = itemView.findViewById(R.id.multirt_view_setdefaultbtn)
        /** 削除ボタン。 */
        val mDeleteButton: ImageButton = itemView.findViewById(R.id.multirt_view_removebtn)
        /** コンテキスト。 */
        val mContext: Context = itemView.context
        /** 現在バインド中のランタイム。 */
        var mCurrentRuntime: Runtime? = null
        /** 現在の位置。 */
        var mCurrentPosition: Int = 0

        init {
            setupOnClickListeners()
        }

        /** ボタンのクリックリスナーを設定する。 */
        @SuppressLint("NotifyDataSetChanged")
        private fun setupOnClickListeners() {
            mSetDefaultButton.setOnClickListener {
                mCurrentRuntime?.let { runtime ->
                    setDefault(runtime)
                    this@RTRecyclerViewAdapter.notifyDataSetChanged()
                }
            }

            mDeleteButton.setOnClickListener {
                val runtime = mCurrentRuntime ?: return@setOnClickListener

                @Suppress("RETURN_TYPE_MISMATCH")
                Task.runTask<Void> {
                    MultiRTUtils.removeRuntimeNamed(runtime.name)
                    mDeleteButton.post {
                        if (bindingAdapter != null) {
                            mData.clear()
                            mData.addAll(MultiRTUtils.runtimes)
                            bindingAdapter!!.notifyDataSetChanged()
                        }
                    }
                    null
                }.onThrowable { e -> Tools.showError(itemView.context, e) }.execute()
            }
        }

        /**
         * ランタイムデータをビューにバインドする。
         * @param runtime ランタイム
         * @param pos 位置
         */
        fun bindRuntime(runtime: Runtime, pos: Int) {
            mCurrentRuntime = runtime
            mCurrentPosition = pos

            updateButtonsVisibility(runtime)
            mProvidedByLauncherTextView.visibility = if (runtime.isProvidedByLauncher) View.VISIBLE else View.GONE

            if (runtime.versionString != null && runtime.arch != null && Tools.DEVICE_ARCHITECTURE == Architecture.archAsInt(runtime.arch)) {
                mJavaVersionTextView.text = getJavaVersionName(runtime)
                mFullJavaVersionTextView.text = runtime.versionString
                mFullJavaVersionTextView.setTextColor(mDefaultColors)

                val defaultRuntime = isDefaultRuntime(runtime)
                mSetDefaultButton.isEnabled = !defaultRuntime
                mSetDefaultButton.setText(if (defaultRuntime) R.string.generic_default else R.string.multirt_config_setdefault)
                return
            }

            mDeleteButton.visibility = View.VISIBLE
            if (runtime.versionString == null) {
                mFullJavaVersionTextView.setText(R.string.multirt_runtime_corrupt)
            } else {
                mFullJavaVersionTextView.text = mContext.getString(R.string.multirt_runtime_incompatiblearch, runtime.arch)
            }
            mJavaVersionTextView.text = runtime.name
            mFullJavaVersionTextView.setTextColor(Color.RED)
            mSetDefaultButton.visibility = View.GONE
        }

        /** 編集モードに応じてボタンの表示状態を更新する。 @param runtime ランタイム */
        private fun updateButtonsVisibility(runtime: Runtime) {
            mSetDefaultButton.visibility = if (isEditing) View.INVISIBLE else View.VISIBLE
            mDeleteButton.visibility = if (!isEditing || runtime.isProvidedByLauncher) View.INVISIBLE else View.VISIBLE
        }
    }

    companion object {
        /** 選択モードを示す定数。 */
        private const val TYPE_MODE_SELECT = 0
        /** 編集モードを示す定数。 */
        private const val TYPE_MODE_EDIT = 1
    }
}
