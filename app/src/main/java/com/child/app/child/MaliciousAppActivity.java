package com.child.app.child;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.LinearInterpolator;
import android.widget.PopupMenu;
import android.widget.ProgressBar;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.child.app.R;
import com.google.android.material.progressindicator.CircularProgressIndicator;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

public class MaliciousAppActivity extends AppCompatActivity {

    private CircularProgressIndicator scanProgressIndicator;
    private TextView txtScanPercent;
    private TextView txtScanningStatus;
    private TextView txtItemsScanned;
    private ProgressBar recordSpinner;
    private View btnScan;
    private View btnCancel;
    private View prePostScanView;
    private View scanningView;
    private RecyclerView rvMaliciousApps;
    private View cardScanRecord;
    private View txtScanRecordsHeader;
    private View manualOptimisationSection;
    private View btnDisableManual;
    
    private List<String> maliciousAppsList = new ArrayList<>();
    private MaliciousAppsAdapter maliciousAppsAdapter;
    private boolean isScanning = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Enable edge-to-edge
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().setNavigationBarColor(Color.TRANSPARENT);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            WindowManager.LayoutParams layoutParams = getWindow().getAttributes();
            layoutParams.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES;
            getWindow().setAttributes(layoutParams);
        }
        
        setContentView(R.layout.malicious);

        handleWindowInsets();

        prePostScanView = findViewById(R.id.prePostScanView);
        scanningView = findViewById(R.id.scanningView);
        scanProgressIndicator = findViewById(R.id.scanProgressIndicator);
        txtScanPercent = findViewById(R.id.txtScanPercent);
        txtScanningStatus = findViewById(R.id.txtScanningStatus);
        txtItemsScanned = findViewById(R.id.txtItemsScanned);
        recordSpinner = findViewById(R.id.recordSpinner);
        btnScan = findViewById(R.id.btnScan);
        btnCancel = findViewById(R.id.btnCancel);
        rvMaliciousApps = findViewById(R.id.rvMaliciousApps);
        cardScanRecord = findViewById(R.id.cardScanRecord);
        txtScanRecordsHeader = findViewById(R.id.txtScanRecordsHeader);
        manualOptimisationSection = findViewById(R.id.manualOptimisationSection);
        btnDisableManual = findViewById(R.id.btnDisableManual);

        maliciousAppsAdapter = new MaliciousAppsAdapter(maliciousAppsList);
        rvMaliciousApps.setLayoutManager(new LinearLayoutManager(this));
        rvMaliciousApps.setAdapter(maliciousAppsAdapter);

        btnScan.setOnClickListener(v -> startScan());
        btnCancel.setOnClickListener(v -> stopScan());
        
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());
        
        findViewById(R.id.btnMore).setOnClickListener(v -> showMoreMenu(v));

        btnDisableManual.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Settings.ACTION_APPLICATION_DEVELOPMENT_SETTINGS));
            } catch (Exception e) {
                // If the intent is not available, open general settings
                startActivity(new Intent(Settings.ACTION_SETTINGS));
            }
        });
        
        // Ensure status bar icons are black
        WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView())
                .setAppearanceLightStatusBars(true);
    }

    @Override
    protected void onResume() {
        super.onResume();
        checkDeveloperOptions();
    }

    private void checkDeveloperOptions() {
        int devOptionsEnabled = Settings.Global.getInt(getContentResolver(), Settings.Global.DEVELOPMENT_SETTINGS_ENABLED, 0);
        if (devOptionsEnabled == 0) {
            manualOptimisationSection.setVisibility(View.GONE);
        } else {
            manualOptimisationSection.setVisibility(View.VISIBLE);
        }
    }

    private void showMoreMenu(View v) {
        PopupMenu popup = new PopupMenu(this, v);
        popup.getMenu().add("Delete scan records");
        popup.setOnMenuItemClickListener(item -> {
            if (item.getTitle().equals("Delete scan records")) {
                deleteScanRecords();
                return true;
            }
            return false;
        });
        popup.show();
    }

    private void deleteScanRecords() {
        if (cardScanRecord != null) cardScanRecord.setVisibility(View.GONE);
        if (txtScanRecordsHeader != null) txtScanRecordsHeader.setVisibility(View.GONE);
    }

    private void handleWindowInsets() {
        View mainView = findViewById(R.id.main);
        View headerLayout = findViewById(R.id.headerLayout);
        View bottomActionContainer = findViewById(R.id.bottomActionContainer);

        ViewCompat.setOnApplyWindowInsetsListener(mainView, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            
            headerLayout.setPadding(headerLayout.getPaddingLeft(), 
                    systemBars.top, 
                    headerLayout.getPaddingRight(), 
                    headerLayout.getPaddingBottom());
            
            ViewGroup.MarginLayoutParams lp = (ViewGroup.MarginLayoutParams) bottomActionContainer.getLayoutParams();
            lp.bottomMargin = systemBars.bottom;
            bottomActionContainer.setLayoutParams(lp);
            
            return insets;
        });
    }

    private void stopScan() {
        isScanning = false;
        onScanComplete();
    }

    private void startScan() {
        if (isScanning) return;
        isScanning = true;
        
        prePostScanView.setVisibility(View.GONE);
        scanningView.setVisibility(View.VISIBLE);
        btnScan.setVisibility(View.GONE);
        btnCancel.setVisibility(View.VISIBLE);
        
        recordSpinner.setVisibility(View.VISIBLE);
        rvMaliciousApps.setVisibility(View.GONE);
        maliciousAppsList.clear();
        maliciousAppsAdapter.notifyDataSetChanged();

        PackageManager pm = getPackageManager();
        List<ApplicationInfo> apps = pm.getInstalledApplications(PackageManager.GET_META_DATA);
        List<ApplicationInfo> userApps = new ArrayList<>();
        for (ApplicationInfo app : apps) {
            if ((app.flags & ApplicationInfo.FLAG_SYSTEM) == 0 && 
                (app.flags & ApplicationInfo.FLAG_UPDATED_SYSTEM_APP) == 0) {
                userApps.add(app);
            }
        }
        
        int totalApps = userApps.size();
        AtomicInteger scannedCount = new AtomicInteger(0);

        ValueAnimator animator = ValueAnimator.ofInt(0, 100);
        animator.setDuration(5000);
        animator.setInterpolator(new LinearInterpolator());
        
        animator.addUpdateListener(animation -> {
            if (!isScanning) {
                animation.cancel();
                return;
            }
            int progress = (int) animation.getAnimatedValue();
            scanProgressIndicator.setProgress(progress);
            txtScanPercent.setText(String.valueOf(progress));
            
            int targetCount = (int) (progress / 100.0 * totalApps);
            while (scannedCount.get() < targetCount && scannedCount.get() < totalApps) {
                ApplicationInfo app = userApps.get(scannedCount.get());
                analyzeApp(app.packageName, pm);
                String appName = pm.getApplicationLabel(app).toString();
                txtScanningStatus.setText("Scanning: " + appName);
                int current = scannedCount.incrementAndGet();
                txtItemsScanned.setText(current + " items scanned");
            }
        });
        
        animator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                if (isScanning) {
                    onScanComplete();
                }
            }
        });
        animator.start();
    }

    private void analyzeApp(String packageName, PackageManager pm) {
        // ML logic here
    }

    private void onScanComplete() {
        isScanning = false;
        
        prePostScanView.setVisibility(View.VISIBLE);
        scanningView.setVisibility(View.GONE);
        btnScan.setVisibility(View.VISIBLE);
        btnCancel.setVisibility(View.GONE);
        
        recordSpinner.setVisibility(View.GONE);
        
        TextView txtItemsScannedLabel = findViewById(R.id.txtItemsScanned);
        txtItemsScannedLabel.setText(maliciousAppsList.isEmpty() ? "Scanned items. No risky apps found." : "Found " + maliciousAppsList.size() + " risks");
        
        if (!maliciousAppsList.isEmpty()) {
            rvMaliciousApps.setVisibility(View.VISIBLE);
            maliciousAppsAdapter.notifyDataSetChanged();
        }
    }
}
