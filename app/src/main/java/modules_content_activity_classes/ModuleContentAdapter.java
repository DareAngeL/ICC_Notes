package modules_content_activity_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.tajos.iccnotes.R;

import java.util.List;

import layouts.RoundedLayout;

public class ModuleContentAdapter extends RecyclerView.Adapter<ModuleContentAdapter.ViewHolder> {
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
        context = cn;
        this.data = data;
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
        txt.setText(data.get((data.size()-1)-position));

        // card on click
        card.setOnClickListener(view -> mListener.onClick());

        // card on long click
        card.setOnLongClickListener(view -> {
            if (!onDeleteMode) {
                onDeleteMode = true;
                card.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.delete_bg_clor, null));
                deleteBtn.setVisibility(View.VISIBLE);
                return true;
            }
            onDeleteMode = false;
            card.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null));
            deleteBtn.setVisibility(View.GONE);
            return true;
        });
        // delete btn on click
        deleteBtn.setOnClickListener(view -> {
            if (onDeleteMode) {
                onDeleteMode = false;
                card.setBgroundColor(ResourcesCompat.getColor(context.getResources(), R.color.colorPrimary, null));
                deleteBtn.setVisibility(View.GONE);
            }
            mListener.onDeleteButtonClick((data.size()-1)-position);
        });
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
