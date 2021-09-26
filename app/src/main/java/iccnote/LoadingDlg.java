package iccnote;

import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.widget.ImageView;

import androidx.appcompat.app.AppCompatDialog;

import com.tajos.iccnotes.R;

public class LoadingDlg extends AppCompatDialog {

    public LoadingDlg(Context context) {
        super(context);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.loading_dlg_layout);
        getWindow().setDimAmount(0.3f);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        setCancelable(false);

        final ImageView iccLogo = findViewById(R.id.iccLogo);

        final ObjectAnimator rotator = ObjectAnimator.ofFloat(iccLogo, "rotation", 0f, 360f);
        rotator.setDuration(800);
        rotator.setInterpolator(new AccelerateDecelerateInterpolator());
        rotator.setRepeatCount(ObjectAnimator.INFINITE);
        rotator.setRepeatMode(ObjectAnimator.RESTART);
        rotator.start();
    }
}
