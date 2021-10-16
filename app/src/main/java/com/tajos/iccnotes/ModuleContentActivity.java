package com.tajos.iccnotes;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.content.res.AppCompatResources;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.TextWatcher;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.text.style.UnderlineSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;

import iccnote.App;
import iccnote.FirebaseDB;
import iccnote.InternetConnection;
import iccnote.Module;
import iccnote.SavedData;
import iccnote.Subject;
import layouts.RoundedLayout;
import modules_content_activity_classes.ModuleContentAdapter;
import modules_content_activity_classes.PaintWindow;
import modules_content_activity_classes.SearchContentThread;
import modules_content_activity_classes.TajosImageSpan;

public class ModuleContentActivity extends AppCompatActivity {

    private static final String TAG = "ModuleContentActivity";

    public static final String BOLD = "BOLD";
    public static final String ITALIC = "ITALIC";
    public static final String UNDERLINE = "UNDERLINE";
    public static final String IMAGE = "IMAGE";
    public static final String FOREGROUND = "FOREGROUND";
    public static final String BACKGROUND = "BACKGROUND";

    private int contentFragmentWidth;

    private RecyclerView recyclerView;
    private FrameLayout scenesRoot;
    private LinearLayout root;

    private final HashMap<String, Object> intentDataMap = new HashMap<>();
    private final static List<Module> meetings = new ArrayList<>();
    private static List<HashMap<String, Object>> contents = new ArrayList<>();
    private List<Module> modules = new ArrayList<>();
    private List<SpannableStringBuilder> spannables;

    private ModuleContentAdapter adapter;
    private ModuleContentAdapter.OnCardClickedListener cardListener;

    private final SearchContentThread searchingThread = new SearchContentThread(this);

