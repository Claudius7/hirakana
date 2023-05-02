package com.mrastudios.hirakana.ui.destinations.quiz.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.view_related.CustomButtonOnTouchListener;

/**
 * A {@link DialogFragment} that shows the differences between the user's incorrect answer
 * and the correct one.
 */
public final class AnswerInfoDialog extends DialogFragment implements View.OnClickListener
{
    public static String TAG = "AnswerInfoDialog777";
    private CustomButtonOnTouchListener onTouchListener;

    @NonNull
    public static AnswerInfoDialog newInstance(@NonNull String userAnswer,
                                               @NonNull String userAnswerGuessForm,
                                               @NonNull String correctAnswer,
                                               @NonNull String correctAnswerGuessForm)
    {
        AnswerInfoDialog fragment = new AnswerInfoDialog();

        Bundle args = new Bundle();
        args.putString("USER_ANSWER", userAnswer);
        args.putString("USER_ANSWER_GUESS_FORM", userAnswerGuessForm);
        args.putString("CORRECT_ANSWER", correctAnswer);
        args.putString("CORRECT_ANSWER_GUESS_FORM", correctAnswerGuessForm);
        fragment.setArguments(args);
        return fragment;
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        View dialogLayout = LayoutInflater.from(getContext()).inflate(R.layout.dialog_answer_info, null);
        dialogLayout.findViewById(R.id.dialog_answerInfo_ok).setOnClickListener(this);
        dialogLayout.findViewById(R.id.dialog_answerInfo_ok).setOnTouchListener(onTouchListener);

        Bundle args = requireArguments();
        String userAnswer = args.getString("USER_ANSWER");
        String userAnswerGuessForm = args.getString("USER_ANSWER_GUESS_FORM");
        String correctAnswer = args.getString("CORRECT_ANSWER");
        String correctAnswerGuessForm = args.getString("CORRECT_ANSWER_GUESS_FORM");

        TextView userAnswerTV = dialogLayout.findViewById(R.id.userAnswerAndTranslation);
        TextView correctAnswerTV = dialogLayout.findViewById(R.id.correctAnswerAndTranslation);
        userAnswerTV.setText(requireContext().getString(
                R.string.textView_quiz_reveal_answer, userAnswer, userAnswerGuessForm));
        correctAnswerTV.setText(requireContext().getString(
                R.string.textView_quiz_reveal_answer, correctAnswer, correctAnswerGuessForm));

        AlertDialog dialog = new AlertDialog.Builder(getContext()).setView(dialogLayout).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onClick(View view) { dismiss(); }

    public void setOnTouchListener(CustomButtonOnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }
}
