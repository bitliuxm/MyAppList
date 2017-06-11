package com.projectsexception.myapplist.fragments;

import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import com.projectsexception.myapplist.R;
import com.projectsexception.myapplist.util.AppUtil;
import com.projectsexception.myapplist.util.ApplicationsReceiver;
import com.projectsexception.myapplist.view.ThemeManager;
import com.projectsexception.myapplist.view.TypefaceProvider;
import com.projectsexception.util.AndroidUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

public class AppInfoFragment extends Fragment implements View.OnClickListener {
    
    public static interface CallBack {
        void removeAppInfoFragment();
        void updateAppInfo(String mName, String mPackage, String mCommentString);
    }
    
    static final String KEY_LISTENER = "AppInfoFragment";
    static final String NAME_ARG = "nameArg";
    static final String PERMISSION_PREFIX = "android.permission.";

    static final String PACKAGE_ARG = "packageArg";
    static final String COMMENT_ARG = "commentArg";

    public static AppInfoFragment newInstance(String name, String packageName, String comment) {
        AppInfoFragment frg = new AppInfoFragment();
        Bundle args = new Bundle();
        args.putString(NAME_ARG, name);
        args.putString(PACKAGE_ARG, packageName);
        args.putString(COMMENT_ARG, comment);
        frg.setArguments(args);
        return frg;
    }
    
    private CallBack mCallBack;
    private String mName;
    private String mPackage;
    private String mCommentString;
    ImageView mIcon;
    TextView mTitle;
    TextView mPackageName;
    TextView mStatus;
    TextView mComment;
    View mInfo;
    View mPlay;
    TextView mVersion;
    View mApplicationData;
    TextView mPlayLinked;
    TextView mStopApplication;
    TextView mUninstallApplication;
    TextView mStartApplication;
    TextView mApplicationDate;
    View mApplicationDateSeparator;
    TextView mApplicationPermissions;

