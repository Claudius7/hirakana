package com.mrastudios.hirakana;

import org.junit.Test;

import static org.junit.Assert.*;

import com.mrastudios.hirakana.domain.Japanese;

import java.util.Arrays;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest
{
    @Test
    public void checkCharacterTypesToString() {
        String[] expectedStrings = {"Hiragana", "Katakana", "Kanji"};
        String[] actualStrings = Arrays.toString(Japanese.Type.values())
                .replaceAll("\\[|],", "").split("\\s");
        assertArrayEquals(expectedStrings, actualStrings);
    }
}