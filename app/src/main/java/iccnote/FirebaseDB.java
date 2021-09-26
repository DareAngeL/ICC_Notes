package iccnote;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.GenericTypeIndicator;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.HashMap;

public class FirebaseDB {

    private final DatabaseReference mReference;

    private OnDataListener mListener;
    public interface OnDataListener {
        void onDataReady(final ArrayList<Subject> data);
    }

    public void setOnDataListener(final OnDataListener listener) {
        mListener = listener;
        _getAllData();
    }

    public FirebaseDB(@NonNull final String reference, final boolean isChild, @Nullable final String childKey) {
        final FirebaseDatabase mDatabase = FirebaseDatabase.getInstance();
        if (isChild && childKey != null) {
            mReference = mDatabase.getReference(reference).child(childKey);
            return;
        }
        mReference = mDatabase.getReference(reference);
    }

    public void pushData(HashMap<String, Object> map) {
        mReference.push().updateChildren(map);
    }

    public void updateData(HashMap<String, Object> map) {
        mReference.updateChildren(map);
    }

    public void removeData() {
        mReference.removeValue();
    }

    private void _getAllData() {
        mReference.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                final ArrayList<Subject> subjectsArray = new ArrayList<>();
                try {
                    final GenericTypeIndicator<HashMap<String, Object>> ind = new GenericTypeIndicator<HashMap<String, Object>>() {};
                    for (final DataSnapshot item : snapshot.getChildren()) {
                        final String key = item.getKey();
                        final HashMap<String, Object> subjectMap = item.getValue(ind);
                        assert subjectMap != null;
                        final Subject subject = App.convertHashMapToSubject(subjectMap);
                        subject.put("key", key);
                        subjectsArray.add(subject);
                    }
                    mListener.onDataReady(subjectsArray);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }
}
