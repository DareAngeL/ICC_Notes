package com.tajos.iccnotes;

import android.Manifest;
import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.google.firebase.FirebaseApp;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import home_activity_classes.AddDayDialog;
import home_activity_classes.AddSubjectCardAnimator;
import home_activity_classes.SubjectLstAdapter;
import iccnote.App;
import iccnote.FirebaseDB;
import iccnote.InternetConnection;
import iccnote.Module;
import iccnote.SavedData;
import iccnote.Subject;

public class HomeActivity extends AppCompatActivity {

    private FirebaseDB mDatabase;

    private EditText mDay;
    private RecyclerView mSubjectLstRecycler;
    private SubjectLstAdapter mSubjectLstAdapter;
    private AddSubjectCardAnimator mAddSubjectCard;
    private FrameLayout mSceneRoot;
    private ImageView mImage;
    private View mSceneRootChild;
    private SwipeRefreshLayout refreshLayout;

    private boolean isFirstInit = true; // boolean to check if its the first time opening the app

    private List<Subject> mSubjects = new ArrayList<>();
    private Uri mImgPath;

    private final Intent mPickImage = new Intent(Intent.ACTION_GET_CONTENT);
    private Subject.InternalStorage mInternalStorageHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.home_activity);
        FirebaseApp.initializeApp(this);

        mPickImage.setType("image/*");
        _initBundles();

        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
                requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
            } else {
                _initLogic();
                // lets check the internet connection of the device if it has internet access before initialization.
                _initializeApp();
            }
            return;
        }
        _initLogic();
        _initializeApp();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 1000) {
            _initLogic();
            _setAdapter();
            _initializeApp();
        }
    }

    /*
        ========= ACTIVITY RESULT AFTER PICKING IMAGE ==========
     */
    private final ActivityResultLauncher<Intent> mLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == AppCompatActivity.RESULT_OK) {
                        final Intent data = result.getData();
                        assert data != null;
                        mImgPath = data.getData();

                        Glide.with(getApplicationContext())
                                .load(mImgPath)
                                .into(mImage);
                    }
                }
            });
    /*
            =========== INITIALIZE THE BUNDLES ==========
    */
    private boolean isDropped = true;
    private void _initBundles() {
        final ImageView mDropDown = findViewById(R.id.drop_down);
        mDay = findViewById(R.id.edittext_day);
        mSubjectLstRecycler = findViewById(R.id.subjects_list_recycler);
        mSceneRoot = findViewById(R.id.scene_root);
        refreshLayout = findViewById(R.id.refresh_layout);
        /*
        ========== LISTENERS ==========
        */
        // swipe refresh listener
        refreshLayout.setOnRefreshListener(this::_initializeApp);
        // drop down card listener
        mDropDown.setOnClickListener(view -> mAddSubjectCard.drop());
        // initialize addSubjectAnimator here
        mAddSubjectCard = new AddSubjectCardAnimator(this, mSceneRoot, R.layout.add_subj_fragment_min, R.layout.add_subj_fragment_max);
        // always listen if the add_subject card is dropped!
        mAddSubjectCard.setOnAnimationFinishedListener(() -> {
             mSceneRootChild = mSceneRoot.getChildAt(0);
             mImage = mSceneRootChild.findViewById(R.id.img);
             final ImageView dropDown = mSceneRootChild.findViewById(R.id.drop_down);

             if (isDropped) {
                 // when the add image is clicked, perform launching the gallery/storage of the user's device.
                 mImage.setOnClickListener(view -> mLauncher.launch(mPickImage));
             }
            // when checked mark is clicked.
             dropDown.setOnClickListener(view -> {
                 // always checks if the click event is coming from the maximize view of add_subject fragment.
                 if (isDropped) {
                     isDropped = false;
                     final EditText subjectName = mSceneRootChild.findViewById(R.id.subject_name);

                     if (!subjectName.getText().toString().isEmpty()) {
                         if (mImgPath != null) {
                             final AddDayDialog dlg = new AddDayDialog(this);
                            // add_day dialog listener
                             dlg.setOnClickedListener(new AddDayDialog.OnClickedListener() {
                                 @Override
                                 public void onAddButtonClicked(String day, String time) {
                                     // update the subject list if all inputs are not empty.
                                     _updateSubjectsLst(subjectName.getText().toString(), day, time, mImgPath);
                                     mAddSubjectCard.hide();
                                 }

                                 @Override
                                 public void onCancel() {
                                     mImgPath = null;
                                     mAddSubjectCard.hide();
                                 }
                             });
                             dlg.show();
                             return;
                         }
                     }
                     mAddSubjectCard.hide();
                 } else {
                     isDropped = true;
                     mAddSubjectCard.drop();
                 }
             });
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _initDatabase() {
        mDatabase = new FirebaseDB(App.getDatabaseReference(),false, null);
        mDatabase.setOnDataListener((ArrayList<Subject> data) -> {
            // onDataReady...
            mSubjects = data;
            Log.i("SUBJECTS", mSubjects.toString());
            _sortSubjects();
            Log.i("SORTED SUBJECTS", mSubjects.toString());
            _setAdapter(); // sets the adapter of subjectLst
            _storeSubjectsToInternal(); // always store the subjects to internal storage
        });
    }

    /*
    ========= LOGIC OF THE PROGRAM ===========
    */
    private void _initLogic() {
        final Calendar calendar = Calendar.getInstance();
        mDay.setText(_getCurrentDay(calendar.get(Calendar.DAY_OF_WEEK)));
        mDay.setKeyListener(null);
        App.setCurrentDay(_getCurrentDay(calendar.get(Calendar.DAY_OF_WEEK)));
    }

    /*
    * this is called every first launch of the app
    * to know whether to fetch subjects from internal storage or from firebase database
    * Not connected to internet ==> subjects will be fetch from internal storage if theres any
    * connected to internet ==> subjects will be fetch from firebase database
    */
    private void _initializeApp() {
        /*
        * @ InternetConnection object is a @Runnable which will
        * be used as a parameter for background thread @Thread.
        */
        new Thread(new InternetConnection(this, new InternetConnection.OnConnectionResponseListener() {
            @Override
            public void isConnected() {
                // if there is a saved data which wasnt pushed to the server, push it first.
                if (SavedData.isHasSavedData()) {
                    HashMap<String, Object> savedMap = new Gson().fromJson(SavedData.get(SavedData.PUSH), new TypeToken<HashMap<String, Object>>() {}.getType());
                    final List<String> savedMapKeys = App.getKeys(savedMap);
                    if (savedMapKeys != null && savedMapKeys.size() > 0) {
                        for (int i=0; i<savedMapKeys.size(); i++) {
                            _initSavedData(savedMap, savedMapKeys.get(i), SavedData.PUSH);
                        }
                    }
                    HashMap<String, Object> savedRemovalMap = new Gson().fromJson(SavedData.get(SavedData.REMOVAL), new TypeToken<HashMap<String, Object>>(){}.getType());
                    final List<String> savedRemovalKeys = App.getKeys(savedRemovalMap); // the keys per subject
                    if (savedRemovalKeys != null && savedRemovalKeys.size() > 0) {
                        for (int i=0; i<savedRemovalKeys.size(); i++) {
                            _initSavedData(savedRemovalMap, savedRemovalKeys.get(i), SavedData.REMOVAL);
                        }
                    }
                }
                _initDatabase();
                Log.i("connection", "is connected");
                _doneRefreshing();
            }

            @Override
            public void isNotConnected() {
                Toast.makeText(HomeActivity.this, "check your connection!", Toast.LENGTH_SHORT).show();
                if (mInternalStorageHandler == null)
                    mInternalStorageHandler = new Subject.InternalStorage(HomeActivity.this);

                mSubjects = mInternalStorageHandler.getSubjects();
                Log.i("SUBJECTS F INTERNAL", mSubjects.toString());
                _sortSubjects();
                Log.i("INTERNAL (SORTED)", mSubjects.toString());
                _setAdapter();
                _doneRefreshing();
            }
        })).start();
    }

    private void _doneRefreshing() {
        if (refreshLayout.isRefreshing())
            refreshLayout.setRefreshing(false);
    }

    private void _initSavedData(@NonNull HashMap<String, Object> map, String key, boolean isForRemovingData) {
        HashMap<String, Object> savedDataPerSubject = App.convertObjectToHashMap(map.get(key));
        Log.i("SAVEDPERSUBJECT", savedDataPerSubject.toString());
        final String reference = Objects.requireNonNull(savedDataPerSubject.get(getString(R.string.PREF))).toString();
        final String childKey = Objects.requireNonNull(savedDataPerSubject.get(getString(R.string.CHILD))).toString();
        final List<Module> modules = App.convertObjectToList(savedDataPerSubject.get(getString(R.string.VALUE)));
        if (!isForRemovingData) {
            final FirebaseDB updateDB = new FirebaseDB(reference, true, childKey);
            for (Module module : modules) {
                updateDB.updateData(module);
            }
            SavedData.clear(SavedData.PUSH);
            return;
        }
        // if the data will not be pushed; it means it is for removal
        final String isThereRemainingModules = Objects.requireNonNull(savedDataPerSubject.get("remainingModule")).toString();
        Log.i("MODULE SIZE", String.valueOf(modules.size()));
        for (Module module : modules) {
            final String moduleKey = App.getKey(module);
            final FirebaseDB updateDB = new FirebaseDB(reference, true, Objects.requireNonNull(module.get(moduleKey)).toString());
            updateDB.removeData();
        }
        if (isThereRemainingModules.equals("false")) {
            final String newReference = reference.substring(0, reference.lastIndexOf("/"));
            Log.i("REMAINING", newReference);
            final FirebaseDB updateDB = new FirebaseDB(newReference, false, null);
            final String term = reference.substring(reference.lastIndexOf("/")+1); // term(Prelim/Midterm/Finals) to be added again that was automatically deleted by the server bcuz of nullability.
            {
                HashMap<String, Object> item = new HashMap<>();
                item.put(term, "null");
                updateDB.updateData(item);
            }
        }
        SavedData.clear(SavedData.REMOVAL);
    }

    private void _storeSubjectsToInternal() {
        if (mInternalStorageHandler == null)
            mInternalStorageHandler = new Subject.InternalStorage(this);

        mInternalStorageHandler.store(mSubjects);
    }

    @SuppressLint("NotifyDataSetChanged")
    private void _setAdapter() {
        mSubjectLstAdapter = new SubjectLstAdapter(this, mSubjects);
        mSubjectLstRecycler.setItemViewCacheSize(4);
        mSubjectLstRecycler.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.VERTICAL, false));
        mSubjectLstRecycler.setAdapter(mSubjectLstAdapter);
        Objects.requireNonNull(mSubjectLstRecycler.getAdapter()).notifyDataSetChanged();
    }

    // always update the subject list whenever theres a new subject added
    private void _updateSubjectsLst(final String subjectName, final String subjectDay, final String time, @NonNull final Uri imagePath) {
        final String imgName = Uri.parse(imagePath.toString()).getLastPathSegment();
        final Subject.FirebaseImageStorage firebaseImageStorage = new Subject.FirebaseImageStorage(this, mDatabase, new Subject(subjectName, subjectDay, time, imagePath));

        firebaseImageStorage.setOnCheckingNetworkConnectionMode();
        // uploading listener
        firebaseImageStorage.setOnUploadingListener((List<Subject> updatedSubjects) -> {
            // on upload success
            mSubjects = updatedSubjects; // always update the content of @mSubjects.
            Log.i("SUBJECT SIZE", String.valueOf(mSubjects.size()));
            _sortSubjects(); // sort the subjects after uploading successfully
            if (mSubjectLstRecycler.getAdapter() == null)
                _setAdapter(); // then set the adapter of recyclerview thereafter.

            _storeSubjectsToInternal();
        }).showProgress();
        /*
        * check internet connection first before uploading we will
        * need an internet to add a subject for the image to upload
        * @ InternetConnection object is a @Runnable which will
        * be used as a parameter for background thread @Thread.
        */
        new Thread(new InternetConnection(this, new InternetConnection.OnConnectionResponseListener() {
            @Override
            public void isConnected() {
                firebaseImageStorage.setOnUploadingMode();
                firebaseImageStorage.upload(imgName, imagePath);
            }

            @Override
            public void isNotConnected() {
                firebaseImageStorage.dismiss();
                Toast.makeText(HomeActivity.this, "check your internet connection!", Toast.LENGTH_SHORT).show();
            }
        })).start();
    }

    /*
    * lets sort the subjectList depending of the current day
    * so we can place the subjects for the day, on top of the recyclerview's lists
    */
    @SuppressLint("NotifyDataSetChanged")
    private void _sortSubjects() {
        final String currentDay = mDay.getText().toString();
        // if first initialization..
        if (isFirstInit) {
            isFirstInit = false;
            _sort();
            mSubjectLstRecycler.setAdapter(new SubjectLstAdapter(this, mSubjects));
            Objects.requireNonNull(mSubjectLstRecycler.getAdapter()).notifyDataSetChanged();
            return;
        }
        // if the first initialization of the app is already done, execute the code block below
        // dont sort if the added subject is not for today, just notify the inserted one for performance improvement
        if (!(Objects.requireNonNull(mSubjects.get(mSubjects.size() - 1).get("Subject Day"))).toString().equals(currentDay)) {
            App.setSubjectsLst(mSubjects);
            mSubjectLstAdapter.notifyItemInserted(mSubjects.size()-1);
            return;
        }
        // sort if the added subject is for today.
        _sort();
        mSubjectLstRecycler.setAdapter(new SubjectLstAdapter(this, mSubjects));
        Objects.requireNonNull(mSubjectLstRecycler.getAdapter()).notifyDataSetChanged();
    }

    private void _sort() {
        final String currentDay = mDay.getText().toString();
        final ArrayList<Subject> subjectsForToDayLst = new ArrayList<>();
        final ArrayList<Subject> subjectsNotForTodayLst = new ArrayList<>();

        for (Subject subject : mSubjects) {
            if (Objects.requireNonNull(subject.get("Subject Day")).toString().equals(currentDay)) {
                final String subjectToAddTimeStr = Objects.requireNonNull(subject.get("Time")).toString();
                final int subjectToAddTime = Integer.parseInt(subjectToAddTimeStr.substring(0, subjectToAddTimeStr.indexOf(":")));

                if (subjectsForToDayLst.size() > 0) {
                    for (int i=0; i<subjectsForToDayLst.size(); i++) {
                        final String timeStr = Objects.requireNonNull(subjectsForToDayLst.get(i).get("Time")).toString();
                        final int time = Integer.parseInt(timeStr.substring(0, timeStr.indexOf(":")));

                        if (subjectToAddTime < time) {
                            subjectsForToDayLst.add(i, subject);
                            break;
                        }
                    }
                }
                // if the size of subjectsForTodayLst is not greater than 0, then just add the subject to that list
                // or just add the subject if its time value is bigger than all of the subjects added to ==> subjectsForTodayLst
                if (!subjectsForToDayLst.contains(subject))
                    subjectsForToDayLst.add(subject);
                continue;
            }
            // if this subject is not for today, then just add it to the ==<not for today arraylist>==
            subjectsNotForTodayLst.add(subject);
        }
        // after sorting just combine both the subjectForTodayLst and subjectNotForTodayLst
        if (subjectsNotForTodayLst.size() > 0)
            subjectsForToDayLst.addAll(subjectsNotForTodayLst);

        mSubjects = subjectsForToDayLst;
        App.setSubjectsLst(mSubjects);
    }

    @Nullable
    @org.jetbrains.annotations.Contract(pure = true)
    private String _getCurrentDay(final int dayInt) {
        switch (dayInt) {
            case 1:
                return "Sunday";
            case 2:
                return "Monday";
            case 3:
                return "Tuesday";
            case 4:
                return "Wednesday";
            case 5:
                return "Thursday";
            case 6:
                return "Friday";
            case 7:
                return "Saturday";
        }
        return null;
    }
}