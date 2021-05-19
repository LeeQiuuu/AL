package com.mrlove.lock.module.splash;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.widget.ImageView;

import com.mrlove.lock.R;
import com.mrlove.lock.module.lock.GestureSelfUnlockActivity;
import com.mrlove.lock.base.BaseActivity;
import com.mrlove.lock.base.AppConstants;
import com.mrlove.lock.module.pwd.CreatePwdActivity;
import com.mrlove.lock.service.LoadAppListService;
import com.mrlove.lock.service.LockService;
import com.mrlove.lock.utils.AppUtils;
import com.mrlove.lock.utils.LockUtil;
import com.mrlove.lock.utils.SpUtil;
import com.mrlove.lock.utils.ToastUtil;
import com.mrlove.lock.widget.DialogPermission;
import com.mrlove.lock.widget.DialogPermissionQidong;

/**
 * Created by Mrlove
 */

public class SplashActivity extends BaseActivity {

    private ImageView mImgSplash;
    private ObjectAnimator animator;
    private int RESULT_ACTION_USAGE_ACCESS_SETTINGS = 1;
    private int RESULT_ACTION_MANAGE_OVERLAY_PERMISSION = 2;

    @Override
    public int getLayoutId() {
        return R.layout.activity_splash;
    }

    @Override
    protected void initViews(Bundle savedInstanceState) {
        AppUtils.hideStatusBar(getWindow(), true);
        mImgSplash = (ImageView) findViewById(R.id.img_splash);
    }

    @Override
    protected void initData() {
        startService(new Intent(this, LoadAppListService.class));
        if (SpUtil.getInstance().getBoolean(AppConstants.LOCK_STATE, false)) {
            startService(new Intent(this, LockService.class));
        }
        animator = ObjectAnimator.ofFloat(mImgSplash, "alpha", 0.5f, 1);
        animator.setDuration(1500);
        animator.start();
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                boolean isFirstLock = SpUtil.getInstance().getBoolean(AppConstants.LOCK_IS_FIRST_LOCK, true);
                if (isFirstLock) { //如果第一次
                    RequestOverlayPermission();
                    showDialog();
                    showDialogQidong();
                } else {
                    Intent intent = new Intent(SplashActivity.this, GestureSelfUnlockActivity.class);
                    intent.putExtra(AppConstants.LOCK_PACKAGE_NAME, AppConstants.APP_PACKAGE_NAME); //传自己的包名
                    intent.putExtra(AppConstants.LOCK_FROM, AppConstants.LOCK_FROM_LOCK_MAIN_ACITVITY);
                    startActivity(intent);
                    finish();
                    overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
                }
            }
        });
    }

    /**
     * 弹出dialog
     */
    private void showDialog() {
        //如果没有获得查看使用情况权限和手机存在查看使用情况这个界面
        if (!LockUtil.isStatAccessPermissionSet(SplashActivity.this) && LockUtil.isNoOption(SplashActivity.this)) {
            DialogPermission dialog = new DialogPermission(SplashActivity.this);
            dialog.show();
            dialog.setOnClickListener(new DialogPermission.onClickListener() {
                @Override
                public void onClick() {
                    Intent intent = new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS);
                    startActivityForResult(intent, RESULT_ACTION_USAGE_ACCESS_SETTINGS);
                }
            });
        } else {
            gotoCreatePwdActivity();
        }
    }

    /**
     * 弹出dialog
     */
    private void showDialogQidong() {
        //如果没有获得查看使用情况权限和手机存在查看使用情况这个界面
        if (!LockUtil.isStatAccessPermissionSet(SplashActivity.this) && LockUtil.isNoOption(SplashActivity.this)) {
            DialogPermissionQidong dialog = new DialogPermissionQidong(SplashActivity.this);
            dialog.show();
            dialog.setOnClickListener(new DialogPermissionQidong.onClickListener() {
                @Override
                public void onClick() {
                    startActivity(getAutostartSettingIntent(SplashActivity.this));
                }
            });
        } else {
            gotoCreatePwdActivity();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RESULT_ACTION_USAGE_ACCESS_SETTINGS) {
            if (LockUtil.isStatAccessPermissionSet(SplashActivity.this)) {
                gotoCreatePwdActivity();
            } else {
                ToastUtil.showToast("没有权限");
                finish();
            }
        }
    }

    private void gotoCreatePwdActivity() {
        Intent intent = new Intent(SplashActivity.this, CreatePwdActivity.class);
        startActivity(intent);
        finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }

    @Override
    protected void initAction() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        animator = null;
    }

    private void RequestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= 23) {
            String ACTION_MANAGE_OVERLAY_PERMISSION = "android.settings.action.MANAGE_OVERLAY_PERMISSION";
            Intent intent = new Intent(ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + getPackageName()));

            startActivity(intent);
        }
    }

    /**
     * 获取自启动管理页面的Intent
     *
     * @param context context
     * @return 返回自启动管理页面的Intent
     */
    public static Intent getAutostartSettingIntent(Context context) {
        ComponentName componentName = null;
        String brand = Build.MANUFACTURER;
        Intent intent = new Intent();
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        switch (brand.toLowerCase()) {
            case "samsung"://三星
                componentName = new ComponentName("com.samsung.android.sm", "com.samsung.android.sm.app.dashboard.SmartManagerDashBoardActivity");
                break;
            case "huawei"://华为
                //荣耀V8，EMUI 8.0.0，Android 8.0上，以下两者效果一样
                componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.appcontrol.activity.StartupAppControlActivity");
                componentName = ComponentName.unflattenFromString("com.huawei.systemmanager/.startupmgr.ui.StartupNormalAppListActivity");//跳自启动管理
//            componentName = new ComponentName("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");//目前看是通用的
                break;
            case "xiaomi"://小米
                componentName = new ComponentName("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity");
                break;
            case "vivo"://VIVO
//            componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.safaguard.PurviewTabActivity");
                componentName = new ComponentName("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.AddWhiteListActivity");
                break;
            case "oppo"://OPPO
//            componentName = new ComponentName("com.oppo.safe", "com.oppo.safe.permission.startup.StartupAppListActivity");
                componentName = new ComponentName("com.coloros.oppoguardelf", "com.coloros.powermanager.fuelgaue.PowerUsageModelActivity");
                break;
            case "yulong":
            case "360"://360
                componentName = new ComponentName("com.yulong.android.coolsafe", "com.yulong.android.coolsafe.ui.activity.autorun.AutoRunListActivity");
                break;
            case "meizu"://魅族
                componentName = new ComponentName("com.meizu.safe", "com.meizu.safe.permission.SmartBGActivity");
                break;
            case "oneplus"://一加
                componentName = new ComponentName("com.oneplus.security", "com.oneplus.security.chainlaunch.view.ChainLaunchAppListActivity");
                break;
            case "letv"://乐视
                intent.setAction("com.letv.android.permissionautoboot");
            default://其他
                intent.setAction("android.settings.APPLICATION_DETAILS_SETTINGS");
                intent.setData(Uri.fromParts("package", context.getPackageName(), null));
                break;
        }
        intent.setComponent(componentName);
        return intent;
    }
}
