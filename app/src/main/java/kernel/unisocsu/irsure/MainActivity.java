package kernel.unisocsu.irsure;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcCodeset;
import kernel.unisocsu.irsure.ui.DeviceAdapter;
import kernel.unisocsu.irsure.ui.DevicePickerActivity;
import kernel.unisocsu.irsure.ui.RemoteActivity;
import kernel.unisocsu.irsure.ui.ScanActivity;
import kernel.unisocsu.irsure.ui.SetupActivity;

public class MainActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    private DbHelper dbHelper;
    private DeviceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        dbHelper = DbHelper.getInstance(this);

        RecyclerView recyclerView = findViewById(R.id.recycler_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new DeviceAdapter(this);
        recyclerView.setAdapter(adapter);

        SearchView searchView = findViewById(R.id.search_devices);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                runSearch(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                runSearch(newText);
                return true;
            }
        });

        Button scanButton = findViewById(R.id.btn_auto_scan);
        scanButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, ScanActivity.class)));

        Button addButton = findViewById(R.id.btn_choose_device);
        addButton.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, DevicePickerActivity.class)));

        runSearch(null);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (dbHelper != null && adapter != null) {
            runSearch(null);
        }
    }

    private void runSearch(String query) {
        adapter.submitList(dbHelper.searchCodesets(query));
    }

    @Override
    public void onDeviceClick(AcCodeset codeset) {
        SharedPreferences prefs =
                getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE);

        prefs.edit()
                .putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId())
                .apply();

        startActivity(new Intent(this, RemoteActivity.class));
    }
}
