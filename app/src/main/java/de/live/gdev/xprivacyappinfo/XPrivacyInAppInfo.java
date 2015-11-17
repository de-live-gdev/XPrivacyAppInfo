package de.live.gdev.xprivacyappinfo;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class XPrivacyInAppInfo implements IXposedHookLoadPackage {

    private final static String PACKAGE_SETTINGS = "com.android.settings";
    private final static String PACKAGE_XPRIVACY = "biz.bokhorst.xprivacy";

    @Override
    public void handleLoadPackage(LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals(PACKAGE_SETTINGS)) {
            try {
                final Class<?> classInstalledAppDetails = XposedHelpers.findClass(PACKAGE_SETTINGS + ".applications.InstalledAppDetails", lpparam.classLoader);

                XposedHelpers.findAndHookMethod(classInstalledAppDetails, "onCreateView", LayoutInflater.class, ViewGroup.class, Bundle.class, new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(final MethodHookParam param) throws Throwable {
                        View mNotificationSwitch = (View) XposedHelpers.getObjectField(param.thisObject, "mNotificationSwitch");
                        View mUninstallButton = (View) XposedHelpers.getObjectField(param.thisObject, "mUninstallButton");
                        ViewGroup viewGroup = (ViewGroup) mNotificationSwitch.getParent();
                        final Context context = viewGroup.getContext();

                        Button xPrivacyButton = new Button(context);
                        xPrivacyButton.setText("XPrivacy");
                        xPrivacyButton.setVisibility(View.GONE);
                        xPrivacyButton.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                final int packageUid = (Integer) v.getTag();
                                if (packageUid != -1)
                                {
                            		Intent intent = new Intent("biz.bokhorst.xprivacy.action.APPLICATION");
                            		intent.setComponent(new ComponentName(PACKAGE_XPRIVACY, "biz.bokhorst.xprivacy.ActivityApp"));
                            		intent.putExtra("Uid", packageUid);
                            		context.startActivity(intent);
                                }
                            }
                        });
                        viewGroup.addView(xPrivacyButton, viewGroup.indexOfChild(mNotificationSwitch) + 1, mUninstallButton.getLayoutParams());

                        XposedHelpers.setAdditionalInstanceField(param.thisObject, "xPrivacyButton", xPrivacyButton);
                    }
                });

                XposedHelpers.findAndHookMethod(classInstalledAppDetails, "refreshUi", new XC_MethodHook() {
                    @Override
                    protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                        Button xPrivacyButton = (Button) XposedHelpers.getAdditionalInstanceField(param.thisObject, "xPrivacyButton");
                        xPrivacyButton.setTag(-1);
                        if ((!((Boolean) param.getResult())) || xPrivacyButton != null && xPrivacyButton.getVisibility() == View.VISIBLE)
                            return;

                 
                        final int packageUid = ((ApplicationInfo) XposedHelpers.getObjectField(XposedHelpers.getObjectField(param.thisObject, "mAppEntry"), "info")).uid;
                        PackageManager pm = (PackageManager) XposedHelpers.getObjectField(param.thisObject, "mPm");

                        // Throws exception if app isnt available -> no button visible
                        try{
							pm.getPackageInfo(PACKAGE_XPRIVACY, PackageManager.GET_ACTIVITIES);
							
							xPrivacyButton.setTag(packageUid);
							xPrivacyButton.setVisibility(View.VISIBLE);
                            }
                        catch(NameNotFoundException ex1){}
                    }
                });
            } catch (Throwable t) { XposedBridge.log(t); }
        }
    }
}