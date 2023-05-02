package com.mrastudios.hirakana.ui.destinations.main_title.dialogs;

import android.app.AlertDialog;
import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mrastudios.hirakana.R;

public final class AppInfoDialog extends DialogFragment
{
    public static final String TAG = "AppInfoDialogTag";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new AlertDialog.Builder(getActivity())
                .setTitle(R.string.dialog_main_title)
                .setMessage(R.string.dialog_main_about)
                .setNeutralButton(R.string.dialog_ok, (dialogInterface, i) -> {})
                .create();
    }
}
