package com.child.app.child;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;
import com.child.app.R;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class MaliciousAppActivity extends AppCompatActivity {
    private ListView listView;
    private ProgressBar progressBar;
    private Button btnScan;
    private AppResultAdapter adapter;
    private List<String> resultList;
    private MLModelHelper modelHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_malicious_app);

        listView = findViewById(R.id.listViewResults);
        progressBar = findViewById(R.id.progressBar);
        btnScan = findViewById(R.id.btnScan);

        resultList = new ArrayList<>();
        adapter = new AppResultAdapter(this, resultList);
        listView.setAdapter(adapter);

        modelHelper = new MLModelHelper(this);

        btnScan.setOnClickListener(v -> scanApps());

        // Start automatic scanning
        startAutoScan();
    }

    private void scanApps() {
        progressBar.setVisibility(View.VISIBLE);
        resultList.clear();
        new Thread(() -> {
            PackageManager pm = getPackageManager();
            List<ApplicationInfo> apps =
                    pm.getInstalledApplications(PackageManager.GET_META_DATA);
            for (ApplicationInfo app : apps) {
                // Ignore system apps
                if ((app.flags & ApplicationInfo.FLAG_SYSTEM) != 0)
                    continue;
                // Ignore updated system apps
                if ((app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) != 0)
                    continue;
                analyzeApp(app.packageName, pm);
            }
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
                adapter.notifyDataSetChanged();
            });
        }).start();
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
            String status = (risk >= 50) ? "MALICIOUS" : "SAFE";
            String value = packageName + "," + risk + "," + status;
            resultList.add(value);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startAutoScan() {
        PeriodicWorkRequest scanRequest =
                new PeriodicWorkRequest.Builder(
                        AutoScanWorker.class,
                        1, // Changed to 1 hour as minimum is 15 mins but usually higher is better for performance
                        TimeUnit.HOURS)
                        .build();
        
        WorkManager.getInstance(this)
                .enqueueUniquePeriodicWork(
                        "malicious_app_scan",
                        ExistingPeriodicWorkPolicy.KEEP,
                        scanRequest
                );
    }
}
