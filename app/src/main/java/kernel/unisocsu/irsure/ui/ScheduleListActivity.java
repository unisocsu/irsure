package kernel.unisocsu.irsure.ui;

import android.content.Intent;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;

public class ScheduleListActivity extends AppCompatActivity {

    private ListView lvScheduleList;
    private Button btnAddSchedule;
    private DbHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_schedule_list);

        dbHelper = DbHelper.getInstance(this);
        lvScheduleList = findViewById(R.id.rvScheduleList);
        btnAddSchedule = findViewById(R.id.btnAddSchedule);
        btnAddSchedule.setOnClickListener(v -> startActivity(new Intent(this, ScheduleEditorActivity.class)));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadTasks();
    }

    private void loadTasks() {
        List<String> tasks = new ArrayList<>();
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        
        Cursor c = db.rawQuery("SELECT id, time_millis, power FROM scheduled_tasks ORDER BY time_millis ASC", null);
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        
        while (c.moveToNext()) {
            long id = c.getLong(0);
            long timeMillis = c.getLong(1);
            String power = c.getString(2);
            
            String dateStr = sdf.format(new Date(timeMillis));
            tasks.add("#" + id + " [" + dateStr + "] - פעולה: " + (power.equals("ON") ? "הדלקה" : "כיבוי"));
        }
        c.close();
        
        if (tasks.isEmpty()) {
            tasks.add("אין תזמונים פעילים. לחץ למטה כדי להוסיף!");
        }
        
        lvScheduleList.setAdapter(new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, tasks));
    }
}
