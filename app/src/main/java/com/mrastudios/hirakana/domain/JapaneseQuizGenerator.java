package com.mrastudios.hirakana.domain;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;

public final class JapaneseQuizGenerator implements Serializable
{
    public static final int NO_ANSWER = 6969;
    public static final int MINIMUM_CHOICES = 4;

    private final List<GuessableJapaneseCharacter> choicesShown = new ArrayList<>();

    private final List<GuessableJapaneseCharacter> charactersHistory = new ArrayList<>();
    private final List<String> charactersGuessFormsHistory = new ArrayList<>();
    private final List<GuessMode> guessModeHistory = new ArrayList<>();
    private final List<String> correctAnswersHistory = new ArrayList<>();
    private final List<String> userAnswersHistory = new ArrayList<>();

    private final GuessableJapaneseCharacters characters;
    private ChoiceType choiceType;
    /**
     * Never use this directly! That's because if <code>guessMode</code> == {@link GuessMode#RANDOM},
     * {@link #guessModeForRandom} will be used instead.
     * Use {@link #getGuessMode()} instead to get the actual {@link GuessMode} that will be used.
     */
    private GuessMode guessMode;
    private int choicesCount;

    private GuessMode guessModeForRandom;
    private int correctAnswerIndex = -1;
    private int userAnswerIndex = -1;

    public enum ChoiceType {
        HIRAGANA_ONLY, KATAKANA_ONLY, KANJI_ONLY,
        HIRAGANA_AND_KATAKANA, KATAKANA_AND_KANJI, HIRAGANA_AND_KANJI,
        ALL
    }

    public enum GuessMode {
        ROMAJI, CHARACTER, MEANING,
        /**
         * If this is selected, {@link #createNewChoices()} will pick one of the {@link GuessMode}
         * randomly (this one excluded).
         */
        RANDOM;

        @NonNull
        @Override
        public String toString() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
    }

    public JapaneseQuizGenerator(@NonNull GuessableJapaneseCharacters characters,
                                 @NonNull ChoiceType choiceType,
                                 @NonNull GuessMode guessMode,
                                 int choicesCount)
    {
        this.characters = characters;
        this.choiceType = choiceType;
        this.guessMode = guessMode;
        this.choicesCount = Math.max(choicesCount, MINIMUM_CHOICES);
    }

    private void createNewChoices() {
        userAnswerIndex = -1;
        choicesShown.clear();

        GuessableJapaneseCharacter correctCharChoice = generateRandomCharacter(choiceType);
        if(charactersHistory.size() > 0) {
            while(correctCharChoice == charactersHistory.get(charactersHistory.size() - 1)) {
                correctCharChoice = generateRandomCharacter(choiceType);
            }
        }

        if (guessMode == GuessMode.RANDOM) {
            if(correctCharChoice.getCharacter().getType() == Japanese.Type.KANJI) {
                int random = new Random().nextInt(GuessMode.values().length - 1);
                guessModeForRandom = GuessMode.values()[random];
            } else {
                guessModeForRandom = new Random().nextBoolean() ? GuessMode.ROMAJI : GuessMode.CHARACTER;
            }
            guessModeHistory.add(guessModeForRandom);
        } else {
            guessModeHistory.add(guessMode);
        }

        Set<GuessableJapaneseCharacter> choicesToAdd = new LinkedHashSet<>(choicesCount);
        while (choicesToAdd.size() < choicesCount - 1) {
            GuessableJapaneseCharacter randomJpChar;
            if(guessMode != GuessMode.RANDOM) {
                randomJpChar = generateRandomCharacter(choiceType);
            } else if(guessModeForRandom != GuessMode.MEANING) {
                randomJpChar = generateRandomCharacter(choiceType);
            } else {
                randomJpChar = getRandomCharacter(Japanese.Type.KANJI);
            }
            boolean isSameEnglishTranslation = randomJpChar.getCharacter().toEnglish().equals(
                    correctCharChoice.getCharacter().toEnglish());
            if(isSameEnglishTranslation) continue;
            choicesToAdd.add(randomJpChar);
        }
        choicesShown.addAll(choicesToAdd);
        choicesShown.add(new Random().nextInt(choicesCount), correctCharChoice);
        correctAnswerIndex = choicesShown.indexOf(correctCharChoice);

        charactersHistory.add(correctCharChoice);
        charactersGuessFormsHistory.add(getGuessForm(correctCharChoice));
        correctAnswersHistory.add(getAnswerForm(correctCharChoice, getGuessMode()));
    }

