package com.arata.yukarilauncher.ui.fragment

import android.Manifest
import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import android.text.method.ScrollingMovementMethod
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.arata.anim.AnimPlayer
import com.arata.anim.animations.Animations
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentHostServerBinding
import com.arata.yukarilauncher.feature.network.HostServerService
import com.arata.yukarilauncher.task.TaskExecutors

/**
 * サーバーホスティングフラグメント（playit.ggトンネル）
 */
class HostServerFragment : FragmentWithAnim(R.layout.fragment_host_server) {

    companion object {
        const val TAG = "HostServerFragment"
        private const val PERMISSION_REQUEST_CODE = 1001
    }

    private lateinit var binding: FragmentHostServerBinding
    private var service: HostServerService? = null
    private var bound = false
    private val logBuilder = StringBuilder()

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            val localBinder = binder as HostServerService.LocalBinder
            service = localBinder.getService()
            bound = true
            observeServiceLiveData()
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            service = null
            bound = false
        }
    }

    /**
     * フラグメントのビューを生成します。
     */
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHostServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    /**
     * ビュー作成後の初期化処理を行います。
     */
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        Intent(requireContext(), HostServerService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        binding.hostServerBackButton.setOnClickListener {
            requireActivity().finish()
        }

        binding.hostServerToggleButton.setOnClickListener {
            val portText = binding.hostServerPortInput.text.toString().trim()
            val port = portText.toIntOrNull()
            if (port == null || port !in 1..65535) {
                Toast.makeText(requireContext(), R.string.host_server_invalid_port, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (service?.isRunning?.value == true) {
                stopTunnel()
            } else {
                if (checkNotificationPermission()) {
                    startTunnel(port)
                }
            }
        }

        binding.hostServerCopyButton.setOnClickListener {
            val address = binding.hostServerTunnelAddress.text.toString()
            if (address.isNotBlank() && address != getString(R.string.host_server_address_unknown)) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Server Address", address))
                Toast.makeText(requireContext(), R.string.host_server_copied, Toast.LENGTH_SHORT).show()
            }
        }

        binding.hostServerOpenAuthButton.setOnClickListener {
            service?.authUrl?.value?.let { url ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.host_server_no_browser, Toast.LENGTH_SHORT).show()
                }
            }
        }

        binding.hostServerOpenConsoleButton.visibility = View.GONE

        binding.hostServerOpenDashboardButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://playit.gg/account/tunnels")))
        }

        binding.hostServerStatusCard.visibility = View.GONE
    }

    /**
     * 通知権限を確認する
     */
    private fun checkNotificationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.POST_NOTIFICATIONS)
                == PackageManager.PERMISSION_GRANTED) {
                true
            } else {
                requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), PERMISSION_REQUEST_CODE)
                false
            }
        } else {
            true
        }
    }

    /**
     * 権限リクエストの結果を処理します。
     */
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                val port = binding.hostServerPortInput.text.toString().toIntOrNull() ?: 25565
                startTunnel(port)
            } else {
                Toast.makeText(requireContext(), R.string.host_server_notification_permission_denied, Toast.LENGTH_LONG).show()
            }
        }
    }

    /**
     * サービスからのLiveDataを監視する
     */
    private fun observeServiceLiveData() {
        service?.let { srv ->
            srv.isRunning.observe(viewLifecycleOwner) { running ->
                binding.hostServerToggleButton.text = if (running == true) {
                    getString(R.string.host_server_stop)
                } else {
                    getString(R.string.host_server_start)
                }
                binding.hostServerPortInput.isEnabled = running != true

                if (running == true) {
                    binding.hostServerStatusCard.visibility = View.VISIBLE
                }
            }

            srv.authUrl.observe(viewLifecycleOwner) { url ->
                if (url != null) {
                    binding.hostServerStatusText.setText(R.string.host_server_status_auth)
                    binding.hostServerAuthLayout.visibility = View.VISIBLE
                    binding.hostServerAuthUrl.text = url
                } else {
                    binding.hostServerAuthLayout.visibility = View.GONE
                }
            }

            srv.tunnelAddress.observe(viewLifecycleOwner) { address ->
                when {
                    address == null -> {
                        binding.hostServerTunnelLayout.visibility = View.GONE
                        binding.hostServerStatusText.setText(R.string.host_server_status_idle)
                    }
                    address.isEmpty() -> {
                        binding.hostServerStatusText.setText(R.string.host_server_status_running)
                        binding.hostServerTunnelLayout.visibility = View.VISIBLE
                        binding.hostServerTunnelAddress.text = getString(R.string.host_server_address_unknown)
                        binding.hostServerOpenDashboardButton.visibility = View.VISIBLE
                    }
                    else -> {
                        binding.hostServerStatusText.setText(R.string.host_server_status_running)
                        binding.hostServerTunnelLayout.visibility = View.VISIBLE
                        binding.hostServerTunnelAddress.text = address
                        binding.hostServerOpenDashboardButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    /**
     * トンネルを開始する
     */
    private fun startTunnel(port: Int) {
        Intent(requireContext(), HostServerService::class.java).apply {
            action = HostServerService.ACTION_START
            putExtra(HostServerService.EXTRA_PORT, port)
        }.also {
            requireContext().startService(it)
        }
        binding.hostServerStatusText.setText(R.string.host_server_status_starting)
    }

    /**
     * トンネルを停止する
     */
    private fun stopTunnel() {
        Intent(requireContext(), HostServerService::class.java).apply {
            action = HostServerService.ACTION_STOP
        }.also {
            requireContext().startService(it)
        }
    }

    /**
     * ビュー破棄時にサービスとのバインドを解除します。
     */
    override fun onDestroyView() {
        super.onDestroyView()
        if (bound) {
            requireContext().unbindService(connection)
            bound = false
        }
    }

    /**
     * スライドインアニメーションを実行します。
     */
    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.hostServerRoot, Animations.BounceInDown))
    }

    /**
     * スライドアウトアニメーションを実行します。
     */
    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.hostServerRoot, Animations.FadeOutUp))
    }
}
