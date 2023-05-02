package com.mrastudios.hirakana.domain;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public final class GuessableJapaneseCharacters implements Serializable
{
    private static GuessableJapaneseCharacters instance = null;
    private final Map<Japanese.Type, List<GuessableJapaneseCharacter>> guessablesMap;

    public static GuessableJapaneseCharacters getInstance() {
        if(instance == null) instance = new GuessableJapaneseCharacters();
        return instance;
    }

    private GuessableJapaneseCharacters() {
        guessablesMap = new EnumMap<>(Japanese.Type.class);
        Japanese japanese = Japanese.getInstance();
        for(Japanese.Type type : Japanese.Type.values()) {
            List<GuessableJapaneseCharacter> guessables = new ArrayList<>();
            for(Japanese.Character japaneseCharacter : japanese.getCharacters(type)) {
                if(type == Japanese.Type.HIRAGANA) {
                    guessables.add(new GuessableJapaneseCharacter(japaneseCharacter));
                } else if(type == Japanese.Type.KATAKANA) {
                    guessables.add(new GuessableJapaneseCharacter(japaneseCharacter));
                } else {
                    guessables.add(new GuessableJapaneseCharacter(japaneseCharacter));
                }
            }
            guessablesMap.put(type, guessables);
        }
    }

    //TODO: maybe improve these comparators.
    interface GuessableComparator<T> extends Comparator<GuessableJapaneseCharacter> {
        enum Type {QUIZ, CHALLENGE, TOTAL}
    }

    public static class SuccessRateCompare implements GuessableComparator<GuessableJapaneseCharacter>
    {
        private final Type type;

        public SuccessRateCompare(Type type) {
            this.type = type;
        }

        @Override
        public int compare(GuessableJapaneseCharacter japaneseCharacter, GuessableJapaneseCharacter t1) {
            float arg1, arg2;
            switch (type){
                case QUIZ:
                    arg1 = japaneseCharacter.getQuizSuccessRate();
                    arg2 = t1.getQuizSuccessRate();
                    break;
                case CHALLENGE:
                    arg1 = japaneseCharacter.getChallengeSuccessRate();
                    arg2 = t1.getChallengeSuccessRate();
                    break;
                default:
                    arg1 = japaneseCharacter.getTotalSuccessRate();
                    arg2 = t1.getTotalSuccessRate();
            }

            if(arg1 == arg2){
                int alternateArg1, alternateArg2;
                switch (type){
                    case QUIZ:
                        alternateArg1 = japaneseCharacter.getQuizAttempts();
                        alternateArg2 = t1.getQuizAttempts();
                        break;
                    case CHALLENGE:
                        alternateArg1 = japaneseCharacter.getChallengeAttempts();
                        alternateArg2 = t1.getChallengeAttempts();
                        break;
                    default:
                        alternateArg1 = japaneseCharacter.getTotalAttempts();
                        alternateArg2 = t1.getTotalAttempts();
                }
                return alternateArg1 - alternateArg2;
            }
            return Float.compare(arg1, arg2);
        }
    }

    public void invalidateAllQuizSuccesses() {
        for(GuessableJapaneseCharacter guessable : getAllGuessables()) {
            guessable.invalidateQuizSuccesses();
        }
    }

    public void invalidateAllChallengeSuccesses() {
        for(GuessableJapaneseCharacter guessable : getAllGuessables()) {
            guessable.invalidateChallengeSuccesses();
        }
    }

    public float getUserTotalSuccessRate() {
        List<GuessableJapaneseCharacter> charsWithAttempts = new ArrayList<>();
        for(GuessableJapaneseCharacter guessable : getAllGuessables()) {
            if(guessable.getTotalAttempts() != 0) charsWithAttempts.add(guessable);
        }

        float successRate = 0f;
        for(GuessableJapaneseCharacter character : charsWithAttempts) {
            successRate += character.getTotalSuccessRate();
        }
        if(successRate == 0f) return 0f;

        return new BigDecimal(successRate / charsWithAttempts.size()).setScale(
                2, RoundingMode.HALF_UP).floatValue();
    }

    /**
     * @param type The type of {@link GuessableJapaneseCharacter} to get.
     * @return a list of the specified {@code type}.
     */
    @NonNull
    public List<GuessableJapaneseCharacter> getGuessables(@NonNull Japanese.Type type) {
        return new ArrayList<>(Objects.requireNonNull(guessablesMap.get(type)));
    }

    @NonNull
    public List<GuessableJapaneseCharacter> getAllGuessables() {
        List<GuessableJapaneseCharacter> characters = new ArrayList<>();
        for (List<GuessableJapaneseCharacter> list : guessablesMap.values()) {
            characters.addAll(list);
        }
        return characters;
    }
}
