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
import android.view.Menu;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatCheckBox;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.MenuItemCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.redlee90.hideusbdebugging.adapter.appListRecyclerViewAdapter;
import com.redlee90.hideusbdebugging.model.Application;

import org.json.JSONArray;
import org.json.JSONException;

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

		sharedPreferences = new WorldReadablePrefs(MainActivity.this, "tickedApps");

		setContentView(R.layout.activity_main);

		recyclerView = findViewById(R.id.recyclerView);
		recyclerView.setLayoutManager(new LinearLayoutManager(this));

		new LoadAppListAsyncTask().execute();

		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && !PreferenceManager.getDefaultSharedPreferences(this).getBoolean(PREFERENCE_KEY_NEVER_SHOW_DISCLAIMER, false)) {
			View view = getLayoutInflater().inflate(R.layout.view_dialog, null, false);
			final AppCompatCheckBox box = view.findViewById(R.id.checkbox);
			AlertDialog.Builder builder = new AlertDialog.Builder(this).setTitle("Attention")
					.setMessage("On Android 9 and above, please utilize EdXposed Manager's whitelist feature to enable this module for targeted apps. \n\nBy default this module is applied to all user apps. \n\nCheck/uncheck apps on this screen on Android 9 or above has no effects.\n")
					.setView(view)
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
