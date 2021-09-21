package com.tajos.iccnotes;

import android.app.Application;

import iccnote.SavedData;

public class ICCNote extends Application {

    @Override
    public void onCreate() {
        new SavedData(this, SavedData.PREFERENCE).initialize();
        super.onCreate();
    }
}
