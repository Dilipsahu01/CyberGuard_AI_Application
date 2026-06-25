package com.example.ui

import android.os.Bundle
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import com.example.R

class PrivacyFragment : Fragment() {
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_privacy, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        // Show a simple dialog with privacy info
        AlertDialog.Builder(requireContext())
            .setTitle("Privacy Policy")
            .setMessage("We only transmit a SHA‑256 hash of your caller number together with risk scores. No raw phone numbers are stored or sent.")
            .setPositiveButton("OK") { d, _ -> d.dismiss() }
            .show()
    }
}
