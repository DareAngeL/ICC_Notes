package com.tajos.iccnotes;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.content.res.ColorStateList;
import android.os.Bundle;
import android.os.Message;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

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
import modules_content_activity_classes.ModuleContentAdapter;
import modules_content_activity_classes.SearchContentThread;

public class ModuleContentActivity extends AppCompatActivity {

    private static final String TAG = "ModuleContentActivity";

    private RecyclerView recyclerView;
    private FrameLayout scenesRoot;
    private LinearLayout root;

    private final HashMap<String, Object> intentDataMap = new HashMap<>();
    private final List<Module> meetings = new ArrayList<>();
    private List<Module> modules = new ArrayList<>();
    private List<String> contents = new ArrayList<>();
    private ModuleContentAdapter adapter;
    private ModuleContentAdapter.OnCardClickedListener cardListener;

    private String jsonContents;

    private SearchContentThread searchingThread = new SearchContentThread(this);

    private FirebaseDB database;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_module_content);
        _initBundles();
        _initLogic();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        searchingThread.quit();
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _initLogic() {
        _initContents();
        _initSearchContentView();

        adapter = new ModuleContentAdapter(this, contents);
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

        if (jsonContentFromIntent.equals("null"))
            return;

        contents = new Gson().fromJson(jsonContentFromIntent, new TypeToken<ArrayList<String>>(){}.getType());
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
        searchingThread.setOnSearchListener((highlightsIndexes, availableContent) -> {
            // on search completed
            adapter = new ModuleContentAdapter(this, availableContent, highlightsIndexes);
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
                adapter.notifyDataSetChanged();
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
                if (isOnSearchContentView) {
                    isOnSearchContentView = false;
                    ((TextView)view).setText(R.string.search_content);
                    _initAddContentView();
                    return;
                }
                isOnSearchContentView = true;
                isFirstInit = false;
                ((TextView)view).setText(R.string.task);
                _initSearchContentView();
            }
        });
    }

    /*
    * This will initialize the content for Add content view;
    */
    private boolean isEditTextContentOnError = false; // boolean for edittext content if its on error or not.
    @SuppressLint("NotifyDataSetChanged")
    private void _initAddContentView() {
        _resetAdapter(); // always reset the adapter.

        scenesRoot.removeAllViews();
        View view = LayoutInflater.from(ModuleContentActivity.this).inflate(R.layout.add_content_view, scenesRoot);
        final ImageButton checkBtn = view.findViewById(R.id.check_btn);
        final EditText editTextContent = view.findViewById(R.id.edittxt_content);

        editTextContent.requestFocus();
        // if the soft keyboard is not shown, show it. Otherwise, don't.
        if(!isKeyboardShown)
            App.showKeyboard(this);

        editTextContent.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence charSequence, int i, int i1, int i2) {}
            @Override
            public void onTextChanged(CharSequence charSequence, int i, int i1, int i2) {
                if (isEditTextContentOnError) {
                    isEditTextContentOnError = false;
                    final int color = ResourcesCompat.getColor(getResources(), R.color.white, null);
                    ViewCompat.setBackgroundTintList(editTextContent, ColorStateList.valueOf(color));
                }
            }
            @Override
            public void afterTextChanged(Editable editable) {}
        });

        checkBtn.setOnClickListener(view1 -> {
            if (!editTextContent.getText().toString().isEmpty()) {
                if (isKeyboardShown)
                    App.hideKeyboard(this, view1);

                contents.add(editTextContent.getText().toString());
                adapter.notifyItemInserted(0);
                editTextContent.setText("");
                _updateData();
                return;
            }
            isEditTextContentOnError = true;
            App.animateErrorEffect(ModuleContentActivity.this, editTextContent);
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

        jsonContents = new Gson().toJson(contents);
        final Message msgToHandler = Message.obtain();
        {
            HashMap<String, String> msgMap = new HashMap<>();
            msgMap.put("search_key", text);
            msgMap.put("contents", jsonContents);
            msgToHandler.obj = new Gson().toJson(msgMap);
        }
        searchingThread.getHandler().sendMessage(msgToHandler);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _resetAdapter() {
        adapter = new ModuleContentAdapter(this, contents);
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
                        moduleMap.put(getIntent().getStringExtra("module_key"), contents);
                        final int modulePosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("module_position")).toString());
                        modules.get(modulePosition).put(Objects.requireNonNull(intentDataMap.get("module_key")).toString(), contents);
                        Log.i("MODULES CONTENT", modules.toString());
                        final int subjectPosition = Integer.parseInt(Objects.requireNonNull(intentDataMap.get("subject_position")).toString());
                        App.getSubjects().get(subjectPosition).put(Objects.requireNonNull(intentDataMap.get("term")).toString(), modules);
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
                    final String moduleKey = Objects.requireNonNull(intentDataMap.get("module_key")).toString();
                    if (contents.size() > 0)
                        modules.get(modulePosition).put(moduleKey, contents);
                    else
                        // if the program will pass here means the the content is empty, the code block below is a must
                        // bcuz the database will automatically delete the key when empty
                        // so we need to add a new key with a string null value as placeholder.
                        modules.get(modulePosition).put(moduleKey, "null");

                    meetings.add(modules.get(modulePosition));

                    itemToSave.put(getString(R.string.VALUE), meetings);
                    Log.i("CONTENT TO SAVE", itemToSave.toString());
                    SavedData.set(Objects.requireNonNull(intentDataMap.get("subject_key")).toString(), itemToSave, false);
                }
            }
        })).start();
    }
}