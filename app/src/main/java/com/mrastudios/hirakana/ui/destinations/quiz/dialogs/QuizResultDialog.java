package com.mrastudios.hirakana.ui.destinations.quiz.dialogs;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.Rect;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.JapaneseQuizGenerator;

import java.util.Objects;

public final class QuizResultDialog extends DialogFragment implements View.OnClickListener
{
    public static String TAG = "QuizDialogTag777";

    @NonNull
    public static QuizResultDialog newInstance(JapaneseQuizGenerator randomizer)
    {
        Bundle args = new Bundle();
        args.putSerializable("CHOICE_RANDOMIZER", randomizer);

        QuizResultDialog fragment = new QuizResultDialog();
        fragment.setArguments(args);
        return fragment;
    }

    private static final class QuizResultAdapter extends RecyclerView.Adapter<QuizResultAdapter.ViewHolder>
    {
        private final JapaneseQuizGenerator quizGenerator;
        private Context context;

        private QuizResultAdapter(@NonNull JapaneseQuizGenerator quizGenerator) { this.quizGenerator = quizGenerator; }

        private static final class ViewHolder extends RecyclerView.ViewHolder
        {
            private final TextView questionNumber, guessType, characterToGuess,
                    characterType, userAnswer, result;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                questionNumber = itemView.findViewById(R.id.dialog_questionNumber);
                guessType = itemView.findViewById(R.id.guessTypeValue);
                characterToGuess = itemView.findViewById(R.id.characterToGuess);
                characterType = itemView.findViewById(R.id.characterType);
                userAnswer = itemView.findViewById(R.id.userAnswer);
                result = itemView.findViewById(R.id.result);
            }
        }

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from((context = parent.getContext()));
            View view = inflater.inflate(R.layout.item_quiz_result, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            holder.questionNumber.setText(context.getString(
                    R.string.dialog_quiz_question_number , position + 1));
            JapaneseQuizGenerator.GuessMode guessMode =
                    quizGenerator.getGuessModeHistory().get(position);
            holder.guessType.setText(context.getResources().getStringArray(
                    R.array.spinner_guess_type)[guessMode.ordinal()]);
            holder.characterToGuess.setText(quizGenerator.getCharactersGuessFormsHistory().get(position));
            holder.characterType.setText(quizGenerator.getCharactersHistory().get(
                    position).getCharacter().getType().toString());

            String correctAnswer = quizGenerator.getCorrectAnswersHistory().get(position);
            String answer = quizGenerator.getUserAnswersHistory().get(position);
            if(answer == null) {
                holder.userAnswer.setText(R.string.dialog_quiz_no_answer);
                holder.result.setText(R.string.dialog_quiz_result_timed_out);
            } else {
                holder.userAnswer.setText(answer);
                holder.result.setText(answer.equals(correctAnswer) ? R.string.dialog_quiz_result_correct : R.string.dialog_quiz_result_incorrect);
                int colorRes = context.getResources().getColor(answer.equals(correctAnswer) ?
                        android.R.color.holo_green_light : android.R.color.holo_red_dark, null);
                holder.result.setTextColor(colorRes);
            }
        }

        @Override
        public int getItemCount() {
            return quizGenerator.getCharactersHistory().size();
        }
    }

    @SuppressLint("InflateParams")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        LayoutInflater inflater = LayoutInflater.from(requireContext());
        View dialogLayout = inflater.inflate(R.layout.dialog_quiz_ended, null);
        dialogLayout.findViewById(R.id.quizEndDismissButton).setOnClickListener(this);

        QuizResultAdapter adapter = new QuizResultAdapter(
                (JapaneseQuizGenerator) requireArguments().getSerializable("CHOICE_RANDOMIZER"));

        RecyclerView recyclerView = dialogLayout.findViewById(R.id.resultItems);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext(),
                RecyclerView.HORIZONTAL, false));
        recyclerView.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                       @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
            {
                int choicePos = parent.getChildAdapterPosition(view);
                int itemCount = Objects.requireNonNull(parent.getAdapter()).getItemCount();
                if(choicePos == 0) outRect.left = 5;
                if(choicePos != itemCount - 1) outRect.right = 5;
            }
        });
        recyclerView.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(requireContext()).setView(dialogLayout).create();
        dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        return dialog;
    }

    @Override
    public void onClick(View view) {
        dismiss();
    }
}
