package com.redlee90.hideusbdebugging;

import android.content.ContentResolver;
import android.provider.Settings;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.IXposedHookZygoteInit;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Rui Li on 12/5/2016.
 */
public class Tutorial implements IXposedHookZygoteInit, IXposedHookLoadPackage {
	private Set<String> packageNames = new HashSet<>();
	private XSharedPreferences preferences;

	@Override
	public void initZygote(StartupParam startupParam) throws Throwable {
		preferences = new XSharedPreferences(Tutorial.class.getPackage().getName(), "tickedApps");
	}

	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
		preferences.reload();
		packageNames.clear();

		JSONArray jsonArray = new JSONArray(preferences.getString("tickedApps", "[]"));
		for (int i = 0; i < jsonArray.length(); ++i) {
			packageNames.add(jsonArray.getString(i));
		}

		if (!packageNames.contains(loadPackageParam.packageName)) {
			return;
		}

		XposedBridge.log("hideUSBDebugging: hook " + loadPackageParam.packageName);

		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});

		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
	}


}
