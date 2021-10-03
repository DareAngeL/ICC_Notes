package home_activity_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.tajos.iccnotes.R;

import java.util.List;
import java.util.Objects;
import java.util.Random;

import iccnote.App;
import iccnote.Subject;
import layouts.RoundedLayout;

public class SubjectLstAdapter extends RecyclerView.Adapter<SubjectLstAdapter.ViewHolder> {
    private static final String TAG = "SUBJECTLISTADAPTER";
    private final List<Subject> data;
    private final Context context;

    public SubjectLstAdapter(final Context cn, @NonNull final List<Subject> data) {
        context = cn;
        this.data = data;
        Log.i(TAG, "SubjectLstAdapter: LIST: " + data.toString());
    }

    public OnItemClickListener mListener;
    public interface OnItemClickListener{
        void onDeleteButtonClick(final String key, final int position);
    }

    public void setOnItemClickListener(final OnItemClickListener listener) {
        mListener = listener;
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @NonNull
    @Override
    public SubjectLstAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subjectlst_fragment, parent, false);
        _view.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return new ViewHolder(_view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, @SuppressLint("RecyclerView") int position) {
        View _view = holder.itemView;
        final CardView card = _view.findViewById(R.id.card_root);
        final RoundedLayout border = _view.findViewById(R.id.border);
        final FrameLayout filterLayout = _view.findViewById(R.id.filter_layout);
        final ImageView imgThumbnail = _view.findViewById(R.id.img);
        final TextView subjectName = _view.findViewById(R.id.subject_name);
        final LinearLayout subjectNameRoot = _view.findViewById(R.id.subject_name_root);
        final TextView subjectTime = _view.findViewById(R.id.time);
        final ImageButton deleteBtn = _view.findViewById(R.id.delete_btn);
        final List<Subject> data = App.getSubjects();

        final boolean[] isOnDeleteMode = {false};
        final String key = Objects.requireNonNull(data.get(position).get("key")).toString();

        // listener ==> when card is clicked
        card.setOnClickListener(view -> {
            if (! isOnDeleteMode[0]) {
                new SchoolTermsDialog(context, position).show();
                return;
            }
            isOnDeleteMode[0] = false;
            deleteBtn.setVisibility(View.GONE);
            border.setBorderColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
            subjectNameRoot.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null)));
            filterLayout.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.imgFilter, null)));
        });

        card.setOnLongClickListener(view -> {
            if (! isOnDeleteMode[0]) {
                isOnDeleteMode[0] = true;

                deleteBtn.setVisibility(View.VISIBLE);
                border.setBorderColor(ResourcesCompat.getColor(context.getResources(), R.color.red, null));
                subjectNameRoot.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.red, null)));
                filterLayout.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.imgFilterRed, null)));
                return true;
            }

            isOnDeleteMode[0] = false;
            deleteBtn.setVisibility(View.GONE);
            border.setBorderColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
            subjectNameRoot.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null)));
            filterLayout.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.imgFilter, null)));
            return true;
        });

        deleteBtn.setOnClickListener(view -> {
            isOnDeleteMode[0] = false;
            deleteBtn.setVisibility(View.GONE);
            border.setBorderColor(ResourcesCompat.getColor(context.getResources(), R.color.toolbarColor, null));
            subjectNameRoot.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null)));
            filterLayout.setBackground(new ColorDrawable(ResourcesCompat.getColor(context.getResources(), R.color.imgFilter, null)));

            mListener.onDeleteButtonClick(key, position);
        });

        if(!(Objects.requireNonNull(data.get(position).get("Subject Day"))).toString().equals(App.getCurrentDay())) {
            final Random random = new Random();
            final float[] percents = {/*70%*/0.88f, /*80%*/0.89f, /*90%*/0.9f};
            card.setAlpha(0.8f);
            card.getLayoutParams().width = (int)(App.getScreenWidth((AppCompatActivity)context) * percents[random.nextInt(3)]);
            card.requestLayout();
        }

        Glide.with(context)
            .load(data.get(position).get("Image"))
            .into(imgThumbnail);

        subjectName.setText(Objects.requireNonNull(data.get(position).get("Subject Name")).toString());

        if (Objects.requireNonNull(data.get(position).get("Subject Day")).toString().equals(App.getCurrentDay())) {
            subjectTime.setText(Objects.requireNonNull(data.get(position).get("Time")).toString());
            return;
        }
        subjectTime.setVisibility(View.GONE);
    }

    @Override
    public int getItemCount() {
        return data.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
        }
    }
}
