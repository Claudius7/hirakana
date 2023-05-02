package com.mrastudios.hirakana.ui.destinations.characters;

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import java.util.Objects;

public final class CharactersViewModel extends ViewModel
{
    private final MutableLiveData<String> queryLiveData;
    private final MutableLiveData<CharactersAdapter> adapterLiveData;
    private final MutableLiveData<Boolean> isUserTypingLiveData;

    public CharactersViewModel(@NonNull SavedStateHandle savedStateHandle) {
        Bundle recoveredSavedState = savedStateHandle.get("KEY");
        if(recoveredSavedState != null) {
            queryLiveData = new MutableLiveData<>(recoveredSavedState.getString("QUERY"));
            adapterLiveData = new MutableLiveData<> ((CharactersAdapter)
                    recoveredSavedState.getSerializable("ADAPTER"));
            isUserTypingLiveData = new MutableLiveData<>(
                    recoveredSavedState.getBoolean("IS_USER_TYPING"));
        } else {
            queryLiveData = new MutableLiveData<>("");
            adapterLiveData = new MutableLiveData<>();
            isUserTypingLiveData = new MutableLiveData<>(Boolean.FALSE);
        }

        savedStateHandle.setSavedStateProvider("KEY", () -> {
            Bundle savedState = new Bundle();
            savedState.putString("QUERY", queryLiveData.getValue());
            savedState.putSerializable("ADAPTER", adapterLiveData.getValue());
            savedState.putBoolean("IS_USER_TYPING", Objects.requireNonNull(
                    isUserTypingLiveData.getValue()));
            return savedState;
        });
    }

    @NonNull
    MutableLiveData<String> getQueryLiveData() {
        return queryLiveData;
    }

    @NonNull
    MutableLiveData<CharactersAdapter> getAdapterLiveData() {
        return adapterLiveData;
    }

    @NonNull
    MutableLiveData<Boolean> getIsUserTypingLiveData() {
        return isUserTypingLiveData;
    }
}