    private GuessableJapaneseCharacter generateRandomCharacter(@NonNull ChoiceType choiceType)
    {
        if(choiceType == ChoiceType.ALL) {
            return getRandomCharacter();
        } else {
            switch (choiceType) {
                case HIRAGANA_AND_KATAKANA:
                    return getRandomCharacter(Japanese.Type.HIRAGANA,
                            Japanese.Type.KATAKANA);
                case HIRAGANA_AND_KANJI:
                    return getRandomCharacter(Japanese.Type.HIRAGANA,
                            Japanese.Type.KANJI);
                case KATAKANA_AND_KANJI:
                    return getRandomCharacter(Japanese.Type.KATAKANA,
                            Japanese.Type.KANJI);
                case HIRAGANA_ONLY:
                    return getRandomCharacter(Japanese.Type.HIRAGANA);
                case KATAKANA_ONLY:
                    return getRandomCharacter(Japanese.Type.KATAKANA);
                default:
                    return getRandomCharacter(Japanese.Type.KANJI);
            }
        }
    }

    /**
     * Returns a random {@link GuessableJapaneseCharacter} with all {@link Japanese.Type}
     * in the pool.
     */
    private GuessableJapaneseCharacter getRandomCharacter() {
        List<GuessableJapaneseCharacter> japaneseCharacters = characters.getAllGuessables();
        return japaneseCharacters.get(new Random().nextInt(japaneseCharacters.size()));
    }

    /**
     * Returns a random {@link GuessableJapaneseCharacter} based on the specified {@code type}.
     * <br/>
     * If {@code type} happens to contain all {@link Japanese.Type} this will simply call
     * {@link #getRandomCharacter()}.
     * @param type the {@link Japanese.Type}(s) to add into the pool.
     */
    private GuessableJapaneseCharacter getRandomCharacter(@NonNull Japanese.Type... type) {
        List<Japanese.Type> types = Arrays.asList(Japanese.Type.values());
        if(type.length > types.size() || Arrays.asList(type).containsAll(types)) return getRandomCharacter();

        List<GuessableJapaneseCharacter> charactersPool = new ArrayList<>();
        for (Japanese.Type type_1 : type) {
            charactersPool.addAll(characters.getGuessables(type_1));
        }
        return charactersPool.get(new Random().nextInt(charactersPool.size()));
    }

    private String getAnswerForm(@NonNull GuessableJapaneseCharacter character,
                                 @NonNull GuessMode guessMode)
    {
        switch (guessMode) {
            case ROMAJI:
                return character.getCharacter().toEnglish();
            case CHARACTER:
                return character.getCharacter().toJapanese();
            default:
                Japanese.Kanji kanji = (Japanese.Kanji) character.getCharacter();
                return kanji.getMeaning();
        }
    }

    private String getGuessForm(GuessableJapaneseCharacter character) {
        GuessMode guessMode = getGuessMode();
        return guessMode == GuessMode.CHARACTER ?
                character.getCharacter().toEnglish() :
                character.getCharacter().toJapanese();
    }


    /**
     * Generates choices and the correct answer for the quiz. <br/>
     * Generates again only if the previous quiz has its answer set via {@link #setUserAnswerIndex(int)}.
     */
    public void generateQuiz() {
        if(userAnswerIndex != -1 || correctAnswerIndex == -1) createNewChoices();
    }

