package com.child.app.child;

import java.util.ArrayList;
import java.util.List;

public class AdvancedRiskCalculator {

    public static class Result {
        public int extraRisk;
        public List<String> reasons;

        public Result(int risk, List<String> reasons) {
            this.extraRisk = risk;
            this.reasons = reasons;
        }
    }

    public static Result analyze(String[] permissions, String installer, String packageName) {

        int risk = 0;
        List<String> reasons = new ArrayList<>();

        if (permissions == null) {
            return new Result(0, reasons);
        }

        boolean hasInternet = false;
        boolean hasContacts = false;
        boolean hasSMS = false;
        boolean hasMic = false;
        boolean hasLocation = false;
        boolean hasAccessibility = false;
        boolean hasCamera = false;
        boolean hasStorage = false;
        boolean hasCallLog = false;
        boolean hasBoot = false;

        int totalPermissions = permissions.length;

        for (String perm : permissions) {

            if (perm.contains("INTERNET")) hasInternet = true;
            if (perm.contains("READ_CONTACTS")) hasContacts = true;
            if (perm.contains("READ_SMS") || perm.contains("SEND_SMS")) hasSMS = true;
            if (perm.contains("RECORD_AUDIO")) hasMic = true;
            if (perm.contains("LOCATION")) hasLocation = true;
            if (perm.contains("BIND_ACCESSIBILITY_SERVICE")) hasAccessibility = true;
            if (perm.contains("CAMERA")) hasCamera = true;
            if (perm.contains("READ_EXTERNAL_STORAGE") || perm.contains("WRITE_EXTERNAL_STORAGE"))
                hasStorage = true;
            if (perm.contains("READ_CALL_LOG")) hasCallLog = true;
            if (perm.contains("RECEIVE_BOOT_COMPLETED")) hasBoot = true;
        }

        // 🔥 DATA THEFT
        if (hasInternet && hasContacts) {
            risk += 15;
            reasons.add("Can access contacts and transmit data");
        }
        if (hasInternet && hasSMS) {
            risk += 20;
            reasons.add("Can read SMS and transmit data");
        }

        // 🔐 PRIVACY
        int privacyCount = 0;
        if (hasLocation) privacyCount++;
        if (hasMic) privacyCount++;
        if (hasCamera) privacyCount++;
        if (hasStorage) privacyCount++;
        if (hasCallLog) privacyCount++;
        if (hasContacts) privacyCount++;
        if (hasSMS) privacyCount++;

        if (privacyCount >= 5) {
            risk += 10;
            reasons.add("Extensive access to personal data");
        } else if (privacyCount >= 3) {
            risk += 5;
            reasons.add("Accesses multiple sensitive data sources");
        }

        // ⚙️ SYSTEM ABUSE
        if (hasAccessibility) {
            risk += 20;
            reasons.add("Has accessibility access (high control over device)");
        }

        // 📁 FILE EXFILTRATION
        if (hasInternet && hasStorage) {
            risk += 10;
            reasons.add("Can access and transfer files");
        }

        // 🔄 BOOT (LOW IMPACT)
        if (hasBoot) {
            risk += 3;
            reasons.add("Runs on device startup");
        }

        // 📦 INSTALLER
        if (installer == null || !installer.equals("com.android.vending")) {
            risk += 5;
            reasons.add("Installed outside Play Store");
        }

        // 🔗 COMBINATION BOOST
        int combo = 0;
        if (hasInternet && hasContacts) combo++;
        if (hasInternet && hasSMS) combo++;
        if (hasAccessibility && hasInternet) combo++;
        if (hasBoot && hasInternet) combo++;

        if (combo >= 3) {
            risk += 15;
            reasons.add("Multiple risky behaviors combined");
        } else if (combo >= 2) {
            risk += 10;
            reasons.add("Combination of suspicious behaviors");
        }

        // 🎰 POLICY CATEGORY (LOW IMPACT)
        String lower = packageName.toLowerCase();

        if (lower.contains("bet") || lower.contains("casino") || lower.contains("gamble")) {
            risk += 5;
            reasons.add("Gambling app (policy-sensitive)");
        }

        if (lower.contains("loan") || lower.contains("credit") || lower.contains("cash")) {
            risk += 5;
            reasons.add("Financial app (potential data risk)");
        }

        if (lower.contains("spy") || lower.contains("tracker") || lower.contains("monitor")) {
            risk += 10;
            reasons.add("Potential tracking functionality");
        }

        // 🔒 LIMIT (VERY IMPORTANT)
        if (risk > 40) risk = 40;

        return new Result(risk, reasons);
    }
}