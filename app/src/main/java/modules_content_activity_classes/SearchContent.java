package modules_content_activity_classes;

import android.content.Context;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SearchContent extends HandlerThread {
    private final Context context;

    public static final String START_INDEX_KEY = "search_start_index";
    public static final String END_INDEX_KEY = "search_end_index";

    @Nullable
    private List<HashMap<String, Object>> contents = new ArrayList<>();

    private Handler handler;

    public OnSearchListener mListener;
    public interface OnSearchListener {
        void onSearchComplete(final List<HashMap<String, Object>> availableContent);
    }

    public SearchContent(final Context cn) {
        super("SearchContent ", Process.THREAD_PRIORITY_DISPLAY);
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
                contents = new Gson().fromJson(msgMap.get("content"), new TypeToken<List<HashMap<String, Object>>>() {}.getType());

                List<HashMap<String, Object>> searchedContentList = new ArrayList<>();
                assert contents != null;
                for (HashMap<String, Object> content : contents) {
                    assert textToSearch != null;
                    final String contentLCase = Objects.requireNonNull(content.get("text")).toString().toLowerCase(Locale.ROOT);
                    final String textToSearchLCase = textToSearch.toLowerCase(Locale.ROOT);

                    if (contentLCase.contains(textToSearchLCase)) {
                        final int startIndex = contentLCase.indexOf(textToSearchLCase);
                        final int lastIndex = startIndex + textToSearchLCase.length();
                        // put the indices of a searched text
                        final HashMap<String, Object> searchMap = new HashMap<>();
                        List<HashMap<String, Object>> spannedIndicesList = new Gson().fromJson(Objects.requireNonNull(content.get("indices")).toString(), new TypeToken<List<HashMap<String, Object>>>() {}.getType());
                        searchMap.put(START_INDEX_KEY, startIndex);
                        searchMap.put(END_INDEX_KEY, lastIndex);

                        if (spannedIndicesList == null) // checks if there's no spanned indices, if theres none then, create new object.
                            spannedIndicesList = new ArrayList<>();

                        spannedIndicesList.add(searchMap);
                        // update the content
                        final String indicesJson = new Gson().toJson(spannedIndicesList);
                        content.put("indices", indicesJson);
                        searchedContentList.add(content);
                    }
                }
                ((AppCompatActivity)context).runOnUiThread(() -> mListener.onSearchComplete(searchedContentList));
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
