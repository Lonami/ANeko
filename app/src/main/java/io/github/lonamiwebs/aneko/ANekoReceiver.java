package io.github.lonamiwebs.aneko;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import io.github.lonamiwebs.aneko.service.AnimationService;

public class ANekoReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_PACKAGE_REPLACED.equals(intent.getAction())) {
            String pkg = intent.getData().getEncodedSchemeSpecificPart();
            if (!context.getPackageName().equals(pkg)) {
                return;
            }
        }

        context.startService(new Intent(context, AnimationService.class)
                .setAction(AnimationService.ACTION_START));
    }
}
