package modules_content_activity_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tajos.iccnotes.R;

import java.util.ArrayList;
import java.util.List;

import layouts.RoundedLayout;

public class ModuleContentAdapter extends RecyclerView.Adapter<ModuleContentAdapter.ViewHolder> {
    private static final String TAG = "ModuleContentAdapter";
    private static boolean isOnSearchMode = false;

    private final int HIGHLIGHT_COLOR = Color.parseColor("#79FFE603");

    private List<int[]> indexes = new ArrayList<>();
    private final List<String> data;
    private final Context context;

    public OnCardClickedListener mListener = null;
    public interface OnCardClickedListener {
        void onClick();
        void onDeleteButtonClick(final int position);
    }

    public void setOnCardClickListener(final OnCardClickedListener listener) {
        mListener = listener;
    }

    public ModuleContentAdapter(final Context cn, final List<String> data) {
        isOnSearchMode = false;
        context = cn;
        this.data = data;
    }

    public ModuleContentAdapter(final Context cn, final List<String> data, final List<int[]> indexes) {
        isOnSearchMode = true;
        context = cn;
        this.data = data;
        this.indexes = indexes;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_fragment, parent, false);
        return new ViewHolder(_view);
    }

    private boolean onDeleteMode = false;
    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        View _view = holder.itemView;
        final RoundedLayout card = _view.findViewById(R.id.cardview);
        final ImageButton deleteBtn = _view.findViewById(R.id.delete_ic);
        final TextView txt = _view.findViewById(R.id.txtview_content);

        if (isOnSearchMode) {
            final int[] indexs = indexes.get(position);
            Log.i(TAG, "onBindViewHolder: " + indexs[0] + ", " + indexs[1]);
            final String textFound = data.get(position).substring(indexs[0], indexs[1]);

            SpannableString spanText = new SpannableString(textFound);
            spanText.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), 0, textFound.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

            SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
            spanBuilder.append(data.get(position).substring(0, indexs[0])); // append the non-highlighted first part of the content
            spanBuilder.append(spanText); // append the highlighted part of the content.
            spanBuilder.append(data.get(position).substring(indexs[1])); // append the non-highlighted second part of the content.

            txt.setText(spanBuilder);
            return; // we will return cuz we dont want to initialize the listeners if we are on search mode.
        }

        txt.setText(data.get(position));
        // card on click
        card.setOnClickListener(view -> {
            if (onDeleteMode) {
                onDeleteMode = false;
                _initNormalView(card, deleteBtn);
            }
            mListener.onClick();
        });

        // card on long click
        card.setOnLongClickListener(view -> {
            if (!onDeleteMode) {
                onDeleteMode = true;
                _initDeleteView(card, deleteBtn);
                return true;
            }
            onDeleteMode = false;
            _initNormalView(card, deleteBtn);
            return true;
        });
        // delete btn on click
        deleteBtn.setOnClickListener(view -> {
            if (onDeleteMode) {
                onDeleteMode = false;
                _initNormalView(card, deleteBtn);
            }
            mListener.onDeleteButtonClick(position);
        });
    }

    private void _initNormalView(View card, @NonNull View deleteBtn) {
        ((RoundedLayout)card).setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null));
        deleteBtn.setVisibility(View.GONE);
    }

    private void _initDeleteView(View card, @NonNull View deleteBtn) {
        ((RoundedLayout)card).setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.delete_bg_clor, null));
        deleteBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    @Override
    public int getItemViewType(int position) {
        return position;
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
