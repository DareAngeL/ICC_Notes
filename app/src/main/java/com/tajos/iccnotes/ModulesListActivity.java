package com.tajos.iccnotes;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import modules_list_activity_classes.ModuleListAdapter;

public class ModulesListActivity extends AppCompatActivity {

    private TextView term;
    private TextView modulesSubjectName;
    private RecyclerView modulesRecyclerview;

    private boolean isOnAddingModules = false;

    private String key; // subject key
    private final HashMap<String, Object> intentDataMap = new HashMap<>();
    private final HashMap<String, Object> itemToSave = new HashMap<>();
    private final HashMap<String, Object> itemToDelete = new HashMap<>();
    private final List<HashMap<String, Object>> meetings = new ArrayList<>();
    private final List<HashMap<String, Object>> meetingsToRemove = new ArrayList<>();

    private List<Module> modules = new ArrayList<>();
    private int moduleCount = 0;
    private String staticTerm;
    private int position; // subject position

    private ModuleListAdapter adapter;
    private ModuleListAdapter.OnModuleClickedListener moduleBtnListener;

    private void _deleteModule(final int position) {
        final String keyToRemove = App.getKey(modules.get(position));
        _checkNetworkAndUpdateDatabase(keyToRemove);
        modules.remove(position);
        isOnAddingModules = false;

        if (moduleCount == 2) {
            final String keyToRemove2 = App.getKey(modules.get(modules.size()-1));
            _checkNetworkAndUpdateDatabase(keyToRemove2);
            modules.remove(modules.size() - 1);
            isOnAddingModules = false;
        }

        moduleCount--;
        App.getSubjects().get(this.position).put(staticTerm, modules);
        new Subject.InternalStorage(ModulesListActivity.this)
                .store(App.getSubjects());
        _checkModulesSize();
    }
    // check the module size, if its only one remaining which is the
    // all modules card - clear the module list.
    private void _checkModulesSize() {
        if (modules.size() == 1) {
            final Module module = modules.get(0);
            if (App.getKey(module).equals(Module.ALL) || App.getKey(module).equals(Module.FIRST_MEETING)) {
                modules.clear();
                App.getSubjects().get(position).put(staticTerm, "null");
                _checkNetworkAndUpdateDatabase(Module.ALL);
            }
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_modules_list);
        _initBundles();
        _initLogic();
    }

    private boolean isFromModuleContent = false; // this is used to know if the onResume was called from ModuleContentActivity.
    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onResume() {
        super.onResume();
        if (isFromModuleContent) {
            final List<Module> updatedModules = App.convertObjectToList(App.getSubjects().get(position).get(staticTerm));
            Log.i("UPDATED MODULES", updatedModules.toString());
            modules = App.sortModules(updatedModules);
            adapter = new ModuleListAdapter(this, modules, intentDataMap);
            adapter.setOnModuleClickedListener(moduleBtnListener);
            modulesRecyclerview.setAdapter(adapter);
            Objects.requireNonNull(modulesRecyclerview.getAdapter()).notifyDataSetChanged();
        }
    }

    private void _initLogic() {
        _initialize();
    }

