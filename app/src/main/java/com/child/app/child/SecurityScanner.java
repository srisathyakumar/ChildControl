package com.child.app.child;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;

import java.util.ArrayList;
import java.util.List;

public class SecurityScanner {

    private Context context;
    private MLModelHelper modelHelper;

    public SecurityScanner(Context context) {
        this.context = context;
        modelHelper = new MLModelHelper(context);
    }

    public List<AppScanResult> scanAllApps() {

        List<AppScanResult> results = new ArrayList<>();

        PackageManager pm = context.getPackageManager();
        List<ApplicationInfo> apps =
                pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : apps) {

            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
                continue;
            }

            try {

                String packageName = app.packageName;
                String appName = pm.getApplicationLabel(app).toString();

                PackageInfo pkgInfo =
                        pm.getPackageInfo(packageName,
                                PackageManager.GET_PERMISSIONS);

                String[] permissions = pkgInfo.requestedPermissions;

                int riskScore = 0;
                List<String> reasons = new ArrayList<>();

                // ---------------------------
                // 1️⃣ ML BASE RISK
                // ---------------------------
                float[] inputFeatures =
                        new float[DatasetPermissions.PERMISSIONS.length];

                if (permissions != null) {
                    for (int i = 0; i < DatasetPermissions.PERMISSIONS.length; i++) {
                        for (String perm : permissions) {
                            if (perm.equals(DatasetPermissions.PERMISSIONS[i])) {
                                inputFeatures[i] = 1f;
                                break;
                            }
                        }
                    }
                }

                float mlScore = modelHelper.predict(inputFeatures);

                if (mlScore < 0.3f) {
                    riskScore += 10;
                } else if (mlScore < 0.6f) {
                    riskScore += 30;
                    reasons.add("Moderate suspicious behavior detected");
                } else {
                    riskScore += 60;
                    reasons.add("High probability of malicious behavior");
                }

                // ---------------------------
                // 2️⃣ ADVANCED ANALYSIS
                // ---------------------------
                String installer =
                        pm.getInstallerPackageName(packageName);

                AdvancedRiskCalculator.Result adv =
                        AdvancedRiskCalculator.analyze(permissions, installer, packageName);

                riskScore += adv.extraRisk;
                reasons.addAll(adv.reasons);

                // ---------------------------
                // FINAL NORMALIZATION
                // ---------------------------
                if (riskScore > 90) riskScore = 90;

                String category;

                if (riskScore >= 70)
                    category = "HIGH RISK";
                else if (riskScore >= 30)
                    category = "MEDIUM RISK";
                else if (riskScore >= 10)
                    category = "LOW RISK";
                else
                    category = "SAFE";

                results.add(new AppScanResult(
                        packageName,
                        appName,
                        riskScore,
                        category,
                        reasons
                ));

            } catch (Exception ignored) {}
        }

        return results;
    }
}