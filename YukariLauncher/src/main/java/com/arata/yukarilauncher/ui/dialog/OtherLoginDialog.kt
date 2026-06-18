package com.arata.yukarilauncher.ui.dialog

import android.content.Context
import android.os.Bundle
import android.view.View
import android.view.Window
import android.widget.Toast
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.DialogOtherLoginBinding
import com.arata.yukarilauncher.feature.accounts.OtherLoginHelper
import com.arata.yukarilauncher.feature.login.Servers.Server
import com.arata.yukarilauncher.ui.dialog.DraggableDialog.DialogInitializationListener
import com.arata.yukarilauncher.utils.YLTools

/**
 * 外部サーバーログインダイアログ
 */
class OtherLoginDialog(
    context: Context,
    private val server: Server,
    private val listener: OtherLoginHelper.OnLoginListener
) : FullScreenDialog(context), View.OnClickListener, DialogInitializationListener {
    private val binding = DialogOtherLoginBinding.inflate(layoutInflater)

    /**
     * ダイアログ作成時にUIを初期化する
     * @param savedInstanceState 保存されたインスタンス状態
     */
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(binding.root)
        binding.apply {

            serverName.text = server.serverName
            if (server.register.isEmpty()) {
                registryText.visibility = View.GONE
            } else {
                registryText.setOnClickListener(this@OtherLoginDialog)
            }

            cancelButton.setOnClickListener(this@OtherLoginDialog)
            loginButton.setOnClickListener(this@OtherLoginDialog)
        }

        DraggableDialog.initDialog(this)
    }

    /**
     * メールアドレスとパスワードの入力有無をチェックする
     */
    private fun checkAccountInformation(email: String?, password: String?): Boolean {
        val emailEmpty = email.isNullOrEmpty()
        val passwordEmpty = password.isNullOrEmpty()

        return if (emailEmpty || passwordEmpty) {
            val errorString = context.getString(R.string.generic_error_field_empty)
            if (emailEmpty) binding.emailEdit.error = errorString
            if (passwordEmpty) binding.passwordEdit.error = errorString
            false
        } else true
    }

    /**
     * ダイアログ初期化時にWindowを返す
     * @return Windowオブジェクト
     */
    override fun onInit(): Window? = window

    /**
     * 各Viewのクリックイベントを処理する
     * @param v クリックされたView
     */
    override fun onClick(v: View) {
        binding.apply {
            when (v) {
                cancelButton -> dismiss()
                registryText -> {
                    server.register.takeIf { it.isNotEmpty() }?.let { link ->
                        YLTools.openLink(context, link)
                        dismiss()
                    }
                }
                loginButton -> {
                    val email = emailEdit.text.toString()
                    val password = passwordEdit.text.toString()
                    if (!checkAccountInformation(email, password)) return
                    if (server.baseUrl.isNullOrEmpty()) {
                        Toast.makeText(context, context.getString(R.string.other_login_server_not_empty), Toast.LENGTH_SHORT).show()
                        return
                    }

                    OtherLoginHelper(server.baseUrl, server.serverName, email, password, listener).createNewAccount(context)

                    dismiss()
                }
            }
        }
    }
}