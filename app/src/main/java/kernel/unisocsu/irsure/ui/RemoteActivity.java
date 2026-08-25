package kernel.unisocsu.irsure.ui;

import android.content.SharedPreferences;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcCodeset;
import kernel.unisocsu.irsure.models.AcFunction;

public class RemoteActivity extends AppCompatActivity {

    private static final int MIN_TEMP = 16;
    private static final int MAX_TEMP = 30;
    private static final String[] MODES = {"AUTO", "COOL", "HEAT", "DRY", "FAN"};
    private static final String[] FAN_SPEEDS = {"AUTO", "LOW", "MED", "HIGH"};

    private DbHelper dbHelper;
    private ConsumerIrManager irManager;
    private long codesetId;

    // Current remote state.
    private boolean isOn = false;
    private int modeIndex = 0;   // index into MODES
    private int temp = 24;
    private int fanIndex = 0;    // index into FAN_SPEEDS
    private boolean swingOn = false;

    private TextView titleView;
    private TextView stateView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_remote);

        dbHelper = DbHelper.getInstance(this);
        irManager = (ConsumerIrManager) getSystemService(CONSUMER_IR_SERVICE);

        SharedPreferences prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE);
        codesetId = prefs.getLong(SetupActivity.KEY_SELECTED_CODESET_ID, -1);

        titleView = findViewById(R.id.text_remote_title);
        stateView = findViewById(R.id.text_remote_state);

        if (codesetId == -1) {
            Toast.makeText(this, R.string.remote_no_device_selected, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        AcCodeset codeset = dbHelper.getCodeset(codesetId);
        titleView.setText(codeset != null ? codeset.getDisplayLabel() : getString(R.string.remote_unknown_device));

        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, R.string.remote_no_ir_blaster, Toast.LENGTH_LONG).show();
            // Screen still works for browsing state; transmit() calls below will just no-op with a toast.
        }

        setupButtons();
        refreshStateLabel();
    }

    private void setupButtons() {
        Button btnPower = findViewById(R.id.btn_power);
        Button btnTempUp = findViewById(R.id.btn_temp_up);
        Button btnTempDown = findViewById(R.id.btn_temp_down);
        Button btnMode = findViewById(R.id.btn_mode_cycle);
        Button btnFan = findViewById(R.id.btn_fan_cycle);
        Button btnSwing = findViewById(R.id.btn_swing_toggle);
        Button btnChangeDevice = findViewById(R.id.btn_change_device);

        btnPower.setOnClickListener(v -> {
            isOn = !isOn;
            refreshStateLabel();
            sendCurrentState();
        });

        btnTempUp.setOnClickListener(v -> {
            if (temp < MAX_TEMP) temp++;
            refreshStateLabel();
            if (isOn) sendCurrentState();
        });

        btnTempDown.setOnClickListener(v -> {
            if (temp > MIN_TEMP) temp--;
            refreshStateLabel();
            if (isOn) sendCurrentState();
        });

        btnMode.setOnClickListener(v -> {
            modeIndex = (modeIndex + 1) % MODES.length;
            refreshStateLabel();
            if (isOn) sendCurrentState();
        });

        btnFan.setOnClickListener(v -> {
            fanIndex = (fanIndex + 1) % FAN_SPEEDS.length;
            refreshStateLabel();
            if (isOn) sendCurrentState();
        });

        btnSwing.setOnClickListener(v -> {
            swingOn = !swingOn;
            refreshStateLabel();
            if (isOn) sendCurrentState();
        });

        btnChangeDevice.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, DevicePickerActivity.class));
        });
    }

    private void refreshStateLabel() {
        String state = getString(R.string.remote_state_fmt,
                isOn ? getString(R.string.state_on) : getString(R.string.state_off),
                MODES[modeIndex], temp, FAN_SPEEDS[fanIndex],
                swingOn ? getString(R.string.state_on) : getString(R.string.state_off));
        stateView.setText(state);
    }

    /** Looks up the matching (or closest) AcFunction for the current state and transmits it. */
    private void sendCurrentState() {
        String power = isOn ? "ON" : "OFF";
        String mode = isOn ? MODES[modeIndex] : null;
        Integer tempParam = isOn ? temp : null;
        String fan = isOn ? FAN_SPEEDS[fanIndex] : null;
        String swing = isOn ? (swingOn ? "ON" : "OFF") : null;

        AcFunction function = dbHelper.findClosestFunction(codesetId, power, mode, tempParam, fan, swing);
        if (function == null) {
            Toast.makeText(this, R.string.remote_no_matching_code, Toast.LENGTH_SHORT).show();
            return;
        }
        transmit(function);
    }

    private void transmit(AcFunction function) {
        if (irManager == null || !irManager.hasIrEmitter()) {
            Toast.makeText(this, R.string.remote_no_ir_blaster, Toast.LENGTH_SHORT).show();
            return;
        }
        int[] pattern = function.getPattern();
        if (pattern.length == 0) {
            Toast.makeText(this, R.string.remote_empty_pattern, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            irManager.transmit(function.getFreqHz(), pattern);
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.remote_transmit_failed_fmt, e.getMessage()),
                    Toast.LENGTH_SHORT).show();
        }
    }
}
