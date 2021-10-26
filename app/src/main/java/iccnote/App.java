package iccnote;

import android.animation.Animator;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.res.ColorStateList;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.TypedValue;
import android.view.View;
import android.view.animation.LinearInterpolator;
import android.view.inputmethod.InputMethodManager;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.res.ResourcesCompat;
import androidx.core.view.ViewCompat;

import com.google.gson.Gson;
import com.google.gson.internal.LinkedTreeMap;
import com.google.gson.reflect.TypeToken;
import com.tajos.iccnotes.R;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class App {
    private static List<Subject> mSubjects;
    @Nullable
    private static String mCurrentDay;
    private static final String mDatabaseReference = "Subjects";

    private static List<HashMap<String, Object>> mContents = new ArrayList<>();

    public static void setContents(final String jsonContents) {
        mContents = new Gson().fromJson(jsonContents, new TypeToken<List<HashMap<String, Object>>>() {}.getType());
    }
    public static List<HashMap<String, Object>> getContents() {
        return mContents;
    }
    public static void clearContents() {
        mContents.clear();
    }

    @Nullable
    public static Bitmap getBitmapFromUri(@NonNull Context ac, Uri uri) {
        try {
            return MediaStore.Images.Media.getBitmap(ac.getContentResolver(), uri);
        } catch (IOException e) {
            new AlertDialog.Builder(ac).setTitle("ERROR")
                    .setMessage("Can't access image, please choose another file manager or select your phone gallery")
                    .show();
        }
        return null;
    }

    public static String getDatabaseReference() {
        return mDatabaseReference;
    }

    public static void setCurrentDay(@Nullable final String currentDay) {
        mCurrentDay = currentDay;
    }

    @Nullable
    public static String getCurrentDay() {
        return mCurrentDay;
    }

    public static void setSubjectsLst(final List<Subject> subjects) {
        mSubjects = subjects;
    }

    public static List<Subject> getSubjects() {
        return mSubjects;
    }

    public static int getScreenWidth(@NonNull AppCompatActivity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.widthPixels;
    }

    public static int getScreenHeight(@NonNull AppCompatActivity activity) {
        final DisplayMetrics metrics = new DisplayMetrics();
        activity.getWindowManager().getDefaultDisplay().getMetrics(metrics);

        return metrics.heightPixels;
    }

    @NonNull
    public static Subject convertHashMapToSubject(@NonNull final HashMap<String, Object> map) {
        final Subject subject = new Subject();
        subject.put("Subject Name", map.get("Subject Name"));
        subject.put("Subject Day", map.get("Subject Day"));
        subject.put("Time", map.get("Time"));
        subject.put("Image", map.get("Image"));
        subject.put("Prelim", map.get("Prelim"));
        subject.put("Midterm", map.get("Midterm"));
        subject.put("Finals", map.get("Finals"));
        if (map.containsKey("key")) // lets check always to make sure that the map contains this key before adding it
            subject.put("key", map.get("key"));

        return subject;
    }

    public static void animateErrorEffect(@NonNull Context context, View view) {
        final int errorColor = ResourcesCompat.getColor(context.getResources(), R.color.red, null);
        ViewCompat.setBackgroundTintList(view, ColorStateList.valueOf(errorColor));
        final ObjectAnimator errorAnimator = ObjectAnimator.ofFloat(view, "translationX", -5f, 5f);
        errorAnimator.setDuration(80);
        errorAnimator.setInterpolator(new LinearInterpolator());
        errorAnimator.setRepeatMode(ValueAnimator.REVERSE);
        errorAnimator.setRepeatCount(5);
        errorAnimator.addListener(new Animator.AnimatorListener() {
            @Override
            public void onAnimationStart(Animator animator) {
            }

            @Override
            public void onAnimationEnd(Animator animator) {
                // after animating always reset day's translationX to its proper value ==> 0
                view.setTranslationX(0);
            }

            @Override
            public void onAnimationCancel(Animator animator) {
            }

            @Override
            public void onAnimationRepeat(Animator animator) {
            }
        });
        errorAnimator.start();
    }

    @NonNull
    public static String cutString(final CharSequence charSeq, final int start, final int end) {
        final StringBuilder strBuilder = new StringBuilder();
        for (int i=start; i<end; i++)
            strBuilder.append(charSeq.charAt(i));

        return strBuilder.toString();
    }

    public static float convertDptoPx(float dp) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, dp, Resources.getSystem().getDisplayMetrics());
    }

    public static void hideKeyboard(@NonNull Context context, @NonNull View view) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }

    public static void showKeyboard(@NonNull Context context) {
        InputMethodManager imm = (InputMethodManager) context.getSystemService(Context.INPUT_METHOD_SERVICE);
        imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);
    }

    @Nullable
    public static String getKey(Map<String, Object> map) {
        if (map == null || map.size() <= 0)
            return null;

        String key = "";
        for (Map.Entry<String, Object> entry : map.entrySet())
            key = entry.getKey();

        return key;
    }

    @Nullable
    public static List<String> getKeys(Map<String, Object> map) {
        if (map == null || map.size() <= 0)
            return null;

        List<String> keys = new ArrayList<>();
        for (Map.Entry<String, Object> entry : map.entrySet())
            keys.add(entry.getKey());

        return keys;
    }

    @NonNull
    public static List<Module> getModulesFromSubject(final String key, @NonNull final Subject mapSubject) {
        if (Objects.requireNonNull(mapSubject.get(key)).toString().equals("null"))
            return new ArrayList<>();

        final String jsonString = new Gson().toJson(mapSubject.get(key));
        final LinkedTreeMap<String, Object> modulesLnkdMap =
                new Gson().fromJson(jsonString, new TypeToken<Object>(){}.getType()) instanceof ArrayList ?
                        convertArrayToLnkedTree(new Gson().fromJson(jsonString, new TypeToken<Object>(){}.getType())) : new Gson().fromJson(jsonString, new TypeToken<Object>(){}.getType());

        final List<String> keys = getKeys(modulesLnkdMap); // keys of the linkedMap
        List<Module> modulesList = new ArrayList<>(); // the list array to be stored and return.

        for (int i = 0; i< modulesLnkdMap.size(); i++) {
            assert keys != null;
            final Module module = new Module(keys.get(i), modulesLnkdMap.get(keys.get(i)));
            modulesList.add(module);
        }
        // perform sorting
        modulesList = sortModules(modulesList);
        return modulesList;
    }

    @NonNull
    public static List<Module> sortModules(@NonNull final List<Module> unsortedModule) {
        final List<Module> sortedModules = new ArrayList<>();
        Module allModulesKeyMap = null;

        for (Module module : unsortedModule) {
            String unsortedModuleKey = getKey(module);
            assert unsortedModuleKey != null;
            int numFromUnsorted = convertCharToInt(unsortedModuleKey);
            if (numFromUnsorted == -1) { // if its value is negative it means its an all modules section
                allModulesKeyMap = module;
                continue;
            }

            if (sortedModules.size() > 0) {
                int pos = 0; // position of the sorted modules
                for (Module sortedModule : sortedModules) {
                    String sortedModuleKey = getKey(sortedModule);
                    assert sortedModuleKey != null;
                    int numFromSorted = convertCharToInt(sortedModuleKey);
                    if (numFromUnsorted < numFromSorted) {
                        sortedModules.add(pos, module);
                        break;
                    }
                    pos++;
                }
                // this means the numFromUnsorted is bigger than all of the children from sortedModule
                // so we should add it to the last part of the list
                if (pos == sortedModules.size())
                    sortedModules.add(module);

                continue;
            }
            // the code block below will execute if the sortedModulesList has no children.
            sortedModules.add(module);
        }
        // always checks if this list map is not null
        if (allModulesKeyMap != null)
            sortedModules.add(allModulesKeyMap);

        return sortedModules;
    }

    private static int convertCharToInt(@NonNull String str) {
        final String nums = "1234567890";
        String firstChar = str.substring(0,1);
        if (nums.contains(firstChar))
            return Integer.parseInt(firstChar);

        return -1;
    }

    @NonNull
    public static List<Module> convertObjectToListModule(@Nullable final Object obj) {
        final String jsonObject = new Gson().toJson(obj);
        LinkedTreeMap<String, Object> lnkTreeMap = new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType()) instanceof ArrayList ?
                convertArrayToLnkedTree(new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType())) : new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType());

        if (lnkTreeMap == null)
            return new ArrayList<>();

        final List<Module> modules = new ArrayList<>();
        final List<String> keys = getKeys(lnkTreeMap);
        for (int i=0; i<lnkTreeMap.size(); i++) {
            assert keys != null;
            modules.add(new Module(keys.get(i), lnkTreeMap.get(keys.get(i))));
        }
        return modules;
    }

    @NonNull
    public static HashMap<String, Object> convertObjectToHashMap(@Nullable final Object obj) {
        final String jsonObject = new Gson().toJson(obj);
        LinkedTreeMap<String, Object> lnkTreeMap = new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType()) instanceof ArrayList ?
                convertArrayToLnkedTree(new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType())) : new Gson().fromJson(jsonObject, new TypeToken<Object>(){}.getType());

        final HashMap<String, Object> map = new HashMap<>();
        final List<String> keys = getKeys(lnkTreeMap);
        assert keys != null;
        for (String key : keys) {
            map.put(key, lnkTreeMap.get(key));
        }
        return map;
    }

    @NonNull
    public static LinkedTreeMap<String, Object> convertArrayToLnkedTree(@NonNull final List<Module> array) {
        LinkedTreeMap<String, Object> map = new LinkedTreeMap<>();
        for (Map<String, Object> mod : array) {
            String key = getKey(mod);
            assert key != null;
            map.put(key, mod.get(key));
        }
        return map;
    }

    private static boolean createNewFile(@NonNull String path) {
        boolean isDirMade = false;
        int lastSep = path.lastIndexOf(File.separator);
        if (lastSep > 0) {
            String dirPath = path.substring(0, lastSep);
            isDirMade = makeDir(dirPath);
        }
        if (isDirMade) {
            File file = new File(path);

            try {
                if (! file.exists()) {
                    return file.createNewFile();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return false;
    }

    public static boolean makeDir(String path) {
        if (!isExistFile(path)) {
            File file = new File(path);
            return file.mkdirs();
        }
        return false;
    }

    public static boolean isExistFile(String path) {
        File file = new File(path);
        return file.exists();
    }

    public static void writeFile(String path, String str, final boolean isForceOverwrite) {
        // if createNewFile method returns false -> it means the file is already exist, dont write anything on it
        // but then, if isForceOverwrite is set to true -> we will overwrite the text on it.
        if (createNewFile(path) || isForceOverwrite) {
            FileWriter fileWriter = null;

            try {
                fileWriter = new FileWriter(path, false);
                fileWriter.write(str);
                fileWriter.flush();
            } catch (IOException e) {
                e.printStackTrace();
            } finally {
                try {
                    if (fileWriter != null)
                        fileWriter.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @NonNull
    public static String readFile(String path) {
        createNewFile(path);

        StringBuilder sb = new StringBuilder();
        FileReader fr = null;
        try {
            fr = new FileReader(path);

            char[] buff = new char[1024];
            int length;

            while ((length = fr.read(buff)) > 0) {
                sb.append(new String(buff, 0, length));
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (fr != null) {
                try {
                    fr.close();
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }

        return sb.toString();
    }
}
