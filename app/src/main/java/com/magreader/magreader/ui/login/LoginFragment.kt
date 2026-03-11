package com.magreader.magreader.ui.login

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.magreader.magreader.R
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentLoginBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        opdsManager = OpdsManager(requireContext())

        // Pre-fill if exists
        val savedUrl = opdsManager.opdsUrl
        val savedUser = opdsManager.username
        val savedPass = opdsManager.password

        binding.editServerUrl.setText(savedUrl)
        binding.editUsername.setText(savedUser)
        binding.editPassword.setText(savedPass)

        // If credentials already exist, try to log in automatically
        if (!savedUrl.isNullOrEmpty()) {
            performLogin(savedUrl, savedUser, savedPass, isAutoLogin = true)
        }

        binding.buttonLogin.setOnClickListener {
            val urlInput = binding.editServerUrl.text.toString().trim()
            val user = binding.editUsername.text.toString().trim()
            val pass = binding.editPassword.text.toString().trim()

            if (urlInput.isEmpty()) {
                Toast.makeText(context, "Please enter OPDS URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            var url = urlInput
            // Auto-append suffix if missing
            if (!url.contains("/api/v1/opds")) {
                url = if (url.endsWith("/")) {
                    "${url}api/v1/opds"
                } else {
                    "$url/api/v1/opds"
                }
                binding.editServerUrl.setText(url)
            }

            performLogin(url, user, pass, isAutoLogin = false)
        }

        binding.buttonOffline.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isOfflineMode", true)
            }
            findNavController().navigate(R.id.action_login_to_library, bundle)
        }
    }

    private fun performLogin(url: String, user: String?, pass: String?, isAutoLogin: Boolean) {
        if (!isAutoLogin) {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.isEnabled = false
        }

        opdsManager.opdsUrl = url
        opdsManager.username = user
        opdsManager.password = pass
        opdsManager.refreshApi()

        lifecycleScope.launch {
            try {
                val feed = opdsManager.getFeed(url)
                if (feed != null) {
                    findNavController().navigate(R.id.action_login_to_library)
                } else if (!isAutoLogin) {
                    Toast.makeText(context, "Failed to connect to server", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                if (!isAutoLogin) {
                    Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                }
            } finally {
                if (!isAutoLogin) {
                    binding.progressBar.visibility = View.GONE
                    binding.buttonLogin.isEnabled = true
                }
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
