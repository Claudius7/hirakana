package com.mrastudios.hirakana.ui.destinations.characters;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.LinearSnapHelper;
import androidx.recyclerview.widget.RecyclerView;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.IBinder;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.SearchView;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.Japanese;
import com.mrastudios.hirakana.ui.destinations.characters.dialogs.FilterDialogFragment;
import com.mrastudios.hirakana.ui.services.BgmService;

import java.util.Map;
import java.util.Objects;

public final class CharactersActivity extends AppCompatActivity
        implements FilterDialogFragment.OnApplyFilterListener
{
    private static final String PREF_TEXT_SIZE = "ItemTextSize777";
    private static final String PREF_CARD_SIZE = "ItemCardSize777";

    private SharedPreferences preferences;
    private CharactersViewModel viewModel;

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

    private RecyclerView recyclerView;
    private CharactersAdapter adapter;
    private LinearSnapHelper snapHelper;
    private Menu menu;

    private int orientation;
    private boolean isReturningToMain;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_characters);
        setSupportActionBar(findViewById(R.id.toolbar_characters));

        viewModel = new ViewModelProvider(this).get(CharactersViewModel.class);
        viewModel.getAdapterLiveData().observe(this, charactersAdapter -> {
            adapter = charactersAdapter;

            recyclerView = findViewById(R.id.charactersRecyclerView);
            recyclerView.setAdapter(adapter);
            recyclerView.addItemDecoration(new CharactersAdapter.CharactersItemDecoration());
            recyclerView.setLayoutManager(new GridLayoutManager(this,
                    orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 3));
        });

        orientation = getResources().getConfiguration().orientation;
        preferences = getPreferences(Context.MODE_PRIVATE);
        if(savedInstanceState == null) restoreUserPreferences();
    }

    private void restoreUserPreferences() {
        CharactersAdapter adapter;
        if(!preferences.contains(PREF_TEXT_SIZE) && !preferences.contains(PREF_CARD_SIZE)) {
            adapter = new CharactersAdapter(null,true);
        } else {
            int selectedOptionIndex = preferences.getInt(PREF_TEXT_SIZE, 0);
            CharactersAdapter.SmallCardItemSize itemSizes =
                    CharactersAdapter.SmallCardItemSize.values()[selectedOptionIndex];
            boolean isPreferredCardSmall = preferences.getBoolean(PREF_CARD_SIZE, true);
            adapter = new CharactersAdapter(itemSizes, isPreferredCardSmall);
        }
        viewModel.getAdapterLiveData().setValue(adapter);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_characters, menu);
        initMenuSearchView(menu);
        restoreMenuPreferences(menu);
        return super.onCreateOptionsMenu((this.menu = menu));
    }

    private void initMenuSearchView(@NonNull Menu menu) {
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchItem.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
            @Override
            public boolean onMenuItemActionExpand(MenuItem item) {
                return true;
            }

            @Override
            public boolean onMenuItemActionCollapse(MenuItem item) {
                searchView.clearFocus();
                searchView.setQuery("", false);
                adapter.resetItemsToDefault();
                viewModel.getQueryLiveData().setValue("");
                return true;
            }
        });
        searchView.setQueryHint(getString(R.string.menu_search_hint));
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String submittedText) {
                int itemsVisible = adapter.searchItems(submittedText);
                findViewById(R.id.textview_no_items).setVisibility(itemsVisible == 0 ?
                        View.VISIBLE : View.INVISIBLE);
                searchView.clearFocus();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                if (newText.isEmpty()) adapter.resetItemsToDefault();
                viewModel.getQueryLiveData().setValue(newText);
                return true;
            }
        });
        searchView.setOnQueryTextFocusChangeListener((v, hasFocus) ->
                viewModel.getIsUserTypingLiveData().setValue(hasFocus));

        boolean isUserTyping = Objects.requireNonNull(viewModel.getIsUserTypingLiveData().getValue());
        String previousQuery = Objects.requireNonNull(viewModel.getQueryLiveData().getValue());
        searchView.setIconified(!isUserTyping && !previousQuery.isEmpty());
        // The call to SearchView.setIconified() sets the focus to the searchView so this
        // if statement is necessary to clear that focus for searchView's behavior to work
        if(!isUserTyping) searchView.clearFocus();

        if(isUserTyping || !previousQuery.isEmpty()) searchItem.expandActionView();
        searchView.setQuery(previousQuery, false);
    }

    private void restoreMenuPreferences(@NonNull Menu menu) {
        int optionSelectedIndex = preferences.getInt(PREF_TEXT_SIZE, 0);
        switch (optionSelectedIndex) {
            case 1:
                menu.findItem(R.id.mediumText).setChecked(true);
                break;
            case 2:
                menu.findItem(R.id.largeText).setChecked(true);
                break;
            default:
                menu.findItem(R.id.smallText).setChecked(true);
        }

        boolean isCardSmall = preferences.getBoolean(PREF_CARD_SIZE, true);
        if (!isCardSmall) {
            MenuItem cardSizeItem = menu.findItem(R.id.action_card_size);
            cardSizeItem.setTitle(R.string.menu_item_text_small_cards);
            cardSizeItem.setIcon(R.drawable.ic_menu_small_cards);

            recyclerView.setLayoutManager(new LinearLayoutManager(this,
                    LinearLayoutManager.HORIZONTAL, false));
            snapHelper = new LinearSnapHelper();
            if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                snapHelper.attachToRecyclerView(recyclerView);
            }
        }

        int itemTextSizeGroupId = menu.findItem(R.id.smallText).getGroupId();
        menu.setGroupEnabled(itemTextSizeGroupId, isCardSmall);
    }

    @Override
    protected void onStart() {
        super.onStart();
        bindService(new Intent(this, BgmService.class),
                connection, Context.BIND_AUTO_CREATE);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        FilterDialogFragment filterDialog = (FilterDialogFragment)
                getSupportFragmentManager().findFragmentByTag(FilterDialogFragment.TAG);
        if(filterDialog != null) filterDialog.setOnApplyFilterListener(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(bgmService != null) bgmService.resumeBgm();
    }

    @Override
    protected void onPause() {
        super.onPause();
        if(bgmService != null && !isChangingConfigurations() &&
                !isReturningToMain) bgmService.pauseBgm();
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
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        int selectedItemId = item.getItemId();
        if(selectedItemId == R.id.action_filter) {
            FilterDialogFragment filterDialog = (FilterDialogFragment)
                    getSupportFragmentManager().findFragmentByTag(FilterDialogFragment.TAG);
            if(filterDialog == null) filterDialog = FilterDialogFragment.newInstance(adapter.getFilters());
            filterDialog.setOnApplyFilterListener(this);
            if(!filterDialog.isAdded()) filterDialog.show(getSupportFragmentManager(), FilterDialogFragment.TAG);
            return true;
        } else if(item.getGroupId() == R.id.action_group_text_size) {
            int itemIndex = 0;
            if(selectedItemId == R.id.mediumText) itemIndex++;
            if(selectedItemId == R.id.largeText) itemIndex += 2;

            adapter.resizeSmallCardItems(CharactersAdapter.SmallCardItemSize.values()[itemIndex]);
            preferences.edit().putInt(PREF_TEXT_SIZE, itemIndex).apply();
            item.setChecked(true);
            return true;
        } else if(selectedItemId == R.id.action_card_size) {
            boolean isChangingToBigCards = item.getTitle().equals(getString(R.string.menu_item_text_big_cards));
            preferences.edit().putBoolean(PREF_CARD_SIZE, !isChangingToBigCards).apply();

            item.setTitle(!isChangingToBigCards ? R.string.menu_item_text_big_cards : R.string.menu_item_text_small_cards);
            item.setIcon(!isChangingToBigCards ?
                    R.drawable.ic_menu_big_cards : R.drawable.ic_menu_small_cards);

            int itemTextSizeGroupId = menu.findItem(R.id.smallText).getGroupId();
            menu.setGroupEnabled(itemTextSizeGroupId, !isChangingToBigCards);

            adapter.swapItemLayout();
            RecyclerView.LayoutManager layoutManager = !isChangingToBigCards ?
                    new GridLayoutManager(this,
                            orientation == Configuration.ORIENTATION_PORTRAIT ? 2 : 3) :
                    new LinearLayoutManager(this,
                            LinearLayoutManager.HORIZONTAL, false);
            recyclerView.setLayoutManager(layoutManager);

            if(snapHelper == null) snapHelper = new LinearSnapHelper();
            if(orientation == Configuration.ORIENTATION_PORTRAIT) {
                snapHelper.attachToRecyclerView(!isChangingToBigCards ? null : recyclerView);
            } else {
                snapHelper.attachToRecyclerView(null);
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void onApplyFilter(@NonNull Map<Japanese.Type, Boolean> filters) {
        int itemsVisible = adapter.filterItems(filters,
                Objects.requireNonNull(viewModel.getQueryLiveData().getValue()));
        findViewById(R.id.textview_no_items).setVisibility(itemsVisible == 0 ?
                View.VISIBLE : View.INVISIBLE);
    }
}