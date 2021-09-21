package iccnote;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialog;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.tajos.iccnotes.R;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

public class Subject extends HashMap<String, Object> {

    public Subject(final String subjectName, final String subjectDay, final String time, @NonNull final Uri imagePath) {
        put("Subject Name", subjectName);
        put("Subject Day", subjectDay);
        put("Time", time);
        put("Image", imagePath.toString());
        put("Prelim", "null");
        put("Midterm", "null");
        put("Finals", "null");
    }

    public Subject() {}

    public static class InternalStorage {
        private final String mPath;

        public InternalStorage(@NonNull Context context) {
            final File deviceInternalStorage = new File(context.getFilesDir().getPath() + "/My Subjects");
            mPath = deviceInternalStorage.getPath();
        }

        // store the subject list to internal storage
        public void store(final List<Subject> subjects) {
            final String mJsonStrFromApp = new Gson().toJson(subjects);
            Log.i("storing", subjects.size() + "-subjects");
            App.writeFile(mPath, mJsonStrFromApp, true); // always overwrite the file if it already exist
        }
        // get the subject list from the device internal storage
        public ArrayList<Subject> getSubjects() {
            final ArrayList<Subject> subject = new ArrayList<>();
            final String jsonStringFromInternal = App.readFile(mPath); // lets read the stored subject list file from device internal storage.
            Log.i("json", jsonStringFromInternal);

            if (!jsonStringFromInternal.isEmpty()) {
                ArrayList<HashMap<String, Object>> mSubjectsMap = new Gson().fromJson(jsonStringFromInternal, new TypeToken<ArrayList<HashMap<String, Object>>>() {}.getType());

                for (HashMap<String, Object> map : mSubjectsMap) {
                    subject.add(App.convertHashMapToSubject(map));
                }
            }

            return subject;
        }
    }

    public static class FirebaseImageStorage extends AppCompatDialog {
        private final Context mContext;

        private TextView mPercentage;
        private final Subject mNewSubject;

        private boolean UPLOADING_MODE;

        private FirebaseDB mDatabase;
        private final FirebaseStorage mStorage = FirebaseStorage.getInstance();
        private final StorageReference mReference = mStorage.getReference("Images");

        private OnCompleteListener<Uri> mUploadSuccesListener;
        private OnProgressListener<UploadTask.TaskSnapshot> mUploadProgressListener;
        private OnFailureListener mFailureListener;

        public OnUploadingListener mListener;
        public interface OnUploadingListener {
            void onUploadSuccess(final List<Subject> subject);
        }
        public FirebaseImageStorage setOnUploadingListener(final OnUploadingListener listener) {
            mListener = listener;
            return this;
        }

        public FirebaseImageStorage(final Context context, final FirebaseDB database, final Subject subject) {
            super(context);
            mContext = context;
            mDatabase = database;
            mNewSubject = subject;
            _initListeners();
            setCancelable(false);
        }

        @Override
        protected void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            setContentView(R.layout.picturestorage_fragment);
            getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
            getWindow().setDimAmount(0.3f);

            mPercentage = findViewById(R.id.percentage);
            if (!UPLOADING_MODE) {
                assert mPercentage != null;
                mPercentage.setText(R.string.checking_connection);
            }
        }

        public void setOnCheckingNetworkConnectionMode() {
            UPLOADING_MODE = false;
        }

        public void setOnUploadingMode() {
           UPLOADING_MODE = true;
        }

        private void _initListeners() {
            // upload progress listener
            mUploadProgressListener = taskSnapshot -> {
                int progress = (int)((100.0 * taskSnapshot.getBytesTransferred()) / taskSnapshot.getTotalByteCount());
                mPercentage.setText(String.valueOf(progress).concat("%"));
            };
            // upload success listener
            mUploadSuccesListener = task -> {
                final String imgUrl = Objects.requireNonNull(task.getResult()).toString();
                mNewSubject.put("Image", imgUrl);
                // before pushing the data to database, ensure first that the passed database object isnt null to avoid null exception
                if (mDatabase == null)
                    mDatabase = new FirebaseDB(App.getDatabaseReference(), false, null);

                mDatabase.pushData(mNewSubject);
                mDatabase.setOnDataListener((ArrayList<Subject> data) -> {
                    // onDataReady...
                    mListener.onUploadSuccess(data);
                    dismiss();
                });
            };
            // failure listener
            mFailureListener = exception -> new AlertDialog.Builder(mContext)
                    .setTitle("Upload Failed!")
                    .setMessage("Reason: " + exception.getMessage())
                    .show();
        }

        public void upload(final String name, final Uri path) {
            mReference.child(name).putFile(path)
                    .addOnFailureListener(mFailureListener)
                    .addOnProgressListener(mUploadProgressListener)
                    .continueWithTask(task -> mReference.child(name).getDownloadUrl()).addOnCompleteListener(mUploadSuccesListener);

        }

        public void showProgress() {
            show();
        }
    }
}
