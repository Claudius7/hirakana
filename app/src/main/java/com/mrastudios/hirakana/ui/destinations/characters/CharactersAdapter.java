package com.mrastudios.hirakana.ui.destinations.characters;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.Build;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mrastudios.hirakana.R;
import com.mrastudios.hirakana.domain.Japanese;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

public final class CharactersAdapter extends RecyclerView.Adapter<CharactersAdapter.CharacterViewModel>
    implements Serializable
{
    private final List<Japanese.Hiragana> hiraganas;
    private final List<Japanese.Katakana> katakanas;
    private final List<Japanese.Kanji> kanjis;

    private final List<Japanese.Character> dataShown;
    private boolean isCardsSmall;

    private final Map<Japanese.Type, Boolean> filters;
    private SmallCardItemSize smallCardItemSize;
    private int orientation;

    enum SmallCardItemSize {
        SMALL(55, 15),
        MEDIUM(65, 20),
        LARGE(75, 25);

        private final int textSizeJPN;
        private final int textSizeENG;

        SmallCardItemSize(int textSizeJPN, int textSizeENG) {
            this.textSizeJPN = textSizeJPN;
            this.textSizeENG = textSizeENG;
        }
    }

    @SuppressWarnings("unchecked")
    public CharactersAdapter(@Nullable SmallCardItemSize smallCardItemSize, boolean isCardsSmall) {
        this.smallCardItemSize = smallCardItemSize == null ? SmallCardItemSize.SMALL : smallCardItemSize;
        this.isCardsSmall = isCardsSmall;

        filters = new EnumMap<>(Japanese.Type.class);
        for (Japanese.Type type : Japanese.Type.values()) {
            filters.put(type, true);
        }

        Japanese japanese = Japanese.getInstance();
        hiraganas = (List<Japanese.Hiragana>) japanese.getCharacters(Japanese.Type.HIRAGANA);
        katakanas = (List<Japanese.Katakana>) japanese.getCharacters(Japanese.Type.KATAKANA);
        kanjis = (List<Japanese.Kanji>) japanese.getCharacters(Japanese.Type.KANJI);

        dataShown = new ArrayList<>();
        dataShown.addAll(japanese.getAllCharacters());
    }

    static class CharacterViewModel extends RecyclerView.ViewHolder
    {
        private final TextView characterJPN;
        private final TextView characterENG;

        public CharacterViewModel(@NonNull View itemView) {
            super(itemView);
            characterJPN = itemView.findViewById(R.id.characterJPN);
            characterENG = itemView.findViewById(R.id.characterENG);
        }
    }

    public static class CharactersItemDecoration extends RecyclerView.ItemDecoration
    {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
        {
            int itemPos = parent.getChildAdapterPosition(view);
//            int totalItemCount = parent.getAdapter().getItemCount();

            if(itemPos != RecyclerView.NO_POSITION &&
                    parent.getLayoutManager() instanceof GridLayoutManager)
            {
                GridLayoutManager layoutManager = (GridLayoutManager) parent.getLayoutManager();
                if(itemPos < layoutManager.getSpanCount()) outRect.top = 15;
                if(itemPos % layoutManager.getSpanCount() == 0) outRect.left = 15;
            } else {
                outRect.top = 15;
                outRect.left = 15;
            }
            outRect.bottom = 15;
            outRect.right = 15;
        }
    }

    @Override
    public void onAttachedToRecyclerView(@NonNull RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);
        orientation = recyclerView.getContext().getResources().getConfiguration().orientation;
    }

    @Override
    public int getItemViewType(int position) {
        return isCardsSmall ? R.layout.item_character_card_small : R.layout.item_character_card_big;
    }

    @NonNull
    @Override
    public CharacterViewModel onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        Context context = parent.getContext();
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(viewType, parent, false);
        new Japanese.Hiragana(Japanese.Hiragana.Character.BA);

        CharacterViewModel holder = new CharacterViewModel(view);
        holder.characterJPN.setOnLongClickListener(view1 -> {
            Japanese.Character japaneseCharacter = dataShown.get(holder.getAdapterPosition());
            if(japaneseCharacter instanceof Japanese.Kanji) {
                Japanese.Kanji kanjiCharacter = (Japanese.Kanji) japaneseCharacter;
                Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
                Toast.makeText(context, kanjiCharacter.getMeaning(), Toast.LENGTH_SHORT).show();
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    v.vibrate(VibrationEffect.createWaveform(new long[]{100, 100}, 0));
                    view1.postDelayed(v::cancel, 150);
                } else {
                    v.vibrate(500);
                }
            }
            return true;
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull CharacterViewModel holder, int position) {
        Japanese.Character data = dataShown.get(position);
        holder.characterJPN.setText(data.toJapanese());
        holder.characterENG.setText(data.toEnglish());

        if(data.toJapanese().length() > 1 && orientation == Configuration.ORIENTATION_PORTRAIT) {
            if(isCardsSmall) {
                holder.characterJPN.setTextSize(smallCardItemSize.textSizeJPN);
                holder.characterENG.setTextSize(smallCardItemSize.textSizeENG);
            } else {
                holder.characterJPN.setTextSize(250 * 0.72f);
            }
        } else if(isCardsSmall) {
            holder.characterJPN.setTextSize(smallCardItemSize.textSizeJPN);
            holder.characterENG.setTextSize(smallCardItemSize.textSizeENG);
        } else {
            if(orientation == Configuration.ORIENTATION_LANDSCAPE) return;
            holder.characterJPN.setTextSize(250); // 250 is the default textSize in xml
        }

        // Fix for when the textView's text is not selectable after recycling says in StackOverFlow.
        // Tested. This does fix it.
        holder.characterJPN.setTextIsSelectable(false);
        holder.characterENG.setTextIsSelectable(false);
        holder.characterJPN.post(() -> {
            holder.characterJPN.setTextIsSelectable(true);
            holder.characterENG.setTextIsSelectable(true);
        });
    }

    @Override
    public int getItemCount() {
        return dataShown.size();
    }

    private int getIndexOfFirstItemChanged(@NonNull List<Japanese.Character> dataBeforeChanges,
                                           @NonNull List<Japanese.Character> dataAfterChanges)
    {
        int posItemFirstChanged = 0;
        for(int i = 0; i < dataAfterChanges.size(); i++) {
            if(i > dataBeforeChanges.size() - 1 || dataAfterChanges.get(i) != dataBeforeChanges.get(i)) break;
            posItemFirstChanged++;
        }
        return posItemFirstChanged;
    }

    private void notifyChanges(@NonNull List<Japanese.Character> dataBeforeChanges,
                               @NonNull List<Japanese.Character> dataAfterChanges)
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

    private void searchData(@NonNull String query) {
        List<Japanese.Character> characters = new ArrayList<>(hiraganas);
        characters.addAll(katakanas);
        characters.addAll(kanjis);

        for (Japanese.Character character : characters) {
            String jpCharJPN = character.toJapanese();
            String jpCharENG = character.toEnglish();
            String jpCharENG_ALT = !jpCharENG.contains("(") ? null :
                    jpCharENG.substring(jpCharENG.indexOf("(") + 1, jpCharENG.indexOf(")"));

            boolean isMatch;
            if(query.length() == 1) {
                isMatch = query.equals(jpCharJPN) || query.equalsIgnoreCase(jpCharENG.substring(0, 1));
                if(!isMatch && jpCharENG_ALT != null) isMatch = query.equalsIgnoreCase(
                        jpCharENG_ALT.substring(0, 1));
            } else {
                isMatch = query.equalsIgnoreCase(jpCharENG) || query.equalsIgnoreCase(jpCharENG_ALT);
                boolean isInvalid = query.length() > 3 && (character instanceof Japanese.Hiragana ||
                        character instanceof Japanese.Katakana);
                if (!isMatch && !isInvalid) {
                    boolean isQueryJapanese = !query.matches("[A-z]+");
                    String stringToCompare = isQueryJapanese ? jpCharJPN : jpCharENG;
                    if(stringToCompare.length() >= query.length()) {
                        for (int i = 0; i < query.length(); i++) {
                            String queryCharAt = String.valueOf(query.charAt(i));
                            String jpCharAt = String.valueOf(stringToCompare.charAt(i));

                            if (!queryCharAt.equalsIgnoreCase(jpCharAt)) {
                                if (!isQueryJapanese && jpCharENG_ALT != null &&
                                        stringToCompare.equals(jpCharENG))
                                {
                                    stringToCompare = jpCharENG_ALT;
                                    i = 0;
                                    continue;
                                }
                                break;
                            }
                            if (i == query.length() - 1) isMatch = true;
                        }
                    }
                }
            }
            if(isMatch && !dataShown.contains(character)) dataShown.add(character);
            if(!isMatch) dataShown.remove(character);
        }
        Japanese.sort(dataShown);
    }

    private void filterData(@NonNull Map<Japanese.Type, Boolean> filters, boolean isRemoveOnly) {
        for(Map.Entry<Japanese.Type, Boolean> entry : filters.entrySet()) {
            boolean isAddingItems = entry.getValue();
            if(isRemoveOnly && isAddingItems) continue;

            List<? extends Japanese.Character> targetData;
            switch (entry.getKey()) {
                case HIRAGANA:
                    targetData = hiraganas;
                    break;
                case KATAKANA:
                    targetData = katakanas;
                    break;
                default:
                    targetData = kanjis;
            }
            if(isAddingItems) {
                if(!dataShown.containsAll(targetData)) dataShown.addAll(targetData);
            } else {
                for(Japanese.Character jpCharToRemove : targetData) {
                    dataShown.remove(jpCharToRemove);
                }
            }
        }
    }

    public void resetItemsToDefault() {
        List<Japanese.Character> dataBeforeChanges = new ArrayList<>(dataShown);
        dataShown.clear();
        for (Map.Entry<Japanese.Type, Boolean> entry : filters.entrySet()) {
            boolean isIncluded = entry.getValue();
            if (!isIncluded) continue;
            switch (entry.getKey()) {
                case HIRAGANA:
                    dataShown.addAll(hiraganas);
                    continue;
                case KATAKANA:
                    dataShown.addAll(katakanas);
                    break;
                default:
                    dataShown.addAll(kanjis);
            }
        }
        Japanese.sort(dataShown);
        notifyChanges(dataBeforeChanges, dataShown);
    }

    /**
     * @param query the character to search for in either Romaji(English) or the exact japanese character.
     * @return number of items visible after the search.
     */
    public int searchItems(@NonNull String query) {
        List<Japanese.Character> dataBeforeChanges = new ArrayList<>(dataShown);
        if (query.isEmpty()) {
            resetItemsToDefault();
            notifyChanges(dataBeforeChanges, dataShown);
        } else if (!query.matches("^([A-z]+|[ぁ-ヿ]{1,2}|[㐂-䶵丂-鿌車-舘])")) {
            int sizeBeforeClearing = dataShown.size();
            dataShown.clear();
            notifyItemRangeRemoved(0, sizeBeforeClearing);
        } else {
            searchData(query);
            if (filters != null) filterData(filters, true);
            notifyChanges(dataBeforeChanges, dataShown);
        }
        return dataShown.size();
    }


    /**
     * @param filters used to add or remove {@link Japanese.Type} types on the datas shown.
     * @param query the character to search for in either Romaji(English) or
     *             the exact japanese character.
     * @return number of items visible after the operation.
     */
    public int filterItems(@NonNull Map<Japanese.Type, Boolean> filters, @Nullable String query) {
        List<Japanese.Character> dataShownBeforeChanges = new ArrayList<>(dataShown);
        this.filters.putAll(filters);
        if(query == null || query.isEmpty()) {
            filterData(filters, false);
        } else {
            searchData(query);
            if(!dataShown.isEmpty()) filterData(filters, true);
        }
        notifyChanges(dataShownBeforeChanges, dataShown);
        return dataShown.size();
    }

    public void resizeSmallCardItems(@NonNull SmallCardItemSize smallCardItemSize) {
        if(!isCardsSmall) return;
        this.smallCardItemSize = smallCardItemSize;
        notifyItemRangeChanged(0, dataShown.size());
    }

    public void swapItemLayout() {
        isCardsSmall = !isCardsSmall;
        notifyItemRangeChanged(0, dataShown.size());
    }

    @NonNull
    public Map<Japanese.Type, Boolean> getFilters() {
        return filters;
    }
}