    /**
     * Change the current settings of this generator and then generates a new quiz with the new settings.
     */
    public void update(@NonNull ChoiceType choiceType,
                       @NonNull GuessMode guessMode,
                       int choicesCount)
    {
        this.choiceType = choiceType;
        this.guessMode = guessMode;
        this.choicesCount = choicesCount;

        if(!charactersHistory.isEmpty()) {
            charactersHistory.remove(charactersHistory.size() - 1);
            correctAnswersHistory.remove(correctAnswersHistory.size() - 1);
            charactersGuessFormsHistory.remove(charactersGuessFormsHistory.size() - 1);
        }
        userAnswerIndex = NO_ANSWER;
        generateQuiz();
    }

    /**
     * Clears every history recorded except the current quiz if still unanswered
     * i.e. {@link #setUserAnswerIndex(int)} has not yet been called.
     */
    public void clearHistory() {
        if(userAnswerIndex == -1) {
            List<List<?>> histories = new ArrayList<>();
            histories.add(charactersHistory);
            histories.add(charactersGuessFormsHistory);
            histories.add(correctAnswersHistory);
            histories.add(guessModeHistory);
            for (List<?> history : histories) {
                while (history.size() > 1) { history.remove(0); }
            }
        } else {
            charactersHistory.clear();
            charactersGuessFormsHistory.clear();
            correctAnswersHistory.clear();
            guessModeHistory.clear();
        }
        userAnswersHistory.clear();
    }

    /**
     * Sets the answer for the current quiz and enables generation of a
     * new quiz via {@link #generateQuiz()}.
     * <br/>
     * Nothing will happen if it has been set once or
     * <code> index <code/> < 0 or
     * <code> index <code/> >= number of choices and
     * <code> index <code/> != {@link #NO_ANSWER}.
     */
    public void setUserAnswerIndex(int index) {
        if(this.userAnswerIndex != -1) return;
        if(index < 0 || (index >= choicesCount && index != NO_ANSWER)) return;
        userAnswerIndex = index;
        userAnswersHistory.add(userAnswerIndex == NO_ANSWER ? null : getUserAnswer());
        GuessableJapaneseCharacter characterToGuess = choicesShown.get(correctAnswerIndex);
        characterToGuess.addQuizAttempt(userAnswerIndex == correctAnswerIndex);
    }

    public int getUserAnswerIndex() {
        return userAnswerIndex;
    }

    public int getCorrectAnswerIndex() {
        return correctAnswerIndex;
    }

    @Nullable
    public String getCorrectChoiceGuessForm() {
        if(charactersGuessFormsHistory.size() - 1 == -1) return null;
        return charactersGuessFormsHistory.get(charactersGuessFormsHistory.size() - 1);
    }

    @Nullable
    public String getCorrectAnswer() {
        if(correctAnswersHistory.size() - 1 == -1) return null;
        return correctAnswersHistory.get(correctAnswersHistory.size() - 1);
    }

    @Nullable
    public String getUserAnswerGuessForm() {
        if(userAnswerIndex == -1 || userAnswerIndex == NO_ANSWER) return null;
        return getGuessForm(choicesShown.get(userAnswerIndex));
    }

    @Nullable
    public String getUserAnswer() {
        if(userAnswerIndex == -1 || userAnswerIndex == NO_ANSWER) return null;
        return getAnswerForm(choicesShown.get(userAnswerIndex), getGuessMode());
    }

    @Nullable
    public String[] getChoices() {
        if(choicesShown.isEmpty()) return null;
        String[] choices = new String[choicesShown.size()];
        for (GuessableJapaneseCharacter character : choicesShown) {
            choices[choicesShown.indexOf(character)] = getAnswerForm(character, getGuessMode());
        }
        return choices;
    }

    @NonNull
    public GuessMode getGuessMode() {
        return guessMode == GuessMode.RANDOM ? guessModeForRandom : guessMode;
    }

    public List<GuessMode> getGuessModeHistory() {
        return guessModeHistory;
    }

    public int getChoicesCount() {
        return choicesCount;
    }

    public List<GuessableJapaneseCharacter> getCharactersHistory() {
        return charactersHistory;
    }

    public List<String> getCorrectAnswersHistory() {
        return correctAnswersHistory;
    }

    public List<String> getCharactersGuessFormsHistory() {
        return charactersGuessFormsHistory;
    }

    public List<String> getUserAnswersHistory() {
        return userAnswersHistory;
    }
}
