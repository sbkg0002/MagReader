package com.magreader.magreader.ui.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.magreader.magreader.data.OpdsManager
import com.magreader.magreader.databinding.FragmentSettingsBinding

class SettingsFragment : Fragment() {

    private var _binding: FragmentSettingsBinding? = null
    private val binding get() = _binding!!
    private lateinit var opdsManager: OpdsManager

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentSettingsBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        opdsManager = OpdsManager(requireContext())

        // Load current settings
        binding.editOpdsUrl.setText(opdsManager.opdsUrl)
        binding.editUsername.setText(opdsManager.username)
        binding.editPassword.setText(opdsManager.password)
        binding.editDataLocation.setText(opdsManager.dataLocation)

        binding.buttonSaveSettings.setOnClickListener {
            val url = binding.editOpdsUrl.text.toString().trim()
            val user = binding.editUsername.text.toString().trim()
            val pass = binding.editPassword.text.toString().trim()
            val location = binding.editDataLocation.text.toString().trim()

            if (url.isEmpty()) {
                Toast.makeText(context, "Server URL cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            
            if (location.isEmpty()) {
                Toast.makeText(context, "Data location cannot be empty", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            opdsManager.opdsUrl = url
            opdsManager.username = user
            opdsManager.password = pass
            opdsManager.dataLocation = location
            
            // Refresh API client with new credentials/URL
            opdsManager.refreshApi()
            
            Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
