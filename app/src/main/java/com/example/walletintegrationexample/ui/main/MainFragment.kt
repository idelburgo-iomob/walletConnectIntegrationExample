package com.example.walletintegrationexample.ui.main

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Intent
import androidx.lifecycle.ViewModelProvider
import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.net.toUri
import com.example.walletintegrationexample.databinding.FragmentMainBinding

class MainFragment : Fragment() {

    companion object {
        fun newInstance() = MainFragment()
    }

    private lateinit var viewModel: MainViewModel
    private lateinit var binding : FragmentMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        viewModel = ViewModelProvider(this).get(MainViewModel::class.java)
        binding = FragmentMainBinding.inflate(layoutInflater)
        binding.connectv1.setOnClickListener {
            connect()
        }
        binding.connectv2.setOnClickListener {
            connectv2()
        }
        binding.disconnect.setOnClickListener {
            disconnect()
        }

        binding.sign.setOnClickListener {
//            viewModel.signMessage()
            viewModel.signV2Message()
        }

    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setListeners()
    }

    private fun connect() {
        viewModel.connect()
    }
    private fun connectv2() {
        viewModel.signV2()
    }
    private fun disconnect() {
        viewModel.disconnect()
    }

    private fun setListeners() {
        viewModel.connectionUri.observe(viewLifecycleOwner) { uri ->
            try {
                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, uri.toUri()))
            } catch (e: Exception) {
                println("WALLET_CONN -> no app compatible")
            }
        }
        viewModel.goMetamask.observe(viewLifecycleOwner) { go ->
            if (go) {
//                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, "https://metamask.app.link/".toUri()))
//                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, "https://valoraapp.com/wc".toUri()))
//                requireActivity().startActivity(Intent(Intent.ACTION_VIEW, .toUri()))
            }
        }

        viewModel.address.observe(viewLifecycleOwner) { walletAddress ->
            if (walletAddress == null) {
                binding.address.text = "DISCONNECTED"
                binding.connectv1.visibility = View.VISIBLE
                binding.connectv2.visibility = View.VISIBLE
                binding.sign.visibility = View.GONE
                binding.disconnect.visibility = View.GONE
            } else {
                binding.address.text = walletAddress
                binding.connectv1.visibility = View.GONE
                binding.connectv2.visibility = View.GONE
                binding.sign.visibility = View.VISIBLE
                binding.disconnect.visibility = View.VISIBLE
            }
        }
    }

}