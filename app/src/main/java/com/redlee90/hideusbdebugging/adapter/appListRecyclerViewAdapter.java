package com.redlee90.hideusbdebugging.adapter;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.provider.Settings;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.Filter;
import android.widget.Filterable;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.redlee90.hideusbdebugging.R;
import com.redlee90.hideusbdebugging.WorldReadablePrefs;
import com.redlee90.hideusbdebugging.model.Application;

import org.json.JSONArray;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Ray Lee (redlee90@gmail.com) on 12/6/16.
 */

public class appListRecyclerViewAdapter extends RecyclerView.Adapter<appListRecyclerViewAdapter.RecyclerViewViewHolder> implements Filterable {
	private List<Application> appList;
	private List<Application> appListFiltered;
	private List<String> tickedApps;
	private PackageManager packageManager;
	private Context context;
	private AppListFilter appListFilter;
	private String filterString;
	private SharedPreferences sharedPreferences;


	public appListRecyclerViewAdapter(Context context, List<Application> appListFiltered, List<String> tickedApps, SharedPreferences sharedPreferences) {
		this.context = context;
		this.packageManager = context.getPackageManager();
		this.tickedApps = tickedApps;
		this.appListFiltered = appListFiltered;
		appList = new ArrayList<>(this.appListFiltered);
		this.sharedPreferences = sharedPreferences;
	}

	@Override
	public RecyclerViewViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
		return new RecyclerViewViewHolder(LayoutInflater.from(parent.getContext()).inflate(R.layout.app_list_row, parent, false));
	}

	@Override
	public void onBindViewHolder(final RecyclerViewViewHolder holder, int position) {
		final Application application = appListFiltered.get(position);

		String appName = application.getApplicationInfo().loadLabel(packageManager).toString();
		holder.tvAppName.setText(appName);

		try {
			holder.tvVersion.setText("v" + packageManager.getPackageInfo(application.getApplicationInfo().packageName, 0).versionName);
		} catch (PackageManager.NameNotFoundException e) {
			e.printStackTrace();
		}

		final String appPackageName = application.getApplicationInfo().packageName;

		if (filterString != null && filterString.length() > 0) {
			Pattern p = Pattern.compile(filterString, Pattern.CASE_INSENSITIVE);

			Editable e = new SpannableStringBuilder(appPackageName);
			Matcher m = p.matcher("");

			m.reset(e);
			while (m.find()) {
				e.setSpan(new ForegroundColorSpan(Color.rgb(0, 150, 136)), m.start(), m.end(), 0);
			}
			holder.tvPackageName.setText(e);

			Editable e1 = new SpannableStringBuilder(packageManager.getApplicationLabel(application.getApplicationInfo()).toString());
			m.reset(e1);
			while (m.find()) {
				e1.setSpan(new ForegroundColorSpan(Color.rgb(0, 150, 136)), m.start(), m.end(), 0);
			}
			holder.tvAppName.setText(e1);

		} else {
			holder.tvPackageName.setText(appPackageName);
			holder.tvAppName.setText(packageManager.getApplicationLabel(application.getApplicationInfo()));
		}

		holder.ivIcon.setImageDrawable(application.getApplicationInfo().loadIcon(packageManager));

		holder.cbTicked.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				if (isChecked) {
					application.setFlag(0);
					if (!tickedApps.contains(appPackageName)) {
						tickedApps.add(appPackageName);
						Toast.makeText(context, "Please force stop selected app and relaunch it", Toast.LENGTH_SHORT).show();
						Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
						intent.setData(Uri.parse("package:" + appPackageName));
						try {
							context.startActivity(intent);
						} catch (ActivityNotFoundException e) {
							e.printStackTrace();
						}
					}
				} else {
					application.setFlag(1);
					if (tickedApps.contains(appPackageName)) {
						tickedApps.remove(appPackageName);
					}
				}

				sharedPreferences.edit().putString("tickedApps", new JSONArray(tickedApps).toString()).commit();
				((WorldReadablePrefs) sharedPreferences).fixPermissions(true);

                /*Collections.sort(appListFiltered, new Comparator<Application>() {
                    @Override
                    public int compare(Application lhs, Application rhs) {
                        if (lhs.getFlag() != rhs.getFlag()) {
                            return lhs.getFlag() - rhs.getFlag();
                        } else {
                            return packageManager.getApplicationLabel(lhs.getApplicationInfo()).toString().compareToIgnoreCase(packageManager.getApplicationLabel(rhs.getApplicationInfo()).toString());
                        }
                    }
                });*/
			}
		});

		if (tickedApps.contains(appPackageName)) {
			holder.cbTicked.setChecked(true);
		} else {
			holder.cbTicked.setChecked(false);
		}

	}

	@Override
	public int getItemCount() {
		return appListFiltered.size();
	}

	@Override
	public Filter getFilter() {
		if (appListFilter == null) {
			appListFilter = new AppListFilter();
		}
		return appListFilter;
	}

	class RecyclerViewViewHolder extends RecyclerView.ViewHolder {
		TextView tvAppName;
		TextView tvVersion;
		TextView tvPackageName;
		ImageView ivIcon;
		CheckBox cbTicked;

		RecyclerViewViewHolder(View itemView) {
			super(itemView);
			tvAppName = (TextView) itemView.findViewById(R.id.app_name);
			tvVersion = (TextView) itemView.findViewById(R.id.app_version);
			tvPackageName = (TextView) itemView.findViewById(R.id.app_package);
			ivIcon = (ImageView) itemView.findViewById(R.id.app_icon);
			cbTicked = (CheckBox) itemView.findViewById(R.id.checkbox);
		}
	}

	private class AppListFilter extends Filter {

		@Override
		protected FilterResults performFiltering(CharSequence charSequence) {
			FilterResults results = new FilterResults();

			filterString = charSequence.toString().trim();

			appListFiltered.clear();

			if (filterString.length() <= 0) {
				appListFiltered.addAll(appList);
			} else {
				for (Application a : appList) {
					if (a.getApplicationInfo().packageName.contains(filterString.toLowerCase()) || packageManager.getApplicationLabel(a.getApplicationInfo()).toString().toLowerCase().contains(filterString.toLowerCase())) {
						appListFiltered.add(a);
					}
				}
				results.values = appListFiltered;
				results.count = appListFiltered.size();
			}
			return results;
		}

		@Override
		protected void publishResults(CharSequence charSequence, FilterResults filterResults) {
			notifyDataSetChanged();
		}
	}
}
