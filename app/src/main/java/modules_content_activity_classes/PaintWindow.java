package modules_content_activity_classes;

import android.annotation.SuppressLint;
import android.content.Context;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.PopupWindow;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.res.ResourcesCompat;

import com.tajos.iccnotes.R;

import java.util.HashMap;
import java.util.Objects;

public class PaintWindow {

    public static final int TEXT_COLOR_TYPE = 0;
    public static final int TEXT_BG_TYPE = 1;
    public static final String TYPE = "type";
    public static final String COLOR = "color";
    private static int RED;
    private static int GREEN;
    private static int BLUE;
    private static int YELLOW;

    public OnSelectedListener mListener;
    public interface OnSelectedListener {
        void onFinishSelecting(final HashMap<String, Integer> data);
    }

    public void show(@NonNull Context context, @NonNull View anchor, OnSelectedListener listener) {
        RED = ResourcesCompat.getColor(context.getResources(), R.color.red, null);
        GREEN = ResourcesCompat.getColor(context.getResources(), R.color.green, null);
        BLUE = ResourcesCompat.getColor(context.getResources(), R.color.blue, null);
        YELLOW = ResourcesCompat.getColor(context.getResources(), R.color.yellow, null);

        mListener = listener;
        @SuppressLint("InflateParams") View popupWindowView = LayoutInflater.from(context).inflate(R.layout.popup_window_layout, null);
        int width = LinearLayout.LayoutParams.WRAP_CONTENT;
        int height = LinearLayout.LayoutParams.WRAP_CONTENT;
        final PopupWindow paintWindow = new PopupWindow(popupWindowView, width, height, true);
        paintWindow.setAnimationStyle(R.style.PaintWindowAnimation);

        final int[] location = new int[2];
        anchor.getLocationOnScreen(location);
        final int posY = location[1] - (anchor.getBottom()*3) - (anchor.getBottom()/5);
        paintWindow.showAtLocation(anchor, 0, location[0], posY);

        final Button txtColorBtn = popupWindowView.findViewById(R.id.txt_color_btn);
        final Button txtBgBtn = popupWindowView.findViewById(R.id.txt_bg_btn);
        final LinearLayout red = popupWindowView.findViewById(R.id.red);
        final LinearLayout green = popupWindowView.findViewById(R.id.green);
        final LinearLayout blue = popupWindowView.findViewById(R.id.blue);
        final LinearLayout yellow = popupWindowView.findViewById(R.id.yellow);
        final TextView txtSample = popupWindowView.findViewById(R.id.txt_sample);

        final HashMap<String, Integer> data = new HashMap<>();

        // on dismiss listener
        paintWindow.setOnDismissListener(() -> {
            // on dismiss
            mListener.onFinishSelecting(data);
        });

        // txt color btn listener
        txtColorBtn.setOnClickListener(view -> {
            data.put("type", TEXT_COLOR_TYPE);
            if (data.containsKey("color")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
        // txt color bg btn listener
        txtBgBtn.setOnClickListener(view -> {
            data.put("type", TEXT_BG_TYPE);
            if (data.containsKey("color")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
        // red color listener
        red.setOnClickListener(view -> {
            data.put("color", RED);
            if (data.containsKey("type")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
        // green color listener
        green.setOnClickListener(view -> {
            data.put("color", GREEN);
            if (data.containsKey("type")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
        // blue color listener
        blue.setOnClickListener(view -> {
            data.put("color", BLUE);
            if (data.containsKey("type")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
        // yellow color listener
        yellow.setOnClickListener(view -> {
            data.put("color", YELLOW);
            if (data.containsKey("type")) {
                final int type = Objects.requireNonNull(data.get("type"));
                final int color = Objects.requireNonNull(data.get("color"));
                txtSample.setText(sampleText(type, color));
            }
        });
    }

    @NonNull
    private SpannableStringBuilder sampleText(int TYPE, int COLOR) {
        if (TYPE == TEXT_COLOR_TYPE) {
            SpannableStringBuilder spanTxtBldr = new SpannableStringBuilder("Sample text");
            spanTxtBldr.setSpan(new ForegroundColorSpan(COLOR), 0, spanTxtBldr.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            return spanTxtBldr;
        }

        SpannableStringBuilder spanTxtBldr = new SpannableStringBuilder("Sample text");
        spanTxtBldr.setSpan(new BackgroundColorSpan(COLOR), 0, spanTxtBldr.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
        return spanTxtBldr;
    }
}
