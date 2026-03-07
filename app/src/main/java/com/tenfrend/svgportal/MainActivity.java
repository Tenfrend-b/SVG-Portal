package com.tenfrend.svgportal;

import android.app.Activity;
import android.content.ComponentName;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;

public class MainActivity extends Activity {

    private static final String[] ENTRANCE_ALIASES = {
            ".EntranceA",
            ".EntranceB",
            ".EntranceC",
            ".EntranceD",
            ".EntranceE",
            ".EntranceF"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText(R.string.entrance_manager_title);
        title.setTextSize(24);
        layout.addView(title);

        for (String aliasSuffix : ENTRANCE_ALIASES) {
            String fullName = getPackageName() + aliasSuffix;
            Switch sw = new Switch(this);
            sw.setText(getEntranceDisplayName(aliasSuffix));
            sw.setChecked(isComponentEnabled(fullName));
            sw.setOnCheckedChangeListener((buttonView, isChecked) ->
                    setComponentEnabled(fullName, isChecked));
            layout.addView(sw);
        }

        setContentView(layout);
    }

    private String getEntranceDisplayName(String suffix) {
        switch (suffix) {
            case ".EntranceA": return getString(R.string.entrance_a_name);
            case ".EntranceB": return getString(R.string.entrance_b_name);
            case ".EntranceC": return getString(R.string.entrance_c_name);
            case ".EntranceD": return getString(R.string.entrance_d_name);
            case ".EntranceE": return getString(R.string.entrance_e_name);
            case ".EntranceF": return getString(R.string.entrance_f_name);
            default: return suffix;
        }
    }

    private boolean isComponentEnabled(String componentName) {
        int state = getPackageManager().getComponentEnabledSetting(
                new ComponentName(this, componentName));
        if (state == PackageManager.COMPONENT_ENABLED_STATE_ENABLED) {
            return true;
        } else if (state == PackageManager.COMPONENT_ENABLED_STATE_DISABLED) {
            return false;
        } else {
            // 读取清单中的默认 enabled 状态
            try {
                ActivityInfo info = getPackageManager().getActivityInfo(
                        new ComponentName(this, componentName), 0);
                return info.enabled;
            } catch (PackageManager.NameNotFoundException e) {
                return false;
            }
        }
    }

    private void setComponentEnabled(String componentName, boolean enabled) {
        int newState = enabled ? PackageManager.COMPONENT_ENABLED_STATE_ENABLED
                               : PackageManager.COMPONENT_ENABLED_STATE_DISABLED;
        getPackageManager().setComponentEnabledSetting(
                new ComponentName(this, componentName),
                newState,
                PackageManager.DONT_KILL_APP);
    }
}