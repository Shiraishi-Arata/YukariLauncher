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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentHostServerBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        // Bind to service (if already running)
        Intent(requireContext(), HostServerService::class.java).also { intent ->
            requireContext().bindService(intent, connection, Context.BIND_AUTO_CREATE)
        }

        // Back button – close activity, service stays alive
        binding.hostServerBackButton.setOnClickListener {
            requireActivity().finish()
        }

        // Start / Stop toggle
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

        // Copy tunnel address to clipboard
        binding.hostServerCopyButton.setOnClickListener {
            val address = binding.hostServerTunnelAddress.text.toString()
            if (address.isNotBlank() && address != getString(R.string.host_server_address_unknown)) {
                val clipboard = requireContext().getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                clipboard.setPrimaryClip(ClipData.newPlainText("Server Address", address))
                Toast.makeText(requireContext(), R.string.host_server_copied, Toast.LENGTH_SHORT).show()
            }
        }

        // Open auth URL in browser
        binding.hostServerOpenAuthButton.setOnClickListener {
            service?.authUrl?.value?.let { url ->
                try {
                    startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)))
                } catch (e: Exception) {
                    Toast.makeText(requireContext(), R.string.host_server_no_browser, Toast.LENGTH_SHORT).show()
                }
            }
        }

        // Hide console entry point
        binding.hostServerOpenConsoleButton.visibility = View.GONE

        // Open dashboard
        binding.hostServerOpenDashboardButton.setOnClickListener {
            startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://playit.gg/account/tunnels")))
        }

        // Initially hide status and log until service reports
        binding.hostServerStatusCard.visibility = View.GONE
    }

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

    private fun observeServiceLiveData() {
        service?.let { srv ->
            // Running state
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

            // Auth URL
            srv.authUrl.observe(viewLifecycleOwner) { url ->
                if (url != null) {
                    binding.hostServerStatusText.setText(R.string.host_server_status_auth)
                    binding.hostServerAuthLayout.visibility = View.VISIBLE
                    binding.hostServerAuthUrl.text = url
                } else {
                    binding.hostServerAuthLayout.visibility = View.GONE
                }
            }

            // Tunnel address
            srv.tunnelAddress.observe(viewLifecycleOwner) { address ->
                when {
                    address == null -> {
                        // Stopped or not ready
                        binding.hostServerTunnelLayout.visibility = View.GONE
                        binding.hostServerStatusText.setText(R.string.host_server_status_idle)
                    }
                    address.isEmpty() -> {
                        // Running but address unknown
                        binding.hostServerStatusText.setText(R.string.host_server_status_running)
                        binding.hostServerTunnelLayout.visibility = View.VISIBLE
                        binding.hostServerTunnelAddress.text = getString(R.string.host_server_address_unknown)
                        binding.hostServerOpenDashboardButton.visibility = View.VISIBLE
                    }
                    else -> {
                        // Address known
                        binding.hostServerStatusText.setText(R.string.host_server_status_running)
                        binding.hostServerTunnelLayout.visibility = View.VISIBLE
                        binding.hostServerTunnelAddress.text = address
                        binding.hostServerOpenDashboardButton.visibility = View.VISIBLE
                    }
                }
            }
        }
    }

    private fun startTunnel(port: Int) {
        Intent(requireContext(), HostServerService::class.java).apply {
            action = HostServerService.ACTION_START
            putExtra(HostServerService.EXTRA_PORT, port)
        }.also {
            requireContext().startService(it)
        }
        binding.hostServerStatusText.setText(R.string.host_server_status_starting)
    }

    private fun stopTunnel() {
        Intent(requireContext(), HostServerService::class.java).apply {
            action = HostServerService.ACTION_STOP
        }.also {
            requireContext().startService(it)
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (bound) {
            requireContext().unbindService(connection)
            bound = false
        }
    }

    override fun slideIn(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.hostServerRoot, Animations.BounceInDown))
    }

    override fun slideOut(animPlayer: AnimPlayer) {
        animPlayer.apply(AnimPlayer.Entry(binding.hostServerRoot, Animations.FadeOutUp))
    }
}
