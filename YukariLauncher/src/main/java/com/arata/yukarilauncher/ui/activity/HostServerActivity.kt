package com.arata.yukarilauncher.ui.activity

import android.os.Bundle
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.ActivityHostServerBinding
import com.arata.yukarilauncher.ui.fragment.HostServerFragment

class HostServerActivity : BaseActivity() {

    private lateinit var binding: ActivityHostServerBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHostServerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .replace(R.id.container_fragment, HostServerFragment())
                .commit()
        }
    }

    override fun onBackPressed() {
        // If there are fragments in back stack, pop them; otherwise finish
        if (supportFragmentManager.backStackEntryCount > 0) {
            supportFragmentManager.popBackStack()
        } else {
            super.onBackPressed()
        }
    }
    override fun shouldIgnoreNotch(): Boolean = true
}