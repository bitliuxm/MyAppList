package com.projectsexception.myapplist.view;

import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.v7.view.ActionMode;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.TextView;

import com.manuelpeinado.multichoiceadapter.extras.actionbarcompat.MultiChoiceBaseAdapter;
import com.projectsexception.myapplist.R;
import com.projectsexception.myapplist.iconloader.IconView;
import com.projectsexception.myapplist.model.AppInfo;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;


public class AppListAdapter extends MultiChoiceBaseAdapter implements View.OnClickListener {

    static class ViewHolder {
        TextView title;
        TextView packageName;
        IconView icon;
        CheckBox checkBox;
        TextView comment;
        ViewHolder(View view) {
            title = (TextView) view.findViewById(R.id.text_list_item);
            packageName = (TextView) view.findViewById(R.id.text2_list_item);
            icon = (IconView) view.findViewById(R.id.icon_list_item);
            checkBox = (CheckBox) view.findViewById(R.id.checkbox_list_item);
            comment = (TextView) view.findViewById(R.id.text3_list_item);
        }
    }

    public static interface ActionListener {
        void actionItemClicked(int id);
    }

    private final Context mContext;
    private final LayoutInflater mInflater;
    private final PackageManager mPm;
    private ArrayList<AppInfo> mAppList;
    private ArrayList<AppInfo> mAppListSearch;
    private String mSearchTerm;
    private int mNotInstalledColor;
    private int mInstalledColor;
    private int mNotInstalledMatchesColor;
    private int mMatchesColor;
    private int mMenu;
    private ActionListener mListener;
    private boolean mAnimations;
    private int mLastAnimatedPosition;

    public AppListAdapter(Context context, Bundle savedInstance, int menu, boolean animations) {
        super(savedInstance);
        this.mContext = context;
        this.mInflater = (LayoutInflater) context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        this.mPm = context.getPackageManager();
        this.mNotInstalledColor = context.getResources().getColor(R.color.app_not_installed);
        this.mNotInstalledMatchesColor = context.getResources().getColor(R.color.app_not_installed_matches);
        this.mMatchesColor = context.getResources().getColor(R.color.app_matches);
        this.mMenu = menu;
        this.mAnimations = animations;
        this.mLastAnimatedPosition = -1;
        setData(new ArrayList<AppInfo>());
    }

    public void setListener(ActionListener mListener) {
        this.mListener = mListener;
    }

    public void setData(ArrayList<AppInfo> data) {
        this.mAppList = data;
        refreshAppList();
    }

    private void refreshAppList() {
        mAppListSearch = new ArrayList<AppInfo>();
        if (mAppList != null) {
            for (AppInfo appInfo : mAppList) {
                if (filter(appInfo)) {
                    mAppListSearch.add(appInfo);
                }
            }
        }
    }

    private boolean filter(AppInfo appInfo) {
        return TextUtils.isEmpty(mSearchTerm)
                || (appInfo.getName() != null && appInfo.getName().toUpperCase().contains(mSearchTerm))
                || (appInfo.getPackageName() != null && appInfo.getPackageName().toUpperCase().contains(mSearchTerm))
                || (appInfo.getComment() != null && appInfo.getComment().toUpperCase().contains(mSearchTerm));
    }

    public ArrayList<AppInfo> getActualItems() {
        return mAppListSearch;
    }

    public void setSearchTerm(String searchTerm) {
        mSearchTerm = searchTerm == null ? null : searchTerm.toUpperCase();
        refreshAppList();
    }

    public void setAnimations(boolean animations) {
        mAnimations = animations;
    }

