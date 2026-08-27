package kernel.unisocsu.irsure.ui;

import android.content.Context;
import android.content.SharedPreferences;
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
    private Handler scanHandler = new Handler();

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

        allCodesets = dbHelper.searchCodesets(null);

        btnToggleScan.setOnClickListener(v -> {
            if (!isScanning) {
                startScanning();
            } else {
                isScanning = false;
            }
        });

        btnStop.setOnClickListener(v -> stopAndShowChoices());
    }

    private void startScanning() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "לא נמצאה עינית IR", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        btnStop.setVisibility(View.VISIBLE);
        runStep();
    }

    private void runStep() {
        if (!isScanning || currentIndex >= allCodesets.size()) {
            isScanning = false;
            return;
        }

        AcCodeset current = allCodesets.get(currentIndex);

        tvStatus.setText("בודק: " + current.getBrands());
        tvProgress.setText(
                (currentIndex + 1) + " / " + allCodesets.size()
        );

        // שליחת Power ON
        AcFunction f = dbHelper.findFunction(
                current.getId(),
                "ON",
                null,
                null,
                null,
                null
        );

        if (f != null && f.getPattern() != null && f.getPattern().length > 0) {
            irManager.transmit(
                    f.getFreqHz(),
                    f.getPattern()
            );
        }

        currentIndex++;

        scanHandler.postDelayed(
                this::runStep,
                2500
        );
    }

    private void stopAndShowChoices() {
        isScanning = false;
        btnStop.setVisibility(View.GONE);

        // יצירת חלון של עד 10 תוצאות סביב המיקום הנוכחי
        int start = Math.max(0, currentIndex - 8);

        List<AcCodeset> window = new ArrayList<>();

        for (int i = start;
             i < Math.min(allCodesets.size(), start + 10);
             i++) {
            window.add(allCodesets.get(i));
        }

        DeviceAdapter adapter = new DeviceAdapter(window, codeset -> {

            SharedPreferences.Editor editor =
                    getSharedPreferences(
                            SetupActivity.PREFS_NAME,
                            MODE_PRIVATE
                    ).edit();

            editor.putLong(
                    SetupActivity.KEY_SELECTED_CODESET_ID,
                    codeset.getId()
            );

            editor.apply();

            Toast.makeText(
                    ScanActivity.this,
                    "השלט נשמר!",
                    Toast.LENGTH_SHORT
            ).show();

            finish();
        });

        rvResults.setAdapter(adapter);
        rvResults.setVisibility(View.VISIBLE);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        scanHandler.removeCallbacksAndMessages(null);
    }
}