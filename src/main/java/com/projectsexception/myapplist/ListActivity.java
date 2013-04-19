package com.projectsexception.myapplist;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.text.TextUtils;
import android.widget.Toast;
import com.actionbarsherlock.app.ActionBar;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.projectsexception.myapplist.fragments.*;
import com.projectsexception.myapplist.model.AppInfo;
import com.projectsexception.myapplist.util.CustomLog;
import com.projectsexception.myapplist.work.AppSaveTask;
import com.projectsexception.myapplist.xml.FileUtil;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class ListActivity extends BaseActivity implements
        AppListFragment.CallBack,
        FileListFragment.CallBack,
        AppInfoFragment.CallBack,
        FileDialogFragment.CallBack,
        AppSaveTask.Listener {

    public static final String ARG_FILE = "fileName";

    private static final int MAX_EXECUTIONS = 50;

    private List<AppInfo> mAppList;
    private String mFileStream;

    @Override
    protected void onCreate(Bundle args) {
        super.onCreate(args);
        
        setContentView(R.layout.activity_list);

        checkRateApp();
        
        String fileName = getIntent().getStringExtra(ARG_FILE);
        
        if (fileName == null) {
            if (getIntent().getData() != null) {
                fileName = getIntent().getDataString();
            }
            if (fileName != null && fileName.startsWith("content://")) {
                // We have a stream file, the user must save the file before continue
                mFileStream = fileName;
                new FileDialogFragment().show(getSupportFragmentManager(), "file_dialog");
            } else {
                // Want installed applications
                loadAppListFragment();
            }
        } else {
            // Load file
            loadFileListFragment(fileName);
        }
    }

    private void checkRateApp() {
        final SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        int numExecutions = sp.getInt(RateAppDialogFragment.NUM_EXECUTIONS, 0);
        if (numExecutions >= MAX_EXECUTIONS) {
            new RateAppDialogFragment().show(getSupportFragmentManager(), "rate_app_dialog");
        } else if (numExecutions >= 0) {
            sp.edit().putInt(RateAppDialogFragment.NUM_EXECUTIONS, numExecutions + 1).commit();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getSupportMenuInflater().inflate(R.menu.activity_list, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.menu_load_file) {
            new FileListTask(this).execute();
            return true;
        } else if (item.getItemId() == R.id.menu_settings) {
            startActivity(new Intent(this, PreferenceActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void saveAppList(List<AppInfo> appList) {
        mAppList = appList;
        new FileDialogFragment().show(getSupportFragmentManager(), "file_dialog");
    }

    @Override
    public void shareAppList(ArrayList<AppInfo> appList) {
        shareAppList(null, appList);
    }

    @Override
    public void updateAppList(String fileName, List<AppInfo> appList) {
        mAppList = appList;
        new AppSaveTask(ListActivity.this, null, mAppList).execute(fileName, Boolean.toString(true));
    }

    @Override
    public void shareAppList(String filePath, ArrayList<AppInfo> appList) {
        Intent intent = new Intent(this, ShareActivity.class);
        intent.putExtra(ShareActivity.FILE_PATH, filePath);
        intent.putParcelableArrayListExtra(ShareActivity.APP_LIST, appList);
        startActivity(intent);
    }

    @Override
    public void removeAppInfoFragment() {
        FragmentManager fm = getSupportFragmentManager();
        AppInfoFragment infoFragment = (AppInfoFragment) fm.findFragmentById(R.id.app_info);
        if (infoFragment != null) { 
            fm.beginTransaction().remove(infoFragment).commit();
        }
    }

    @Override
    public void nameAccepted(String name) {
        if (TextUtils.isEmpty(name)) {
            Toast.makeText(this, R.string.empty_name_error, Toast.LENGTH_SHORT).show();
        } else if (mAppList != null) {
            new AppSaveTask(ListActivity.this, null, mAppList).execute(name);
        } else if (mFileStream != null) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(Uri.parse(mFileStream));
                new AppSaveTask(ListActivity.this, inputStream, null).execute(name);
            } catch (FileNotFoundException e) {
                CustomLog.error("ListActivity", e);
            }
        }

        mAppList = null;
        mFileStream = null;
    }

    @Override
    public Context getContext() {
        return getApplicationContext();
    }

    @Override
    public void saveFinished(String fileName, String errorMsg, int operation) {
         if (errorMsg == null) {
             if (operation == AppSaveTask.OP_SAVE_STREAM) {
                 loadFileListFragment(fileName);
             } else if (operation == AppSaveTask.OP_SAVE_LIST) {
                 Toast.makeText(this, R.string.export_successfully_update, Toast.LENGTH_SHORT).show();
             } else {
                 Toast.makeText(this, R.string.export_successfully, Toast.LENGTH_SHORT).show();
             }
         } else {
             Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
         }
    }

    void loadAppListFragment() {
        FragmentManager fm = getSupportFragmentManager();
        getSupportActionBar().setTitle(R.string.ab_title_app_list);
        Fragment fragment = fm.findFragmentById(R.id.app_list);
        if (fragment instanceof AppListFragment) {
            ((AppListFragment) fragment).reloadApplications();
        } else {
            fm.beginTransaction().replace(R.id.app_list, new AppListFragment()).commit();
        }
    }

    void loadFileListFragment(String fileName) {
        FragmentManager fm = getSupportFragmentManager();
        getSupportActionBar().setTitle(R.string.ab_title_app_list);
        Fragment fragment = fm.findFragmentById(R.id.app_list);
        if (fragment instanceof FileListFragment) {
            ((FileListFragment) fragment).reloadFile(fileName);
        } else {
            fm.beginTransaction().replace(R.id.app_list, FileListFragment.newInstance(fileName)).commit();
        }
    }

    static class FileListTask extends AsyncTask<Void, Void, String[]> {

        private Context context;

        public FileListTask(Context context) {
            this.context = context;
        }

        @Override
        protected String[] doInBackground(Void... params) {
            return FileUtil.loadFiles();
        }

        @Override
        protected void onPostExecute(final String[] result) {
            if (result == null || result.length == 0) {
                Toast.makeText(context, R.string.main_no_files, Toast.LENGTH_SHORT).show();
            } else {
                AlertDialog.Builder builder = new AlertDialog.Builder(context);
                builder.setTitle(R.string.main_select_files);
                builder.setItems(result, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String fileName = result[which];
                        Intent intent = new Intent(context, ListActivity.class);
                        intent.putExtra(ListActivity.ARG_FILE, fileName);
                        context.startActivity(intent);
                    }
                });
                builder.create().show();
            }
        }

    }

}
