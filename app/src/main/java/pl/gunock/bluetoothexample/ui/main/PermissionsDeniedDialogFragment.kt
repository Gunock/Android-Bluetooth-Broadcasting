package pl.gunock.bluetoothexample.ui.main

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pl.gunock.bluetoothexample.R
import kotlin.system.exitProcess

class PermissionsDeniedDialogFragment : DialogFragment() {
    companion object {
        const val TAG = "PermissionsDeniedDialogFragment"
    }

    init {
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()
        return AlertDialog.Builder(fragmentActivity)
            .setMessage(R.string.dialog_fragment_permissions_denied_message)
            .setPositiveButton(R.string.dialog_fragment_permissions_denied_go_to_settings) { _, _ -> goToSettings() }
            .setNegativeButton(R.string.dialog_fragment_permission_close_app) { _, _ -> exitApp() }
            .create()
    }

    fun show(manager: FragmentManager) {
        super.show(manager, TAG)
    }

    private fun goToSettings() {
        val fragmentActivity = requireActivity()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", fragmentActivity.packageName, null)
        intent.data = uri
        startActivity(intent)
        // TODO: Check permissions after returing from the settings
    }

    private fun exitApp() {
        activity?.finish()
        exitProcess(0)
    }
}