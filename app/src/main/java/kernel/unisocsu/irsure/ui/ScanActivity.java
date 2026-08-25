package kernel.unisocsu.irsure.ui;

import android.content.Context;
import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
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
    private TextView tvStatus;
    private ListView lvResults;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        dbHelper = DbHelper.getInstance(this);
        irManager = (ConsumerIrManager) getSystemService(Context.CONSUMER_IR_SERVICE);
        tvStatus = findViewById(R.id.tvStatus);
        lvResults = findViewById(R.id.lvResults);
        
        allCodesets = dbHelper.searchCodesets(null);

        findViewById(R.id.btnStartScan).setOnClickListener(v -> startScanning());
        findViewById(R.id.btnStopScan).setOnClickListener(v -> stopAndShowChoices());
    }

    private void startScanning() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "אין עינית IR", Toast.LENGTH_SHORT).show();
            return;
        }
        isScanning = true;
        currentIndex = 0;
        runStep();
    }

    private void runStep() {
        if (!isScanning || currentIndex >= allCodesets.size()) return;
        
        AcCodeset codeset = allCodesets.get(currentIndex);
        tvStatus.setText("בודק קוד " + (currentIndex + 1) + " מתוך " + allCodesets.size());

        // שליחת Power ON
        AcFunction f = dbHelper.findFunction(codeset.getId(), "ON", null, null, null, null);
        if (f != null) {
            irManager.transmit(f.getFreq(), f.getRawPattern());
        }

        currentIndex++;
        scanHandler.postDelayed(this::runStep, 2500);
    }

    private void stopAndShowChoices() {
        isScanning = false;
        // לקיחת חלון של 10 (לרוב לוקח למשתמש זמן להגיב, אז לוקחים קצת אחורה)
        int start = Math.max(0, currentIndex - 8);
        int end = Math.min(allCodesets.size(), start + 10);
        
        List<AcCodeset> window = new ArrayList<>();
        for (int i = start; i < end; i++) window.add(allCodesets.get(i));

        DeviceAdapter adapter = new DeviceAdapter(this, window);
        lvResults.setAdapter(adapter);
        lvResults.setVisibility(View.VISIBLE);

        lvResults.setOnItemClickListener((p, v, pos, id) -> {
            AcCodeset selected = window.get(pos);
            SharedPreferences.Editor editor = getSharedPreferences("IR_PREFS", MODE_PRIVATE).edit();
            editor.putLong("SELECTED_ID", selected.getId());
            editor.apply();
            Toast.makeText(this, "השלט נשמר!", Toast.LENGTH_SHORT).show();
            finish();
        });
    }
}