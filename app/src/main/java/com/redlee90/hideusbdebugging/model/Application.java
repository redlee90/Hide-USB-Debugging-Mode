package com.redlee90.hideusbdebugging.model;

import android.content.pm.ApplicationInfo;

/**
 * Created by Ray Lee (redlee90@gmail.com) on 12/6/16.
 */

public class Application {
    private ApplicationInfo applicationInfo;
    private int flag;

    public Application (ApplicationInfo applicationInfo, int flag) {
        this.applicationInfo = applicationInfo;
        this.flag = flag;
    }

    public ApplicationInfo getApplicationInfo() {
        return applicationInfo;
    }

    public void setApplicationInfo(ApplicationInfo applicationInfo) {
        this.applicationInfo = applicationInfo;
    }

    public int getFlag() {
        return flag;
    }

    public void setFlag(int flag) {
        this.flag = flag;
    }
}
