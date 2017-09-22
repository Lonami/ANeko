package com.example.aneko.sample;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.widget.Toast;

public class SkinActivity extends Activity
{
    private static final String ANEKO_PACKAGE = "org.tamanegi.aneko";
    private static final String ANEKO_ACTIVITY =
        "org.tamanegi.aneko.ANekoActivity";

    private static final Uri ANEKO_MARKET_URI =
        Uri.parse("market://search?q=" + ANEKO_PACKAGE);

    @Override
    public void onResume()
    {
        super.onResume();

        boolean packageFound = false;
        try {
            PackageManager pm = getPackageManager();
            packageFound = (pm.getPackageInfo(ANEKO_PACKAGE, 0) != null);
        }
        catch(PackageManager.NameNotFoundException e) {
            // ignore
        }

        int msgId;
        final Intent intent;
        if(packageFound) {
            msgId = R.string.msg_usage;
            intent = new Intent(Intent.ACTION_MAIN)
                .addCategory(Intent.CATEGORY_LAUNCHER)
                .setClassName(ANEKO_PACKAGE, ANEKO_ACTIVITY);
        }
        else {
            msgId = R.string.msg_no_package;
            intent = new Intent(Intent.ACTION_VIEW, ANEKO_MARKET_URI);
        }

        new AlertDialog.Builder(this)
            .setTitle(R.string.app_name)
            .setMessage(msgId)
            .setPositiveButton(
                android.R.string.ok,
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        try {
                            startActivity(intent);
                        }
                        catch(ActivityNotFoundException e) {
                            e.printStackTrace();
                            Toast.makeText(SkinActivity.this,
                                           R.string.msg_unexpected_err,
                                           Toast.LENGTH_SHORT)
                                .show();
                        }

                        finish();
                    }
                })
            .setOnCancelListener(
                new DialogInterface.OnCancelListener() {
                    public void onCancel(DialogInterface dialog) {
                        finish();
                    }
                })
            .show();
    }
}
