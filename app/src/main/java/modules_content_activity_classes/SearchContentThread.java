package modules_content_activity_classes;

import android.content.Context;
import android.graphics.Color;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;

import iccnote.App;

public class SearchContentThread extends HandlerThread {
    private static final String TAG = "SearchContentThread";
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
                final List<String> contents = new Gson().fromJson(msgMap.get("contents"), new TypeToken<List<String>>() {}.getType());

                assert contents != null;
                List<SpannableStringBuilder> avContent = new ArrayList<>();
                List<int[]> highlightIndexes = new ArrayList<>();
                for (String content : contents) {
                    assert textToSearch != null;
                    final String contentLCase = content.toLowerCase(Locale.ROOT);
                    final String textToSearchLCase = textToSearch.toLowerCase(Locale.ROOT);

                    if (contentLCase.contains(textToSearchLCase)) {
                        Log.i(TAG, "handleMessage: content: " + content.toLowerCase() + ", " + textToSearch.toLowerCase());
                        final int startIndex = contentLCase.indexOf(textToSearchLCase);
                        final int lastIndex = startIndex + textToSearchLCase.length();

                        final String textFound = App.cutString(content, startIndex, lastIndex);

                        SpannableString spanText = new SpannableString(textFound);
                        spanText.setSpan(new BackgroundColorSpan(HIGHLIGHT_COLOR), 0, textFound.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                        SpannableStringBuilder spanBuilder = new SpannableStringBuilder();
                        spanBuilder.append(App.cutString(content, 0, startIndex)); // append the non-highlighted first part of the content
                        spanBuilder.append(spanText); // append the highlighted part of the content.
                        spanBuilder.append(App.cutString(content, lastIndex, content.length())); // append the non-highlighted second part of the content.

                        avContent.add(spanBuilder);
                        //highlightIndexes.add(new int[] {startIndex, lastIndex});
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