    private boolean isAddedAllModules;
    @SuppressLint("NotifyDataSetChanged")
    private void _initBundles() {
        term = findViewById(R.id.term);
        modulesRecyclerview = findViewById(R.id.modules_recyclerview);
        modulesSubjectName = findViewById(R.id.modules_subject_name);
        FloatingActionButton fab = findViewById(R.id.fab);

        // on delete btn clicked
        moduleBtnListener = new ModuleListAdapter.OnModuleClickedListener() {
            @Override
            public void onDeleteBtnClick(int position) {
                _deleteModule(position);
            }

            @Override
            public void onClick() {
                isFromModuleContent = true;
            }
        };
        // on fab btn clicked
        fab.setOnClickListener(view -> {
            switch (moduleCount) {
                case 0:
                    isAddedAllModules = false;
                    isOnAddingModules = true;
                    modules.add(new Module(Module.FIRST_MEETING, "null"));
                    adapter.notifyItemInserted(modules.size()-1);
                    App.getSubjects().get(position).put(getIntent().getStringExtra("term"), App.convertArrayToLnkedTree(modules));
                    _checkNetworkAndUpdateDatabase(Module.FIRST_MEETING);
                    moduleCount++;
                    break;
                case 1:
                    isAddedAllModules = true;
                    isOnAddingModules = true;
                    modules.add(new Module(Module.SECOND_MEETING, "null"));
                    modules.add(new Module(Module.ALL, "null"));
                    adapter.notifyItemInserted(modules.size()-1);
                    App.getSubjects().get(position).put(getIntent().getStringExtra("term"), modules);
                    _checkNetworkAndUpdateDatabase(Module.SECOND_MEETING);
                    moduleCount++;
                    break;
                case 2:
                    isAddedAllModules = false;
                    isOnAddingModules = true;
                    modules.add(modules.size()-1, new Module(Module.THIRD_MEETING, "null"));
                    adapter.notifyDataSetChanged();
                    App.getSubjects().get(position).put(getIntent().getStringExtra("term"), modules);
                    _checkNetworkAndUpdateDatabase(Module.THIRD_MEETING);
                    moduleCount++;
                    break;
                case 3:
                    isAddedAllModules = false;
                    isOnAddingModules = true;
                    modules.add(modules.size()-1, new Module(Module.FOURTH_MEETING, "null"));
                    adapter.notifyDataSetChanged();
                    App.getSubjects().get(position).put(getIntent().getStringExtra("term"), modules);
                    _checkNetworkAndUpdateDatabase(Module.FOURTH_MEETING);
                    moduleCount++;
                    break;
                case 4:
                    isAddedAllModules = false;
                    isOnAddingModules = true;
                    modules.add(modules.size()-1, new Module(Module.FIFTH_MEETING, "null"));
                    adapter.notifyDataSetChanged();
                    App.getSubjects().get(position).put(getIntent().getStringExtra("term"), modules);
                    _checkNetworkAndUpdateDatabase(Module.FIFTH_MEETING);
                    moduleCount++;
                    break;
            }
        });
    }

    /*
    * check network and update the database if connected to internet.
    * or just only store to internal storage if not connected to internet
    */
    private void _checkNetworkAndUpdateDatabase(String keyForRemoving) {
        Log.i("SUBJECT", App.getSubjects().get(position).toString());
        new Thread(new InternetConnection(this, new InternetConnection.OnConnectionResponseListener() {
            @Override
            public void isConnected() {
                _updateDatabase(keyForRemoving, isOnAddingModules);
                Toast.makeText(ModulesListActivity.this, "Database updated!", Toast.LENGTH_SHORT).show();
                new Subject.InternalStorage(ModulesListActivity.this)
                        .store(App.getSubjects());
            }

            @Override
            public void isNotConnected() {
                _saveData(keyForRemoving);
                new Subject.InternalStorage(ModulesListActivity.this)
                        .store(App.getSubjects());

                Toast.makeText(ModulesListActivity.this, "Storage updated!", Toast.LENGTH_SHORT).show();
            }
        })).start();
    }

