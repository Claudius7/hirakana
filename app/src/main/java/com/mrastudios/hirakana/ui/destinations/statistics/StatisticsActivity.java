package com.mrastudios.hirakana.ui.destinations.statistics;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.chip.Chip;
import com.google.android.material.chip.ChipGroup;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.GuessableJapaneseCharacter;
import com.mrastudios.hirakana.domain.Japanese;
import com.mrastudios.hirakana.ui.services.BgmService;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

public final class StatisticsActivity extends AppCompatActivity implements View.OnClickListener,
        CompoundButton.OnCheckedChangeListener, CharacterStatsAdapter.OnItemClickListener
{
    private StatisticsViewModel viewModel;
    private RecyclerView recyclerView;
    private CharacterStatsAdapter adapter;

    private BgmService bgmService;
    private final ServiceConnection connection = new ServiceConnection()
    {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            BgmService.BgmBinder binder = (BgmService.BgmBinder) service;
            bgmService = binder.getService();
            bgmService.startBgm(R.raw.bgm_default);        }

        @Override
        public void onServiceDisconnected(ComponentName name) {}
    };

    private ChipGroup filtersChipGroup;
    private ChipGroup sortChipGroup;

    private TextView characterRomajiTV;
    private TextView characterTypeTV;
    private TextView totalCharacterAttemptsTV;
    private TextView characterAttemptsTV;
    private TextView totalSuccessCountTV;
    private TextView characterSuccessCountTV;
    private TextView totalSuccessRateTV;
    private TextView characterSuccessRateTV;

    private final Map<MutableLiveData<ChipState>, CharacterStatsAdapter.SortType> sortTypeMap = new HashMap<>();
    private final Map<Bundle, MutableLiveData<ChipState>> chipStateMap = new HashMap<>();
    private static final String BUNDLE_KEY_TEXT_ASC = "BundleKeyTextAsc";
    private static final String BUNDLE_KEY_TEXT_DESC = "BundleKeyTextDesc";
    private boolean isReturningToMain;

    enum ChipState implements Serializable { DEACTIVATED, ASCENDING, DESCENDING }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);
        viewModel = new ViewModelProvider(this, new StatisticsViewModel.Factory(
                this, new Bundle())).get(StatisticsViewModel.class);

        initViews();
        setupViews();
        setupChips();
    }

    private void initViews() {
        filtersChipGroup = findViewById(R.id.filtersChipGroup);
        sortChipGroup = findViewById(R.id.sortChipGroup);

        characterRomajiTV = findViewById(R.id.textView_statistics_characterRomaji);
        characterTypeTV = findViewById(R.id.textView_statistics_characterType);
        totalCharacterAttemptsTV = findViewById(R.id.textView_statistics_characterTotalAttempts);
        characterAttemptsTV = findViewById(R.id.textView_statistics_characterAttempts);
        totalSuccessCountTV = findViewById(R.id.textView_statistics_characterTotalSuccesCount);
        characterSuccessCountTV = findViewById(R.id.textView_statistics_characterSuccessCount);
        totalSuccessRateTV = findViewById(R.id.textView_statistics_characterTotalSuccessRate);
        characterSuccessRateTV = findViewById(R.id.textView_statistics_characterSuccessRate);

        recyclerView = findViewById(R.id.character_stats_items);
        if(viewModel.getAdapterLiveData().getValue() != null) {
            adapter = new CharacterStatsAdapter(viewModel.getAdapterLiveData().getValue());
        } else {
            adapter = new CharacterStatsAdapter(viewModel.getUser().getGuessableJapaneseCharacters(),
                    null);
        }
        adapter.setOnItemClickListener(this);
    }

    private void setupViews() {
        int phoneOrientation = getResources().getConfiguration().orientation;
        int layoutManagerOrientation = phoneOrientation == Configuration.ORIENTATION_PORTRAIT ?
                RecyclerView.HORIZONTAL : RecyclerView.VERTICAL;
        recyclerView.setLayoutManager(new LinearLayoutManager(this,
                layoutManagerOrientation, false));
        recyclerView.addItemDecoration(new CharacterStatsAdapter.ItemDecoration());
        recyclerView.setAdapter(adapter);
        viewModel.getAdapterLiveData().setValue(adapter);

        TextView quizAttemptsTV = findViewById(R.id.user_total_quiz);
        quizAttemptsTV.setText(getString(R.string.textView_quiz_attempts,
                viewModel.getUser().getQuizAttempts()));

        TextView challengeAttemptsTV = findViewById(R.id.user_total_challenge);
        challengeAttemptsTV.setText(getString(R.string.textView_challenge_attempts,
                viewModel.getUser().getChallengeAttempts()));

        TextView userSuccessRateTV = findViewById(R.id.user_total_success_rate);
        userSuccessRateTV.setText(getString(R.string.textView_user_success_rate,
                getSuccessRateToPrint(viewModel.getUser().getGuessableJapaneseCharacters().getUserTotalSuccessRate())));

        findViewById(R.id.text_size_items_button).setOnClickListener(view -> {
            Boolean isTextBig = (Boolean) view.getTag();
            if(isTextBig == null) isTextBig = Boolean.FALSE;
            view.setTag(!isTextBig);

            adapter.swapItemLayout();
            recyclerView.swapAdapter(adapter, false);
            LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
            int previousScrollPos = Objects.requireNonNull(layoutManager).findFirstVisibleItemPosition();
            recyclerView.scrollToPosition(previousScrollPos);
        });
    }

    private void setupChips() {
        sortTypeMap.put(viewModel.getAlphabeticalChipState(), CharacterStatsAdapter.SortType.ALPHABETICAL);
        sortTypeMap.put(viewModel.getSuccessRateChipState(), CharacterStatsAdapter.SortType.SUCCESS_RATES);

        for (int i = 0; i < filtersChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) filtersChipGroup.getChildAt(i);
            chip.setOnCheckedChangeListener(this);
        }

        for(int i = 0; i < sortChipGroup.getChildCount(); i++) {
            Chip chip = (Chip) sortChipGroup.getChildAt(i);
            Bundle bundle = new Bundle();
            String textAscending = null;
            String textDescending = null;
            ChipState chipState = ChipState.DEACTIVATED;
            if(chip.getId() == R.id.chip_alphabetical) {
                bundle.putString(BUNDLE_KEY_TEXT_ASC, (textAscending = getString(R.string.chip_alphabet_asc)));
                bundle.putString(BUNDLE_KEY_TEXT_DESC, (textDescending = getString(R.string.chip_alphabet_desc)));
                chipStateMap.put(bundle, viewModel.getAlphabeticalChipState());
                chipState = viewModel.getAlphabeticalChipState().getValue();
            } else if(chip.getId() == R.id.chip_success_rate) {
                bundle.putString(BUNDLE_KEY_TEXT_ASC, (textAscending = getString(R.string.chip_success_asc)));
                bundle.putString(BUNDLE_KEY_TEXT_DESC, (textDescending = getString(R.string.chip_success_desc)));
                chipStateMap.put(bundle, viewModel.getSuccessRateChipState());
                chipState = viewModel.getSuccessRateChipState().getValue();
            }

            if(chipState != ChipState.DEACTIVATED) {
                chip.setChipBackgroundColorResource(android.R.color.holo_green_light);
                chip.setText(chipState == ChipState.DESCENDING ? textDescending : textAscending);
            }
            chip.setTag(bundle);
            chip.setOnClickListener(this);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BgmService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bgmService != null) bgmService.resumeBgm();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bgmService != null && !isChangingConfigurations() && !isReturningToMain) bgmService.pauseBgm();
    }

    @Override
    protected void onDestroy() {
        if(bgmService != null && !isChangingConfigurations()) unbindService(connection);
        super.onDestroy();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        isReturningToMain = true;
    }

    @Override
    public void onClick(@NonNull View view) {
        Bundle bundle = (Bundle) view.getTag();
        MutableLiveData<ChipState> chipStateLiveData = Objects.requireNonNull(chipStateMap.get(bundle));
        ChipState currentChipState = Objects.requireNonNull(chipStateLiveData.getValue());
        ChipState newChipState = currentChipState == ChipState.DEACTIVATED ? ChipState.ASCENDING :
                (currentChipState == ChipState.ASCENDING ? ChipState.DESCENDING : ChipState.DEACTIVATED);
        chipStateLiveData.setValue(newChipState);

        Chip chipClicked = (Chip) view;
        chipClicked.setText(newChipState == ChipState.DESCENDING ?
                bundle.getString(BUNDLE_KEY_TEXT_DESC) : bundle.getString(BUNDLE_KEY_TEXT_ASC));
        if(newChipState == ChipState.DEACTIVATED) {
            chipClicked.setChipBackgroundColorResource(android.R.color.darker_gray);
            adapter.sortItemsToDefault();
        } else {
            chipClicked.setChipBackgroundColorResource(android.R.color.holo_green_light);
            adapter.sortItemsShown(Objects.requireNonNull(sortTypeMap.get(chipStateLiveData)),
                    newChipState == ChipState.DESCENDING);
            for (int i = 0; i < sortChipGroup.getChildCount(); i++) {
                Chip otherSimilarChip = (Chip) sortChipGroup.getChildAt(i);
                if (otherSimilarChip == chipClicked) continue;

                Bundle bundle1 = (Bundle) otherSimilarChip.getTag();
                MutableLiveData<ChipState> liveData = chipStateMap.get(bundle1);
                Objects.requireNonNull(liveData).setValue(ChipState.DEACTIVATED);
                otherSimilarChip.setChipBackgroundColorResource(android.R.color.darker_gray);
            }
        }
    }

    @Override
    public void onCheckedChanged(@NonNull CompoundButton compoundButton, boolean isChipChecked) {
        Chip child = (Chip) compoundButton;
        int colorResId = isChipChecked ?
                android.R.color.holo_green_light : android.R.color.darker_gray;
        child.setChipBackgroundColorResource(colorResId);

        if(compoundButton.getId() == R.id.chip_hiragana) {
            adapter.filterItemsShown(Japanese.Type.HIRAGANA, !isChipChecked);
        } else if(compoundButton.getId() == R.id.chip_katakana) {
            adapter.filterItemsShown(Japanese.Type.KATAKANA, !isChipChecked);
        } else if(compoundButton.getId() == R.id.chip_kanji) {
            adapter.filterItemsShown(Japanese.Type.KANJI, !isChipChecked);
        } else {
            if(compoundButton.getId() == R.id.chip_quiz) {
                Chip otherSimilarChip = findViewById(R.id.chip_challenge);
                adapter.filterItemsShown(isChipChecked, otherSimilarChip.isChecked());
            } else {
                Chip otherSimilarChip = findViewById(R.id.chip_quiz);
                adapter.filterItemsShown(otherSimilarChip.isChecked(), isChipChecked);
            }
        }
    }

    @Override
    public void onItemClick(@NonNull GuessableJapaneseCharacter itemClickedContents) {
        characterRomajiTV.setText(getString(R.string.textView_character_romaji,
                itemClickedContents.getCharacter().toEnglish()));
        characterTypeTV.setText(getString(R.string.textView_character_type,
                itemClickedContents.getCharacter().getType().toString()));
        totalCharacterAttemptsTV.setText(getString(R.string.textView_character_total_integer_stats,
                itemClickedContents.getQuizAttempts()));
        characterAttemptsTV.setText(getString(R.string.textView_character_integer_stats,
                itemClickedContents.getQuizAttempts(), itemClickedContents.getChallengeAttempts()));

        totalSuccessCountTV.setText(getString(R.string.textView_character_total_integer_stats,
                itemClickedContents.getQuizSuccessCount()));
        characterSuccessCountTV.setText(getString(R.string.textView_character_integer_stats,
                itemClickedContents.getQuizSuccessCount(), itemClickedContents.getChallengeSuccessCount()));

        totalSuccessRateTV.setText(getString(R.string.textView_character_total_rate_stats,
                getSuccessRateToPrint(itemClickedContents.getTotalSuccessRate())));
        characterSuccessRateTV.setText(getString(R.string.textView_character_rates_stats,
                getSuccessRateToPrint(itemClickedContents.getQuizSuccessRate()),
                getSuccessRateToPrint(itemClickedContents.getChallengeSuccessRate())));
    }

    private String getSuccessRateToPrint(float successRate) {
        if(successRate == 0) return "0";

        String successRateInString = Float.toString(successRate);
        boolean isDecimalRedundant = Pattern.matches("^\\d+\\.[0]$", successRateInString);
        return isDecimalRedundant ?
                successRateInString.substring(0, successRateInString.indexOf(".")) :
                successRateInString;
    }
}