    @Override
    public View getViewImpl(int position, View convertView, ViewGroup parent) {
        View view;
        ViewHolder viewHolder;
        if (convertView == null) {
            view = mInflater.inflate(R.layout.list_item, parent, false);
            viewHolder = new ViewHolder(view);
            mInstalledColor = viewHolder.title.getCurrentTextColor();
            view.setTag(viewHolder);
        } else {
            view = convertView;
            viewHolder = (ViewHolder) view.getTag();
        }

        AppInfo item = (AppInfo) getItem(position);
        if (item.getName() == null) {
            viewHolder.title.setText(null);
        } else if (item.isInstalled()) {
            applyMatches(item.getName(), viewHolder.title, mInstalledColor, mMatchesColor, true);
        } else {
            applyMatches(item.getName(), viewHolder.title, mNotInstalledColor, mNotInstalledMatchesColor, false);
        }

        if (item.getName() == null) {
            viewHolder.packageName.setText(null);
        } else {
            applyMatches(item.getPackageName(), viewHolder.packageName, mInstalledColor, mMatchesColor, false);
        }

        if (item.getComment() == null) {
            viewHolder.comment.setText(null);
        } else {
            applyMatches(item.getComment(), viewHolder.comment, mInstalledColor, mMatchesColor, false);
        }

        viewHolder.icon.setPackageName(mPm, item.getPackageName(), R.drawable.ic_default_launcher, true);

        if (viewHolder.checkBox.getVisibility() == View.GONE) {
            viewHolder.icon.setTag(position);
            viewHolder.icon.setOnClickListener(this);
        }

        if (ThemeManager.isFlavoredTheme(mContext)) {
            TypefaceProvider.setTypeFace(mContext, viewHolder.title, TypefaceProvider.FONT_BOLD);
            TypefaceProvider.setTypeFace(mContext, viewHolder.packageName, TypefaceProvider.FONT_REGULAR);
            TypefaceProvider.setTypeFace(mContext, viewHolder.comment, TypefaceProvider.FONT_REGULAR);
            if (mAnimations && position > mLastAnimatedPosition) {
                AnimationUtil.animateIn(view);
                mLastAnimatedPosition = position;
            }
        }

        return view;
    }

    @Override
    public int getCount() {
        if (mAppListSearch == null) {
            return 0;
        } else {
            return mAppListSearch.size();
        }
    }

    @Override
    public Object getItem(int position) {
        if (mAppListSearch == null || position >= mAppListSearch.size()) {
            return null;
        } else {
            return mAppListSearch.get(position);
        }
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        MenuInflater inflater = mode.getMenuInflater();
        inflater.inflate(mMenu, menu);
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        if (mListener != null) {
            mListener.actionItemClicked(item.getItemId());
            finishActionMode();
            return true;
        }
        return false;
    }

    @Override
    public void onClick(View v) {
        Integer position = (Integer) v.getTag();
        if (v.getTag() != null) {
            setItemChecked(position, !isChecked(position));
        }
    }

    public ArrayList<AppInfo> getSelectedItems() {
        ArrayList<AppInfo> selectedApps = new ArrayList<AppInfo>();
        Set<Long> selection = getCheckedItems();
        if (selection != null) {
            List<AppInfo> allApps = getActualItems();
            int size = getCount();
            for (int i = 0 ; i < size ; i++) {
                if (selection.contains(Long.valueOf(i))) {
                    selectedApps.add(allApps.get(i));
                }
            }
        }
        return selectedApps;
    }

    void applyMatches(String value, TextView textView, int color, int colorMatches, boolean bold) {
        final Spannable spannable = new SpannableString(value);
        if (TextUtils.isEmpty(mSearchTerm)) {
            spannable.setSpan(new ForegroundColorSpan(color), 0, value.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            if (bold) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, value.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        } else {
            final int searchSize = mSearchTerm.length();
            final String valueUpper = value.toUpperCase();
            int pos = 0;
            int startMatches = valueUpper.indexOf(mSearchTerm, pos);
            while (startMatches >= 0) {
                if (startMatches > pos) {
                    spannable.setSpan(new ForegroundColorSpan(color), pos, startMatches, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                if (startMatches + searchSize <= value.length()) {
                    // "Always" in theory
                    spannable.setSpan(new ForegroundColorSpan(colorMatches), startMatches, startMatches + searchSize, Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                }
                pos = startMatches + searchSize;
                startMatches = valueUpper.indexOf(mSearchTerm, pos);
            }
            if (pos <= value.length()) {
                spannable.setSpan(new ForegroundColorSpan(color), pos, value.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            if (bold) {
                spannable.setSpan(new StyleSpan(Typeface.BOLD), 0, value.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
            }
        }
        textView.setText(spannable);
    }
}