    public String getShownPackage() {
        return mPackage;
    }
    
    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        try {
            mCallBack = (CallBack) activity;
        } catch (ClassCastException e) {
            throw new IllegalArgumentException("Activity must implement AppInfoFragment.CallBack");
        }
    }
    
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        ApplicationsReceiver.getInstance(getActivity()).registerListener(KEY_LISTENER);
        mName = getArguments().getString(NAME_ARG);
        mPackage = getArguments().getString(PACKAGE_ARG);
        mCommentString = getArguments().getString(COMMENT_ARG);
        if (mPackage != null) {
            PackageManager pManager = getActivity().getPackageManager();
            final PackageInfo packageInfo = AppUtil.loadPackageInfo(pManager, mPackage);
            final boolean isFromGPlay = packageInfo != null && AppUtil.isFromGooglePlay(pManager, mPackage);
            populateView(pManager, packageInfo, isFromGPlay);
        } else {
            getView().setVisibility(View.GONE);
        }
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.app_info, container, false);
        mIcon = (ImageView) view.findViewById(R.id.icon);
        mTitle = (TextView) view.findViewById(R.id.title);
        mPackageName = (TextView) view.findViewById(R.id.package_name);
        mStatus = (TextView) view.findViewById(R.id.status);
        mComment = (TextView) view.findViewById(R.id.comment);
        mInfo = view.findViewById(R.id.info);
        mPlay = view.findViewById(R.id.play);
        mVersion = (TextView) view.findViewById(R.id.version);
        mApplicationData = view.findViewById(R.id.app_data);
        mPlayLinked = (TextView) view.findViewById(R.id.play_linked);
        mStopApplication = (TextView) view.findViewById(R.id.stop_application);
        mUninstallApplication = (TextView) view.findViewById(R.id.uninstall_application);
        mStartApplication = (TextView) view.findViewById(R.id.start_application);
        mApplicationDate = (TextView) view.findViewById(R.id.app_date);
        mApplicationDateSeparator = view.findViewById(R.id.app_date_sep);
        mApplicationPermissions = (TextView) view.findViewById(R.id.permissions);

        mIcon.setOnClickListener(this);
        mComment.setOnClickListener(this);
        mInfo.setOnClickListener(this);
        mPlay.setOnClickListener(this);
        return view;
    }
    
    @Override
    public void onResume() {
        super.onResume();
        final FragmentActivity activity = getActivity();
        final ApplicationsReceiver receiver = ApplicationsReceiver.getInstance(activity);
        if (receiver.isContextChanged(KEY_LISTENER)) {
            mName = null;
            mPackage = null;
            mCommentString= null;
            receiver.removeListener(KEY_LISTENER);
            mCallBack.removeAppInfoFragment();
        }
        checkStopButton();
    }

    @TargetApi(9)
    private void populateView(PackageManager pManager, PackageInfo packageInfo, boolean isFromGPlay) {
        if (packageInfo == null) {
            // Not installed
            mIcon.setImageResource(R.drawable.ic_default_launcher);
            mTitle.setText(mName);
            mPackageName.setText(mPackage);
            mComment.setText(mCommentString);
            mInfo.setEnabled(false);
            mVersion.setVisibility(View.INVISIBLE);
            mStatus.setText(R.string.app_info_not_installed);
            mApplicationData.setVisibility(View.GONE);
            mPlayLinked.setVisibility(View.GONE);
            mStopApplication.setEnabled(false);
            if (AndroidUtils.isICSOrHigher()) {
                mUninstallApplication.setEnabled(false);
            } else {
                mUninstallApplication.setVisibility(View.GONE);
            }
            mStartApplication.setEnabled(false);
        } else {
            final ApplicationInfo applicationInfo = packageInfo.applicationInfo;
            final String packageName = packageInfo.packageName;
            final Intent launchIntent = pManager.getLaunchIntentForPackage(packageName);
            
            mIcon.setImageDrawable(applicationInfo.loadIcon(pManager));
            mTitle.setText(applicationInfo.loadLabel(pManager));
            mPackageName.setText(packageName);
            mComment.setText(mCommentString);
            mInfo.setEnabled(true);
            mVersion.setText(getString(R.string.app_info_version, packageInfo.versionName, packageInfo.versionCode));
            if ((applicationInfo.flags & ApplicationInfo.FLAG_EXTERNAL_STORAGE) == ApplicationInfo.FLAG_EXTERNAL_STORAGE) {
                mStatus.setText(R.string.app_info_sd_installed);
            } else {
                mStatus.setText(R.string.app_info_local_installed);
            }
            
            if (isFromGPlay) {
                mPlayLinked.setText(R.string.app_info_play_linked);
            } else {
                mPlayLinked.setText(R.string.app_info_play_not_linked);
            }
            
            checkStopButton();

            if (AndroidUtils.isICSOrHigher()) {
                mUninstallApplication.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            try {
                                Uri packageUri = Uri.parse("package:" + packageName);
                                Intent i = new Intent(Intent.ACTION_UNINSTALL_PACKAGE, packageUri);
                                activity.startActivity(i);
                            } catch (Exception e) {
                                Crouton.makeText(activity, R.string.error_uninstall_application, Style.ALERT).show();
                            }
                        }
                    }
                });
            } else {
                mUninstallApplication.setVisibility(View.GONE);
            }
            
            if (launchIntent == null) {
                mStartApplication.setEnabled(false);
            } else {                
                mStartApplication.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        final Activity activity = getActivity();
                        if (activity != null) {
                            try {
                                activity.startActivity(launchIntent);
                            } catch (Exception e) {
                                Crouton.makeText(activity, R.string.error_start_application, Style.ALERT).show();
                            }
                        }
                    }
                });
            }
            
            if (AndroidUtils.isGingerbreadOrHigher()) {
                final SimpleDateFormat dateFormat = new SimpleDateFormat("MM/dd/yyyy HH:mm", Locale.getDefault());
                mApplicationDate.setText(getString(R.string.app_info_date,
                        dateFormat.format(new Date(packageInfo.firstInstallTime)),
                        dateFormat.format(new Date(packageInfo.lastUpdateTime))));
                mApplicationDateSeparator.setVisibility(View.VISIBLE);
            } else {
                mApplicationDate.setVisibility(View.GONE);
                mApplicationDateSeparator.setVisibility(View.GONE);
            }
            
            String[] permissions = packageInfo.requestedPermissions;
            if (permissions == null || permissions.length == 0) {
                mApplicationPermissions.setText(R.string.app_info_no_permissions);
            } else {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < permissions.length; i++) {
                    if (i > 0) {
                        sb.append('\n');
                    }
                    if (permissions[i].startsWith(PERMISSION_PREFIX)) {
                        sb.append(permissions[i].substring(PERMISSION_PREFIX.length()));
                    } else {
                        sb.append(permissions[i]);
                    }
                }
                mApplicationPermissions.setText(sb);
            }

            if (AppUtil.isGooglePlayAvailable(pManager)) {
                mPlay.setVisibility(View.VISIBLE);
            } else {
                mPlay.setVisibility(View.GONE);
            }
        }

        if (ThemeManager.isFlavoredTheme(getActivity())) {
            TypefaceProvider.setTypeFace(getActivity(), mTitle, TypefaceProvider.FONT_BOLD);
            TypefaceProvider.setTypeFace(getActivity(), mPackageName, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mComment, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mStatus, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mVersion, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mPlayLinked, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mStopApplication, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mUninstallApplication, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mStartApplication, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mApplicationDate, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(getActivity(), mApplicationPermissions, TypefaceProvider.FONT_REGULAR);
        }
    }

    private void checkStopButton() {
        final boolean isRunning = AppUtil.isRunning(getActivity(), mPackage);
        if (mStopApplication != null) {
            mStopApplication.setEnabled(isRunning);
            if (isRunning) {
                mStopApplication.setOnClickListener(new View.OnClickListener() {
                    @SuppressLint("NewApi")
                    @SuppressWarnings("deprecation")
                    @Override
                    public void onClick(View v) {
                        ActivityManager manager = getActivityManager();
                        if (manager != null) {
                            if (AndroidUtils.isFroyoOrHigher()) {
                                manager.killBackgroundProcesses(mPackage);
                            } else {
                                manager.restartPackage(mPackage);
                            }
                            v.setEnabled(false);
                        }
                    }
                });
            }
        }
    }

    @Override
    public void onClick(View v) {
        if (v == mInfo) {
            AppUtil.showInstalledAppDetails(getActivity(), mPackage);
        } else if (v == mPlay) {
            AppUtil.showPlayGoogleApp(getActivity(), mPackage, false);
        } else if (v == mComment || v == mIcon) {
            final Context context = getActivity();
            if (context != null) {
                AlertDialog.Builder alert = new AlertDialog.Builder(context);
                alert.setTitle("comment:");
                // alert.setMessage(R.string.new_file_dialog_msg);
                // By default, the name is rmb-<date>.xml
                // Time time = new Time();
                // time.setToNow();
                // String fileName = context.getString(R.string.new_file_dialog_name, time.format("%Y%m%d"));
                // Set an EditText view to get user input
                final EditText input = new EditText(context);
                // input.setText(fileName);
                alert.setView(input);
                alert.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        mCommentString = input.getText().toString();
                        mComment.setText(mCommentString);
                        mCallBack.updateAppInfo(mName, mPackage, mCommentString);
                    }
                });

                alert.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int whichButton) {
                        // Canceled.
                    }
                });
                alert.create().show();
            }
        }
    }
    
    protected ActivityManager getActivityManager() {
        ActivityManager  manager = null;
        if (getActivity() != null) {
            manager = (ActivityManager) getActivity().getSystemService(Context.ACTIVITY_SERVICE);
        }
        return manager;
    }

}
