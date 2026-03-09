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

        binding.editDataLocation.setText(opdsManager.dataLocation)

        binding.buttonSaveSettings.setOnClickListener {
            val newLocation = binding.editDataLocation.text.toString().trim()
            if (newLocation.isNotEmpty()) {
                opdsManager.dataLocation = newLocation
                Toast.makeText(context, "Settings saved", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(context, "Location cannot be empty", Toast.LENGTH_SHORT).show()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}
