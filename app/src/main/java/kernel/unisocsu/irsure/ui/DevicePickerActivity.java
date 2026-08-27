package kernel.unisocsu.irsure.ui;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcCodeset;

/**
 * Two visual modes in one existing activity:
 *  - picker mode: manual model selection + Auto Scan + search
 *  - saved mode: saved remotes + rename + Add AC
 */
public class DevicePickerActivity extends AppCompatActivity implements DeviceAdapter.OnDeviceClickListener {

    public static final String EXTRA_MODE = "device_picker_mode";
    public static final int MODE_PICKER = 0;
    public static final int MODE_SAVED = 1;

    private static final String SAVED_PREFS = "saved_ac_devices";
    private static final String KEY_IDS = "ids";
    private static final String NAME_PREFIX = "name_";

    private DbHelper dbHelper;
    private DeviceAdapter adapter;
    private RecyclerView recyclerView;
    private SearchView searchView;
    private final Handler searchHandler = new Handler();
    private Runnable pendingSearch;
    private List<AcCodeset> allCodesets = new ArrayList<>();

    public static Set<String> getSavedIds(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(SAVED_PREFS, Context.MODE_PRIVATE);
        return new TreeSet<>(prefs.getStringSet(KEY_IDS, Collections.<String>emptySet()));
    }

    public static void saveDevice(Context context, AcCodeset codeset, String displayName) {
        SharedPreferences prefs = context.getSharedPreferences(SAVED_PREFS, Context.MODE_PRIVATE);
        Set<String> ids = new TreeSet<>(getSavedIds(context));
        String id = String.valueOf(codeset.getId());
        String safeName = displayName;
        if (safeName == null || safeName.trim().isEmpty()) {
            safeName = codeset.getDisplayLabel();
        }

        prefs.edit()
                .putStringSet(KEY_IDS, idsWith(ids, id))
                .putString(NAME_PREFIX + id, safeName)
                .apply();
    }

    private static Set<String> idsWith(Set<String> current, String id) {
        Set<String> result = new TreeSet<>(current);
        result.add(id);
        return result;
    }

    private static String getSavedName(Context context, AcCodeset codeset) {
        SharedPreferences prefs = context.getSharedPreferences(SAVED_PREFS, Context.MODE_PRIVATE);
        String name = prefs.getString(NAME_PREFIX + codeset.getId(), null);
        return (name == null || name.trim().isEmpty())
                ? codeset.getDisplayLabel()
                : name;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        int mode = getIntent().getIntExtra(EXTRA_MODE, MODE_PICKER);
        if (mode == MODE_SAVED) {
            showSavedScreen();
        } else {
            showPickerScreen();
        }
    }

    private void showPickerScreen() {
        setContentView(R.layout.activity_picker);

        dbHelper = DbHelper.getInstance(this);
        recyclerView = findViewById(R.id.recycler_devices);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);

        adapter = new DeviceAdapter(this);
        recyclerView.setAdapter(adapter);

        Button autoScanButton = findViewById(R.id.btn_auto_scan);
        autoScanButton.setOnClickListener(v ->
                startActivity(new Intent(DevicePickerActivity.this, ScanActivity.class)));

