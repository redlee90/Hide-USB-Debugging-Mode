package com.redlee90.hideusbdebugging;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SearchView;
import android.view.Gravity;
import android.view.Menu;
import android.widget.LinearLayout;

import com.redlee90.hideusbdebugging.adapter.appListRecyclerViewAdapter;
import com.redlee90.hideusbdebugging.model.Application;

import org.json.JSONArray;
import org.json.JSONException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class MainActivity extends AppCompatActivity {
	private static final String PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER = "never_show_disclaimer";
	private RecyclerView recyclerView;
	private appListRecyclerViewAdapter recyclerViewAdapter;
	private SharedPreferences sharedPreferences;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		File pkgFolder;
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
			pkgFolder = getDataDir();
		} else {
			pkgFolder = new File(getApplicationInfo().dataDir);
		}

		if (pkgFolder.exists()) {
			pkgFolder.setExecutable(true, false);
			pkgFolder.setReadable(true, false);
		}

		sharedPreferences = new WorldReadablePrefs(MainActivity.this, "tickedApps");

		setContentView(R.layout.activity_main);

		recyclerView = (RecyclerView) findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		new LoadAppListAsyncTask().execute();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER, false)) {
			LinearLayout linearLayout = new LinearLayout(this);
			linearLayout.setGravity(Gravity.CENTER_HORIZONTAL);

			final AppCompatCheckBox box = new AppCompatCheckBox(this);
			box.setText("Never show this dialog again");

			linearLayout.addView(box);

			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Disclaimer")
					.setMessage("For Android P, most likely you are using EdXposed. Please refer to EdXposed installer's whitelist/blacklist feature to enabl" +
							"e/disable this module for individual packages. By default this module is enabled for all packages. Check/uncheck box in this app has no effect")
					.setView(linearLayout)
					.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							if (box.isChecked()) {
								PreferenceManager.getDefaultSharedPreferences(MainActivity.this).edit().putBoolean(PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER, true).apply();
							}
						}
					});
			AlertDialog alertDialog = builder.create();
			alertDialog.show();
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		getMenuInflater().inflate(R.menu.menu_main, menu);

		SearchView searchView = (SearchView) MenuItemCompat.getActionView(menu.findItem(R.id.action_search));
		searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
			@Override
			public boolean onQueryTextSubmit(String query) {
				return false;
			}

			@Override
			public boolean onQueryTextChange(String newText) {
				recyclerViewAdapter.getFilter().filter(newText);
				return true;
			}
		});

		return super.onCreateOptionsMenu(menu);
	}

	private class LoadAppListAsyncTask extends AsyncTask<Void, Void, Void> {
		private ProgressDialog progressDialog;
		private List<Application> appList;
		private PackageManager packageManager;
		private List<String> tickedPackages;

		@Override
		protected void onPreExecute() {
			progressDialog = ProgressDialog.show(MainActivity.this, null, "Loading Packages ... ");
			appList = new ArrayList<>();
			packageManager = MainActivity.this.getPackageManager();
			tickedPackages = new ArrayList<>();
		}

		@Override
		protected Void doInBackground(Void... params) {
			try {
				JSONArray jsonArray = new JSONArray(sharedPreferences.getString("tickedApps", "[]"));
				for (int i = 0; i < jsonArray.length(); ++i) {
					tickedPackages.add(jsonArray.getString(i));
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}


			for (ApplicationInfo info : packageManager.getInstalledApplications(PackageManager.GET_META_DATA)) {
				if (tickedPackages.contains(info.packageName)) {
					appList.add(new Application(info, 0));
				} else {
					appList.add(new Application(info, 1));
				}
			}

			Collections.sort(appList, new Comparator<Application>() {
				@Override
				public int compare(Application lhs, Application rhs) {
					return packageManager.getApplicationLabel(lhs.getApplicationInfo()).toString().compareToIgnoreCase(packageManager.getApplicationLabel(rhs.getApplicationInfo()).toString());
				}
			});
			return null;
		}

		@Override
		protected void onPostExecute(Void aVoid) {
			recyclerViewAdapter = new appListRecyclerViewAdapter(MainActivity.this, appList, tickedPackages, sharedPreferences);
			recyclerView.setAdapter(recyclerViewAdapter);

			progressDialog.dismiss();
		}
	}
}
