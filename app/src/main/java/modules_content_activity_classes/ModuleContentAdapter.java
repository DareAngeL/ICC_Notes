package modules_content_activity_classes;

import static android.content.ContentValues.TAG;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.SpannableStringBuilder;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tajos.iccnotes.R;

import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import layouts.RoundedLayout;

public class ModuleContentAdapter extends RecyclerView.Adapter<ModuleContentAdapter.ViewHolder> {
    private static boolean isOnSearchMode = false;

    @Nullable
    private final List<HashMap<String, Object>> data;
    private final SpanHelper spanHelper;
    private final Context context;

    public OnCardClickedListener mListener = null;
    public interface OnCardClickedListener {
        void onClick();
        void onDeleteButtonClick(final int position);
    }

    public void setOnCardClickListener(final OnCardClickedListener listener) {
        mListener = listener;
    }

    public ModuleContentAdapter(final Context cn, @Nullable final List<HashMap<String, Object>> data, final SpanHelper spanHelper, final boolean isFromSearch) {
        isOnSearchMode = isFromSearch;
        context = cn;
        this.spanHelper = spanHelper;
        this.data = data;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _view = LayoutInflater.from(parent.getContext()).inflate(R.layout.content_fragment, parent, false);
        return new ViewHolder(_view);
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        View _view = holder.itemView;
        final RoundedLayout card = _view.findViewById(R.id.cardview);
        final ImageButton deleteBtn = _view.findViewById(R.id.delete_ic);
        final TextView txt = _view.findViewById(R.id.txtview_content);

        final boolean[] onDeleteMode = {false};

        if (isOnSearchMode) {
            assert data != null;
            if (position == data.size()) {
                card.setVisibility(View.INVISIBLE);
                return;
            }
            txt.setText(_spanText(data.get(position)));
            return; // we will return cuz we dont want to initialize the listeners if we are on search mode.
        }

        // we will just return if we are already in the last position which is only for the margin effect
        assert data != null;
        if (position == data.size()) {
            card.setVisibility(View.INVISIBLE);
            return;
        }

        txt.setText(_spanText(data.get(position)));
        // card on click
        card.setOnClickListener(view -> {
            if (onDeleteMode[0]) {
                onDeleteMode[0] = false;
                _initNormalView(card, deleteBtn);
            }
            mListener.onClick();
        });

        // card on long click
        card.setOnLongClickListener(view -> {
            if (! onDeleteMode[0]) {
                onDeleteMode[0] = true;
                _initDeleteView(card, deleteBtn);
                return true;
            }
            onDeleteMode[0] = false;
            _initNormalView(card, deleteBtn);
            return true;
        });
        // delete btn on click
        deleteBtn.setOnClickListener(view -> {
            if (onDeleteMode[0]) {
                onDeleteMode[0] = false;
                _initNormalView(card, deleteBtn);
            }
            mListener.onDeleteButtonClick(position);
        });
    }

    @NonNull
    private SpannableStringBuilder _spanText(@NonNull final HashMap<String, Object> spanMap) {
        final String text = Objects.requireNonNull(spanMap.get("text")).toString();
        final List<HashMap<String, Object>> spannedIndicesList = new Gson().fromJson(Objects.requireNonNull(spanMap.get("indices")).toString(), new TypeToken<List<HashMap<String, Object>>>() {}.getType());
        Log.i(TAG, "_spanText: " + spannedIndicesList);
        SpanHelper.spannedIndices.set(spannedIndicesList);

        SpannableStringBuilder spannedContent = new SpannableStringBuilder(text);

        // checks if there is an indices of spanned text and if there is, span it. Otherwise, it is plain text, dont span.
        if (spannedIndicesList != null && spannedIndicesList.size() > 0)
            spanHelper.resetSpan(spannedContent, isOnSearchMode);

        return spannedContent;
    }

    private void _initNormalView(View card, @NonNull View deleteBtn) {
        final int bgColor = ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null);
        final int borderColor = ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null);
        ((RoundedLayout)card).setBgroundColor(bgColor);
        ((RoundedLayout)card).setBorderColor(borderColor);
        deleteBtn.setVisibility(View.GONE);
    }

    private void _initDeleteView(View card, @NonNull View deleteBtn) {
        final int color = ResourcesCompat.getColor(context.getResources(), R.color.delete_bg_clor, null);
        ((RoundedLayout)card).setBgroundColor(color);
        ((RoundedLayout)card).setBorderColor(color);
        deleteBtn.setVisibility(View.VISIBLE);
    }

    @Override
    public int getItemCount() {
        assert data != null;
        return data.size() + 1; // we add one so we can have margin effect at the bottom of recyclerview
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
