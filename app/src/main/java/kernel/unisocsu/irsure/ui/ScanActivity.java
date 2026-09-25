package kernel.unisocsu.irsure.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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
    private static final String PREFS = "scan_settings";
    private static final String KEY_DWELL_MS = "dwell_ms";
    private static final long DEFAULT_DWELL_MS = 2500L;
    private static final long MIN_DWELL_MS = 250L;
    private static final long MAX_DWELL_MS = 30000L;

    private DbHelper dbHelper;
    private ConsumerIrManager irManager;
    private List<AcCodeset> allCodesets;
    private int currentIndex;
    private boolean isScanning;
    private final Handler scanHandler = new Handler();
    private TextView tvStatus, tvProgress;
    private Button btnToggleScan, btnStop;
    private RecyclerView rvResults;
    private EditText etDelay;
    private long dwellMs = DEFAULT_DWELL_MS;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        dbHelper = DbHelper.getInstance(this);
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        tvStatus = findViewById(R.id.tvStatus);
        tvProgress = findViewById(R.id.tvProgress);
        btnToggleScan = findViewById(R.id.btnToggleScan);
        btnStop = findViewById(R.id.btnStop);
        etDelay = findViewById(R.id.etDelay);
        etDelay.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL);
        rvResults = findViewById(R.id.rvResults);
        rvResults.setLayoutManager(new LinearLayoutManager(this));

        dwellMs = getSharedPreferences(PREFS, MODE_PRIVATE)
                .getLong(KEY_DWELL_MS, DEFAULT_DWELL_MS);
        etDelay.setText(String.valueOf(dwellMs));

        allCodesets = dbHelper.searchCodesets(null);

        btnToggleScan.setOnClickListener(v -> {
            if (!isScanning) startScanning();
            else stopScanning();
        });
        btnStop.setOnClickListener(v -> stopAndShowChoices());
    }

    private void startScanning() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, R.string.remote_no_ir_blaster, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            dwellMs = Long.parseLong(etDelay.getText().toString().trim());
        } catch (NumberFormatException e) {
            dwellMs = DEFAULT_DWELL_MS;
        }

        dwellMs = Math.max(MIN_DWELL_MS, Math.min(MAX_DWELL_MS, dwellMs));
        etDelay.setText(String.valueOf(dwellMs));
        getSharedPreferences(PREFS, MODE_PRIVATE).edit().putLong(KEY_DWELL_MS, dwellMs).apply();

        currentIndex = 0;
        isScanning = true;
        btnToggleScan.setText("השהה סריקה");
        btnToggleScan.setEnabled(true);
        etDelay.setEnabled(false);
        btnStop.setVisibility(View.VISIBLE);
        rvResults.setVisibility(View.GONE);
        runStep();
    }

    private void stopScanning() {
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        btnToggleScan.setText("המשך סריקה");
        etDelay.setEnabled(true);
        btnStop.setVisibility(View.VISIBLE);
    }

    private void runStep() {
        if (!isScanning || currentIndex >= allCodesets.size()) {
            isScanning = false;
            scanHandler.removeCallbacksAndMessages(null);
            btnToggleScan.setText("התחל סריקה");
            btnToggleScan.setEnabled(true);
            etDelay.setEnabled(true);
            return;
        }

        AcCodeset current = allCodesets.get(currentIndex);
        tvStatus.setText("בודק: " + current.getDisplayLabel());
        tvProgress.setText((currentIndex + 1) + " / " + allCodesets.size());

        AcFunction f = dbHelper.findFunction(current.getId(), "ON", null, null, null, null);
        if (f != null) {
            try {
                irManager.transmit(f.getFreqHz(), f.getPattern());
            } catch (RuntimeException e) {
                tvStatus.setText("שגיאה בשידור: " + current.getDisplayLabel());
            }
        }

        currentIndex++;
        // The configured dwell time is the exact delay before testing the next AC.
        scanHandler.postDelayed(this::runStep, dwellMs);
    }

    private void stopAndShowChoices() {
        isScanning = false;
        scanHandler.removeCallbacksAndMessages(null);
        btnToggleScan.setText("התחל סריקה");
        btnToggleScan.setEnabled(true);
        etDelay.setEnabled(true);
        btnStop.setVisibility(View.GONE);

        int start = Math.max(0, currentIndex - 8);
        List<AcCodeset> window = new ArrayList<>();
        for (int i = start; i < Math.min(allCodesets.size(), start + 10); i++) {
            window.add(allCodesets.get(i));
        }

        DeviceAdapter adapter = new DeviceAdapter(window, codeset -> {
            SharedPreferences.Editor editor = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE).edit();
            editor.putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId()).apply();
            Toast.makeText(ScanActivity.this, "נבחר מזגן!", Toast.LENGTH_LONG).show();
            finish();
        });
        rvResults.setAdapter(adapter);
        rvResults.setVisibility(View.VISIBLE);
    }

    @Override protected void onDestroy() {
        scanHandler.removeCallbacksAndMessages(null);
        super.onDestroy();
    }
}