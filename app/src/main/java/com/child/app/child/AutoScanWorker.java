package com.child.app.child;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;
import java.util.List;

public class AutoScanWorker extends Worker {
    private MLModelHelper modelHelper;

    public AutoScanWorker(@NonNull Context context,
                          @NonNull WorkerParameters params) {
        super(context, params);
        modelHelper = new MLModelHelper(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        PackageManager pm = getApplicationContext().getPackageManager();
        List<ApplicationInfo> apps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);
        for (ApplicationInfo app : apps) {
            // Ignore system apps
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) continue;
            if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0) continue;
            analyzeApp(app.packageName, pm);
        }
        return Result.success();
    }

    private void analyzeApp(String packageName, PackageManager pm) {
        try {
            PackageInfo pkgInfo =
                    pm.getPackageInfo(packageName,
                            PackageManager.GET_PERMISSIONS);
            String[] requestedPermissions = pkgInfo.requestedPermissions;
            float[] inputFeatures =
                    new float[DatasetPermissions.PERMISSIONS.length];
            
            if (requestedPermissions != null) {
                for (int i = 0; i < DatasetPermissions.PERMISSIONS.length; i++) {
                    for (String perm : requestedPermissions) {
                        if (perm.equals(DatasetPermissions.PERMISSIONS[i])) {
                            inputFeatures[i] = 1f;
                            break;
                        }
                    }
                }
            }
            
            float prediction = modelHelper.predict(inputFeatures);
            int risk = (int) (prediction * 100);
            
            // Logic to report malicious apps can be added here
            // e.g., send to Firestore or show notification if risk > 50
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
