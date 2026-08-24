package kernel.unisocsu.irsure;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    private ConsumerIrManager irManager;
    private TextView statusText;
    private Button btnStart, btnStop;
    
    private int currentPatternIndex = 0;
    private boolean isScanning = false;
    private Handler scanHandler = new Handler();

    // רשימת פרוטוקולים (כאן תכניס את ה-200+ שלך)
    private int[][] allAcPatterns = {
        {9000, 4500, 560, 1690, 560, 560}, // NEC Example
        {8000, 4000, 440, 1200, 440, 440}, // Samsung Example
        {3000, 3000, 500, 1000, 500, 1000}  // LG Example
        // ... (הוסף עוד כאן)
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        statusText = findViewById(R.id.status_text);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        btnStart.setOnClickListener(v -> startAutoScan());
        btnStop.setOnClickListener(v -> stopAutoScan());
    }

    private void startAutoScan() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "No IR Blaster found!", Toast.LENGTH_LONG).show();
            return;
        }
        
        isScanning = true;
        currentPatternIndex = 0;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        runScanStep();
    }

    private void runScanStep() {
        if (!isScanning || currentPatternIndex >= allAcPatterns.length) {
            stopAutoScan();
            return;
        }

        statusText.setText("Scanning Code #" + (currentPatternIndex + 1));
        
        // שליחת הפקודה ב-38kHz
        irManager.transmit(38000, allAcPatterns[currentPatternIndex]);

        currentPatternIndex++;

        // המתנה של 2 שניות בין פקודות כדי לתת למזגן זמן להגיב
        scanHandler.postDelayed(this::runScanStep, 2000);
    }

    private void stopAutoScan() {
        isScanning = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        
        if (currentPatternIndex > 0) {
            statusText.setText("Match Found! Last Code: " + currentPatternIndex);
            Toast.makeText(this, "Protocol #" + currentPatternIndex + " Saved", Toast.LENGTH_LONG).show();
        } else {
            statusText.setText("Scan Stopped");
        }
    }
}