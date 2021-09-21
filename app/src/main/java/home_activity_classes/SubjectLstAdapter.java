package home_activity_classes;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
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
    private final List<Subject> data;
    private final Context context;

    public SubjectLstAdapter(final Context cn, final List<Subject> data) {
        context = cn;
        this.data = data;
    }

    @NonNull
    @Override
    public SubjectLstAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View _view = LayoutInflater.from(parent.getContext()).inflate(R.layout.subjectlst_fragment, parent, false);
        return new ViewHolder(_view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        View _view = holder.itemView;
        final RoundedLayout card = _view.findViewById(R.id.card_root);
        final ImageView imgThumbnail = _view.findViewById(R.id.img);
        final TextView subjectName = _view.findViewById(R.id.subject_name);
        final TextView subjectTime = _view.findViewById(R.id.time);
        final List<Subject> data = App.getSubjects();

        // listener ==> when card is clicked
        card.setOnClickListener(view -> new SchoolTermsDialog(context, position).show());

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
