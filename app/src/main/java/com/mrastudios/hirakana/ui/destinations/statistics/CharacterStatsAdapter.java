package com.mrastudios.hirakana.ui.destinations.statistics;

import android.graphics.Color;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.RecyclerView;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.GuessableJapaneseCharacter;
import com.mrastudios.hirakana.domain.GuessableJapaneseCharacters;
import com.mrastudios.hirakana.domain.Japanese;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

final class CharacterStatsAdapter extends RecyclerView.Adapter<CharacterStatsAdapter.ViewHolder>
        implements Serializable
{
    private final List<GuessableJapaneseCharacter> hiraganas;
    private final List<GuessableJapaneseCharacter> katakanas;
    private final List<GuessableJapaneseCharacter> kanjis;
    private final List<GuessableJapaneseCharacter> defaultDatas;

    private final List<GuessableJapaneseCharacter> datasShown = new ArrayList<>();
    private boolean isHiraganaShown = true;
    private boolean isKatakanaShown = true;
    private boolean isKanjiShown = true;

    private boolean hasQuizItems = false;
    private boolean hasChallengeItems = false;

    private transient final Map<SortType, Comparator<GuessableJapaneseCharacter>> sortTypeComparatorMap;
    {
        sortTypeComparatorMap = new HashMap<>();
        sortTypeComparatorMap.put(SortType.ALPHABETICAL, Comparator.comparing(Object::toString));
        sortTypeComparatorMap.put(SortType.SUCCESS_RATES, new GuessableJapaneseCharacters.SuccessRateCompare(
                GuessableJapaneseCharacters.SuccessRateCompare.Type.TOTAL));
    }
    private SortType activeSort;
    private boolean isActiveSortReversed = false;
    private boolean isItemLayoutDefault;

    private transient OnItemClickListener listener;
    private transient ViewHolder itemSelected;
    private GuessableJapaneseCharacter itemSelectedContent;

    enum SortType { ALPHABETICAL, SUCCESS_RATES }

    interface OnItemClickListener {
        void onItemClick(@NonNull GuessableJapaneseCharacter itemClickedContents);
    }

    CharacterStatsAdapter(@NonNull GuessableJapaneseCharacters guessableJapaneseCharacters,
                          @Nullable GuessableJapaneseCharacter itemToSetAsSelected)
    {
        itemSelectedContent = itemToSetAsSelected;

        hiraganas = guessableJapaneseCharacters.getGuessables(Japanese.Type.HIRAGANA);
        katakanas = guessableJapaneseCharacters.getGuessables(Japanese.Type.KATAKANA);
        kanjis = guessableJapaneseCharacters.getGuessables(Japanese.Type.KANJI);

        datasShown.addAll(guessableJapaneseCharacters.getAllGuessables());
        defaultDatas = new ArrayList<>(datasShown);
    }

    CharacterStatsAdapter(@NonNull CharacterStatsAdapter previousAdapter)
    {
        hiraganas = previousAdapter.hiraganas;
        katakanas = previousAdapter.katakanas;
        kanjis = previousAdapter.kanjis;

        defaultDatas = previousAdapter.defaultDatas;
        datasShown.addAll(previousAdapter.datasShown);

        itemSelected = previousAdapter.itemSelected;
        itemSelectedContent = previousAdapter.itemSelectedContent;

        isHiraganaShown = previousAdapter.isHiraganaShown;
        isKatakanaShown = previousAdapter.isKatakanaShown;
        isKanjiShown = previousAdapter.isKanjiShown;

        hasQuizItems = previousAdapter.hasQuizItems;
        hasChallengeItems = previousAdapter.hasChallengeItems;

        activeSort = previousAdapter.activeSort;
        isActiveSortReversed = previousAdapter.isActiveSortReversed;
        isItemLayoutDefault = previousAdapter.isItemLayoutDefault;
    }

    static class ViewHolder extends RecyclerView.ViewHolder
    {
        private final TextView character;
        private final ProgressBar successRate;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            character = itemView.findViewById(R.id.charactersRecyclerView);
            successRate = itemView.findViewById(R.id.successRateProgressBar);
        }
    }

    static class ItemDecoration extends RecyclerView.ItemDecoration
    {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
        {
            int itemPos = parent.getChildAdapterPosition(view);
            int totalItemCount = Objects.requireNonNull(parent.getAdapter()).getItemCount();

            if(itemPos != RecyclerView.NO_POSITION && itemPos != totalItemCount - 1) {
                if(Objects.requireNonNull(parent.getLayoutManager()).canScrollHorizontally()) {
                    outRect.right = 5;
                } else {
                    outRect.bottom = 5;
                }
            }
        }
    }

    @Override
    public int getItemViewType(int position) {
        return isItemLayoutDefault ? R.layout.item_statistics_character_jpn_big :
                R.layout.item_statistics_character_jpn_small;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(viewType, parent, false);

        ViewHolder holder = new ViewHolder(view);
        view.setOnClickListener(itemClicked -> selectItem(holder));
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        GuessableJapaneseCharacter japaneseCharacter = datasShown.get(position);
        holder.character.setText(japaneseCharacter.getCharacter().toJapanese());
        holder.successRate.setProgress(Math.round(japaneseCharacter.getQuizSuccessRate()), true);

        if(position == 0 && itemSelectedContent == null) selectItem(holder);
        if(itemSelectedContent != null) {
            if(itemSelectedContent == japaneseCharacter) {
                selectItem(holder);
            } else {
                holder.character.setBackgroundResource(android.R.color.holo_green_light);
            }
        }
    }

    @Override
    public int getItemCount() {
        return datasShown.size();
    }

    private void selectItem(@NonNull ViewHolder holder) {
        if(itemSelected != null) itemSelected.character.setBackgroundResource(
                android.R.color.holo_green_light);
        itemSelected = holder;
        itemSelected.character.setBackgroundColor(Color.CYAN);

        GuessableJapaneseCharacter previousItemContent = itemSelectedContent;
        itemSelectedContent = datasShown.get(holder.getAdapterPosition());
        if(previousItemContent != itemSelectedContent && listener != null) listener.onItemClick(itemSelectedContent);
    }

    private int getIndexOfFirstItemChanged(@NonNull List<GuessableJapaneseCharacter> dataBeforeChanges,
                                           @NonNull List<GuessableJapaneseCharacter> dataAfterChanges)
    {
        int posItemFirstChanged = 0;
        for(int i = 0; i < dataAfterChanges.size(); i++) {
            if(i > dataBeforeChanges.size() - 1 || dataAfterChanges.get(i) != dataBeforeChanges.get(i)) break;
            posItemFirstChanged++;
        }
        return posItemFirstChanged;
    }

    private void notifyChanges(@NonNull List<GuessableJapaneseCharacter> dataBeforeChanges,
                               @NonNull List<GuessableJapaneseCharacter> dataAfterChanges)
    {
        if(dataAfterChanges.size() > dataBeforeChanges.size()) {
            notifyItemRangeInserted(dataBeforeChanges.size(),
                    dataAfterChanges.size() - dataBeforeChanges.size());
        } else if(dataAfterChanges.size() < dataBeforeChanges.size()) {
            notifyItemRangeRemoved(dataAfterChanges.size(),
                    dataBeforeChanges.size() - dataAfterChanges.size());
        }
        int index = getIndexOfFirstItemChanged(dataBeforeChanges, dataAfterChanges);
        notifyItemRangeChanged(index, dataAfterChanges.size() - index);
    }

    private void notifyItemSelectedRemoved() {
        itemSelected = null;
        itemSelectedContent = null;
        if(!datasShown.isEmpty()) {
            notifyItemChanged(0);
            listener.onItemClick(datasShown.get(0));
        }
    }

    private boolean filterItemsByCharacterStats() {
        List<GuessableJapaneseCharacter> targetData = new ArrayList<>(defaultDatas);
        if(!isHiraganaShown) targetData.removeAll(hiraganas);
        if(!isKatakanaShown) targetData.removeAll(katakanas);
        if(!isKanjiShown) targetData.removeAll(kanjis);

        boolean isItemSelectedRemoved = false;
        for (GuessableJapaneseCharacter jpChar : targetData) {
            if(hasQuizItems && hasChallengeItems) {
                if(jpChar.getQuizAttempts() == 0 && jpChar.getChallengeAttempts() == 0) {
                    if(datasShown.remove(jpChar) && jpChar == itemSelectedContent) {
                        isItemSelectedRemoved = true;
                    }
                } else {
                    if(!datasShown.contains(jpChar)) datasShown.add(jpChar);
                }
            } else if((hasQuizItems && jpChar.getQuizAttempts() == 0) ||
                    (hasChallengeItems && jpChar.getChallengeAttempts() == 0))
            {
                if(datasShown.remove(jpChar) && jpChar == itemSelectedContent) {
                    isItemSelectedRemoved = true;
                }
            } else {
                if(!datasShown.contains(jpChar)) datasShown.add(jpChar);
            }
        }
        datasShown.sort(Comparator.comparing(GuessableJapaneseCharacter::getCharacter));
        return isItemSelectedRemoved;
    }

    private boolean filterItemByCharacterType(@NonNull Japanese.Type type, boolean isRemoving) {
        List<GuessableJapaneseCharacter> targetCharactersList;
        switch (type) {
            case HIRAGANA:
                targetCharactersList = new ArrayList<>(hiraganas);
                isHiraganaShown = !isRemoving;
                break;
            case KATAKANA:
                targetCharactersList = new ArrayList<>(katakanas);
                isKatakanaShown = !isRemoving;
                break;
            default:
                targetCharactersList = new ArrayList<>(kanjis);
                isKanjiShown = !isRemoving;
        }

        List<GuessableJapaneseCharacter> dataShownBeforeChanges = new ArrayList<>(datasShown);
        boolean isSelectedItemRemoved = false;
        for(GuessableJapaneseCharacter targetCharacter : targetCharactersList) {
            if(isRemoving) {
                if(datasShown.remove(targetCharacter)) {
                    if (targetCharacter == itemSelectedContent) isSelectedItemRemoved = true;
                }
            } else if ((!hasQuizItems && !hasChallengeItems) ||
                    (hasQuizItems && targetCharacter.getQuizAttempts() != 0) ||
                    (hasChallengeItems && targetCharacter.getChallengeAttempts() != 0))
            {
                datasShown.add(targetCharacter);
            }
        }
        datasShown.sort(Comparator.comparing(GuessableJapaneseCharacter::getCharacter));
        notifyChanges(dataShownBeforeChanges, datasShown);
        return isSelectedItemRemoved;
    }

    private void sortToDefault(boolean isExplicitlySortedByUser) {
        if(isExplicitlySortedByUser){
            activeSort = null;
            isActiveSortReversed = false;
        }

        List<GuessableJapaneseCharacter> dataShownBeforeChanges = new ArrayList<>(datasShown);
        datasShown.clear();

        if(isHiraganaShown) datasShown.addAll(hiraganas);
        if(isKatakanaShown) datasShown.addAll(katakanas);
        if(isKanjiShown) datasShown.addAll(kanjis);

        if(hasQuizItems || hasChallengeItems) {
            boolean isItemSelectedRemoved = filterItemsByCharacterStats();
            if(isItemSelectedRemoved) notifyItemSelectedRemoved();
        }
        if(!datasShown.isEmpty() && activeSort != null) {
            sortItemsShown(activeSort, isActiveSortReversed);
        } else {
            datasShown.sort(Comparator.comparing(GuessableJapaneseCharacter::getCharacter));
        }

        notifyChanges(dataShownBeforeChanges, datasShown);
    }

    void filterItemsShown(@NonNull Japanese.Type type, boolean isRemoving) {
        boolean isSelectedItemRemoved = filterItemByCharacterType(type, isRemoving);
        if(isSelectedItemRemoved) notifyItemSelectedRemoved();
        if(activeSort != null) sortItemsShown(activeSort, isActiveSortReversed);
    }

    void filterItemsShown(boolean showItemsWithQuizStats, boolean showItemsWithChallengeStats) {
        hasQuizItems = showItemsWithQuizStats;
        hasChallengeItems = showItemsWithChallengeStats;
        if(!showItemsWithQuizStats && !showItemsWithChallengeStats) {
            sortToDefault(false);
            return;
        }

        if(isHiraganaShown || isKatakanaShown || isKanjiShown) {
            List<GuessableJapaneseCharacter> dataShownBeforeChanges = new ArrayList<>(datasShown);
            boolean isItemSelectedRemoved = filterItemsByCharacterStats();
            notifyChanges(dataShownBeforeChanges, datasShown);
            if(isItemSelectedRemoved) notifyItemSelectedRemoved();

            if (!datasShown.isEmpty() && activeSort != null) {
                sortItemsShown(activeSort, isActiveSortReversed);
            }
        }
    }

    void sortItemsShown(@NonNull SortType sortType, boolean reverse) {
        List<GuessableJapaneseCharacter> dataShownBeforeChanges = new ArrayList<>(datasShown);
        datasShown.sort(Comparator.comparing(GuessableJapaneseCharacter::getCharacter));
        datasShown.sort(sortTypeComparatorMap.get(sortType));

        activeSort = sortType;
        if(reverse) {
            Collections.reverse(datasShown);
            isActiveSortReversed = true;
        }
        notifyChanges(dataShownBeforeChanges, datasShown);
    }

    void sortItemsToDefault() {
        sortToDefault(true);
    }

    void setOnItemClickListener(@NonNull OnItemClickListener listener) {
        this.listener = listener;
    }

    void swapItemLayout() {
        isItemLayoutDefault = !isItemLayoutDefault;
    }
}