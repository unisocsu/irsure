package kernel.unisocsu.irsure;

import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.os.Handler;
import android.view.KeyEvent;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private ConsumerIrManager irManager;
    private TextView statusText;
    private Button btnStart, btnStop;
    
    private List<IrLoader.Protocol> protocols;
    private int currentIndex = 0;
    private boolean isScanning = false;
    private Handler scanHandler = new Handler();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);
        statusText = findViewById(R.id.status_text);
        btnStart = findViewById(R.id.btn_start);
        btnStop = findViewById(R.id.btn_stop);

        // טעינת הפרוטוקולים מה-XML
        protocols = IrLoader.loadCodesFromXml(this);

        btnStart.setOnClickListener(v -> startAutoScan());
        btnStop.setOnClickListener(v -> stopAutoScan());
    }

    private void startAutoScan() {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, "לא נמצאה עינית IR במכשיר", Toast.LENGTH_LONG).show();
            return;
        }
        
        if (protocols == null || protocols.isEmpty()) {
            Toast.makeText(this, "רשימת הקודים ריקה", Toast.LENGTH_SHORT).show();
            return;
        }

        isScanning = true;
        currentIndex = 0;
        btnStart.setEnabled(false);
        btnStop.setEnabled(true);
        runScanStep();
    }

    private void runScanStep() {
        if (!isScanning || currentIndex >= protocols.size()) {
            stopAutoScan();
            return;
        }

        IrLoader.Protocol p = protocols.get(currentIndex);
        statusText.setText("בודק: " + p.name + "\n(" + (currentIndex + 1) + " מתוך " + protocols.size() + ")");
        
        // שליחה בתדר 38kHz
        irManager.transmit(38000, p.pattern);
        
        currentIndex++;
        // המתנה של 2 שניות
        scanHandler.postDelayed(this::runScanStep, 2000);
    }

    private void stopAutoScan() {
        isScanning = false;
        btnStart.setEnabled(true);
        btnStop.setEnabled(false);
        statusText.setText("הסריקה נעצרה.\nקוד אחרון שנבדק: " + currentIndex);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && isScanning) {
            stopAutoScan();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
