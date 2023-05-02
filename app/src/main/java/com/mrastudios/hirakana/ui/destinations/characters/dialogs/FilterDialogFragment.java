package com.mrastudios.hirakana.ui.destinations.characters.dialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.Japanese;

import java.io.Serializable;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;

public final class FilterDialogFragment extends DialogFragment
{
    public static final String TAG = "FilterDialogFragment777";

    private static final String KEY_FILTERS = "FilterValue_";
    private static final String KEY_FILTERS_MODIFY = "FilterBeforeChangesValue_";

    private OnApplyFilterListener listener;

    public interface OnApplyFilterListener {
        void onApplyFilter(@NonNull Map<Japanese.Type, Boolean> filters);
    }

    @NonNull
    public static FilterDialogFragment newInstance(@NonNull Map<Japanese.Type, Boolean> filters)
    {
        FilterDialogFragment dialogFragment = new FilterDialogFragment();
        Bundle args = new Bundle();
        args.putSerializable(KEY_FILTERS, (Serializable) filters);
        args.putSerializable(KEY_FILTERS_MODIFY, (Serializable) new EnumMap<>(filters));

        dialogFragment.setArguments(args);
        return dialogFragment;
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Map<Japanese.Type, Boolean> modifiedFilters = (EnumMap<Japanese.Type, Boolean>)
                requireArguments().getSerializable(KEY_FILTERS_MODIFY);
        Japanese.Type[] types = Japanese.Type.values();
        boolean[] filterValues = new boolean[types.length];
        for (int i = 0; i < types.length; i++) {
            filterValues[i] = Objects.requireNonNull(modifiedFilters.get(types[i]));
        }

        return new AlertDialog.Builder(requireActivity())
                .setTitle(R.string.dialog_characters_filter_title)
                .setIcon(R.drawable.ic_menu_filter)
                .setMultiChoiceItems(R.array.dialog_character_filter_items, filterValues,
                        (dialogInterface, index, isChecked) -> {
                            String[] typesString = getResources().getStringArray(R.array.dialog_character_filter_items);
                            for(Japanese.Type type : types) {
                                if(typesString[index].equalsIgnoreCase(type.name())) {
                                    modifiedFilters.put(type, isChecked);
                                    break;
                                }
                            }
                        }).setPositiveButton(R.string.dialog_apply, (dialogInterface, i) -> {
                            if (listener != null) {
                                Map<Japanese.Type, Boolean> originalMap =
                                        (Map<Japanese.Type, Boolean>) requireArguments()
                                                .getSerializable(KEY_FILTERS);
                                originalMap.putAll(modifiedFilters);
                                listener.onApplyFilter(originalMap);
                            }
                        }).setNegativeButton(R.string.dialog_cancel, (dialogInterface, i) -> {
                        }).create();
    }

    public void setOnApplyFilterListener(@NonNull OnApplyFilterListener listener) {
        this.listener = listener;
    }
}
