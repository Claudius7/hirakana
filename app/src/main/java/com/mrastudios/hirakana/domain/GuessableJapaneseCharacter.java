package com.mrastudios.hirakana.domain;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

public final class GuessableJapaneseCharacter implements Serializable
{
    private final Japanese.Character japaneseCharacter;

    private int quizAttempts;
    private int quizSuccessCount;
    private int challengeAttempts;
    private int challengeSuccessCount;

    private int uncommittedSuccessfulQuizAttempts;
    private int uncommittedSuccessfulChallengeAttempts;

    GuessableJapaneseCharacter(@NonNull Japanese.Character japaneseCharacter) {
        this.japaneseCharacter = japaneseCharacter;
    }

    private float calculateSuccessRate(int attempts, int successCount) {
        if(attempts == 0 && successCount == 0) return 0f;

        BigDecimal bigDecimal = new BigDecimal((float) successCount / attempts * 100);
        return bigDecimal.setScale(2, RoundingMode.HALF_UP).floatValue();
    }

    /**
     * Add a Quiz attempt for this character but caches a successful attempt (<code>isCorrect</code> == true).
     * The successful attempts only counts when {@link #commitSuccessfulQuizAttempts()} is called
     * otherwise all of it will be deleted.
     *
     * @param isCorrect whether the guess attempt for this character is successful or not.
     */
    public void addQuizAttempt(boolean isCorrect) {
        if(isCorrect) {
            uncommittedSuccessfulQuizAttempts++;
            return;
        }
        this.quizAttempts += 1;
    }

    /**
     * Add a Challenge attempt for this character but caches a successful attempt (<code>isCorrect</code> == true).
     * The successful attempts only counts when {@link #commitSuccessfulQuizAttempts()} is called
     * otherwise all of it will be deleted.
     *
     * @param isCorrect whether the guess attempt for this character is successful or not.
     */
    public void addChallengeAttempt(boolean isCorrect) {
        if(isCorrect) {
            uncommittedSuccessfulChallengeAttempts++;
            return;
        }
        this.challengeAttempts += 1;
    }

    /**
     * Applies all cached successful attempts for this character.
     */
    public void commitSuccessfulQuizAttempts() {
        this.quizAttempts += uncommittedSuccessfulQuizAttempts;
        quizSuccessCount += uncommittedSuccessfulQuizAttempts;
        uncommittedSuccessfulQuizAttempts = 0;
    }

    /**
     * Applies all cached successful attempts for this character.
     */
    public void commitSuccessfulChallengeAttempts() {
        this.challengeAttempts += uncommittedSuccessfulChallengeAttempts;
        challengeSuccessCount += uncommittedSuccessfulChallengeAttempts;
        uncommittedSuccessfulChallengeAttempts = 0;
    }

    /**
     * Clears all cached successful quiz attempts for this character.
     */
    public void invalidateQuizSuccesses() {
        uncommittedSuccessfulQuizAttempts = 0;
    }

    /**
     * Clears all cached successful challenge attempts for this character.
     */
    public void invalidateChallengeSuccesses() {
        uncommittedSuccessfulChallengeAttempts = 0;
    }

    public int getTotalAttempts() {
        return quizAttempts + challengeAttempts;
    }

    public int getTotalSuccessCount() {
        return quizSuccessCount + challengeSuccessCount;
    }

    public float getQuizSuccessRate() {
        return calculateSuccessRate(quizAttempts, quizSuccessCount);
    }

    public float getChallengeSuccessRate() {
        return calculateSuccessRate(challengeAttempts, challengeSuccessCount);
    }

    public float getTotalSuccessRate() {
        return calculateSuccessRate(getTotalAttempts(), getTotalSuccessCount());
    }

    // <---- Standard getters below ---->
    public Japanese.Character getCharacter() {
        return japaneseCharacter;
    }

    public int getQuizAttempts() {
        return quizAttempts;
    }

    public int getQuizSuccessCount() {
        return quizSuccessCount;
    }

    public int getChallengeSuccessCount() {
        return challengeSuccessCount;
    }

    public int getChallengeAttempts() {
        return challengeAttempts;
    }
}
