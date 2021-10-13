package modules_content_activity_classes;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tajos.iccnotes.ModuleContentActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

public class SearchContentThread extends HandlerThread {
    private final Context context;

    private final int HIGHLIGHT_COLOR = Color.parseColor("#79FFE603");

    private Handler handler;

    public OnSearchListener mListener;
    public interface OnSearchListener {
        void onSearchComplete(final List<SpannableStringBuilder> availableContent);
    }

    public SearchContentThread(final Context cn) {
        super("SearchContentThread ", Process.THREAD_PRIORITY_DEFAULT);
        context = cn;
    }

    @Override
    protected void onLooperPrepared() {
        handler = new Handler(Looper.myLooper()) {
            @Override
            public void handleMessage(@NonNull Message msg) {
                final String jsonMsgMap = msg.obj.toString();
                final HashMap<String, String> msgMap = new Gson().fromJson(jsonMsgMap, new TypeToken<HashMap<String, String>>() {}.getType());
                final String textToSearch = msgMap.get("search_key");
                final List<SpannableStringBuilder> contents = ModuleContentActivity._initSpannables();

                List<SpannableStringBuilder> avContent = new ArrayList<>();
                for (SpannableStringBuilder content : contents) {
                    assert textToSearch != null;
                    final String contentLCase = content.toString().toLowerCase(Locale.ROOT);
                    final String textToSearchLCase = textToSearch.toLowerCase(Locale.ROOT);

                    if (contentLCase.contains(textToSearchLCase)) {
                        final int startIndex = contentLCase.indexOf(textToSearchLCase);
                        final int lastIndex = startIndex + textToSearchLCase.length();

                        content.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), startIndex, lastIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                        avContent.add(content);
                    }
                }
                ((AppCompatActivity)context).runOnUiThread(() -> mListener.onSearchComplete(avContent));
            }
        };
    }

    public void setOnSearchListener(final OnSearchListener listener) {
        mListener = listener;
    }

    public Handler getHandler() {
        return handler;
    }
}
