package com.mrastudios.hirakana.ui.destinations.quiz;

import android.annotation.SuppressLint;
import android.graphics.Rect;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.annotation.DrawableRes;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.mrastudios.hirakana.domain.view_related.CustomButtonOnTouchListener;
import com.mrastudios.hirakana.R;

import java.io.Serializable;
import java.util.Objects;

public final class QuizChoicesAdapter extends RecyclerView.Adapter<QuizChoicesAdapter.ChoicesViewHolder>
    implements View.OnTouchListener, Serializable
{
    private transient OnChoiceClickListener onChoiceClickListener;
    private transient CustomButtonOnTouchListener onTouchListener;

    private final int defaultChoiceBackground;

    private String[] choicesShown;
    private int[] onChoiceClickBackground = new int[0];
    private int indexOfItemClicked = -1;

    public interface OnChoiceClickListener {
        void onChoiceClick(int index);
    }

    public QuizChoicesAdapter(@NonNull String[] choices, @DrawableRes int defaultChoiceBackground) {
        this.choicesShown = choices;
        this.defaultChoiceBackground = defaultChoiceBackground;
    }

    static class ChoicesViewHolder extends RecyclerView.ViewHolder
    {
        private final Button choice;

        public ChoicesViewHolder(@NonNull View itemView) {
            super(itemView);
            choice = itemView.findViewById(R.id.choice);
        }
    }

    public static class ChoicesItemDecoration extends RecyclerView.ItemDecoration
    {
        @Override
        public void getItemOffsets(@NonNull Rect outRect, @NonNull View view,
                                   @NonNull RecyclerView parent, @NonNull RecyclerView.State state)
        {
            int choicePos = parent.getChildAdapterPosition(view);
            int itemCount = Objects.requireNonNull(parent.getAdapter()).getItemCount();

            if(parent.getLayoutManager() instanceof GridLayoutManager) {
                if (choicePos != RecyclerView.NO_POSITION &&
                        choicePos != itemCount - 1 && choicePos != itemCount - 2) {
                    outRect.bottom = 8;
                }
                if (choicePos % 2 == 1) {
                    outRect.left = 4;
                } else {
                    outRect.right = 4;
                }
            } else {
                if(choicePos != itemCount - 1) outRect.bottom = 16;
                outRect.left = 100;
                outRect.right = 100;
            }
        }
    }

    @NonNull
    @Override
    public ChoicesViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        View view = inflater.inflate(R.layout.item_quiz_choices, parent, false);

        ChoicesViewHolder holder = new ChoicesViewHolder(view);
        view.findViewById(R.id.choice).setOnTouchListener(this);
        view.findViewById(R.id.choice).setOnClickListener((userAnswer) -> {
            indexOfItemClicked = holder.getAdapterPosition();
            if(onChoiceClickListener != null) onChoiceClickListener.onChoiceClick(indexOfItemClicked);
        });
        return holder;
    }

    @Override
    public void onBindViewHolder(@NonNull ChoicesViewHolder holder, int position) {
        holder.choice.setText(choicesShown[position]);
        int res = onChoiceClickBackground.length != 0 ?
                onChoiceClickBackground[position] : defaultChoiceBackground;
        holder.choice.setBackgroundResource(res);
    }

    @Override
    public int getItemCount() {
        return choicesShown.length;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouch(View view, @NonNull MotionEvent motionEvent) {
        if(indexOfItemClicked != -1 || onChoiceClickBackground.length != 0) {
            if(onChoiceClickListener != null) onChoiceClickListener.onChoiceClick(indexOfItemClicked);
            view.setPadding(0,0,0,8);
            return true;
        }
        if(onTouchListener != null) onTouchListener.onTouch(view, motionEvent);
        return false;
    }

    public void setNewChoices(String[] newChoices) {
        indexOfItemClicked = -1;
        this.choicesShown = newChoices;
        onChoiceClickBackground = new int[0];
        notifyItemRangeChanged(0, getItemCount());
    }

    /**
     * Changes the background of the choices and will only revert to its default background supplied
     * to this adapter when {@link #setNewChoices(String[])} is called.<br/><br/>
     *
     * This will only work if the number of items in this adapter matches the number of resources
     * supplied in <code> drawableRes </code>. For precision use {@link #getItemCount()}.
     */
    public void setChoicesBackground(@NonNull @DrawableRes int[] drawableRes) {
        if(drawableRes.length != getItemCount()) return;
        this.onChoiceClickBackground = drawableRes;
        notifyItemRangeChanged(0, getItemCount());
    }

    public void setOnTouchListener(CustomButtonOnTouchListener onTouchListener) {
        this.onTouchListener = onTouchListener;
    }

    public void setOnChoiceClickListener(OnChoiceClickListener onChoiceClickListener) {
        this.onChoiceClickListener = onChoiceClickListener;
    }
}
