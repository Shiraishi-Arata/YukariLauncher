package com.arata.yukarilauncher.ui.fragment.about

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.arata.yukarilauncher.R
import com.arata.yukarilauncher.databinding.FragmentAboutSponsorPageBinding
import com.arata.yukarilauncher.feature.CheckSponsor
import com.arata.yukarilauncher.feature.CheckSponsor.Companion.check
import com.arata.yukarilauncher.feature.CheckSponsor.Companion.getSponsorData
import com.arata.yukarilauncher.feature.log.Logging
import com.arata.yukarilauncher.task.TaskExecutors
import com.arata.yukarilauncher.ui.subassembly.about.SponsorMeta
import com.arata.yukarilauncher.ui.subassembly.about.SponsorRecyclerAdapter

class AboutSponsorPageFragment : Fragment(R.layout.fragment_about_sponsor_page) {
    private lateinit var binding: FragmentAboutSponsorPageBinding

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = FragmentAboutSponsorPageBinding.inflate(layoutInflater)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        check(object : CheckSponsor.CheckListener {
            override fun onFailure() {
                setSponsorVisible(false)
            }

            override fun onSuccessful(data: SponsorMeta?) {
                setSponsorVisible(true)
            }
        })
    }

    private fun setSponsorVisible(visible: Boolean) {
        TaskExecutors.runInUIThread {
            try {
                binding.sponsorLayout.visibility = if (visible) {
                    binding.sponsorRecycler.apply {
                        layoutManager = LinearLayoutManager(requireContext())
                        adapter = SponsorRecyclerAdapter(getSponsorData())
                    }
                    binding.loadingProgress.visibility = View.GONE
                    View.VISIBLE
                } else View.GONE
            } catch (e: Exception) {
                Logging.e("setSponsorVisible", e.toString())
            }
        }
    }
}

