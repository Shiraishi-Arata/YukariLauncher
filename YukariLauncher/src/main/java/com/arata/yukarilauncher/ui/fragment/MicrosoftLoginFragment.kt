package com.arata.yukarilauncher.ui.fragment

import android.annotation.SuppressLint
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.CookieManager
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.annotation.NonNull
import androidx.annotation.Nullable
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentMicrosoftLoginBinding
import com.arata.yukarilauncher.event.value.MicrosoftLoginEvent
import com.arata.yukarilauncher.feature.log.Logging
import org.greenrobot.eventbus.EventBus

/** MicrosoftアカウントのOAuth2ログインをWebViewで行うフラグメント。 */
class MicrosoftLoginFragment : BaseFragment(R.layout.fragment_microsoft_login) {

    companion object {
        /** フラグメントのタグ。 */
        const val TAG = "MICROSOFT_LOGIN_FRAGMENT"
    }

    /** ビューバインディングインスタンス。 */
    private var binding: FragmentMicrosoftLoginBinding? = null
    /** WebViewClientが未設定かどうかを示すフラグ。 */
    private var mBlankClient = true

    @Nullable
    override fun onCreateView(@NonNull inflater: LayoutInflater, @Nullable container: ViewGroup?, @Nullable savedInstanceState: Bundle?): View? {
        binding = FragmentMicrosoftLoginBinding.inflate(layoutInflater)
        return binding?.root
    }

    /** ビュー作成後の初期化。WebViewの設定とセッションの開始または復元を行う。 */
    override fun onViewCreated(@NonNull view: View, @Nullable savedInstanceState: Bundle?) {
        binding?.returnButton?.setOnClickListener { forceBack() }

        setWebViewSettings()
        if (savedInstanceState == null) startNewSession()
        else restoreWebViewState(savedInstanceState)
    }

    /** WebViewのJavaScriptを有効化し、URLトラッキング用のWebViewClientを設定する。 */
    @SuppressLint("SetJavaScriptEnabled")
    private fun setWebViewSettings() {
        val settings = binding?.webView?.settings
        settings?.javaScriptEnabled = true
        binding?.webView?.webViewClient = WebViewTrackClient()
        mBlankClient = false
    }

    /** 新しいログインセッションを開始する。クッキーを削除後、MicrosoftのOAuth2認証URLを読み込む。 */
    private fun startNewSession() {
        CookieManager.getInstance().removeAllCookies { _ ->
            binding?.webView?.apply {
                clearHistory()
                clearCache(true)
                clearFormData()
                clearHistory()
                loadUrl("https://login.live.com/oauth20_authorize.srf" +
                        "?client_id=00000000402b5328" +
                        "&response_type=code" +
                        "&scope=service%3A%3Auser.auth.xboxlive.com%3A%3AMBI_SSL" +
                        "&redirect_url=https%3A%2F%2Flogin.live.com%2Foauth20_desktop.srf")
            }
        }
    }

    /** 保存されたインスタンス状態からWebViewの状態を復元する。 */
    private fun restoreWebViewState(savedInstanceState: Bundle) {
        Logging.i("MSAuthFragment", "Restoring state...")
        if (binding?.webView?.restoreState(savedInstanceState) == null) {
            Logging.w("MSAuthFragment", "Failed to restore state, starting afresh")
            startNewSession()
        }
    }

    override fun onStart() {
        super.onStart()
        if (mBlankClient) binding?.webView?.webViewClient = WebViewTrackClient()
    }

    override fun onBackPressed(): Boolean {
        return if (canGoBack()) {
            goBack()
            false
        } else {
            super.onBackPressed()
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        binding?.webView?.webViewClient = WebViewClient()
        mBlankClient = true
        super.onSaveInstanceState(outState)
        binding?.webView?.saveState(outState)
    }

    /** WebViewが戻れるかどうかを返す。 */
    fun canGoBack(): Boolean = binding?.webView?.canGoBack() ?: false

    /** WebViewを1ページ戻す。 */
    fun goBack() {
        binding?.webView?.goBack()
    }

    /** MicrosoftログインのコールバックURLを追跡するWebViewClient。 */
    inner class WebViewTrackClient : WebViewClient() {
        override fun shouldOverrideUrlLoading(view: WebView, url: String): Boolean {
            if (url.startsWith("ms-xal-00000000402b5328")) {
                EventBus.getDefault().post(MicrosoftLoginEvent(Uri.parse(url)))
                Toast.makeText(view.context, getString(R.string.account_login_start), Toast.LENGTH_SHORT).show()
                forceBack()
                return true
            }

            if (url.contains("res=cancel")) {
                forceBack()
                return true
            }

            return super.shouldOverrideUrlLoading(view, url)
        }
    }
}