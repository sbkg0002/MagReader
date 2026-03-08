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
import com.magreader.magreader.data.KomgaManager
import com.magreader.magreader.databinding.FragmentLoginBinding
import kotlinx.coroutines.launch

class LoginFragment : Fragment() {

    private var _binding: FragmentLoginBinding? = null
    private val binding get() = _binding!!
    private lateinit var komgaManager: KomgaManager

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
        komgaManager = KomgaManager(requireContext())

        binding.buttonLogin.setOnClickListener {
            val url = binding.editServerUrl.text.toString().trim()
            val user = binding.editUsername.text.toString().trim()
            val pass = binding.editPassword.text.toString().trim()

            if (url.isEmpty() || user.isEmpty() || pass.isEmpty()) {
                Toast.makeText(context, "Please fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            binding.progressBar.visibility = View.VISIBLE
            binding.buttonLogin.isEnabled = false

            // Use the new login method which ensures credentials are used immediately
            komgaManager.login(url, user, pass)

            lifecycleScope.launch {
                try {
                    val api = komgaManager.api
                    if (api != null) {
                        val response = api.authenticate()
                        if (response.isSuccessful) {
                            findNavController().navigate(R.id.action_login_to_library)
                        } else {
                            val errorMsg = when (response.code()) {
                                401 -> "Invalid credentials"
                                403 -> "403 Forbidden: Access denied. Check user roles."
                                else -> "Error: ${response.code()}"
                            }
                            Toast.makeText(context, errorMsg, Toast.LENGTH_SHORT).show()
                            komgaManager.logout()
                        }
                    } else {
                        Toast.makeText(context, "Failed to initialize API", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    val message = e.message ?: "Unknown error"
                    Toast.makeText(context, "Login failed: $message", Toast.LENGTH_LONG).show()
                    komgaManager.logout()
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
