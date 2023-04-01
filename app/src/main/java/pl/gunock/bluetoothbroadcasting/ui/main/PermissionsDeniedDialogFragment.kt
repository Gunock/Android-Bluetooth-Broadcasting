package pl.gunock.bluetoothbroadcasting.ui.main

import android.app.Dialog
import android.content.DialogInterface
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pl.gunock.bluetoothbroadcasting.R
import kotlin.system.exitProcess

class PermissionsDeniedDialogFragment(
    private val onDismissCallback: () -> Unit
) : DialogFragment() {
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

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissCallback.invoke()
    }

    private fun goToSettings() {
        val fragmentActivity = requireActivity()
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        val uri: Uri = Uri.fromParts("package", fragmentActivity.packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    private fun exitApp() {
        activity?.finish()
        exitProcess(0)
    }
}