        searchView = findViewById(R.id.search_devices);
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                filterNow(query);
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                scheduleFilter(newText);
                return true;
            }
        });

        allCodesets = dbHelper.searchCodesets(null);
        adapter.submitList(allCodesets);
    }

    private void filterNow(String query) {
        if (query == null || query.trim().isEmpty()) {
            adapter.submitList(allCodesets);
            return;
        }

        String needle = query.trim().toLowerCase();
        List<AcCodeset> filtered = new ArrayList<>();
        for (AcCodeset codeset : allCodesets) {
            if (contains(codeset.getName(), needle)
                    || contains(codeset.getBrands(), needle)
                    || contains(codeset.getModel(), needle)
                    || contains(codeset.getRegion(), needle)) {
                filtered.add(codeset);
            }
        }
        adapter.submitList(filtered);
    }

    private boolean contains(String value, String needle) {
        return value != null && value.toLowerCase().contains(needle);
    }

    private void scheduleFilter(final String query) {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        pendingSearch = () -> filterNow(query);
        searchHandler.postDelayed(pendingSearch, 120L);
    }

    private void showSavedScreen() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16), dp(16), dp(16), dp(16));

        TextView title = new TextView(this);
        title.setText("המזגנים שלי");
        title.setTextSize(24f);
        title.setGravity(Gravity.CENTER);
        root.addView(title, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        Button addButton = new Button(this);
        addButton.setText("הוספת מזגן");
        addButton.setOnClickListener(v -> {
            Intent intent = new Intent(this, DevicePickerActivity.class);
            intent.putExtra(EXTRA_MODE, MODE_PICKER);
            startActivity(intent);
        });
        root.addView(addButton, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));

        recyclerView = new RecyclerView(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setHasFixedSize(true);
        recyclerView.setItemAnimator(null);
        root.addView(recyclerView, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        recyclerView.setAdapter(new SavedAdapter());
    }

    private void renameDevice(final AcCodeset codeset, final SavedAdapter savedAdapter) {
        final EditText input = new EditText(this);
        input.setSingleLine(true);
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_CAP_SENTENCES);
        input.setText(getSavedName(this, codeset));
        input.setSelection(input.length());

        new AlertDialog.Builder(this)
                .setTitle("שינוי שם")
                .setView(input)
                .setNegativeButton("ביטול", null)
                .setPositiveButton("שמירה", (dialog, which) -> {
                    String name = input.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, "השם לא יכול להיות ריק", Toast.LENGTH_SHORT).show();
                        return;
                    }
                    getSharedPreferences(SAVED_PREFS, MODE_PRIVATE)
                            .edit()
                            .putString(NAME_PREFIX + codeset.getId(), name)
                            .apply();
                    savedAdapter.reload();
                })
                .show();
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + 0.5f);
    }

    @Override
    public void onDeviceClick(AcCodeset codeset) {
        saveDevice(this, codeset, codeset.getDisplayLabel());
        getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                .edit()
                .putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId())
                .apply();

        startActivity(new Intent(this, RemoteActivity.class));
        finish();
    }

    private class SavedAdapter extends RecyclerView.Adapter<SavedAdapter.Holder> {
        private final List<AcCodeset> items = new ArrayList<>();

        SavedAdapter() {
            reload();
        }

        void reload() {
            items.clear();
            DbHelper db = DbHelper.getInstance(DevicePickerActivity.this);
            for (String idString : getSavedIds(DevicePickerActivity.this)) {
                try {
                    AcCodeset codeset = db.getCodeset(Long.parseLong(idString));
                    if (codeset != null) items.add(codeset);
                } catch (NumberFormatException ignored) {
                    // Ignore malformed ids.
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LinearLayout row = new LinearLayout(parent.getContext());
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setGravity(Gravity.CENTER_VERTICAL);
            row.setPadding(dp(8), dp(8), dp(8), dp(8));

            LinearLayout textBox = new LinearLayout(parent.getContext());
            textBox.setOrientation(LinearLayout.VERTICAL);
            row.addView(textBox, new LinearLayout.LayoutParams(0,
                    ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

            TextView name = new TextView(parent.getContext());
            name.setTextSize(17f);
            textBox.addView(name);

            TextView details = new TextView(parent.getContext());
            details.setTextSize(13f);
            details.setTextColor(0xFF888888);
            textBox.addView(details);

            Button rename = new Button(parent.getContext());
            rename.setText("שנה שם");
            row.addView(rename, new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.WRAP_CONTENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            return new Holder(row, name, details, rename);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            final AcCodeset codeset = items.get(position);
            holder.name.setText(getSavedName(DevicePickerActivity.this, codeset));
            holder.details.setText(codeset.getDisplayLabel());

            holder.itemView.setOnClickListener(v -> {
                getSharedPreferences(SetupActivity.PREFS_NAME, MODE_PRIVATE)
                        .edit()
                        .putLong(SetupActivity.KEY_SELECTED_CODESET_ID, codeset.getId())
                        .apply();
                startActivity(new Intent(DevicePickerActivity.this, RemoteActivity.class));
            });

            holder.rename.setOnClickListener(v -> renameDevice(codeset, this));
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        class Holder extends RecyclerView.ViewHolder {
            final TextView name;
            final TextView details;
            final Button rename;

            Holder(View itemView, TextView name, TextView details, Button rename) {
                super(itemView);
                this.name = name;
                this.details = details;
                this.rename = rename;
            }
        }
    }

    @Override
    protected void onDestroy() {
        if (pendingSearch != null) {
            searchHandler.removeCallbacks(pendingSearch);
        }
        super.onDestroy();
    }
}
