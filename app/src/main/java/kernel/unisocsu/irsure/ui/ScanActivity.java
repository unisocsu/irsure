package kernel.unisocsu.irsure.ui;

import android.content.Context;
import android.content.Intent;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcCodeset;
import kernel.unisocsu.irsure.models.AcFunction;

public class ScanActivity extends AppCompatActivity {

    private DbHelper dbHelper;
    private ConsumerIrManager irManager;
    private List<AcCodeset> allCodesets;
    private int currentIndex = 0;
    private boolean isScanning = false;
    private final Handler scanHandler = new Handler();

    private TextView tvStatus, tvProgress;
    private Button btnToggleScan, btnStop;
    private RecyclerView rvResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        dbHelper = DbHelper.getInstance(this);
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);

        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        btnStop = findViewById(R.id.btnStop);
        rvResults = findViewById(R.id.rvResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));
        rvResults.setHasFixedSize(true);
        rvResults.setItemAnimator(null);

        allCodesets = dbHelper.searchCodesets(null);

        btnToggleScan.setOnClickListener(v -> {
            if (!isScanning) startScanning();
            else stopScanning();
        });

        btnStop.setOnClickListener(v -> stopAndShowChoices());
    }

    private void startScanning() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "לא נמצאה עינית IR", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        currentIndex = 0;
        btnStop.setVisibility(View.VISIBLE);
        runStep();
    }

    private void runStep() {
        if (!isScanning) return;

        if (currentIndex >= allCodesets.size()) {
            isScanning = false;
            btnStop.setVisibility(View.GONE);
            tvStatus.setText("הסריקה הסתיימה");
            tvProgress.setText(allCodesets.size() + " / " + allCodesets.size());
            stopAndShowChoices();
            return;
        }

        AcCodeset current = allCodesets.get(currentIndex);
        tvStatus.setText("בודק: " + current.getBrands());
        tvProgress.setText((currentIndex + 1) + " / " + allCodesets.size());

        AcFunction f = dbHelper.findFunction(current.getId(), "ON", null, null, null, null);
        if (f != null && f.getPattern() != null && !f.getPattern().isEmpty()) {
            irManager.transmit(f.getFreqHz(), f.getPattern());
        }

        currentIndex++;
        scanHandler.postDelayed(this::runStep, 2500L);
    }

    private void stopScanning() {
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        btnStop.setVisibility(View.GONE);
    }

    private void stopAndShowChoices() {
        stopScanning();

        if (allCodesets == null || allCodesets.isEmpty()) {
            Toast.makeText(this, "לא נמצאו דגמים", Toast.LENGTH_SHORT).show();
            return;
        }

        int start = Math.max(0, currentIndex - 8);
        List<AcCodeset> window = new ArrayList<>();
        for (int i = start; i < Math.min(allCodesets.size(), start + 10); i++) {
            window.add(allCodesets.get(i));
        }

        DeviceAdapter adapter = new DeviceAdapter(window, codeset -> {
            DevicePickerActivity.saveDevice(
                    ScanActivity.this,
                    codeset,
                    codeset.getDisplayLabel());

            getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                    .edit()
                    .putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId())
                    .apply();

            Toast.makeText(ScanActivity.this, "השלט נשמר!", Toast.LENGTH_SHORT).show();
            startActivity(new Intent(ScanActivity.this, RemoteActivity.class));
            finish();
        });

        rvResults.setAdapter(adapter);
        rvResults.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        scanHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}
