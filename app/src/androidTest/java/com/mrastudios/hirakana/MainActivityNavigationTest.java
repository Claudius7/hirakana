package com.mrastudios.hirakana;

import android.content.Context;

import static androidx.test.espresso.Espresso.*;

import androidx.test.espresso.action.ViewActions;
import static androidx.test.espresso.matcher.ViewMatchers.*;

import androidx.test.espresso.assertion.ViewAssertions;
import androidx.test.ext.junit.rules.ActivityScenarioRule;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.*;

import com.mrastudios.hirakana.ui.destinations.main_title.MainActivity;

/**
 * Instrumented test, which will execute on an Android device.
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
@RunWith(AndroidJUnit4.class)
public class MainActivityNavigationTest
{
    @Rule
    public ActivityScenarioRule<MainActivity> activityScenarioRule =
            new ActivityScenarioRule<>(MainActivity.class);

    @Test
    public void useAppContext() {
        // Context of the app under test.
        Context appContext = InstrumentationRegistry.getInstrumentation().getTargetContext();
        assertEquals("com.mrastudios.hirakana", appContext.getPackageName());
    }

    @Test
    public void moveToCharactersActivity() {
        onView(withId(R.id.toCharactersButton)).perform(ViewActions.click());
        onView(withId(R.id.action_search)).check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void moveToQuizActivity() {
        onView(withId(R.id.toQuizButton)).perform(ViewActions.click());
        onView(withText(R.string.dialog_customizer_title)).check(ViewAssertions.matches(isDisplayed()));
    }

    @Test
    public void moveToStatisticsActivity() {
        onView(withId(R.id.toStatisticButton)).perform(ViewActions.click());
        onView(withId(R.id.text_size_items_button)).check(ViewAssertions.matches(isDisplayed()));
    }
}