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

import iccnote.App;
import iccnote.FirebaseDB;
import iccnote.InternetConnection;
import iccnote.Module;
import iccnote.SavedData;
import iccnote.Subject;
import layouts.RoundedLayout;
import modules_content_activity_classes.ModuleContentAdapter;
import modules_content_activity_classes.PaintWindow;
import modules_content_activity_classes.SearchContent;
import modules_content_activity_classes.SpanHelper;
import modules_content_activity_classes.TajosImageSpan;

public class ModuleContentActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private FrameLayout scenesRoot;
    private LinearLayout root;

    private final HashMap<String, Object> intentDataMap = new HashMap<>();
    private final static List<Module> meetings = new ArrayList<>();
    private static List<HashMap<String, Object>> contents = new ArrayList<>();
    private List<Module> modules = new ArrayList<>();

    private ModuleContentAdapter adapter;
    private ModuleContentAdapter.OnCardClickedListener cardListener;

    private SearchContent searchingThread;
    private FirebaseDB database;
    private SpanHelper spanHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_content);
        database = new FirebaseDB(App.getDatabaseReference(), false, null);

        // calculate the content fragment width for the image span.
        final float contentPadding = App.convertDptoPx(8f);
        final int screenWidth = App.getScreenWidth(this);
        final int contentFragmentWidth = screenWidth - (int) contentPadding - (int) App.convertDptoPx(28f);
        spanHelper = new SpanHelper(this, contentFragmentWidth);
        // content fragment width will be passed to searchingThread bcuz we will parse
        // the image span also there, so we need it there.
        searchingThread = new SearchContent(this);

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

        adapter = new ModuleContentAdapter(this, contents,  spanHelper, false);
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
        modules = new Gson().fromJson(getIntent().getStringExtra("modules"), new TypeToken<List<Module>>(){}.getType());
        intentDataMap.put("subject_key", getIntent().getStringExtra("subject_key"));
        intentDataMap.put("module_key", getIntent().getStringExtra("module_key"));
        intentDataMap.put("term", getIntent().getStringExtra("term"));
        intentDataMap.put("subject_position", getIntent().getIntExtra("subject_position", -1));
        intentDataMap.put("module_position", getIntent().getIntExtra("module_position", -1));

        contents = App.getContents();
    }

    private boolean isKeyboardShown;
    private boolean isFirstInit = true;
    @SuppressLint({"NotifyDataSetChanged", "ClickableViewAccessibility"})
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
            adapter = new ModuleContentAdapter(this, availableContent, spanHelper, true);
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
            /*
            * This will listen for a double click and will scroll either up/down
            * depending of where the double click event is detected
            */
            long clickedInterval = 0;
            long lastClickedMillis = 0;
            final int DOUBLE_CLICK_THRESHOLD = 500; // 500ms interval threshold to be considered a double click
            final int scrnCenter = App.getScreenHeight(ModuleContentActivity.this) / 2;
            @Override
            public void onClick(int xPosition, int yPosition) {
                if (lastClickedMillis == 0) {
                    lastClickedMillis = System.currentTimeMillis();
                    return;
                }

                clickedInterval = System.currentTimeMillis() - lastClickedMillis;
                if (clickedInterval <= DOUBLE_CLICK_THRESHOLD) {
                    if (yPosition < scrnCenter) { // double click is detected on the top of the screen, so it will scroll to the very top of the list
                        recyclerView.smoothScrollToPosition(0);
                    } else { // double click is detected on the bottom of the screen, so it will scroll to the very bottom of the list.
                        recyclerView.smoothScrollToPosition(adapter.getItemCount()-1);
                    }
                    lastClickedMillis = 0;
                    return;
                }
                lastClickedMillis = System.currentTimeMillis();
            }
            @SuppressLint("NotifyDataSetChanged")
            @Override
            public void onDeleteButtonClick(int position) {
                contents.remove(position);
                adapter = new ModuleContentAdapter(ModuleContentActivity.this, contents, spanHelper, false);
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
                img = spanHelper.readjustBitmap(img, editTextContentView.getMeasuredWidth());

            _initAddContentView(editTextContentView.getText().toString() + " ", img, imgUri[0].toString(), isOrigWidth);
        }
    });

    /*
    * This will initialize the content for Add content view;
    */
    private boolean isEditTextContentOnError = false; // boolean for edittext content if its on error or not.
    @SuppressLint({"NotifyDataSetChanged", "InflateParams"})
    private void _initAddContentView(@Nullable final String str, @Nullable final Bitmap img, @Nullable final String source, boolean isOrigWidth) {
        _resetAdapter(); // always reset the adapter of recyclerview.
        scenesRoot.removeAllViews();

        final SpannableStringBuilder spannedContent = new SpannableStringBuilder(str); // spanned content for the edittext of add content view
        @SuppressWarnings("rawtypes") final List[] indices = new List[]{null};

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
            SpanHelper.spannedIndices.set(spanHelper.getSpannedIndices(spannedContent, isOrigWidth));
            indices[0] = SpanHelper.spannedIndices.get();
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
        boldBtn.setOnClickListener(view13 -> {
            spanHelper.setSpan(editTextContent, spannedContent, new StyleSpan(Typeface.BOLD), -1, SpanHelper.BOLD);
            indices[0] = SpanHelper.spannedIndices.get();
        });
        // italic btn listener
        italicBtn.setOnClickListener(view14 -> {
            spanHelper.setSpan(editTextContent, spannedContent, new StyleSpan(Typeface.ITALIC), - 1, SpanHelper.ITALIC);
            indices[0] = SpanHelper.spannedIndices.get();
        });
        // underline btn listener
        underlineBtn.setOnClickListener(view15 -> {
            spanHelper.setSpan(editTextContent, spannedContent, new UnderlineSpan(), - 1, SpanHelper.UNDERLINE);
            indices[0] = SpanHelper.spannedIndices.get();
        });
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
                        spanHelper.setSpan(editTextContent, spannedContent, new ForegroundColorSpan(Objects.requireNonNull(data.get(PaintWindow.COLOR))), Objects.requireNonNull(data.get(PaintWindow.COLOR)), SpanHelper.FOREGROUND);
                        indices[0] = SpanHelper.spannedIndices.get();
                        return;
                    }
                    //background type highlight
                    spanHelper.setSpan(editTextContent, spannedContent, new BackgroundColorSpan(Objects.requireNonNull(data.get(PaintWindow.COLOR))), Objects.requireNonNull(data.get(PaintWindow.COLOR)), SpanHelper.BACKGROUND);
                    indices[0] = SpanHelper.spannedIndices.get();
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
                final String indicesJson = new Gson().toJson(indices[0]);

                mapContents.put("text", editTextContent.getText().toString());
                mapContents.put("indices", indicesJson);
                contents.add(mapContents);

                adapter = new ModuleContentAdapter(this, contents, spanHelper, false);
                adapter.setOnCardClickListener(cardListener);
                recyclerView.setAdapter(adapter);
                Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
                recyclerView.scrollToPosition(contents.size()-1);
                editTextContent.setText("");
                spannedContent.clear();
                spannedContent.clearSpans();
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
        adapter = new ModuleContentAdapter(this, contents, spanHelper, false);
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