package com.mrastudios.hirakana.domain;

import androidx.annotation.NonNull;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public final class Japanese implements Serializable
{
    private static Japanese instance = null;

    private final List<Hiragana> hiraganaCharacters = new ArrayList<>();
    private final List<Japanese.Katakana> katakanaCharacters = new ArrayList<>();
    private final List<Japanese.Kanji> kanjiCharacters = new ArrayList<>();

    public static Japanese getInstance() {
        if(instance == null) instance = new Japanese();
        return instance;
    }

    private Japanese() {
        for(Japanese.Hiragana.Character c : Japanese.Hiragana.Character.values()) {
            hiraganaCharacters.add(new Japanese.Hiragana(c));
        }
        for(Japanese.Katakana.Character c : Japanese.Katakana.Character.values()) {
            katakanaCharacters.add(new Japanese.Katakana(c));
        }
        for(Japanese.Kanji.Character c : Japanese.Kanji.Character.values()) {
            kanjiCharacters.add(new Japanese.Kanji(c));
        }
    }

    public enum Type {
        HIRAGANA, KATAKANA, KANJI;

        @NonNull
        @Override
        public String toString() {
            return this.name().charAt(0) + this.name().substring(1).toLowerCase();
        }
    }

    public abstract static class Character implements Serializable, Comparable<Character>
    {
        protected final Type type;
        protected final Translatable translatable;

        interface Translatable {
            String toJapanese();
            String toEnglish();
        }

        Character(@NonNull Type type, @NonNull Translatable translatable) {
            this.type = type;
            this.translatable = translatable;
        }

        @Override
        public int compareTo(@NonNull Character o) {
            if(getType() == o.getType()) return getType().ordinal() - o.getType().ordinal();
            switch (getType()) {
                case HIRAGANA:
                    return -1;
                case KATAKANA:
                    return o.getType() == Japanese.Type.HIRAGANA ? 1 : -1;
                default:
                    return 1;
            }
        }

        public String toJapanese() {
            return translatable.toJapanese();
        }

        public String toEnglish() {
            return translatable.toEnglish();
        }

        public final Type getType() {
            return type;
        }
    }

    public static final class Hiragana extends Character
    {
        public Hiragana(Hiragana.Character character) {
            super(Japanese.Type.HIRAGANA, character);
        }

        public enum Character implements Translatable {
            A("あ"), I("い"), U("う"), E("え"), O("お"),

            KA("か"), KI("き"), KU("く"), KE("け"), KO("こ"),
            GA("が"), GI("ぎ"), GU("ぐ"), GE("げ"), GO("ご"),

            SA("さ"), SHI("し"), SU("す"), SE("せ"), SO("そ"),
            ZA("ざ"), JI("じ"), ZU("ず"), ZE("ぜ"), ZO("ぞ"),

            TA("た"), CHI("ち"), TSU("つ"), TE("て"), TO("と"),
            DA("だ"), DI( "ぢ"), DU("づ"), DE("で"), DO("ど"),

            NA("な"), NI("に"), NU("ぬ"), NE("ね"), NO("の"),

            HA("は"), HI("ひ"), FU("ふ"), HE("へ"), HO("ほ"),
            BA("ば"), BI("び"), BU("ぶ"), BE("べ"), BO("ぼ"),
            PA("ぱ"), PI("ぴ"), PU("ぷ"), PE("ぺ"), PO("ぽ"),

            MA("ま"), MI("み"), MU("む"), ME("め"), MO("も"),

            RA("ら"), RI("り"), RU("る"), RE("れ"), RO("ろ"),

            YA("や"), YU("ゆ"), YO("よ"),

            WA("わ"), WO("を"),

            N("ん"),

            KYA("きゃ"), KYU("きゅ"), KYO("きょ"),
            GYA("ぎゃ"), GYU("ぎゅ"), GYO("ぎょ"),

            NYA("にゃ"), NYU("にゅ"), NYO("にょ"),

            HYA("ひゃ"), HYU("ひゅ"), HYO("ひょ"),
            BYA("びゃ"), BYU("びゅ"), BYO("びょ"),
            PYA("ぴゃ"), PYU("ぴゅ"), PYO("ぴょ"),

            MYA("みゃ"), MYU("みゅ"), MYO("みょ"),

            RYA("りゃ"), RYU("りゅ"), RYO("りょ"),

            SHA("しゃ"), SHU("しゅ"), SHO("しょ"),
            CHA("ちゃ"), CHU("ちゅ"), CHO("ちょ"),

            JA("じゃ"), JU("じゅ"), JO("じょ");

            private final String CHARACTER;
            private final String CHARACTER_ENG;

            Character(String jpChar) {
                CHARACTER = jpChar;

                // "DI" and "DU" is actually "JI" and "ZU" also in Japanese but different when typed
                //  and enums does not allow duplicate names so changed their CHARACTER_ENG instead.
                switch (this.name()) {
                    case "DI":
                        CHARACTER_ENG = "JI (DI)";
                        break;
                    case "DU":
                        CHARACTER_ENG = "ZU (DU)";
                        break;
                    default:
                        CHARACTER_ENG = this.name();
                }
            }

            @Override
            public String toJapanese() {
                return CHARACTER;
            }

            @Override
            public String toEnglish() {
                return CHARACTER_ENG;
            }
        }
    }

    public static final class Katakana extends Character
    {
        public Katakana(Katakana.Character character) {
            super(Japanese.Type.KATAKANA, character);
        }

        public enum Character implements Translatable {
            A("ア"), I("イ"), U("ウ"), E("エ"), O("オ"),

            KA("カ"), KI("キ"), KU("ク"), KE("ケ"), KO("コ"),
            GA("ガ"), GI("ギ"), GU("グ"), GE("ゲ"), GO("ゴ"),

            SA("サ"), SHI("シ"), SU("ス"), SE("セ"), SO("ソ"),
            ZA("ザ"), JI("ジ"), ZU("ズ"), ZE("ゼ"), ZO("ゾ"),

            TA("タ"), CHI("チ"), TSU("ツ"), TE("テ"), TO("ト"),
            DA("ダ"), DI( "ヂ"), DU("ヅ"), DE("デ"), DO("ド"),

            NA("ナ"), NI("ニ"), NU("ヌ"), NE("ネ"), NO("ノ"),

            HA("ハ"), HI("ヒ"), FU("フ"), HE("ヘ"), HO("ホ"),
            BA("バ"), BI("ビ"), BU("ブ"), BE("ベ"), BO("ボ"),
            PA("パ"), PI("ピ"), PU("プ"), PE("ペ"), PO("ポ"),

            MA("マ"), MI("ミ"), MU("ム"), ME("メ"), MO("モ"),

            RA("ラ"), RI("リ"), RU("ル"), RE("レ"), RO("ロ"),

            YA("ヤ"), YU("ユ"), YO("ヨ"),

            WA("ワ"), WO("ヲ"),

            N("ン"),

            KYA("キャ"), KYU("キュ"), KYO("キョ"),
            GYA("ギャ"), GYU("ギュ"), GYO("ギョ"),

            NYA("ニャ"), NYU("ニュ"), NYO("ニョ"),

            HYA("ヒャ"), HYU("ヒュ"), HYO("ヒョ"),
            BYA("ビャ"), BYU("ビュ"), BYO("ビョ"),
            PYA("ピャ"), PYU("ピュ"), PYO("ピョ"),

            MYA("ミャ"), MYU("ミュ"), MYO("ミョ"),

            RYA("リャ"), RYU("リュ"), RYO("リョ"),

            SHA("シャ"), SHU("シュ"), SHO("ショ"),
            CHA("チャ"), CHU("チュ"), CHO("チョ"),

            JA("ジャ"), JU("ジュ"), JO("ジョ");

            private final String CHARACTER;
            private final String CHARACTER_ENG;

            Character(String jpChar) {
                CHARACTER = jpChar;

                // "DI" and "DU" is actually "JI" and "ZU" also in Japanese but different when typed
                //  and enums does not allow duplicate names so changed their CHARACTER_ENG instead.
                switch (this.name()) {
                    case "DI":
                        CHARACTER_ENG = "JI (DI)";
                        break;
                    case "DU":
                        CHARACTER_ENG = "ZU (DU)";
                        break;
                    default:
                        CHARACTER_ENG = this.name();
                }
            }

            @Override
            public String toJapanese() {
                return CHARACTER;
            }

            @Override
            public String toEnglish() {
                return CHARACTER_ENG;
            }
        }
    }

    public static final class Kanji extends Character
    {
        public Kanji(Kanji.Character character) { super(Japanese.Type.KANJI, character); }

        public enum Character implements Translatable {
            OTOKO("男", "Man"),
            ONNA("女", "Woman"),
            KO("子", "Child"),
            HAHA("母", "Mother"),
            CHICHI("父", "Father"),
            AME("雨", "Rain"),
            KAWA("川", "River"),
            YAMA("山", "Mountain"),
            MORI("森", "Forest"),
            KI("木", "Wood");
            //SAN("飡", "YO MAMA HA!");

            private final String CHARACTER;
            private final String CHARACTER_ENG;
            private final String MEANING;

            Character(String jpChar, String meaning) {
                CHARACTER = jpChar;
                CHARACTER_ENG = this.name();
                MEANING = meaning;
            }

            @Override
            public String toJapanese() {
                return CHARACTER;
            }

            @Override
            public String toEnglish() {
                return CHARACTER_ENG;
            }
        }

        public String getMeaning() {
            Character character = (Character) translatable;
            return character.MEANING;
        }
    }

    /**
     * Sorts characters based on their {@link Japanese.Type} in the following order:
     * (Hiragana - Katakana - Kanji) and if the same the . <br/> <br/>
     * Example: &#009 Before sorting: ( 雨 , ア , きゃ , キャ , あ )
     * <br/> &#009 After sorting: ( あ , きゃ , ア , キャ , 雨)
     */
    public static void sort(@NonNull List<? extends Japanese.Character> japaneseCharacters) {
        japaneseCharacters.sort(Japanese.Character::compareTo);
    }

    /**
     * @param type The type of japanese characters to get.
     * @return a list of the specified {@code type}.
     */
    @NonNull
    public List<? extends Japanese.Character> getCharacters(@NonNull Japanese.Type type) {
        switch (type) {
            case HIRAGANA:
                return new ArrayList<>(hiraganaCharacters);
            case KATAKANA:
                return new ArrayList<>(katakanaCharacters);
            default:
                return new ArrayList<>(kanjiCharacters);
        }
    }

    /**
     * @return a list of all {@link Japanese.Type} that is sorted with {@link #sort(List)}.
     * @see  #getCharacters(Japanese.Type)
     */
    @NonNull
    public List<Japanese.Character> getAllCharacters() {
        List<Japanese.Character> japaneseCharacters = new ArrayList<>();
        japaneseCharacters.addAll(hiraganaCharacters);
        japaneseCharacters.addAll(katakanaCharacters);
        japaneseCharacters.addAll(kanjiCharacters);
        sort(japaneseCharacters);
        return japaneseCharacters;
    }
}
