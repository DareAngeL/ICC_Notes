package modules_content_activity_classes;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Typeface;
import android.net.Uri;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.widget.EditText;

import androidx.annotation.NonNull;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import iccnote.App;

public class SpanHelper {
    private final Context mContext;

    private static final int BG_HIGHLIGHT_COLOR = Color.parseColor("#79FFE603");

    public static final String BOLD = "BOLD";
    public static final String ITALIC = "ITALIC";
    public static final String UNDERLINE = "UNDERLINE";
    public static final String IMAGE = "IMAGE";
    public static final String FOREGROUND = "FOREGROUND";
    public static final String BACKGROUND = "BACKGROUND";

    public static final AtomicReference<List<HashMap<String, Object>>> spannedIndices = new AtomicReference<>();
    private final int contentFragmentWidth;

    public SpanHelper(final Context context, final int contentFragmentWidth) {
        mContext = context;
        this.contentFragmentWidth = contentFragmentWidth;
    }

    /*
     * this will reset the span of contents, cuz it was changed drastically. It will try to find the text
     * if it cant find the text then it will remove the indices that was stored for that particular text
     * and set a new list of indices.
     */
    public void resetSpan(final SpannableStringBuilder spannedContent, final boolean isFromSearch) {
        final List<HashMap<String, Object>> newSpannedList = new ArrayList<>();
        final String TYPE = "type_span";

        for (HashMap<String, Object> indexMap : spannedIndices.get()) {
            if (indexMap.containsKey(SearchContent.START_INDEX_KEY)) {
                // highlight the text that is being searched for.
                if (isFromSearch)
                    //noinspection ConstantConditions
                    spannedContent.setSpan(new BackgroundColorSpan(BG_HIGHLIGHT_COLOR), (int)Math.round((double)indexMap.get(SearchContent.START_INDEX_KEY)), (int)Math.round((double)indexMap.get(SearchContent.END_INDEX_KEY)), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);

                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(BOLD)) {
                _span("bold_i", new StyleSpan(Typeface.BOLD), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(ITALIC)) {
                _span("italic_i", new StyleSpan(Typeface.ITALIC), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(UNDERLINE)) {
                _span("underline_i", new UnderlineSpan(), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(FOREGROUND)) {
                final int color = new Gson().fromJson(Objects.requireNonNull(indexMap.get("color")).toString(), new TypeToken<Integer>() {}.getType());
                _span("foreground_i", new ForegroundColorSpan(color), indexMap, spannedContent, newSpannedList);
                continue;
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(BACKGROUND)) {
                final int color = new Gson().fromJson(Objects.requireNonNull(indexMap.get("color")).toString(), new TypeToken<Integer>() {}.getType());
                _span("background_i", new BackgroundColorSpan(color), indexMap, spannedContent, newSpannedList);
            }

            if (Objects.requireNonNull(indexMap.get(TYPE)).toString().equals(IMAGE)) {
                final String imgSrc = Objects.requireNonNull(indexMap.get("image")).toString();
                final Uri imgUri = Uri.parse(imgSrc);
                @SuppressWarnings("ConstantConditions")
                final boolean isOrigWidth = (boolean)indexMap.get("is_orig_w");
                Bitmap img;
                img = App.getBitmapFromUri(mContext, imgUri);
                if (!isOrigWidth) {
                    assert img != null;
                    img = readjustBitmap(img, contentFragmentWidth);
                }
                assert img != null;
                _span("image_i", new TajosImageSpan(mContext, img, imgUri.toString()), indexMap, spannedContent, newSpannedList);
            }
        }
        spannedIndices.set(newSpannedList);
        newSpannedList.clear();
    }

    public Bitmap readjustBitmap(@NonNull final Bitmap bmp, final int newWidth) {
        final int bmpWidth = bmp.getWidth();
        final int bmpHeight = bmp.getHeight();

        // calculation to know the new height of the bitmap
        final float rate = newWidth / (float) bmpWidth;
        final int newHeight = (int)(bmpHeight * rate);

        return Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
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

    /*
     * this will get the spanned indices from editTextContent of AddContentView and store it to spannedIndices variable
     * @everytime the BOLD, ITALIC and UNDERLINE button is clicked, this will be called so we can update the indices list
     */
    @NonNull
    public List<HashMap<String, Object>> getSpannedIndices(@NonNull final SpannableStringBuilder span, boolean isOrigWidth) {
        final List<HashMap<String, Object>> spannedIndices = new ArrayList<>();
        final StyleSpan[] styleSpans = span.getSpans(0, span.length(), StyleSpan.class);
        final UnderlineSpan[] underlineSpans = span.getSpans(0, span.length(), UnderlineSpan.class);
        final TajosImageSpan[] imageSpans = span.getSpans(0, span.length(), TajosImageSpan.class);
        final ForegroundColorSpan[] foregroundColorSpans = span.getSpans(0, span.length(), ForegroundColorSpan.class);
        final BackgroundColorSpan[] backgroundColorSpans = span.getSpans(0, span.length(), BackgroundColorSpan.class);

        for (StyleSpan styleSpan : styleSpans) {
            if (styleSpan.getStyle() == Typeface.BOLD) {
                final HashMap<String, Object> map = new HashMap<>();
                final int[] indices = { span.getSpanStart(styleSpan), span.getSpanEnd(styleSpan) };
                if (indices[0] == 0 && indices[1] == 0)
                    continue;

                final String text = App.cutString(span.toString(), indices[0], indices[1]);
                final String indicesJson = new Gson().toJson(indices);
                map.put("type_span", BOLD);
                map.put("text", text);
                map.put("bold_i", indicesJson);
                spannedIndices.add(map);
                continue;
            }
            // ITALIC span style
            final HashMap<String, Object> map = new HashMap<>();
            int[] indices = { span.getSpanStart(styleSpan), span.getSpanEnd(styleSpan) };
            if (indices[0] == 0 && indices[1] == 0)
                continue;

            final String text = App.cutString(span.toString(), indices[0], indices[1]);
            final String indicesJson = new Gson().toJson(indices);
            map.put("type_span", ITALIC);
            map.put("text", text);
            map.put("italic_i", indicesJson);
            spannedIndices.add(map);
        }
        // UNDERLINE span style
        for (UnderlineSpan underlineSpan : underlineSpans) {
            final HashMap<String, Object> map = new HashMap<>();
            int[] indices = { span.getSpanStart(underlineSpan), span.getSpanEnd(underlineSpan) };
            if (indices[0] == 0 && indices[1] == 0)
                continue;

            final String text = App.cutString(span.toString(), indices[0], indices[1]);
            final String indicesJson = new Gson().toJson(indices);
            map.put("type_span", UNDERLINE);
            map.put("text", text);
            map.put("underline_i", indicesJson);
            spannedIndices.add(map);
        }
        // IMAGE SPAN
        for (TajosImageSpan imageSpan : imageSpans) {
            final HashMap<String, Object> map = new HashMap<>();
            int[] indices = {span.getSpanStart(imageSpan), span.getSpanEnd(imageSpan)};
            if (indices[0] == 0 && indices[1] == 0)
                continue;

            final String imgSource = imageSpan.getImageSource();
            final String indicesJson = new Gson().toJson(indices);
            map.put("type_span", IMAGE);
            map.put("image", imgSource);
            map.put("is_orig_w", isOrigWidth);
            map.put("image_i", indicesJson);
            spannedIndices.add(map);
        }
        // FOREGROUND span
        for (ForegroundColorSpan foregroundColorSpan : foregroundColorSpans) {
            final HashMap<String, Object> map = new HashMap<>();
            int[] indices = {span.getSpanStart(foregroundColorSpan), span.getSpanEnd(foregroundColorSpan)};
            if (indices[0] == 0 && indices[1] == 0)
                continue;

            final int foreGroundColor = foregroundColorSpan.getForegroundColor();
            final String text = App.cutString(span.toString(), indices[0], indices[1]);
            final String indicesJson = new Gson().toJson(indices);
            final String colorJson = new Gson().toJson(foreGroundColor);
            map.put("type_span", FOREGROUND);
            map.put("text", text);
            map.put("color", colorJson);
            map.put("foreground_i", indicesJson);
            spannedIndices.add(map);
        }
        // BACKGROUND span
        for (BackgroundColorSpan backgroundColorSpan : backgroundColorSpans) {
            final HashMap<String, Object> map = new HashMap<>();
            int[] indices = {span.getSpanStart(backgroundColorSpan), span.getSpanEnd(backgroundColorSpan)};
            if (indices[0] == 0 && indices[1] == 0)
                continue;

            final int bgColor = backgroundColorSpan.getBackgroundColor();
            final String text = App.cutString(span.toString(), indices[0], indices[1]);
            final String indicesJson = new Gson().toJson(indices);
            final String colorJson = new Gson().toJson(bgColor);
            map.put("type_span", BACKGROUND);
            map.put("text", text);
            map.put("color", colorJson);
            map.put("background_i", indicesJson);
            spannedIndices.add(map);
        }

        return spannedIndices;
    }
    /*
     * this will set the span of editTextContent from AddContentView
     * @when BOLD is clicked, this will be called
     * @when ITALIC is clicked, this will be called
     * @when UNDERLINE is clicked, this will be called
     */
    public void setSpan(@NonNull EditText contentView, SpannableStringBuilder spannedContent, Object spanType, int COLOR, String SPAN_TYPE) {
        if (contentView.getSelectionStart() != contentView.getSelectionEnd()) {
            final int start = contentView.getSelectionStart();
            final int end = contentView.getSelectionEnd();
            final CharSequence selectedText = App.cutString(contentView.getText(), start, end);

            if (spannedContent.length() < contentView.length()) {
                final String spanned = spannedContent.toString();
                final String newContent = App.cutString(contentView.getText().toString(), 0, spannedContent.length());
                if (spanned.equals(newContent)) {
                    spannedContent.append(App.cutString(contentView.getText().toString(), spannedContent.length(), contentView.length()));
                } else {
                    spannedContent.clear();
                    spannedContent.append(contentView.getText());
                    resetSpan(spannedContent, false);
                }
            }
            // if spanned content is bigger than the new content text it means, the text was changed decremently in text size.
            if (spannedContent.length() > contentView.length()) {
                spannedContent.clear();
                spannedContent.append(contentView.getText());
                resetSpan(spannedContent, false);
            }

            // this will remove a span if there's an existing span already.
            if (spannedContent.length()>0) {
                boolean isExist = false;
                if (SpanHelper.spannedIndices.get() != null) {
                    for (HashMap<String, Object> indexMap : spannedIndices.get()) {
                        if (!indexMap.containsKey("text"))
                            continue;

                        if (selectedText.equals(Objects.requireNonNull(indexMap.get("text")).toString())) {
                            if (Objects.requireNonNull(indexMap.get("type_span")).toString().equals(SPAN_TYPE)) {
                                if (SPAN_TYPE.equals(FOREGROUND) || SPAN_TYPE.equals(BACKGROUND)) {
                                    final int color = new Gson().fromJson(Objects.requireNonNull(indexMap.get("color")).toString(), new TypeToken<Integer>() {}.getType());
                                    if (COLOR == color) {
                                        _removeSpan(spannedContent, start, end);
                                        isExist = true;
                                    }
                                    continue;
                                }
                                _removeSpan(spannedContent, start, end);
                                isExist = true;
                            }
                            if (isExist)
                                break;
                        }
                    }
                }
                if (!isExist)
                    spannedContent.setSpan(spanType, start, end, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }

            contentView.setText(spannedContent);
            contentView.setSelection(start, end);
            spannedIndices.set(getSpannedIndices(spannedContent, false)); // updates new spanned indices
        }
    }

    private void _removeSpan(@NonNull final SpannableStringBuilder strBldr, final int start, final int end) {
        final StyleSpan[] styles = strBldr.getSpans(start, end, StyleSpan.class);
        final UnderlineSpan[] underlineSpans = strBldr.getSpans(start, end, UnderlineSpan.class);
        final ForegroundColorSpan[] foregroundColorSpans = strBldr.getSpans(start, end, ForegroundColorSpan.class);
        final BackgroundColorSpan[] backgroundColorSpans = strBldr.getSpans(start, end, BackgroundColorSpan.class);

        for (StyleSpan styleSpan : styles) {
            if (styleSpan.getStyle() == Typeface.BOLD) {
                strBldr.removeSpan(styleSpan);
                continue;
            }
            strBldr.removeSpan(styleSpan);
        }

        for (UnderlineSpan underlineSpan : underlineSpans) {
            strBldr.removeSpan(underlineSpan);
        }

        for (ForegroundColorSpan foregroundColorSpan : foregroundColorSpans) {
            strBldr.removeSpan(foregroundColorSpan);
        }

        for (BackgroundColorSpan backgroundColorSpan : backgroundColorSpans) {
            strBldr.removeSpan(backgroundColorSpan);
        }
    }
}
