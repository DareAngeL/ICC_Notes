package modules_content_activity_classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.text.style.ImageSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

public class TajosImageSpan extends ImageSpan {

    private final String mSource;

    public TajosImageSpan(@NonNull Context context, @NonNull Bitmap bitmap, @Nullable String source) {
        super(context, bitmap);
        mSource = source;
    }

    @Nullable
    public String getImageSource() {
        return mSource;
    }
}
