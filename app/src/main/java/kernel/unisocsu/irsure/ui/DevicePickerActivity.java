package kernel.unisocsu.irsure.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcCodeset;

public class DevicePickerActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    private DbHelper dbHelper;
    private DeviceAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_picker);

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

        runSearch(null); // show all 226 on first load
    }

    private void runSearch(String query) {
        // For 226 rows a plain synchronous query is fast enough to run on the main
        // thread; move to a background thread/AsyncTask if this list grows a lot.
        adapter.submitList(dbHelper.searchCodesets(query));
    }

    @Override
    public void onDeviceClick(AcCodeset codeset) {
        SharedPreferences prefs = getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE);
        prefs.edit()
                .putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId())
                .apply();

        startActivity(new Intent(this, RemoteActivity.class));
        finish();
    }
}
