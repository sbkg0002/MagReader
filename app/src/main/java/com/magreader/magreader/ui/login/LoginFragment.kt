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

            performLogin(urlInput, user, pass, isAutoLogin = false)
        }

        binding.buttonOffline.setOnClickListener {
            val bundle = Bundle().apply {
                putBoolean("isOfflineMode", true)
            }
            findNavController().navigate(R.id.action_login_to_library, bundle)
        }
    }

    private fun performLogin(urlInput: String, user: String?, pass: String?, isAutoLogin: Boolean) {
        if (!isAutoLogin) {
            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.isEnabled = false
        }

        lifecycleScope.launch {
            val urlsToTry = mutableListOf<String>()
            if (!urlInput.startsWith("http://") && !urlInput.startsWith("https://")) {
                urlsToTry.add("http://$urlInput")
                urlsToTry.add("https://$urlInput")
            } else {
                urlsToTry.add(urlInput)
            }

            var success = false
            var lastException: Exception? = null

            for (baseUrl in urlsToTry) {
                var url = baseUrl
                // Auto-append suffix if missing
                if (!url.contains("/api/v1/opds")) {
                    url = if (url.endsWith("/")) {
                        "${url}api/v1/opds"
                    } else {
                        "$url/api/v1/opds"
                    }
                }

                opdsManager.opdsUrl = url
                opdsManager.username = user
                opdsManager.password = pass
                opdsManager.refreshApi()

                try {
                    val feed = opdsManager.getFeed(url)
                    if (feed != null) {
                        success = true
                        if (!isAutoLogin) {
                            binding.editServerUrl.setText(url)
                        }
                        findNavController().navigate(R.id.action_login_to_library)
                        break
                    }
                } catch (e: Exception) {
                    lastException = e
                }
            }

            if (!success && !isAutoLogin) {
                val errorMsg = lastException?.message ?: "Failed to connect to server"
                Toast.makeText(context, "Connection failed: $errorMsg", Toast.LENGTH_LONG).show()
            }

            if (!isAutoLogin) {
                binding.progressBar.visibility = View.GONE
                binding.buttonLogin.isEnabled = true
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