    private void _updateDatabase(final String key, final boolean isAddingModules) {
        final String referenceForAdding = App.getDatabaseReference() + "/" + this.key;
        final String referenceForRemoving = App.getDatabaseReference()+"/"+this.key+"/"+staticTerm;
        Log.i("REFERENCE", referenceForRemoving);
        FirebaseDB database;
        if (isAddingModules) {
            database = new FirebaseDB(referenceForAdding, true, staticTerm);
            Log.i("MODULES", modules.toString());
            database.updateData(modules.get(moduleCount - 1));
            if (isAddedAllModules)
                database.updateData(modules.get(moduleCount));

            return;
        }
        // remove the data from the database
        database = new FirebaseDB(referenceForRemoving, true, key);
        database.removeData();
        // when removing all modules/meetings always update the database
        // with a null string value
        if (key.equals(Module.FIRST_MEETING)) {
            database = new FirebaseDB(referenceForAdding, false, null);
            database.updateData(new Module(staticTerm, "null"));
        }
    }
    /*
    * save the modules data locally if no internet access to push the data to the server
    * we save it , so when this app will be opened w/ internet access, the saved data
    * will be push to the server automatically.
    */
    private int removalItemsCount = 0;
    private void _saveData(final String keyToRemove) {
        if (isOnAddingModules) {
            {
                if (! itemToSave.containsKey(getString(R.string.PREF)) && ! itemToSave.containsKey(getString(R.string.CHILD))) {
                    itemToSave.put(getString(R.string.PREF), App.getDatabaseReference() + "/" + key);
                    itemToSave.put(getString(R.string.CHILD), staticTerm);
                }
                meetings.add(modules.get(moduleCount - 1));
                if (isAddedAllModules)
                    meetings.add(modules.get(moduleCount));

                itemToSave.put(getString(R.string.VALUE), meetings);
                SavedData.set(key, itemToSave, false);
            }
            return;
        }
        // if is not on adding modules.
        removalItemsCount++;
        {
            if (!itemToDelete.containsKey(getString(R.string.PREF)) && ! itemToDelete.containsKey(getString(R.string.CHILD))) {
                itemToDelete.put(getString(R.string.PREF), App.getDatabaseReference() + "/" + key + "/" + staticTerm);
                itemToDelete.put(getString(R.string.CHILD), "null");
            }
            meetingsToRemove.add(new Module("key"+removalItemsCount, keyToRemove));

            itemToDelete.put("remainingModule", true);
            if (keyToRemove.equals(Module.FIRST_MEETING)) {
                itemToDelete.put("remainingModule", false);
            }

            itemToDelete.put(getString(R.string.VALUE), meetingsToRemove);
            Log.i("ITEM TO DEL", itemToDelete.toString());
            SavedData.set(key, itemToDelete, true);
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _initialize() {
        Log.i("CHILD", getIntent().getStringExtra("term"));
        term.setText(getIntent().getStringExtra("term"));
        modulesSubjectName.setText(getIntent().getStringExtra("subject_name"));
        position = getIntent().getIntExtra("position", 0);
        staticTerm = getIntent().getStringExtra("term");
        key = getIntent().getStringExtra("key");

        final String fromJsonStr = getIntent().getStringExtra("modules");
        final List<HashMap<String, Object>> mappedModules = new Gson().fromJson(fromJsonStr, new TypeToken<List<HashMap<String, Object>>>() {}.getType());
        final List<Module> pModules = new ArrayList<>();
        // convert HashMap to Module Object
        for (HashMap<String, Object> map : mappedModules) {
            final String moduleKey = App.getKey(map);
            if (moduleKey == null)
                return;

            pModules.add(new Module(moduleKey, map.get(moduleKey)));
        }
        modules = pModules; // store the pModules to modules after getting it
        if (modules.size() > 2)
            moduleCount = modules.size()-1; // always update the module count from the module size
        else
            moduleCount = modules.size();

        Log.i("MODULES", modules.toString());
        // put the intent data to a hashmap then we pass it to recyclerviews adapter.
        {
            intentDataMap.put("key", getIntent().getStringExtra("key"));
            intentDataMap.put("term", getIntent().getStringExtra("term"));
            intentDataMap.put("position", getIntent().getIntExtra("position", -1));
            Log.i("INTENT DATA", intentDataMap.toString());
            adapter = new ModuleListAdapter(this, modules, intentDataMap);
        }
        // initialize the recyclerview
        adapter.setOnModuleClickedListener(moduleBtnListener);
        modulesRecyclerview.setItemViewCacheSize(4);
        modulesRecyclerview.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        modulesRecyclerview.setAdapter(adapter);
        Objects.requireNonNull(modulesRecyclerview.getAdapter()).notifyDataSetChanged();
    }
}