package com.child.app.child;

import java.util.List;

public class AppScanResult {

    private String packageName;
    private String appName;
    private int riskScore;
    private String riskCategory;
    private List<String> reasons;

    public AppScanResult(String packageName,
                         String appName,
                         int riskScore,
                         String riskCategory,
                         List<String> reasons) {

        this.packageName = packageName;
        this.appName = appName;
        this.riskScore = riskScore;
        this.riskCategory = riskCategory;
        this.reasons = reasons;
    }

    public String getPackageName() { return packageName; }
    public String getAppName() { return appName; }
    public int getRiskScore() { return riskScore; }
    public String getRiskCategory() { return riskCategory; }
    public List<String> getReasons() { return reasons; }
}
