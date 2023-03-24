package pl.gunock.bluetoothexample.ui.main

import android.app.Dialog
import android.os.Bundle
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.FragmentManager
import pl.gunock.bluetoothexample.R
import kotlin.system.exitProcess

class PermissionsRequestDialogFragment(
    private val onPermissionRequestResult: (() -> Unit)
) : DialogFragment() {
    companion object {
        const val TAG = "PermissionsRequestDialogFragment"
    }

    init {
        isCancelable = false
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val fragmentActivity = requireActivity()


        return AlertDialog.Builder(fragmentActivity)
            .setMessage(R.string.dialog_fragment_permissions_request_message)
            .setPositiveButton(R.string.dialog_fragment_permissions_request_proceed) { _, _ ->
                onPermissionRequestResult.invoke()
            }
            .setNegativeButton(R.string.dialog_fragment_permission_close_app) { _, _ -> exitApp() }
            .create()
    }

    fun show(manager: FragmentManager) {
        super.show(manager, TAG)
    }

    private fun exitApp() {
        activity?.finish()
        exitProcess(0)
    }
}