package de.live.gdev.xprivacyappinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import de.robv.android.xposed.IXposedHookInitPackageResources;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_InitPackageResources;
import de.robv.android.xposed.callbacks.XC_LayoutInflated;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XPrivacyInAppInfo implements IXposedHookLoadPackage, IXposedHookInitPackageResources {
    private final static String PACKAGE_XPRIVACY = "biz.bokhorst.xprivacy";
    private final static String PACKAGE_SETTINGS = "com.android.settings";

    private final static String BUTTON_TEXT = "XPrivacy";
    private Button myAppInfoButton = null;

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_SETTINGS)) {
            try {
                final Class<?> classInstalledAppDetails = XposedHelpers.findClass(PACKAGE_SETTINGS + ".applications.InstalledAppDetails", lpparam.classLoader);
                XposedHelpers.findAndHookMethod(classInstalledAppDetails, "refreshUi", new XC_MethodHook() {
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        if (myAppInfoButton == null || myAppInfoButton.getVisibility() != View.INVISIBLE)
                            return;

                        final int packageUid = ((ApplicationInfo) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mAppEntry"), "info")).uid;
                        final PackageManager pm = (PackageManager) XposedHelpers.getObjectField(param.thisObject, "mPm");

                        boolean isXPrivacyInstalled = isAppInstalled(pm, PACKAGE_XPRIVACY);
                        myAppInfoButton.setTag(isXPrivacyInstalled ? packageUid : -1);
                        myAppInfoButton.setVisibility(isXPrivacyInstalled ? View.VISIBLE : View.GONE);
                    }
                });
            } catch (Throwable t) {
                XposedBridge.log(t);
            }
        }
    }

    @Override
    public void handleInitPackageResources(XC_InitPackageResources.InitPackageResourcesParam resparam) throws Throwable {
        if (!resparam.packageName.equals(PACKAGE_SETTINGS))
            return;

        resparam.res.hookLayout(PACKAGE_SETTINGS, "layout", "installed_app_details", new XC_LayoutInflated() {
            // App Detail Layout was inflated
            public void handleLayoutInflated(LayoutInflatedParam liparam) throws Throwable {
                try {
                    ViewGroup linearLayoutAllDetails = (ViewGroup) liparam.view.findViewById(
                            liparam.res.getIdentifier("all_details", "id", PACKAGE_SETTINGS));

                    myAppInfoButton = newMyAppInfoButton(linearLayoutAllDetails.getContext());
                    linearLayoutAllDetails.addView(myAppInfoButton, 1);

                } catch (Exception ex) {
                    XposedBridge.log(ex);
                }
            }
        });
    }

    /**
     * Generate a new App Info Button
     *
     * @param context Contexxt
     * @return A button
     */
    public Button newMyAppInfoButton(final Context context) {
        Button button = new Button(context);
        button.setText(BUTTON_TEXT);
        button.setVisibility(View.INVISIBLE);
        button.setTag("-1");
        button.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                final int packageUid = (Integer) v.getTag();
                if (packageUid != -1) {
                    Intent intent = new Intent(PACKAGE_XPRIVACY + ".action.APPLICATION");
                    intent.setComponent(new ComponentName(PACKAGE_XPRIVACY, PACKAGE_XPRIVACY + ".ActivityApp"));
                    intent.putExtra("Uid", packageUid);
                    context.startActivity(intent);
                }
            }
        });
        return button;
    }

    /**
     * Check if application is installed
     *
     * @param pm  PackageManager
     * @param uri packageName
     * @return true if app is installed
     */
    private boolean isAppInstalled(final PackageManager pm, final String uri) {
        try {
            pm.getPackageInfo(uri, PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }
}