    private FirebaseDB database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_content);
        database = new FirebaseDB(App.getDatabaseReference(), false, null);

        // calculate the content fragment width for the image span.
        final float contentPadding = App.convertDptoPx(8f);
        final int screenWidth = App.getScreenWidth(this);
        contentFragmentWidth = screenWidth - (int)contentPadding - (int)App.convertDptoPx(28f);

        _initBundles();
        _initLogic();
    }

    @Override
    protected void onDestroy() {
        searchingThread.quit();
        super.onDestroy();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _initLogic() {
        _initContents();
        _initSearchContentView();

        spannables = _initSpannables();
        adapter = new ModuleContentAdapter(this, spannables, null);
        adapter.setOnCardClickListener(cardListener);
        recyclerView.setItemViewCacheSize(4);
        recyclerView.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        recyclerView.setAdapter(adapter);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
    }

    /*
    * initialize all the contents of the subject's module.
    */
    private void _initContents() {
        final String jsonContentFromIntent = getIntent().getStringExtra("contents");
        modules = new Gson().fromJson(getIntent().getStringExtra("modules"), new TypeToken<List<Module>>(){}.getType());
        intentDataMap.put("subject_key", getIntent().getStringExtra("subject_key"));
        intentDataMap.put("module_key", getIntent().getStringExtra("module_key"));
        intentDataMap.put("term", getIntent().getStringExtra("term"));
        intentDataMap.put("subject_position", getIntent().getIntExtra("subject_position", -1));
        intentDataMap.put("module_position", getIntent().getIntExtra("module_position", -1));

        if (jsonContentFromIntent.equals("null")) {
            contents = new ArrayList<>();
            return;
        }

        contents = new Gson().fromJson(jsonContentFromIntent, new TypeToken<List<HashMap<String, Object>>>(){}.getType());
    }

    private boolean isKeyboardShown;
    private boolean isFirstInit = true;
    @SuppressLint("NotifyDataSetChanged")
    private void _initBundles() {
        root = findViewById(R.id.root);
        recyclerView = findViewById(R.id.module_content_recyclerview);
        TextView addSearchContentTask = findViewById(R.id.add_search_content_task);
        scenesRoot = findViewById(R.id.scene_root);

        /* * * * * * * * * * *
         * searching listener
         * * * * * * * * * * */
        //when search is completed we need to set new adapter and notify the recyclerview of the changes.
        searchingThread.setOnSearchListener((availableContent) -> {
            // on search completed
            adapter = new ModuleContentAdapter(this, null, availableContent);
            adapter.setOnCardClickListener(cardListener);
            recyclerView.setAdapter(adapter);
            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        });
        searchingThread.start();

        /* * * * * * * * * * * * *
        * soft keyboard listener
        * * * * * * * * * * * * */
        root.getViewTreeObserver().addOnGlobalLayoutListener(() -> {
            int heightDiff = root.getRootView().getHeight() - root.getHeight();
            Log.i("HEIGHT_DIFF", String.valueOf(heightDiff));
            if (heightDiff > App.convertDptoPx(200)) { // if more than 200 dp, it's probably a keyboard...
                isKeyboardShown = true;
                return;
            }
            isKeyboardShown = false;
        });

        /* * * * * * * * * * * * *
         * card listener
         * * * * * * * * * * * * */
        cardListener = new ModuleContentAdapter.OnCardClickedListener() {
            @Override
            public void onClick() {
                // TO-DO
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDeleteButtonClick(int position) {
                contents.remove(position);
                spannables = _initSpannables();
                adapter = new ModuleContentAdapter(ModuleContentActivity.this, spannables, null);
                adapter.setOnCardClickListener(cardListener);
                recyclerView.setAdapter(adapter);
                Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                _updateData();
            }
        };

        /* * * * * * * * * * * * *
         * add/searchContent listener
         * * * * * * * * * * * * */
        addSearchContentTask.setOnClickListener(new View.OnClickListener() {
            boolean isOnSearchContentView = true;
            @Override
            public void onClick(View view) {
                if (Objects.requireNonNull(intentDataMap.get("module_key")).toString().equals(Module.ALL)) {
                    Toast.makeText(ModuleContentActivity.this, "You can't add content, you are in ALL MODULES page!", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (isOnSearchContentView) {
                    isOnSearchContentView = false;
                    ((TextView)view).setText(R.string.search_content);
                    _initAddContentView("", null, null, false);
                    return;
                }
                isOnSearchContentView = true;
                isFirstInit = false;
                ((TextView)view).setText(R.string.task);
                _initSearchContentView();
            }
        });
    }

    private final ActivityResultLauncher<Intent> launcher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
        if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
            final Intent data = result.getData();
            assert data != null;
            final Uri[] imgUri = {data.getData()};

            final RoundedLayout layout = (RoundedLayout) scenesRoot.getChildAt(0);
            EditText editTextContentView = layout.findViewById(R.id.edittxt_content);

            // fix Uri path
            if (imgUri[0].toString().startsWith("content://com.android")) {
                if (imgUri[0].toString().contains("%3A")) {
                    String[] split =imgUri[0].toString().split("%3A");
                    imgUri[0] = Uri.parse("content://media/external/images/media/"+split[1]);
                } else {
                    final int lastSlashIndex = imgUri[0].toString().lastIndexOf("/");
                    final String id = App.cutString(imgUri[0].toString(), lastSlashIndex+1, imgUri[0].toString().length());
                    imgUri[0] = Uri.parse("content://media/external/images/media/"+id);
                }
            }

            Bitmap img;
            boolean isOrigWidth;
            img = App.getBitmapFromUri(this, imgUri[0]);
            // if image is null it means cant acces the image, just return.
            if (img == null)
                return;

            isOrigWidth = img.getWidth() <= editTextContentView.getMeasuredWidth();
            if (!isOrigWidth)
                img = _readjustBitmap(img, editTextContentView.getMeasuredWidth());

            _initAddContentView(editTextContentView.getText().toString() + " ", img, imgUri[0].toString(), isOrigWidth);
        }
    });

    private Bitmap _readjustBitmap(@NonNull final Bitmap bmp, final int newWidth) {
        final int bmpWidth = bmp.getWidth();
        final int bmpHeight = bmp.getHeight();

        // calculation to know the new height of the bitmap
        final float rate = newWidth / (float) bmpWidth;
        final int newHeight = (int)(bmpHeight * rate);

        return Bitmap.createScaledBitmap(bmp, newWidth, newHeight, true);
    }

    /*
    * This will initialize the content for Add content view;
    */
    private boolean isEditTextContentOnError = false; // boolean for edittext content if its on error or not.
    public static final AtomicReference<List<HashMap<String, Object>>> spannedIndices = new AtomicReference<>();
    @SuppressLint({"NotifyDataSetChanged", "InflateParams"})
    private void _initAddContentView(@Nullable final String str, @Nullable final Bitmap img, @Nullable final String source, boolean isOrigWidth) {
        _resetAdapter(); // always reset the adapter of recyclerview.
        scenesRoot.removeAllViews();

        final SpannableStringBuilder spannedContent = new SpannableStringBuilder(str); // spanned content for the edittext of add content view

        final Intent pickImage = new Intent(Intent.ACTION_GET_CONTENT);
        pickImage.setType("image/*");

        View view = LayoutInflater.from(this).inflate(R.layout.add_content_view, scenesRoot);
        final LinearLayout stylesRoot = view.findViewById(R.id.styles_root);
        final ImageButton checkBtn = view.findViewById(R.id.check_btn);
        final EditText editTextContent = view.findViewById(R.id.edittxt_content);
        final ImageButton boldBtn = view.findViewById(R.id.bold);
        final ImageButton italicBtn = view.findViewById(R.id.italic);
        final ImageButton underlineBtn = view.findViewById(R.id.underline);
        final ImageButton paintBtn = view.findViewById(R.id.paint);
        final ImageButton addImageBtn = view.findViewById(R.id.add_img);

        editTextContent.requestFocus();
        // if the soft keyboard is not shown, show it. Otherwise, don't.
        if(!isKeyboardShown)
            App.showKeyboard(this);

        assert str != null;
        if (!str.isEmpty()) {
            assert img != null;
            spannedContent.setSpan(new TajosImageSpan(this, img, source), str.length() - 1, str.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
            editTextContent.setText(spannedContent);
            spannedIndices.set(_getSpannedIndices(spannedContent, isOrigWidth));
        }

        /* * * * * * * * * * * * * * * * *
        * LISTENERS FOR ADD CONTENT VIEW
        * * * * * * * * * * * * * * * * */
        // edittext content click listener
        editTextContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
                if (isEditTextContentOnError) {
                    isEditTextContentOnError = false;
                    final int color = ResourcesCompat.getColor(getResources(), R.color.white, null);
                    ViewCompat.setBackgroundTintList(editTextContent, ColorStateList.valueOf(color));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });
        // bold btn listener
        boldBtn.setOnClickListener(view13 -> _setSpan(editTextContent, spannedContent, new StyleSpan(Typeface.BOLD), -1, BOLD));
        // italic btn listener
        italicBtn.setOnClickListener(view14 -> _setSpan(editTextContent, spannedContent, new StyleSpan(Typeface.ITALIC), -1, ITALIC));
        // underline btn listener
        underlineBtn.setOnClickListener(view15 -> _setSpan(editTextContent, spannedContent, new UnderlineSpan(), -1, UNDERLINE));
        // add image button click listener
        addImageBtn.setOnClickListener(view16 -> launcher.launch(pickImage));

        // paint button click listener
        paintBtn.setOnClickListener(view12 -> {
            paintBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_paint_blue));
            // show the paint window and set its listener
            new PaintWindow().show(this, paintBtn, (data) -> {
                // on finish selecting color
                paintBtn.setImageDrawable(AppCompatResources.getDrawable(this, R.drawable.ic_paint_black));
                // text color type highlight
                if (data.containsKey(PaintWindow.TYPE) && data.containsKey(PaintWindow.COLOR)) {
                    if (Objects.requireNonNull(data.get(PaintWindow.TYPE)) == PaintWindow.TEXT_COLOR_TYPE) {
                        _setSpan(editTextContent, spannedContent, new ForegroundColorSpan(Objects.requireNonNull(data.get(PaintWindow.COLOR))), Objects.requireNonNull(data.get(PaintWindow.COLOR)), FOREGROUND);
                        return;
                    }
                    //background type highlight
                    _setSpan(editTextContent, spannedContent, new BackgroundColorSpan(Objects.requireNonNull(data.get(PaintWindow.COLOR))), Objects.requireNonNull(data.get(PaintWindow.COLOR)), BACKGROUND);
                }
            });
        });

        // checkBtn listener
        checkBtn.setOnClickListener(view1 -> {
            if (!editTextContent.getText().toString().isEmpty()) {
                if (isKeyboardShown)
                    App.hideKeyboard(this, view1);

                editTextContent.getLayoutParams().height = LinearLayout.LayoutParams.WRAP_CONTENT;
                editTextContent.requestLayout();

                final HashMap<String, Object> mapContents = new HashMap<>();
                final String indicesJson = new Gson().toJson(spannedIndices.get());

                mapContents.put("text", editTextContent.getText().toString());
                mapContents.put("indices", indicesJson);
                contents.add(mapContents);

                spannables = _initSpannables();
                adapter = new ModuleContentAdapter(this, spannables, null);
                adapter.setOnCardClickListener(cardListener);
                recyclerView.setAdapter(adapter);
                Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                editTextContent.setText("");
                _updateData();
                return;
            }
            isEditTextContentOnError = true;
            App.animateErrorEffect(ModuleContentActivity.this, editTextContent);
        });
        // stylesRoot listener
        stylesRoot.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            private boolean isFirstInit = true;
            private int stylesRootHeight;
            @Override
            public void onGlobalLayout() {
                // store the original height of the root of the styles.
                if (isFirstInit) {
                    isFirstInit = false;
                    stylesRootHeight = stylesRoot.getMeasuredHeight();
                    return;
                }
                // checks if the height of the root layout of styles button are still the same.
                // if its not, set a fixed height for the text content so the styles button wont dissapear
                // on the screen if the text input are too long.
                if (stylesRootHeight != stylesRoot.getMeasuredHeight()) {
                    final int lostHeight = Math.abs(stylesRootHeight - stylesRoot.getMeasuredHeight());

                    editTextContent.getLayoutParams().height = editTextContent.getMeasuredHeight() - lostHeight;
                    editTextContent.requestLayout();
                }
            }
        });
    }
    /*
    * This will initialize the content for search content view;
    */
    private void _initSearchContentView() {
        scenesRoot.removeAllViews();
        View view = LayoutInflater.from(ModuleContentActivity.this).inflate(R.layout.search_content_view, scenesRoot);
        final SearchView searchView = view.findViewById(R.id.searchview);

        if (!isFirstInit)
            searchView.setIconified(false);
        // ===> LISTENERS <===
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                _searchContent(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                _searchContent(newText);
                return true;
            }
        });
    }

    /*
    * this will initialize the spannables for each contents before passing it to @ModuleContentAdapter.
    * so we can display the spanned contents on recyclerview.
    * @used in initLogic method => this will be called every time this@ModuleContentActivity activity will be opened.
    * @used in checkBtn View listener inside _initAddContentView method.
    */
    @NonNull
    private List<SpannableStringBuilder> _initSpannables() {
        List<SpannableStringBuilder> spannedContents = new ArrayList<>(); // list of spanned content for recyclerview

        for (HashMap<String, Object> spanMap : contents) {
            final String text = Objects.requireNonNull(spanMap.get("text")).toString();
            final List<HashMap<String, Object>> spannedIndicesList = new Gson().fromJson(Objects.requireNonNull(spanMap.get("indices")).toString(), new TypeToken<List<HashMap<String, Object>>>() {}.getType());
            spannedIndices.set(spannedIndicesList);

            SpannableStringBuilder spannedContent = new SpannableStringBuilder(text);

            // checks if there is an indices of spanned text and if there is, span it. Otherwise, it is plain text, dont span.
            if (spannedIndicesList != null && spannedIndicesList.size() > 0)
                _resetSpan(spannedContent);

            spannedContents.add(spannedContent);
        }

        return spannedContents;
    }
    /*
     * this will get the spanned indices from editTextContent of AddContentView and store it to spannedIndices variable
     * @everytime the BOLD, ITALIC and UNDERLINE button is clicked, this will be called so we can update the indices list
     */
    @NonNull
    private List<HashMap<String, Object>> _getSpannedIndices(@NonNull final SpannableStringBuilder span, boolean isOrigWidth) {
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

        Log.i(TAG, "getSpannedIndices: " + spannedIndices.toString());
        return spannedIndices;
    }
    /*
     * this will set the span of editTextContent from AddContentView
     * @when BOLD is clicked, this will be called
     * @when ITALIC is clicked, this will be called
     * @when UNDERLINE is clicked, this will be called
     */
    private void _setSpan(@NonNull EditText contentView, SpannableStringBuilder spannedContent, Object spanType, int COLOR, String SPAN_TYPE) {
        if (contentView.getSelectionStart() != contentView.getSelectionEnd()) {
            final int start = contentView.getSelectionStart();
            final int end = contentView.getSelectionEnd();
            final CharSequence selectedText = App.cutString(contentView.getText(), start, end);

            if (spannedContent.length() < contentView.length()) {
                final String spanned = spannedContent.toString();
                final String newContent = App.cutString(contentView.getText().toString(), 0, spannedContent.length());
                if (spanned.equals(newContent))
                    spannedContent.append(App.cutString(contentView.getText().toString(), spannedContent.length(), contentView.length()));
                else {
                    spannedContent.clear();
                    spannedContent.append(contentView.getText());
                    _resetSpan(spannedContent);
                }
            }
            // if spanned content is bigger than the new content text it means, the text was changed decremently in text size.
            if (spannedContent.length() > contentView.length()) {
                spannedContent.clear();
                spannedContent.append(contentView.getText());
                _resetSpan(spannedContent);
            }

            // this will remove a span if there's an existing span already.
            if (spannedContent.length()>0) {
                boolean isExist = false;
                if (spannedIndices.get() != null) {
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
            spannedIndices.set(_getSpannedIndices(spannedContent, false)); // updates new spanned indices
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

    /*
    * this will reset the span of contents, cuz it was changed drastically. It will try to find the text
    * if it cant find the text then it will remove the indices that was stored for that particular text
    * and set a new list of indices.
    */
    private void _resetSpan(final SpannableStringBuilder spannedContent) {
        final List<HashMap<String, Object>> newSpannedList = new ArrayList<>();
        final String TYPE = "type_span";

        for (HashMap<String, Object> indexMap : spannedIndices.get()) {
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
                img = App.getBitmapFromUri(this, imgUri);
                if (!isOrigWidth) {
                    assert img != null;
                    img = _readjustBitmap(img, contentFragmentWidth);
                }
                assert img != null;
                _span("image_i", new TajosImageSpan(this, img, imgUri.toString()), indexMap, spannedContent, newSpannedList);
            }
        }
        spannedIndices.set(newSpannedList);
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

    @SuppressLint("NotifyDataSetChanged")
    private void _searchContent(@NonNull final String text) {
        if (text.isEmpty()) {
            _resetAdapter();
            return;
        }

        final Message msgToHandler = Message.obtain();
        final String contentJson = new Gson().toJson(contents);
        {
            HashMap<String, String> msgMap = new HashMap<>();
            msgMap.put("search_key", text);
            msgMap.put("content", contentJson);
            msgToHandler.obj = new Gson().toJson(msgMap);
        }
        searchingThread.getHandler().sendMessage(msgToHandler);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _resetAdapter() {
        spannables = _initSpannables();
        adapter = new ModuleContentAdapter(this, spannables, null);
        adapter.setOnCardClickListener(cardListener);
        recyclerView.setAdapter(adapter);
        Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
    }

    /*
    * always checks internet access before pushing the data to the server
     */
    private void _updateData() {
        new Thread(new InternetConnection(this, new InternetConnection.OnConnectionResponseListener() {
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void isConnected() {
                final String reference = App.getDatabaseReference() + "/" + intentDataMap.get("subject_key") + "/" + intentDataMap.get("term");
                database = new FirebaseDB(reference, false, null);

                if (contents.size() > 0) {
                    {
                        HashMap<String, Object> moduleMap = new HashMap<>();
                        moduleMap.put(Objects.requireNonNull(intentDataMap.get("module_key")).toString(), contents);
                        final int modulePosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("module_position")).toString());
                        modules.get(modulePosition).put(Objects.requireNonNull(intentDataMap.get("module_key")).toString(), contents);
                        Log.i("MODULES CONTENT", modules.toString());
                        final int subjectPosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("subject_position")).toString());
                        App.getSubjects().get(subjectPosition).put(Objects.requireNonNull(intentDataMap.get("term")).toString(), modules);
                        database.updateData(moduleMap);

                        // if the module size is not greater than one then just return it
                        // cuz we dont need to update the ALL SUBJECT module if there is only one module
                        if (!(modules.size() > 1)) {
                            return;
                        }

                        // update the ALL SUBJECT module.
                        List<HashMap<String, Object>> allContents = new ArrayList<>();
                        for (Module module : modules) {
                            final String key = App.getKey(module);
                            assert key != null;
                            if (!key.equals(Module.ALL)) {
                                final String jsonContent = new Gson().toJson(module.get(key));
                                if (!jsonContent.equals("\"null\"")) {
                                    List<HashMap<String, Object>> contents = new Gson().fromJson(jsonContent, new TypeToken<List<HashMap<String, Object>>>() {}.getType());
                                    if (contents.size() > 0)
                                        allContents.addAll(contents);
                                }
                            }
                        }

                        modules.get(modules.size()-1).put(Module.ALL, allContents);
                        App.getSubjects().get(subjectPosition).put(Objects.requireNonNull(intentDataMap.get("term")).toString(), modules);
                        new Subject.InternalStorage(ModuleContentActivity.this).store(App.getSubjects());

                        moduleMap = new HashMap<>();
                        moduleMap.put(Module.ALL, allContents);
                        database.updateData(moduleMap);
                    }
                    return;
                }
                // if the program will pass here means the content is empty, the code block below is a must
                // bcuz the database will automatically delete the key when empty
                // so we need to add a new key with a string null value as placeholder.
                HashMap<String, Object> moduleMap = new HashMap<>();
                moduleMap.put(getIntent().getStringExtra("module_key"), "null");
                database.updateData(moduleMap);
            }
            @Override
            public void isNotConnected() {
                /*
                * this will save the content data locally on the device cache
                * bcuz the device is not connected to the internet. This will be used
                * every time the user open the app with internet connectivity, the program in homeactivity
                * will automatically push the saved data to the server immediately.
                */
                {
                    HashMap<Object, Object> itemToSave = new HashMap<>();
                    itemToSave.put(getString(R.string.PREF), App.getDatabaseReference() + "/" + Objects.requireNonNull(intentDataMap.get("subject_key")).toString());
                    itemToSave.put(getString(R.string.CHILD), Objects.requireNonNull(intentDataMap.get("term")).toString());
                    final int modulePosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("module_position")).toString());
                    final int subjectPosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("subject_position")).toString());
                    final String moduleKey = Objects.requireNonNull(intentDataMap.get("module_key")).toString();

                    if (contents.size() > 0) {
                        modules.get(modulePosition).put(moduleKey, contents);

                        if (modules.size() > 1) {
                            // update the ALL SUBJECT module.
                            List<HashMap<String, Object>> allContents = new ArrayList<>();
                            for (Module module : modules) {
                                final String key = App.getKey(module);
                                assert key != null;
                                if (!key.equals(Module.ALL)) {
                                    final String jsonContent = new Gson().toJson(module.get(key));
                                    if (!jsonContent.equals("\"null\"")) {
                                        List<HashMap<String, Object>> contents = new Gson().fromJson(jsonContent, new TypeToken<List<HashMap<String, Object>>>() {}.getType());
                                        allContents.addAll(contents);
                                    }
                                }
                            }
                            Log.i(TAG, "isConnected: ALLCONTENTS: " + allContents.toString());
                            modules.get(modules.size()-1).put(Module.ALL, allContents);
                        }
                    } else
                        // if the program will pass here means the the content is empty, the code block below is a must
                        // bcuz the database will automatically delete the key when empty
                        // so we need to add a new key with a string null value as placeholder.
                        modules.get(modulePosition).put(moduleKey, "null");

                    App.getSubjects().get(subjectPosition).put(Objects.requireNonNull(intentDataMap.get("term")).toString(), modules);
                    new Subject.InternalStorage(ModuleContentActivity.this).store(App.getSubjects());

                    /*                      TO-DO
                    * THIS WILL NEED FIXING => this will always add the modules even if
                    * it already exist in the list.
                    */
                    meetings.add(modules.get(modulePosition));
                    if (modules.size() > 0)
                        meetings.add(modules.get(modules.size()-1));

                    itemToSave.put(getString(R.string.VALUE), meetings);
                    SavedData.set(Objects.requireNonNull(intentDataMap.get("subject_key")).toString(), itemToSave, false);
                }
            }
        })).start();
    }
}