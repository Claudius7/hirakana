package com.mrastudios.hirakana.ui.destinations.quiz;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.AudioAttributes;
import android.media.SoundPool;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;

import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.mrastudios.hirakana.domain.JapaneseQuizGenerator;
import com.mrastudios.hirakana.domain.GuessableJapaneseCharacter;
import com.mrastudios.hirakana.domain.GuessableJapaneseCharacters;
import com.mrastudios.hirakana.domain.view_related.CustomButtonOnTouchListener;
import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.ui.destinations.quiz.dialogs.AnswerInfoDialog;
import com.mrastudios.hirakana.ui.destinations.quiz.dialogs.QuizCustomizerDialog;
import com.mrastudios.hirakana.ui.destinations.quiz.dialogs.QuizResultDialog;
import com.mrastudios.hirakana.ui.services.BgmService;

import java.util.Date;
import java.util.Locale;
import java.util.Objects;

public final class QuizActivity extends AppCompatActivity
        implements QuizCustomizerDialog.OnApplyDifficultyListener,
        QuizCustomizerDialog.OnDialogCancelListener,
        QuizChoicesAdapter.OnChoiceClickListener
{
    private QuizViewModel viewModel;
    private RecyclerView recyclerView;
    private QuizChoicesAdapter adapter;
    private JapaneseQuizGenerator quizGenerator;

    private BgmService bgmService;
    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BgmService.BgmBinder binder = (BgmService.BgmBinder) service;
            bgmService = binder.getService();
            bgmService.startBgm(R.raw.bgm_default);
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private TextView questionNumberTV;
    private TextView characterToGuessTV;
    private TextView questionTextTV;
    private Button nextAndRetryButton;

    private ImageButton answerInfoButton;
    private Animation answerInfoAnimation;

    private SoundPool soundPool;
    public int sfxIdButtonPressed;
    public int sfxIdButtonReleased;

    private ImageView timerBar;
    private TextView timerCountTV;
    private Thread timerThread;
    private Animation timerBarAnimation;

    private Date onCreateStart;
    private int timeToDeduct;

    private boolean isExplicitExit;
    private boolean isToastShowing;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        onCreateStart = new Date();
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_quiz);

        setupObservers();
        initSfx();
        initViews();
        setupViews();
    }

    private void setupObservers() {
        viewModel = new ViewModelProvider(this,
                new QuizViewModel.QuizActivityViewModelFactory(this, new Bundle()))
                .get(QuizViewModel.class);

        viewModel.getIsCustomizingLiveData().observe(this, aBoolean -> {
            if(aBoolean) {
                QuizCustomizerDialog difficultyDialog = (QuizCustomizerDialog)
                        getSupportFragmentManager().findFragmentByTag(QuizCustomizerDialog.TAG);
                if(difficultyDialog == null) difficultyDialog =
                        QuizCustomizerDialog.newInstance(quizGenerator != null);
                difficultyDialog.setOnDialogCancelListener(QuizActivity.this);
                difficultyDialog.setOnApplyDifficultyListener(QuizActivity.this);
                if(!difficultyDialog.isAdded()) difficultyDialog.show(
                        getSupportFragmentManager(), QuizCustomizerDialog.TAG);
            }
        });

        viewModel.getJapaneseQuizGeneratorLiveData().observe(this, generator -> {
            if(generator == null) return;
            quizGenerator = generator;
            recyclerView.setLayoutManager(
                    quizGenerator.getChoicesCount() <= JapaneseQuizGenerator.MINIMUM_CHOICES ?
                            new LinearLayoutManager(this) {
                                @Override
                                public boolean canScrollVertically() {
                                    return false;
                                }
                            } :
                            new GridLayoutManager(this, 2) {
                                @Override
                                public boolean canScrollVertically() {
                                    return false;
                                }
                            });
        });

        viewModel.getTimerPerQuestionLiveData().observe(this, integer -> {
            timerBar.setVisibility(integer == null ? View.INVISIBLE : View.VISIBLE);
            timerCountTV.setVisibility(integer == null ? View.INVISIBLE : View.VISIBLE);
        });

        viewModel.getTimerBarRotationLiveData().observe(this, rotation ->
                timerBar.setRotation(rotation != null ? rotation : -90));

        viewModel.getQuestionCountLiveData().observe(this, integer -> {
            if (quizGenerator == null || integer == null) return;
            displayQuiz(Objects.requireNonNull(quizGenerator.getChoices()),
                    Objects.requireNonNull(quizGenerator.getCorrectChoiceGuessForm()),
                    quizGenerator.getGuessMode(),
                    quizGenerator.getCharactersHistory().size());
            if (viewModel.getTimerPerQuestionLiveData().getValue() == null) {
                int userAnswerIndex = quizGenerator.getUserAnswerIndex();
                if(userAnswerIndex != -1) showAnswerResult(userAnswerIndex);
            }
        });

        viewModel.getTimeLeftPerQuestionLiveData().observe(this, integer -> {
            if(integer == null) return;

            int timeInSeconds = (int) Math.ceil((double) integer / 1000);
            timerCountTV.setText(String.valueOf(timeInSeconds));

            int userAnswerIndex = quizGenerator.getUserAnswerIndex();
            if(timerThread == null) {
                if(userAnswerIndex == -1 && integer > 0) {
                    startTimer();
                    timerBar.startAnimation(timerBarAnimation);
                }
                if(integer == 0 || userAnswerIndex != -1) {
                    showAnswerResult(integer == 0 ? JapaneseQuizGenerator.NO_ANSWER : userAnswerIndex);
                }
            } else {
                if(userAnswerIndex == -1 || integer != 0) timerBar.startAnimation(timerBarAnimation);
            }
            timerBarAnimation.setDuration(1000);
        });
    }

    private void initSfx() {
        AudioAttributes at = new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build();
        soundPool = new SoundPool.Builder()
                .setAudioAttributes(at)
                .setMaxStreams(3)
                .build();
        sfxIdButtonPressed = soundPool.load(this, R.raw.button_pressed, 1);
        sfxIdButtonReleased = soundPool.load(this, R.raw.button_released, 2);
    }

    private void initViews() {
        timerBar = findViewById(R.id.timerBar);
        characterToGuessTV = findViewById(R.id.userAnswerAndTranslation);
        questionNumberTV = findViewById(R.id.questionNumber);
        timerCountTV = findViewById(R.id.timerCount);
        questionTextTV = findViewById(R.id.userAnswerText);
        nextAndRetryButton = findViewById(R.id.next_and_retry_button);
        answerInfoButton = findViewById(R.id.info_image_button);

        recyclerView = findViewById(R.id.recyclerView_choices);
        timerBarAnimation = AnimationUtils.loadAnimation(this, R.anim.rotate_360_1_second);
        answerInfoAnimation = AnimationUtils.loadAnimation(this, R.anim.zoom_in);
    }

    @SuppressLint("ClickableViewAccessibility")
    private void setupViews() {
        recyclerView.setItemAnimator(null);
        recyclerView.addItemDecoration(new QuizChoicesAdapter.ChoicesItemDecoration());


        CustomButtonOnTouchListener customOnTouchListener = new CustomButtonOnTouchListener(
                R.drawable.button_main_pressed, R.drawable.button_main_unpressed, 0);
        customOnTouchListener.setSoundEffects(soundPool, 0.3f, 0.3f,
                sfxIdButtonPressed, sfxIdButtonReleased);

        nextAndRetryButton.setOnTouchListener(customOnTouchListener);
        nextAndRetryButton.setOnClickListener(view -> {
            int questionCount = Objects.requireNonNull(viewModel.getQuestionCountLiveData().getValue());
            if(questionCount > 1) {
                quizGenerator.generateQuiz();
                viewModel.getTimerBarRotationLiveData().setValue(null);
                viewModel.getQuestionCountLiveData().setValue(questionCount - 1);
                Integer timerPerQuestion = viewModel.getTimerPerQuestionLiveData().getValue();
                viewModel.getTimeLeftPerQuestionLiveData().setValue(timerPerQuestion != null ?
                        timerPerQuestion * 1000 : null);
            } else {
                viewModel.getIsCustomizingLiveData().setValue(true);
            }
        });
        answerInfoButton.setOnClickListener(view -> {
            AnswerInfoDialog answerInfoDialog = (AnswerInfoDialog)
                    getSupportFragmentManager().findFragmentByTag(AnswerInfoDialog.TAG);
            if (answerInfoDialog == null) answerInfoDialog = AnswerInfoDialog.newInstance(
                    Objects.requireNonNull(quizGenerator.getUserAnswer()),
                    Objects.requireNonNull(quizGenerator.getUserAnswerGuessForm()),
                    Objects.requireNonNull(quizGenerator.getCorrectAnswer()),
                    Objects.requireNonNull(quizGenerator.getCorrectChoiceGuessForm()));

            answerInfoDialog.setOnTouchListener(customOnTouchListener);
            if(!answerInfoDialog.isAdded()) answerInfoDialog.show(
                    getSupportFragmentManager(), AnswerInfoDialog.TAG);
        });
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        if(quizGenerator != null && viewModel.getTimerPerQuestionLiveData() != null &&
                quizGenerator.getUserAnswerIndex() == -1)
        {
            timeToDeduct = savedInstanceState.getInt("TIME_TO_DEDUCT");
            timeToDeduct += new Date().getTime() - onCreateStart.getTime();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BgmService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bgmService != null && !isChangingConfigurations() && !isExplicitExit) bgmService.pauseBgm();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bgmService != null) bgmService.resumeBgm();
    }

    @Override
    protected void onDestroy() {
        if(timerThread != null && isChangingConfigurations()) timerThread.interrupt();
        if(!isChangingConfigurations()) {
            soundPool.release();
            soundPool = null;
            if(bgmService != null) unbindService(connection);
        }
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        if(!isExplicitExit && !isToastShowing) {
            showShortToast(R.string.toast_quiz_click_back_again);
            return;
        }
        isExplicitExit = true;

        Integer questionCount = viewModel.getQuestionCountLiveData().getValue();
        if(questionCount != null && questionCount != 0) {
            if(timerThread != null && timerThread.isAlive()) {
                quizGenerator.setUserAnswerIndex(JapaneseQuizGenerator.NO_ANSWER);
                timerThread.interrupt();
            }
            for (GuessableJapaneseCharacter entry : quizGenerator.getCharactersHistory()) {
                entry.invalidateQuizSuccesses();
            }
        }
        super.onBackPressed();
    }

    @Override
    public void onApplyDifficulty(@NonNull JapaneseQuizGenerator.ChoiceType choiceType,
                                  @NonNull JapaneseQuizGenerator.GuessMode guessMode,
                                  int choicesCount,
                                  int questionCount,
                                  int timerPerQuestion)
    {
        viewModel.getIsCustomizingLiveData().setValue(false);
        viewModel.getTimerPerQuestionLiveData().setValue(timerPerQuestion == 0 ? null : timerPerQuestion);

        Objects.requireNonNull(viewModel.getUserLiveData().getValue()).addQuizAttempt();
        if(quizGenerator != null) {
            nextAndRetryButton.setText(R.string.button_next);
            quizGenerator.clearHistory();
            quizGenerator.update(choiceType, guessMode, choicesCount);
        } else {
            GuessableJapaneseCharacters characters = viewModel.getUserLiveData().getValue()
                    .getGuessableJapaneseCharacters();
            JapaneseQuizGenerator generator = new JapaneseQuizGenerator(characters, choiceType,
                    guessMode, choicesCount);
            generator.generateQuiz();
            viewModel.getJapaneseQuizGeneratorLiveData().setValue(generator);
        }
        viewModel.getQuestionCountLiveData().setValue(questionCount);
        viewModel.getTimeLeftPerQuestionLiveData().setValue(
                timerPerQuestion != 0 ? timerPerQuestion * 1000 : null); // needs to be called last
    }

    @Override
    public void onDialogCancel() {
        viewModel.getIsCustomizingLiveData().setValue(false);
        if(quizGenerator == null) {
            isExplicitExit = true;
            onBackPressed();
        }
    }

    @Override
    public void onChoiceClick(int index) {
        if(timerThread != null) timerThread.interrupt();
        if(quizGenerator.getUserAnswerIndex() != -1) {
            if(isToastShowing) return;
            int messageId = nextAndRetryButton.getText().toString().equals(getString(R.string.button_retry)) ?
                    R.string.toast_quiz_finished : R.string.toast_quiz_click_next;
            showShortToast(messageId);
            return;
        }
        quizGenerator.setUserAnswerIndex(index);
        showAnswerResult(index);
    }

    private void showAnswerResult(int selectedChoiceIndex) {
        nextAndRetryButton.setVisibility(View.VISIBLE);
        timerBar.clearAnimation();

        int[] drawableRes = new int[quizGenerator.getChoicesCount()];
        for(int i = 0; i < adapter.getItemCount(); i++) {
            if(i == selectedChoiceIndex) continue;
            drawableRes[i] = i == quizGenerator.getCorrectAnswerIndex() ?
                    R.drawable.button_quiz_choices_correct :
                    R.drawable.button_quiz_choices_greyed_out;
        }

        if(selectedChoiceIndex == JapaneseQuizGenerator.NO_ANSWER) {
            questionTextTV.setText(R.string.textView_quiz_no_answer);
        } else if (selectedChoiceIndex == quizGenerator.getCorrectAnswerIndex()) {
            questionTextTV.setText(R.string.textView_quiz_correct_answer);
            drawableRes[selectedChoiceIndex] = R.drawable.button_quiz_choices_correct;
        } else {
            questionTextTV.setText(R.string.textView_quiz_incorrect_answer);
            answerInfoButton.setVisibility(View.VISIBLE);
            answerInfoButton.startAnimation(answerInfoAnimation);
            drawableRes[selectedChoiceIndex] = R.drawable.button_quiz_choices_incorrect;
        }
        adapter.setChoicesBackground(drawableRes);
        characterToGuessTV.setText(getString(R.string.textView_quiz_reveal_answer,
                characterToGuessTV.getText().toString(), quizGenerator.getCorrectAnswer()));

        if(Objects.requireNonNull(viewModel.getQuestionCountLiveData().getValue()) == 1) {
            nextAndRetryButton.setText(R.string.button_retry);
            QuizResultDialog quizResultDialog = (QuizResultDialog)
                    getSupportFragmentManager().findFragmentByTag(QuizResultDialog.TAG);
            if (quizResultDialog == null) quizResultDialog = QuizResultDialog.newInstance(quizGenerator);
            if (!quizResultDialog.isAdded()) quizResultDialog.show(
                    getSupportFragmentManager(), QuizResultDialog.TAG);

            for (GuessableJapaneseCharacter entry : quizGenerator.getCharactersHistory()) {
                entry.commitSuccessfulQuizAttempts();
            }
        }
    }

    private void displayQuiz(@NonNull String[] choices,
                             @NonNull String guessForm,
                             @NonNull JapaneseQuizGenerator.GuessMode guessMode,
                             int questionNumber)
    {
        answerInfoButton.clearAnimation();
        answerInfoButton.setVisibility(View.INVISIBLE);
        nextAndRetryButton.setVisibility(View.INVISIBLE);

        if(adapter != null) {
            adapter.setNewChoices(choices);
        } else {
            adapter = new QuizChoicesAdapter(choices,
                    R.drawable.button_quiz_choices_unpressed);

            CustomButtonOnTouchListener customOnTouchListener = new CustomButtonOnTouchListener(
                    R.drawable.button_quiz_choice_pressed, R.drawable.button_quiz_choices_unpressed,
                    0);
            customOnTouchListener.setSoundEffects(soundPool, 0.3f, 0.3f,
                    sfxIdButtonPressed, sfxIdButtonReleased);
            adapter.setOnTouchListener(customOnTouchListener);
            adapter.setOnChoiceClickListener(QuizActivity.this);
            recyclerView.setAdapter(adapter);
        }

        questionNumberTV.setText(getString(R.string.textView_quiz_question_number, questionNumber));
        String[] spinnerChoices = getResources().getStringArray(R.array.spinner_guess_type);
        questionTextTV.setText(getString(R.string.textView_quiz_question,
                spinnerChoices[guessMode.ordinal()].toUpperCase(Locale.ROOT)));
        characterToGuessTV.setText(guessForm);
    }

    private void startTimer() {
        MutableLiveData<Integer> timeLeftLiveData = viewModel.getTimeLeftPerQuestionLiveData();
        timerThread = new Thread(() -> {
            Date start = new Date();
            try {
                int timeLeftPerQuestion = Objects.requireNonNull(timeLeftLiveData.getValue());
                int timeToWait = 1000;
                // If true, an activity recreation has occurred and must deduct the time it took
                // recreate to the timer to avoid cheating through repetitive device rotation.
                if(timeLeftPerQuestion % 1000 != 0) {
                    timeToWait = timeLeftPerQuestion % 1000;
                    if(timeToDeduct != 0) {
                        timeToWait -= timeToDeduct;
                        timeToDeduct = 0;
                    }
                    if(timeToWait > 0) {
                        int finalTimeToWait = timeToWait;
                        runOnUiThread(() -> timerBarAnimation.setDuration(finalTimeToWait));
                    }
                }

                if(timeToWait > 0) {
                    while (timeLeftPerQuestion > 0) {
                        Thread.sleep(timeToWait);
                        int timeLeft = timeLeftPerQuestion -= timeToWait;
                        start = new Date();
                        timeToWait = 1000;
                        runOnUiThread(() -> timeLeftLiveData.setValue(Math.max(timeLeft, 0)));
                    }
                } else {
                    runOnUiThread(() -> timeLeftLiveData.setValue(0));
                }
                quizGenerator.setUserAnswerIndex(JapaneseQuizGenerator.NO_ANSWER);
            } catch (InterruptedException ie) {
                // interrupted when activity is recreating, user picked an answer or user is exiting mid-quiz
                runOnUiThread(() -> timerBar.clearAnimation());
                if(quizGenerator.getUserAnswerIndex() != -1) {
                    timerThread = null;
                    int timeToWaitLeft = (int) (new Date().getTime() - start.getTime());
                    runOnUiThread(() -> {
                        float rotation = 360 * ((1000 - timeToWaitLeft) / 1000.00f);
                        viewModel.getTimerBarRotationLiveData().setValue(Math.abs(rotation - 90.0f));
                    });
                } else {
                    int timePassed = (int) (new Date().getTime() - start.getTime());
                    int timeNeeded = timeLeftLiveData.getValue();
                    //Log.e(QuizActivity.this.getClass().getSimpleName(), "" + timeNeeded + " - " + timePassed + " = " + (timeNeeded - timePassed));
                    runOnUiThread(() -> timeLeftLiveData.setValue(timeNeeded - timePassed));
                }
            }
            timerThread = null;
        });
        timerThread.start();
    }

    private void showShortToast(@StringRes int stringRes) {
        Toast.makeText(this, stringRes, Toast.LENGTH_SHORT).show();
        isToastShowing = true;
        new Handler().postDelayed(() -> isToastShowing = false, 2000);
    }
}