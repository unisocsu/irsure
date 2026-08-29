package kernel.unisocsu.irsure.ui;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Intent;
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

    private Calendar calendar = Calendar.getInstance();
    private Spinner spinnerAction;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_editor);

        Button btnDatePicker = findViewById(R.id.btnDatePicker);
        Button btnTimePicker = findViewById(R.id.btnTimePicker);
        spinnerAction = findViewById(R.id.spinnerAction);
        Button btnSave = findViewById(R.id.btnSave);

        String[] actions = {"הדלק", "כבה"};
        spinnerAction.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, actions));

        btnDatePicker.setOnClickListener(v -> showDatePicker());
        btnTimePicker.setOnClickListener(v -> showTimePicker());
        btnSave.setOnClickListener(v -> saveSchedule());
    }

    private void showDatePicker() {
        new DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            calendar.set(Calendar.YEAR, year);
            calendar.set(Calendar.MONTH, month);
            calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showTimePicker() {
        new TimePickerDialog(this, (view, hourOfDay, minute) -> {
            calendar.set(Calendar.HOUR_OF_DAY, hourOfDay);
            calendar.set(Calendar.MINUTE, minute);
        }, calendar.get(Calendar.HOUR_OF_DAY), calendar.get(Calendar.MINUTE), true).show();
    }

    private void saveSchedule() {
        long codesetId = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                            .getLong(SetupActivity.KEY_SELECTED_CODESET_ID, -1);
        
        if (codesetId == -1) {
            Toast.makeText(this, "לא נבחר מזגן", Toast.LENGTH_SHORT).show();
            return;
        }

        String power = spinnerAction.getSelectedItemPosition() == 0 ? "ON" : "OFF";
        long triggerTime = calendar.getTimeInMillis();
        
        // Insert into database and get unique ID
        long taskId = DbHelper.getInstance(this).insertScheduledTask(
                codesetId, 
                triggerTime, 
                spinnerAction.getSelectedItemPosition(), 
                power, null, null, null, null);
                
        // Set dynamic AlarmManager using the unique taskId
        Intent intent = new Intent(this, ScheduledActionReceiver.class);
        intent.putExtra(ScheduledActionReceiver.EXTRA_CODESET_ID, codesetId);
        intent.putExtra(ScheduledActionReceiver.EXTRA_POWER, power);
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this, 
                (int) taskId, // Unique request code to prevent overwriting
                intent, 
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
                
        AlarmManager alarmManager = (AlarmManager) getSystemService(ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, triggerTime, pendingIntent);
        }

        Toast.makeText(this, "התזמון נשמר והופעל בהצלחה!", Toast.LENGTH_SHORT).show();
        finish();
    }
}
