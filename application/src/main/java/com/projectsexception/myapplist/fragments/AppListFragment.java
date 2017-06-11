package com.projectsexception.myapplist.fragments;

import android.app.Activity;
import android.os.Bundle;
import android.support.v4.content.Loader;
import android.support.v7.app.ActionBarActivity;
import android.view.MenuItem;

import com.projectsexception.myapplist.R;
import com.projectsexception.myapplist.model.AppInfo;
import com.projectsexception.myapplist.work.AppListLoader;

import java.util.ArrayList;
import java.util.List;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class AppListFragment extends AbstractAppListFragment {

    public static interface CallBack {
        void loadFile();
        void settings();
        void saveAppList(List<AppInfo> appList);
        void shareAppList(ArrayList<AppInfo> appList, boolean copyToClipboard);
        void showAppInfo(String name, String packageName, String comment);
    }

    private CallBack mCallBack;

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        if (activity instanceof CallBack) {
            mCallBack = (CallBack) activity;
        } else {
            throw new IllegalStateException("Activity must implement fragment's callback");
        }
    }

    @Override
    public void onDetach() {
        mCallBack = null;
        super.onDetach();
    }

    @Override
    int getMenuAdapter() {
        return R.menu.adapter_app;
    }

    @Override
    int getMenuResource() {
        return R.menu.fragment_app;
    }

    @Override
    Loader<ArrayList<AppInfo>> createLoader(int id, Bundle args) {
        return new AppListLoader(getActivity());
    }

    @Override
    void showAppInfo(String name, String packageName, String comment) {
        if (mCallBack != null) {
            mCallBack.showAppInfo(name, packageName, comment);
        }
    }

    @Override
    int getTitle() {
        return R.string.app_name;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        
        setHasOptionsMenu(true);

        // Prepare the loader.  Either re-connect with an existing one,
        // or start a new one.
        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (mCallBack != null) {
            if (item.getItemId() == R.id.menu_load_file) {
                mCallBack.loadFile();
                return true;
            } else if (item.getItemId() == R.id.menu_settings) {
                mCallBack.settings();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void actionItemClicked(int id) {
        if (mCallBack != null && getAdapter() != null) {
            ArrayList<AppInfo> appList = getAdapter().getSelectedItems();
            if (appList == null || appList.isEmpty()) {
                Crouton.makeText(getActivity(), R.string.empty_list_error, Style.ALERT).show();
            } else if (id == R.id.menu_save) {
                mCallBack.saveAppList(appList);
            } else if (id == R.id.menu_share) {
                mCallBack.shareAppList(appList, false);
            } else if (id == R.id.menu_copy) {
                mCallBack.shareAppList(appList, true);
            }
        }
    }

    public void reloadApplications() {
        getLoaderManager().initLoader(0, null, this);
    }
}
