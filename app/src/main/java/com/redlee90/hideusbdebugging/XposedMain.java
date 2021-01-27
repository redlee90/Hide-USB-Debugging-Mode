package com.redlee90.hideusbdebugging;

import android.content.ContentResolver;
import android.os.Build;
import android.provider.Settings;

import org.json.JSONArray;

import java.util.HashSet;
import java.util.Set;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * Created by Rui Li on 12/5/2016.
 */
public class XposedMain implements IXposedHookLoadPackage {
	@Override
	public void handleLoadPackage(XC_LoadPackage.LoadPackageParam loadPackageParam) throws Throwable {
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
			XSharedPreferences preferences = new XSharedPreferences(XposedMain.class.getPackage().getName(), "tickedApps");
			preferences.reload();

			Set<String> packageNames = new HashSet<>();
			JSONArray jsonArray = new JSONArray(preferences.getString("tickedApps", "[]"));
			for (int i = 0; i < jsonArray.length(); ++i) {
				packageNames.add(jsonArray.getString(i));
			}
			if (!packageNames.contains(loadPackageParam.packageName)) {
				return;
			}
		}

		// Skip the Settings app
		if (loadPackageParam.packageName.equals("com.android.settings")) {
			return;
		}

		XposedBridge.log("hideUSBDebugging: hook " + loadPackageParam.packageName);

		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt(3) method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});

		XposedHelpers.findAndHookMethod("android.provider.Settings.Global", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Global.getInt(2) method");
				if (param.args[1].equals(Settings.Global.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});

		XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, int.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Secure.getInt(3) method");
				if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});

		XposedHelpers.findAndHookMethod("android.provider.Settings.Secure", loadPackageParam.classLoader, "getInt", ContentResolver.class, String.class, new XC_MethodHook() {
			@Override
			protected void afterHookedMethod(MethodHookParam param) throws Throwable {
				XposedBridge.log("hideUSBDebugging: hook Settings.Secure.getInt(2) method");
				if (param.args[1].equals(Settings.Secure.ADB_ENABLED)) {
					param.setResult(0);
				}
			}
		});
	}
}
