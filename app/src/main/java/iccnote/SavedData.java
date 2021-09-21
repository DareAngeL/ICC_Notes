package iccnote;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

import com.google.gson.Gson;

import java.util.HashMap;

public class SavedData {

    private static SharedPreferences savedData;
    private static boolean hasSavedData = false;

    public static final boolean PUSH = false;
    public static final boolean REMOVAL = true;
    public static final String PREFERENCE = "SavedData";
    private static final String KEY = "saved_data";
    private static final String REMOVAL_KEY = "for_removal_data";
    private static final HashMap<String, Object> savedDataMap = new HashMap<>();
    private static final HashMap<String, Object> forRemovalDataMap = new HashMap<>();

    public SavedData(@NonNull final Context context, final String preferences) {
        savedData = context.getSharedPreferences(preferences, AppCompatActivity.MODE_PRIVATE);
    }

    public static String get(boolean forRemovingData) {
        return savedData.getString(!forRemovingData?KEY: REMOVAL_KEY, "");
    }

    public static void set(String key, Object value, boolean forRemovingData) {
        if (!forRemovingData)
            savedDataMap.put(key, value);
        else
            forRemovalDataMap.put(key, value);

        savedData.edit().putString(!forRemovingData?KEY: REMOVAL_KEY,
                new Gson().toJson(!forRemovingData?savedDataMap:forRemovalDataMap)).apply();
    }

    public static void clear(final boolean isForRemoval) {
        savedData.edit().putString(!isForRemoval?KEY: REMOVAL_KEY, "").apply();
    }

    public static boolean isHasSavedData() {
        return hasSavedData;
    }

    public void initialize() {
        // this will check if the saved data for pushing is not empty
        String savedDataStr = get(false);
        if (!savedDataStr.isEmpty()) {
            hasSavedData = true;
            return;
        }
        // this will check if the saved data for removal is not empty
        savedDataStr = get(true);
        if (!savedDataStr.isEmpty()) {
            hasSavedData = true;
            return;
        }
        hasSavedData = false;
    }
}
