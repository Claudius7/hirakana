package com.mrastudios.hirakana.data;

import android.content.Context;

import androidx.annotation.NonNull;

import com.mrastudios.hirakana.domain.GuessableJapaneseCharacters;

import java.io.IOException;

public final class User
{
    private static User instance;
    private final MySQLiteOpenHelper sqLiteOpenHelper;

    private GuessableJapaneseCharacters guessables;
    private int quizAttempts;
    private int challengeAttempts;

    public static User getInstance(Context context) {
        if(instance == null) instance = new User(context);
        return instance;
    }

    private User(Context context) {
        this.sqLiteOpenHelper = MySQLiteOpenHelper.getInstance(context);

        quizAttempts = sqLiteOpenHelper.loadQuizAttempts();
        challengeAttempts = sqLiteOpenHelper.loadChallengeAttempts();
        guessables = sqLiteOpenHelper.loadGuessableJapaneseCharacters();

        if(guessables == null) this.guessables = GuessableJapaneseCharacters.getInstance();
    }

    public void addChallengeAttempt() { challengeAttempts++; }

    public void addQuizAttempt() { quizAttempts++; }

    public int getQuizAttempts() {
        return quizAttempts;
    }

    public int getChallengeAttempts() {
        return challengeAttempts;
    }

    @NonNull
    public GuessableJapaneseCharacters getGuessableJapaneseCharacters() {
        return guessables;
    }

    public void saveUser() throws IOException {
        sqLiteOpenHelper.saveChallengeAttempts(challengeAttempts);
        sqLiteOpenHelper.saveQuizAttempts(quizAttempts);
        sqLiteOpenHelper.saveGuessableJapaneseCharacters(guessables);
    }
}