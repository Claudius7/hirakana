package com.mrastudios.hirakana.ui.destinations.statistics;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;
import androidx.savedstate.SavedStateRegistryOwner;

import com.mrastudios.hirakana.data.User;

public final class StatisticsViewModel extends ViewModel
{
    private final User user;
    private final MutableLiveData<CharacterStatsAdapter> adapterLiveData;
    private final MutableLiveData<StatisticsActivity.ChipState> alphabeticalChipState;
    private final MutableLiveData<StatisticsActivity.ChipState> successRateChipState;

    public static final class Factory extends AbstractSavedStateViewModelFactory
    {
        private final StatisticsActivity context;

        public Factory(@NonNull SavedStateRegistryOwner owner, @Nullable Bundle defaultArgs) {
            super(owner, defaultArgs);
            context = (StatisticsActivity) owner;
        }

        @SuppressWarnings("unchecked")
        @NonNull
        @Override
        protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                                                 @NonNull SavedStateHandle handle)
        {
            return (T) new StatisticsViewModel(context, handle);
        }
    }

    private StatisticsViewModel(@NonNull StatisticsActivity context,
                                @NonNull SavedStateHandle savedStateHandle)
    {
        this.user = User.getInstance(context);

        Bundle savedState = savedStateHandle.get("SAVED_STATE");
        if(savedState != null) {
            adapterLiveData = new MutableLiveData<>((CharacterStatsAdapter) savedState.get("ADAPTER"));
            alphabeticalChipState = new MutableLiveData<>((StatisticsActivity.ChipState)
                    savedState.getSerializable("CHIP_STATE_ALPHABETICAL"));
            successRateChipState = new MutableLiveData<>((StatisticsActivity.ChipState)
                    savedState.getSerializable("CHIP_STATE_SUCCESS_RATE"));
        } else {
            adapterLiveData = new MutableLiveData<>();
            alphabeticalChipState = new MutableLiveData<>(StatisticsActivity.ChipState.DEACTIVATED);
            successRateChipState = new MutableLiveData<>(StatisticsActivity.ChipState.DEACTIVATED);
        }

        savedStateHandle.setSavedStateProvider("SAVED_STATE", () -> {
            Bundle save = new Bundle();
            save.putSerializable("ADAPTER", adapterLiveData.getValue());
            save.putSerializable("CHIP_STATE_ALPHABETICAL", alphabeticalChipState.getValue());
            save.putSerializable("CHIP_STATE_SUCCESS_RATE", successRateChipState.getValue());
            return save;
        });
    }

    User getUser() {
        return user;
    }

    MutableLiveData<CharacterStatsAdapter> getAdapterLiveData() {
        return adapterLiveData;
    }

    MutableLiveData<StatisticsActivity.ChipState> getAlphabeticalChipState() {
        return alphabeticalChipState;
    }

    MutableLiveData<StatisticsActivity.ChipState> getSuccessRateChipState() {
        return successRateChipState;
    }
}