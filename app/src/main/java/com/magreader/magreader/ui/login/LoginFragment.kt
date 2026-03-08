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
        binding.editServerUrl.setText(opdsManager.opdsUrl)
        binding.editUsername.setText(opdsManager.username)
        binding.editPassword.setText(opdsManager.password)

        binding.buttonLogin.setOnClickListener {
            val url = binding.editServerUrl.text.toString().trim()
            val user = binding.editUsername.text.toString().trim()
            val pass = binding.editPassword.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(context, "Please enter OPDS URL", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.isEnabled = false

            opdsManager.opdsUrl = url
            opdsManager.username = user
            opdsManager.password = pass
            opdsManager.refreshApi()

            lifecycleScope.launch {
                try {
                    val api = opdsManager.api
                    if (api != null) {
                        // Attempt to fetch the root feed to verify connection
                        api.getFeed(url)
                        findNavController().navigate(R.id.action_login_to_library)
                    } else {
                        Toast.makeText(context, "Failed to initialize API", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(context, "Connection failed: ${e.message}", Toast.LENGTH_LONG).show()
                } finally {
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
