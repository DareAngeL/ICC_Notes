package home_activity_classes;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatDialog;
import androidx.cardview.widget.CardView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.tajos.iccnotes.R;

import java.util.Objects;

import iccnote.App;

public class AddDayDialog extends AppCompatDialog {

    private final Context context;

    public AddDayDialog(Context context) {
        super(context);
        this.context = context;
    }

    @Nullable
    private EditText mDayEditTxt;
    @Nullable
    private EditText mHourEditTxt;
    @Nullable
    private EditText mMinutesEditTxt;
    @Nullable
    private CardView mRoot;
    @Nullable
    private Spinner mDaysSpinner;
    @Nullable
    private Spinner mMeridiemSpinner;

    private boolean hourEditTxtIsError = false;
    private boolean minutesEditTxtIsError = false;

    public OnClickedListener mListener;
    public interface OnClickedListener {
        void onAddButtonClicked(final String day, final String time);
        void onCancel();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.add_day_fragment);
        getWindow().setDimAmount(0.2f);
        getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        final Button mAddBtn = findViewById(R.id.add_btn);
        mDayEditTxt = findViewById(R.id.day);
        mHourEditTxt = findViewById(R.id.hour);
        mMinutesEditTxt = findViewById(R.id.minutes);
        mRoot = findViewById(R.id.root);
        mDaysSpinner = findViewById(R.id.spinner);
        mMeridiemSpinner = findViewById(R.id.meridiem_spinner);
        assert mDayEditTxt != null;
        mDayEditTxt.setKeyListener(null);
        _initEnterFadeAnimEffect();

        /*
        ========== LISTENERS ==========
        */

        assert mAddBtn != null;
        // add button listener
        mAddBtn.setOnClickListener(view -> {
            // if the edittext is empty it will error and wont dismiss
            if (mDayEditTxt.getText().toString().isEmpty() ||
                Objects.requireNonNull(mHourEditTxt).getText().toString().isEmpty() ||
                Objects.requireNonNull(mMinutesEditTxt).getText().toString().isEmpty()) {

                _error();
                return;
            }
            // check hour and minutes format first before dismissing the dialog
            if (isHourAndMinutesCorrectFormat()) {
                dismiss();
                mListener.onAddButtonClicked(mDayEditTxt.getText().toString(), _getTime());
            }
        });
        // days spinner listener
        assert mDaysSpinner != null;
        mDaysSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int position, long l) {
                if (!mDaysSpinner.getSelectedItem().toString().isEmpty()) {
                    mDayEditTxt.setText(mDaysSpinner.getSelectedItem().toString());
                    final int color = ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null);
                    ViewCompat.setBackgroundTintList(mDayEditTxt, ColorStateList.valueOf(color));
                    mDaysSpinner.setSelection(0);
                }
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
        // hour edittext listener
        assert mHourEditTxt != null;
        mHourEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // if hour edittext is from error state, then change the underline color to its proper color
                if (hourEditTxtIsError) {
                    hourEditTxtIsError = false;
                    final int color = ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null);
                    ViewCompat.setBackgroundTintList(mHourEditTxt, ColorStateList.valueOf(color));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        // minutes edittext listener
        assert mMinutesEditTxt != null;
        mMinutesEditTxt.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                // if minutes edittext is from error state, then change the underline color to its proper color
                if (minutesEditTxtIsError) {
                    minutesEditTxtIsError = false;
                    final int color = ResourcesCompat.getColor(context.getResources(), R.color.colorAccent, null);
                    ViewCompat.setBackgroundTintList(mMinutesEditTxt, ColorStateList.valueOf(color));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        setCanceledOnTouchOutside(true);
        setOnCancelListener(dialogInterface -> mListener.onCancel());
    }

    private boolean isHourAndMinutesCorrectFormat() {
        assert mHourEditTxt != null;
        final int hour = Integer.parseInt(mHourEditTxt.getText().toString());
        assert mMinutesEditTxt != null;
        final int minutes = Integer.parseInt(mMinutesEditTxt.getText().toString());

        if (hour <= 12) {
            if (minutes <= 59) {
                return true;
            }
            minutesEditTxtIsError = true;
            App.animateErrorEffect(context, mMinutesEditTxt);
            return false;
        }
        hourEditTxtIsError = true;
        App.animateErrorEffect(context, mHourEditTxt);
        if (!(minutes <= 59)) {
            minutesEditTxtIsError = true;
            App.animateErrorEffect(context, mMinutesEditTxt);
        }
        return false;
    }

    private void _initEnterFadeAnimEffect() {
        // enter effect
        final ObjectAnimator enterEffect = ObjectAnimator.ofFloat(mRoot, "translationY", 100f, 0f);
        enterEffect.setDuration(300);
        enterEffect.setInterpolator(new DecelerateInterpolator());
        // fade in effect
        final ObjectAnimator fadeInEffect = ObjectAnimator.ofFloat(mRoot, "alpha", 0f, 1f);
        fadeInEffect.setDuration(300);
        // animation effect player.
        final AnimatorSet dialogAnimator = new AnimatorSet();
        dialogAnimator.play(enterEffect).with(fadeInEffect);
        dialogAnimator.start();
    }

    @NonNull
    private String _getTime() {
        assert mHourEditTxt != null;
        final String hour = mHourEditTxt.getText().toString();
        assert mMinutesEditTxt != null;
        final String minutes = mMinutesEditTxt.getText().toString();

        assert mMeridiemSpinner != null;
        return hour + ":" + minutes + " " + mMeridiemSpinner.getSelectedItem().toString();
    }

    private void _error() {
        assert mDayEditTxt != null;
        if (mDayEditTxt.getText().toString().isEmpty()) {
            App.animateErrorEffect(context, mDayEditTxt);
        }
        assert mHourEditTxt != null;
        if (mHourEditTxt.getText().toString().isEmpty()) {
            hourEditTxtIsError = true;
            App.animateErrorEffect(context, mHourEditTxt);
        }
        assert mMinutesEditTxt != null;
        if (mMinutesEditTxt.getText().toString().isEmpty()) {
            minutesEditTxtIsError= true;
            App.animateErrorEffect(context, mMinutesEditTxt);
        }
    }

    public void setOnClickedListener(final OnClickedListener listener) {
        mListener = listener;
    }
}