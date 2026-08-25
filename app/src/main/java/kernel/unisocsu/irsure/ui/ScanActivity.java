package kernel.unisocsu.irsure;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.View;
import android.widget.*;
import androidx.appcompat.app.AppCompatActivity;
import java.util.ArrayList;
import java.util.List;

public class ScanActivity extends AppCompatActivity {
    private List<AcCodeset> allCodesets;
    private int currentIndex = 0;
    private boolean isScanning = false;
    private Handler scanHandler = new Handler();
    private ConsumerIrManager irManager;
    private DbHelper dbHelper;
    
    private TextView txtStatus;
    private ListView listViewResults; // יוצג רק כשנעצור
    private Button btnStop;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_scan);

        dbHelper = new DbHelper(this);
        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        txtStatus = findViewById(R.id.txt_status);
        btnStop = findViewById(R.id.btn_stop);
        listViewResults = findViewById(R.id.list_results);

        // טעינת רשימת השמות מה-DB (קליל ב-RAM)
        allCodesets = dbHelper.getAllCodesets(); // פונקציה שמחזירה רשימת אובייקטים בסיסיים

        findViewById(R.id.btn_start_scan).setOnClickListener(v -> startScanning());
        btnStop.setOnClickListener(v -> stopScanningAndShowResults());
    }

    private void startScanning() {
        isScanning = true;
        currentIndex = 0;
        listViewResults.setVisibility(View.GONE);
        runScanStep();
    }

    private void runScanStep() {
        if (!isScanning || currentIndex >= allCodesets.size()) return;

        AcCodeset current = allCodesets.get(currentIndex);
        txtStatus.setText("סורק דגם: " + current.name + "\n(" + (currentIndex + 1) + "/" + allCodesets.size() + ")");

        // שליפת קוד ההפעלה מה-DB ושליחה
        AcFunction powerCmd = dbHelper.getPowerOnForScanner(current.id);
        if (powerCmd != null) {
            irManager.transmit(powerCmd.freqHz, powerCmd.rawPattern);
        }

        currentIndex++;
        // מזגנים צריכים פולס ארוך, אז ניתן 2.5 שניות הפרש כדי לשמוע את ה"ביפ"
        scanHandler.postDelayed(this::runScanStep, 2500);
    }

    private void stopScanningAndShowResults() {
        isScanning = false;
        txtStatus.setText("הסריקה נעצרה. בחר את הדגם המדויק:");

        // חישוב חלון 10 השלטים
        int start = Math.max(0, currentIndex - 7); 
        int end = Math.min(allCodesets.size(), start + 10);
        
        List<AcCodeset> candidates = new ArrayList<>();
        for (int i = start; i < end; i++) {
            candidates.add(allCodesets.get(i));
        }

        // הצגה ב-ListView (כדי שהמשתמש יוכל לנסות את ה-10 הקרובים)
        DeviceAdapter adapter = new DeviceAdapter(this, candidates);
        listViewResults.setAdapter(adapter);
        listViewResults.setVisibility(View.VISIBLE);
        
        listViewResults.setOnItemClickListener((parent, view, position, id) -> {
            AcCodeset selected = candidates.get(position);
            // שמירת השלט הנבחר ומעבר למסך השלט המלא
            saveAndFinish(selected);
        });
    }

    private void saveAndFinish(AcCodeset selected) {
        getSharedPreferences("IRSURE_PREFS", MODE_PRIVATE)
            .edit().putInt("SELECTED_ID", selected.id).apply();
        // חזרה למסך השלט
        finish();
    }
}