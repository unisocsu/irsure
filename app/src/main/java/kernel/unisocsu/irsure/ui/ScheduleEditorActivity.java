package kernel.unisocsu.irsure.ui;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import java.util.Calendar;
import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;

public class ScheduleEditorActivity extends AppCompatActivity {
    private final Calendar calendar = Calendar.getInstance();
    private Spinner spinnerAction;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_editor);
        Button date = findViewById(R.id.btnDatePicker);
        Button time = findViewById(R.id.btnTimePicker);
        spinnerAction = findViewById(R.id.spinnerAction);
        Button save = findViewById(R.id.btnSave);

        String[] actions = {"Turn ON", "Turn OFF"};
        spinnerAction.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, actions));

        date.setOnClickListener(v -> showDatePicker());
        time.setOnClickListener(v -> showTimePicker());
        save.setOnClickListener(v -> saveSchedule());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (v, y, m, d) -> {
            calendar.set(Calendar.YEAR, y);
            calendar.set(Calendar.MONTH, m);
            calendar.set(Calendar.DAY_OF_MONTH, d);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (v, h, m) -> {
            calendar.set(Calendar.HOUR_OF_DAY, h);
            calendar.set(Calendar.MINUTE, m);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 0);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void saveSchedule() {
        long codesetId = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                .getLong(SetupActivity.KEY_SELECTED_CODESET_ID, -1);
        if (codesetId == -1) {
            Toast.makeText(this, "No AC selected", Toast.LENGTH_SHORT).show();
            return;
        }

        long triggerTime = calendar.getTimeInMillis();
        if (triggerTime <= System.currentTimeMillis()) {
            Toast.makeText(this, "Please choose a future time", Toast.LENGTH_SHORT).show();
            return;
        }

        String power = spinnerAction.getSelectedItemPosition() == 0 ? "ON" : "OFF";
        long taskId = DbHelper.getInstance(this).insertScheduledTask(
                codesetId, triggerTime, spinnerAction.getSelectedItemPosition(),
                power, null, null, null, null);
        if (taskId <= 0) {
            Toast.makeText(this, "Could not save schedule", Toast.LENGTH_SHORT).show();
            return;
        }

        Intent intent = new Intent(this, ScheduledActionReceiver.class);
        intent.putExtra(ScheduledActionReceiver.EXTRA_TASK_ID, taskId);
        intent.putExtra(ScheduledActionReceiver.EXTRA_CODESET_ID, codesetId);
        intent.putExtra(ScheduledActionReceiver.EXTRA_POWER, power);

        PendingIntent pending = PendingIntent.getBroadcast(
                this, (int) taskId, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | (Build.VERSION.SDK_INT >= 23 ? PendingIntent.FLAG_IMMUTABLE : 0));

        AlarmManager alarm = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarm != null) {
            if (Build.VERSION.SDK_INT >= 23) alarm.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerTime, pending);
            else alarm.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pending);
        }

        Toast.makeText(this, "Schedule saved", Toast.LENGTH_SHORT).show();
        finish();
    }
}