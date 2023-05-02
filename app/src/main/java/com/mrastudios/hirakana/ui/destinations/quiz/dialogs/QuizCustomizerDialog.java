package com.mrastudios.hirakana.ui.destinations.quiz.dialogs;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.JapaneseQuizGenerator;
import com.mrastudios.hirakana.ui.destinations.quiz.QuizActivity;

public final class QuizCustomizerDialog extends DialogFragment implements View.OnClickListener,
        RadioGroup.OnCheckedChangeListener
{
    public static final String TAG = "QuizCustomizerDialog777";
    private static final String PREFERENCE_DIFFICULTY = "SelectedDifficulty777";

    private SharedPreferences preferences;
    private View dialogLayout;

    private RadioGroup difficultyRadioGroup;

    private CheckBox checkBoxHiragana;
    private CheckBox checkBoxKatakana;
    private CheckBox checkBoxKanji;

    private Spinner spinnerQuestionCount;
    private Spinner spinnerChoiceCount;
    private Spinner spinnerTimer;
    private Spinner spinnerGuessMode;

    private OnApplyDifficultyListener onApplyDifficultyListener;
    private OnDialogCancelListener onDialogCancelListener;

    public interface OnApplyDifficultyListener {
        void onApplyDifficulty(@NonNull JapaneseQuizGenerator.ChoiceType choiceType,
                               @NonNull JapaneseQuizGenerator.GuessMode guessMode,
                               int choicesCount,
                               int questionCount,
                               int timerPerQuestion);
    }
    public interface OnDialogCancelListener { void onDialogCancel(); }

    @NonNull
    public static QuizCustomizerDialog newInstance(boolean isCancellable) {
        Bundle args = new Bundle();
        args.putBoolean("IS_CANCELLABLE", isCancellable);

        QuizCustomizerDialog fragment = new QuizCustomizerDialog();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        QuizActivity quizActivity = (QuizActivity) context;
        preferences = quizActivity.getPreferences(Context.MODE_PRIVATE);
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(getContext());
        dialogLayout = inflater.inflate(R.layout.dialog_quiz_customizer, null);

        difficultyRadioGroup = dialogLayout.findViewById(R.id.radioGrp_difficulties);
        checkBoxHiragana = dialogLayout.findViewById(R.id.checkBoxHiragana);
        checkBoxKatakana = dialogLayout.findViewById(R.id.checkBoxKatakana);
        checkBoxKanji = dialogLayout.findViewById(R.id.checkBoxKanji);
        spinnerChoiceCount = dialogLayout.findViewById(R.id.spinner_choice_count);
        spinnerQuestionCount = dialogLayout.findViewById(R.id.spinner_quiz_count);
        spinnerGuessMode = dialogLayout.findViewById(R.id.spinner_guess_type);
        spinnerTimer = dialogLayout.findViewById(R.id.spinner_quiz_timer);

        checkBoxKanji.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if(!isChecked && spinnerGuessMode.getSelectedItemPosition() == 2) {
                spinnerGuessMode.setSelection(0);
            }
        });
        difficultyRadioGroup.setOnCheckedChangeListener(this);
        dialogLayout.findViewById(R.id.dialog_button_apply).setOnClickListener(this);

        spinnerChoiceCount.setEnabled(false);
        spinnerQuestionCount.setEnabled(false);
        spinnerTimer.setEnabled(false);
        spinnerGuessMode.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                int selection = position;
                if (position == 2) { // if equals ChoiceRandomizer.GuessMode.GUESS_KANJI_MEANING
                    if(!checkBoxKanji.isChecked()) {
                        Toast.makeText(requireContext(), R.string.toast_quiz_toggle_kanji_on,
                                Toast.LENGTH_SHORT).show();
                    } else if (checkBoxHiragana.isChecked() || checkBoxKatakana.isChecked()){
                        Toast.makeText(requireContext(), R.string.toast_quiz_kanjis_only,
                                Toast.LENGTH_SHORT).show();
                    }
                    selection = 0;
                }
                spinnerGuessMode.setSelection(selection);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        restoreDifficultyPreferences(preferences);

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogLayout).create();
        // The line below is said to remove some of unwanted color on the edges. Proven to work.
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        dialog.setCanceledOnTouchOutside(requireArguments().getBoolean("IS_CANCELLABLE"));
        return dialog;
    }

    @Override
    public void onCancel(@NonNull DialogInterface dialog) {
        if(onDialogCancelListener == null) {
            super.onCancel(dialog);
        } else {
            onDialogCancelListener.onDialogCancel();
        }
    }

    private void restoreDifficultyPreferences(@NonNull SharedPreferences sharedPref) {
        int index = sharedPref.getInt(PREFERENCE_DIFFICULTY, -1);
        if(index != -1) {
            int selectedRadioId = difficultyRadioGroup.getChildAt(index).getId();
            if(selectedRadioId == R.id.radioBtn_custom) {
                restoreCustomDifficultyPreferences(sharedPref);
            } else {
                difficultyRadioGroup.check(selectedRadioId);
            }
        }
    }

    private void restoreCustomDifficultyPreferences(@NonNull SharedPreferences sharedPref) {
        difficultyRadioGroup.check(R.id.radioBtn_custom);
        spinnerChoiceCount.setSelection(sharedPref.getInt("ChoiceCount", 0));
        spinnerQuestionCount.setSelection(sharedPref.getInt("QuestionCount", 0));
        spinnerTimer.setSelection(sharedPref.getInt("TimerCount", 0));
        checkBoxHiragana.setChecked(sharedPref.getBoolean("Hiragana", true));
        checkBoxKatakana.setChecked(sharedPref.getBoolean("Katakana", false));
        checkBoxKanji.setChecked(sharedPref.getBoolean("Kanji", false));
        spinnerGuessMode.setSelection(sharedPref.getInt("GuessType", 0));
    }

    @Override
    public void onClick(View view) {
        boolean isHiraganaOn = checkBoxHiragana.isChecked();
        boolean isKatakanaOn = checkBoxKatakana.isChecked();
        boolean isKanjiOn = checkBoxKanji.isChecked();
        if(!isHiraganaOn && !isKatakanaOn && !isKanjiOn) {
            Toast.makeText(getContext(), "Please check at least one type", Toast.LENGTH_SHORT).show();
            return;
        }

        int questionCount = Integer.parseInt((String) spinnerQuestionCount.getSelectedItem());
        int choicesCount = Integer.parseInt((String) spinnerChoiceCount.getSelectedItem());
        JapaneseQuizGenerator.GuessMode guessMode = JapaneseQuizGenerator.GuessMode
                .values()[spinnerGuessMode.getSelectedItemPosition()];
        int timerPerQuestion;
        try {
            timerPerQuestion = Integer.parseInt((String) spinnerTimer.getSelectedItem());
        } catch (NumberFormatException exception) {
            timerPerQuestion = 0;
        }

        JapaneseQuizGenerator.ChoiceType choiceType;
        if(isHiraganaOn && isKatakanaOn && isKanjiOn) {
            choiceType = JapaneseQuizGenerator.ChoiceType.ALL;
        } else if (isHiraganaOn) {
            if(!isKatakanaOn && !isKanjiOn) {
                choiceType = JapaneseQuizGenerator.ChoiceType.HIRAGANA_ONLY;
            } else {
                choiceType = isKatakanaOn ?
                        JapaneseQuizGenerator.ChoiceType.HIRAGANA_AND_KATAKANA :
                        JapaneseQuizGenerator.ChoiceType.HIRAGANA_AND_KANJI;
            }
        } else if (isKatakanaOn) {
            choiceType = isKanjiOn ?
                    JapaneseQuizGenerator.ChoiceType.KATAKANA_AND_KANJI :
                    JapaneseQuizGenerator.ChoiceType.KATAKANA_ONLY;
        } else {
            choiceType = JapaneseQuizGenerator.ChoiceType.KANJI_ONLY;
        }
        if(onApplyDifficultyListener != null) onApplyDifficultyListener.onApplyDifficulty(
                choiceType, guessMode, choicesCount, questionCount,  timerPerQuestion);

        if(difficultyRadioGroup.getCheckedRadioButtonId() == R.id.radioBtn_custom) {
            saveCustomDifficultyPreference();
        } else {
            View checkedDifficulty = dialogLayout.findViewById(difficultyRadioGroup.getCheckedRadioButtonId());
            preferences.edit().putInt(PREFERENCE_DIFFICULTY,
                            difficultyRadioGroup.indexOfChild(checkedDifficulty)).apply();
        }
        dismiss();
    }

    private void saveCustomDifficultyPreference() {
        int position = difficultyRadioGroup.indexOfChild(
                dialogLayout.findViewById(R.id.radioBtn_custom));
        preferences.edit()
                .putInt(PREFERENCE_DIFFICULTY, position)
                .putInt("ChoiceCount", spinnerChoiceCount.getSelectedItemPosition())
                .putInt("QuestionCount", spinnerQuestionCount.getSelectedItemPosition())
                .putInt("GuessType", spinnerGuessMode.getSelectedItemPosition())
                .putInt("TimerCount", spinnerTimer.getSelectedItemPosition())
                .putBoolean("Hiragana", checkBoxHiragana.isChecked())
                .putBoolean("Katakana", checkBoxKatakana.isChecked())
                .putBoolean("Kanji", checkBoxKanji.isChecked())
                .apply();
    }

    @Override
    public void onCheckedChanged(RadioGroup radioGroup, int checkedRadioBtnId) {
        boolean isCustom = checkedRadioBtnId == R.id.radioBtn_custom;
        checkBoxHiragana.setClickable(isCustom);
        checkBoxKatakana.setClickable(isCustom);
        checkBoxKanji.setClickable(isCustom);

        spinnerChoiceCount.setEnabled(isCustom);
        spinnerQuestionCount.setEnabled(isCustom);
        spinnerTimer.setEnabled(isCustom);

        if(isCustom) return;

        checkBoxHiragana.setChecked(true);
        checkBoxKatakana.setChecked(checkedRadioBtnId != R.id.radioBtn_easy);
        checkBoxKanji.setChecked(checkedRadioBtnId != R.id.radioBtn_easy &&
                checkedRadioBtnId != R.id.radioBtn_med);

        int selectionPosition = 0;
        if(checkedRadioBtnId == R.id.radioBtn_med) selectionPosition++;
        if(checkedRadioBtnId == R.id.radioBtn_hard) selectionPosition += 2;
        spinnerChoiceCount.setSelection(selectionPosition, true);
        spinnerQuestionCount.setSelection(selectionPosition, true);
        spinnerTimer.setSelection(selectionPosition, true);
    }

    public void setOnApplyDifficultyListener(OnApplyDifficultyListener onApplyDifficultyListener) {
        this.onApplyDifficultyListener = onApplyDifficultyListener;
    }

    public void setOnDialogCancelListener(OnDialogCancelListener onDialogCancelListener) {
        this.onDialogCancelListener = onDialogCancelListener;
    }
}