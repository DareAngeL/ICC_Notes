package modules_content_activity_classes;

import android.content.Context;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.ImageSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tajos.iccnotes.ModuleContentActivity;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class SearchContentThread extends HandlerThread {
    private final Context context;

    private final int HIGHLIGHT_COLOR = Color.parseColor("#79FFE603");

    @Nullable
    private List<HashMap<String, Object>> contents = new ArrayList<>();

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
                contents = new Gson().fromJson(msgMap.get("content"), new TypeToken<List<HashMap<String, Object>>>() {}.getType());
                final List<SpannableStringBuilder> spannedContents = _initSpannables();

                List<SpannableStringBuilder> avContent = new ArrayList<>();
                for (SpannableStringBuilder content : spannedContents) {
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

    @NonNull
    private List<SpannableStringBuilder> _initSpannables() {
        List<SpannableStringBuilder> spannedContents = new ArrayList<>(); // list of spanned content for recyclerview

        assert contents != null;
        for (HashMap<String, Object> spanMap : contents) {
            final String text = Objects.requireNonNull(spanMap.get("text")).toString();
            final List<HashMap<String, Object>> spannedIndicesList = new Gson().fromJson(Objects.requireNonNull(spanMap.get("indices")).toString(), new TypeToken<List<HashMap<String, Object>>>() {}.getType());
            ModuleContentActivity.spannedIndices.set(spannedIndicesList);

            SpannableStringBuilder spannedContent = new SpannableStringBuilder(text);

            // checks if there is an indices of spanned text and if there is, span it. Otherwise, it is plain text, dont span.
            if (spannedIndicesList != null && spannedIndicesList.size() > 0)
                _resetSpan(spannedContent);

            spannedContents.add(spannedContent);
        }

        return spannedContents;
    }

    private void _resetSpan(final SpannableStringBuilder spannedContent) {
        final List<HashMap<String, Object>> newSpannedList = new ArrayList<>();
        final String TYPE = "type_span";

        for (HashMap<String, Object> indexMap : ModuleContentActivity.spannedIndices.get()) {
            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.BOLD)) {
                _span("bold_i", new StyleSpan(Typeface.BOLD), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.ITALIC)) {
                _span("italic_i", new StyleSpan(Typeface.ITALIC), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.UNDERLINE)) {
                _span("underline_i", new UnderlineSpan(), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.FOREGROUND)) {
                final int color = new Gson().fromJson(Objects.requireNonNull(indexMap.get("color")).toString(), new TypeToken<Integer>() {}.getType());
                _span("foreground_i", new ForegroundColorSpan(color), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.BACKGROUND)) {
                final int color = new Gson().fromJson(Objects.requireNonNull(indexMap.get("color")).toString(), new TypeToken<Integer>() {}.getType());
                _span("background_i", new BackgroundColorSpan(color), indexMap, spannedContent, newSpannedList);
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ModuleContentActivity.IMAGE)) {
                final String imgSrc = Objects.requireNonNull(indexMap.get("image")).toString();
                final Uri imgUri = Uri.parse(imgSrc);
                _span("image_i", new ImageSpan(context, imgUri), indexMap, spannedContent, newSpannedList);
            }
        }
        ModuleContentActivity.spannedIndices.set(newSpannedList);
        newSpannedList.clear();
    }

    private void _span(final String key, final Object spanType, @NonNull final HashMap<String, Object> indexMap, @NonNull final SpannableStringBuilder spannedContent, final List<HashMap<String, Object>> newSpannedList) {
        final int[] indices = new Gson().fromJson(Objects.requireNonNull(indexMap.get(key)).toString(), new TypeToken<int[]>() {}.getType());

        if (indexMap.containsKey("text")) {
            final int newStartIndex = spannedContent.toString().contains(Objects.requireNonNull(indexMap.get("text")).toString()) ?
                    spannedContent.toString().indexOf(Objects.requireNonNull(indexMap.get("text")).toString()) : - 1;
            final int newLastIndex = newStartIndex != - 1 ? newStartIndex + (indices[1] - indices[0]) : - 1;
            // it is negative means , it wasnt able to found the text.
            if (newStartIndex == - 1)
                return;

            spannedContent.setSpan(spanType, newStartIndex, newLastIndex, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            newSpannedList.add(indexMap);
            return;
        }

        spannedContent.setSpan(spanType, indices[0], indices[1], Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        newSpannedList.add(indexMap);
    }

    public void setOnSearchListener(final OnSearchListener listener) {
        mListener = listener;
    }

    public Handler getHandler() {
        return handler;
    }
}
