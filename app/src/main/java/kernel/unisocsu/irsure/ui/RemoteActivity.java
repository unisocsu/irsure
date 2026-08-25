package kernel.unisocsu.irsure.ui;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.hardware.ConsumerIrManager;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
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
    private static final int SCHEDULE_REQUEST_CODE = 9142;

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
        Button btnSchedule = findViewById(R.id.btn_schedule);

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

        btnSchedule.setOnClickListener(v -> showScheduleDialog());

        btnChangeDevice.setOnClickListener(v -> {
            startActivity(new android.content.Intent(this, DevicePickerActivity.class));
        });
    }

    private void showScheduleDialog() {
        final View view = getLayoutInflater().inflate(R.layout.dialog_schedule, null);
        final Spinner actionSpinner = view.findViewById(R.id.spinner_schedule_action);
        final Spinner unitSpinner = view.findViewById(R.id.spinner_schedule_unit);
        final EditText delayEdit = view.findViewById(R.id.edit_schedule_delay);

        String[] actions = {
                getString(R.string.schedule_action_current),
                getString(R.string.schedule_action_power_on),
                getString(R.string.schedule_action_power_off)
        };
        String[] units = {
                getString(R.string.schedule_unit_minutes),
                getString(R.string.schedule_unit_hours)
        };

        actionSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, actions));
        unitSpinner.setAdapter(new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_dropdown_item, units));

        new AlertDialog.Builder(this)
                .setTitle(R.string.schedule_title)
                .setView(view)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.schedule_confirm, (dialog, which) -> {
                    scheduleAction(actionSpinner.getSelectedItemPosition(),
                            unitSpinner.getSelectedItemPosition(), delayEdit.getText().toString());
                })
                .setNeutralButton(R.string.schedule_cancel_existing, (dialog, which) -> cancelScheduledAction())
                .show();
    }

    private void scheduleAction(int action, int unit, String delayText) {
        long delay;
        try {
            delay = Long.parseLong(delayText.trim());
        } catch (Exception e) {
            Toast.makeText(this, R.string.schedule_invalid_delay, Toast.LENGTH_SHORT).show();
            return;
        }
        if (delay <= 0) {
            Toast.makeText(this, R.string.schedule_invalid_delay, Toast.LENGTH_SHORT).show();
            return;
        }

        long delayMillis = unit == 0 ? delay * 60L * 1000L : delay * 60L * 60L * 1000L;

        String power = isOn ? "ON" : "OFF";
        String mode = isOn ? MODES[modeIndex] : null;
        Integer tempValue = isOn ? temp : null;
        String fan = isOn ? FAN_SPEEDS[fanIndex] : null;
        String swing = isOn ? (swingOn ? "ON" : "OFF") : null;

        if (action == 1) {
            power = "ON";
            mode = MODES[modeIndex];
            tempValue = temp;
            fan = FAN_SPEEDS[fanIndex];
            swing = swingOn ? "ON" : "OFF";
        } else if (action == 2) {
            power = "OFF";
            mode = null;
            tempValue = null;
            fan = null;
            swing = null;
        }

        Intent intent = new Intent(this, ScheduledActionReceiver.class);
        intent.putExtra(ScheduledActionReceiver.EXTRA_CODESET_ID, codesetId);
        intent.putExtra(ScheduledActionReceiver.EXTRA_POWER, power);
        if (mode != null) intent.putExtra(ScheduledActionReceiver.EXTRA_MODE, mode);
        if (tempValue != null) intent.putExtra(ScheduledActionReceiver.EXTRA_TEMP, tempValue);
        if (fan != null) intent.putExtra(ScheduledActionReceiver.EXTRA_FAN, fan);
        if (swing != null) intent.putExtra(ScheduledActionReceiver.EXTRA_SWING, swing);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, SCHEDULE_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + delayMillis, pendingIntent);
            Toast.makeText(this, getString(R.string.schedule_set_fmt, formatDelay(delay, unit)), Toast.LENGTH_LONG).show();
        }
    }

    private String formatDelay(long delay, int unit) {
        if (unit == 0) return delay + " " + getString(R.string.schedule_unit_minutes);
        return delay + " " + getString(R.string.schedule_unit_hours);
    }

    private void cancelScheduledAction() {
        Intent intent = new Intent(this, ScheduledActionReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, SCHEDULE_REQUEST_CODE, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) alarmManager.cancel(pendingIntent);
        pendingIntent.cancel();
        Toast.makeText(this, R.string.schedule_cancelled, Toast.LENGTH_SHORT).show();
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
