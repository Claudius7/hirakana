package com.mrastudios.hirakana.ui.destinations.quiz;

import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.AbstractSavedStateViewModelFactory;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.SavedStateHandle;
import androidx.lifecycle.ViewModel;

import com.mrastudios.hirakana.data.User;
import com.mrastudios.hirakana.domain.JapaneseQuizGenerator;

import java.io.IOException;
import java.util.Date;
import java.util.Objects;

public final class QuizViewModel extends ViewModel
{
    private static final String KEY_PROVIDER = "QuizActivity777";

    private final LiveData<User> userLiveData;
    private final MutableLiveData<Boolean> isCustomizingLiveData;
    private final MutableLiveData<JapaneseQuizGenerator> japaneseQuizGeneratorLiveData;
    private final MutableLiveData<Integer> questionCountLiveData;
    private final MutableLiveData<Integer> timeLeftPerQuestionLiveData;
    private final MutableLiveData<Integer> timerPerQuestionLiveData;
    private final MutableLiveData<Float> timerBarRotationLiveData;
    @SuppressWarnings("unchecked")
    public static class QuizActivityViewModelFactory extends AbstractSavedStateViewModelFactory
    {
        private final QuizActivity context;

        public QuizActivityViewModelFactory(@NonNull QuizActivity owner, @Nullable Bundle defaultArgs) {
            super(owner, defaultArgs);
            context = owner;
        }

        @NonNull
        @Override
        protected <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass,
                                                 @NonNull SavedStateHandle handle)
        {
            return (T) new QuizViewModel(handle, context);
        }
    }

    private QuizViewModel(@NonNull SavedStateHandle savedStateHandle, QuizActivity quizActivity) {
        userLiveData = new MutableLiveData<>(User.getInstance(quizActivity));

        Bundle savedState = savedStateHandle.get(KEY_PROVIDER);
        if(savedState != null && !savedState.isEmpty()) {
            isCustomizingLiveData = new MutableLiveData<>(savedState.getBoolean("IS_CUSTOMIZING"));
            japaneseQuizGeneratorLiveData = new MutableLiveData<>(
                    (JapaneseQuizGenerator) savedState.getSerializable("JAPANESE_QUIZ_GENERATOR"));
            timerPerQuestionLiveData = new MutableLiveData<>(
                    (Integer) savedState.getSerializable("TIME_PER_QUESTION"));
            int timeLeft = savedState.getInt("TIME_LEFT_PER_QUESTION");
            Date systemDeathStart = (Date) savedState.getSerializable("SYSTEM_DEATH_START");
            if(systemDeathStart != null) timeLeft -= ((int)
                    (new Date().getTime() - systemDeathStart.getTime()));
            timeLeftPerQuestionLiveData = new MutableLiveData<>(Math.max(timeLeft, 0));
            questionCountLiveData = new MutableLiveData<>(savedState.getInt("QUESTION_COUNT"));
            timerBarRotationLiveData = new MutableLiveData<>(
                    (Float) savedState.getSerializable("TIMER_BAR_ROTATION"));
        } else {
            isCustomizingLiveData = new MutableLiveData<>(true);
            japaneseQuizGeneratorLiveData = new MutableLiveData<>();
            timerPerQuestionLiveData = new MutableLiveData<>();
            timeLeftPerQuestionLiveData = new MutableLiveData<>();
            questionCountLiveData = new MutableLiveData<>();
            timerBarRotationLiveData = new MutableLiveData<>();
            Objects.requireNonNull(userLiveData.getValue())
                    .getGuessableJapaneseCharacters().invalidateAllQuizSuccesses();
        }

        savedStateHandle.setSavedStateProvider(KEY_PROVIDER, () -> {
            Bundle savedUiState = new Bundle();
            JapaneseQuizGenerator generator = japaneseQuizGeneratorLiveData.getValue();
            if(generator != null && timerPerQuestionLiveData.getValue() != null &&
                    generator.getUserAnswerIndex() == -1)
            {
                savedUiState.putSerializable("SYSTEM_DEATH_START", new Date());
            }
            savedUiState.putSerializable("IS_CUSTOMIZING", isCustomizingLiveData.getValue());
            savedUiState.putSerializable("JAPANESE_QUIZ_GENERATOR", japaneseQuizGeneratorLiveData.getValue());
            savedUiState.putSerializable("TIME_PER_QUESTION", timerPerQuestionLiveData.getValue());
            savedUiState.putSerializable("QUESTION_COUNT", questionCountLiveData.getValue());
            savedUiState.putSerializable("TIME_LEFT_PER_QUESTION", timeLeftPerQuestionLiveData.getValue());
            savedUiState.putSerializable("TIMER_BAR_ROTATION", timerBarRotationLiveData.getValue());
            return savedUiState;
        });
    }

    @Override
    protected void onCleared() {
        try {
            Objects.requireNonNull(userLiveData.getValue()).saveUser();
        } catch (IOException ioException) {
            Log.e(getClass().getSimpleName(), Objects.requireNonNull(ioException.getMessage()));
        }
        super.onCleared();
    }

    // Standard getters and setters below
    public LiveData<User> getUserLiveData() {
        return userLiveData;
    }

    public MutableLiveData<Boolean> getIsCustomizingLiveData() {
        return isCustomizingLiveData;
    }

    public MutableLiveData<JapaneseQuizGenerator> getJapaneseQuizGeneratorLiveData() {
        return japaneseQuizGeneratorLiveData;
    }

    public MutableLiveData<Integer> getQuestionCountLiveData() {
        return questionCountLiveData;
    }

    public MutableLiveData<Integer> getTimeLeftPerQuestionLiveData() {
        return timeLeftPerQuestionLiveData;
    }

    public MutableLiveData<Integer> getTimerPerQuestionLiveData() {
        return timerPerQuestionLiveData;
    }

    public MutableLiveData<Float> getTimerBarRotationLiveData() {
        return timerBarRotationLiveData;
    }
}
