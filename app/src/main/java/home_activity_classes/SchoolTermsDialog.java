package home_activity_classes;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.transition.Slide;
import android.util.Log;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDialog;

import com.google.gson.Gson;
import com.tajos.iccnotes.ModulesListActivity;
import com.tajos.iccnotes.R;

import java.util.List;
import java.util.Objects;

import iccnote.App;
import iccnote.Module;

public class SchoolTermsDialog extends AppCompatDialog {

    private final Context context;
    private final int position;

    public SchoolTermsDialog(Context context, int position) {
        super(context);
        this.context = context;
        this.position = position;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.school_terms_fragment);
        getWindow().setEnterTransition(new Slide(Gravity.BOTTOM));
        getWindow().setLayout(
                (int)(App.getScreenWidth((AppCompatActivity)context) * 0.9f),
                LinearLayout.LayoutParams.WRAP_CONTENT);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        getWindow().setDimAmount(0.5f);

        // bundles
        final TextView mPrelim = findViewById(R.id.prelim);
        final TextView mMidterm = findViewById(R.id.midterm);
        final TextView mFinals = findViewById(R.id.finals);
        // listeners
        assert mPrelim != null;
        mPrelim.setOnClickListener(view -> {
            final Intent intent = new Intent(context, ModulesListActivity.class);
            Log.i("SUBJECTS FROM STORAGE", App.getSubjects().toString());
            List<Module> modules = App.getModulesFromSubject("Prelim", App.getSubjects().get(position));
            intent.putExtra("key", Objects.requireNonNull(App.getSubjects().get(position).get("key")).toString());
            intent.putExtra("term", "Prelim");
            intent.putExtra("subject_name", Objects.requireNonNull(App.getSubjects().get(position).get("Subject Name")).toString());
            intent.putExtra("modules", new Gson().toJson(modules));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });

        assert mMidterm != null;
        mMidterm.setOnClickListener(view -> {
            final Intent intent = new Intent(context, ModulesListActivity.class);
            List<Module> modules = App.getModulesFromSubject("Midterm", App.getSubjects().get(position));
            intent.putExtra("key", Objects.requireNonNull(App.getSubjects().get(position).get("key")).toString());
            intent.putExtra("term", "Midterm");
            intent.putExtra("subject_name", Objects.requireNonNull(App.getSubjects().get(position).get("Subject Name")).toString());
            intent.putExtra("modules", new Gson().toJson(modules));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });

        assert mFinals != null;
        mFinals.setOnClickListener(view -> {
            final Intent intent = new Intent(context, ModulesListActivity.class);
            List<Module> modules = App.getModulesFromSubject("Finals", App.getSubjects().get(position));
            intent.putExtra("key", Objects.requireNonNull(App.getSubjects().get(position).get("key")).toString());
            intent.putExtra("term", "Finals");
            intent.putExtra("subject_name", Objects.requireNonNull(App.getSubjects().get(position).get("Subject Name")).toString());
            intent.putExtra("modules", new Gson().toJson(modules));
            intent.putExtra("position", position);
            context.startActivity(intent);
        });
    }
}