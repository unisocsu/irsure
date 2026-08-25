package kernel.unisocsu.irsure.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DataImporter;

/**
 * First screen shown on every launch. If the SQLite DB is already populated
 * (checked via DbHelper.hasData()) this just forwards immediately; otherwise
 * it runs the one-time ZIP/XML -> SQLite import with a progress bar.
 */
public class SetupActivity extends AppCompatActivity {

    public static final String PREFS_NAME = "ac_remote_prefs";
    public static final String KEY_SELECTED_CODESET_ID = "selected_codeset_id";

    private ProgressBar progressBar;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        progressBar = findViewById(R.id.progress_setup);
        statusText = findViewById(R.id.text_setup_status);

        new ImportTask().execute();
    }

    private void goToNextScreen() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        long savedCodesetId = prefs.getLong(KEY_SELECTED_CODESET_ID, -1);

        Intent intent = (savedCodesetId != -1)
                ? new Intent(this, RemoteActivity.class)
                : new Intent(this, DevicePickerActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Runs DataImporter off the main thread. AsyncTask is used here (not a newer
     * Executor/LiveData setup) to keep the sample compatible with API 19 with no
     * extra library dependencies.
     */
    private class ImportTask extends AsyncTask<Void, String, Exception> {

        private int totalCodesets = 0;
        private int totalFunctions = 0;
        private boolean alreadyImported = false;

        @Override
        protected Exception doInBackground(Void... voids) {
            final Exception[] errorHolder = new Exception[1];
            DataImporter.importIfNeeded(SetupActivity.this, new DataImporter.ProgressListener() {
                @Override
                public void onProgress(int codesetsSoFar, String currentCodesetName) {
                    publishProgress(getString(R.string.setup_importing_fmt, codesetsSoFar, currentCodesetName));
                }

                @Override
                public void onFinished(int totalCs, int totalFn) {
                    if (totalCs == -1) {
                        alreadyImported = true;
                    } else {
                        totalCodesets = totalCs;
                        totalFunctions = totalFn;
                    }
                }

                @Override
                public void onError(Exception e) {
                    errorHolder[0] = e;
                }
            });
            return errorHolder[0];
        }

        @Override
        protected void onProgressUpdate(String... values) {
            statusText.setText(values[0]);
        }

        @Override
        protected void onPostExecute(Exception error) {
            progressBar.setVisibility(android.view.View.GONE);
            if (error != null) {
                statusText.setText(getString(R.string.setup_error_fmt, error.getMessage()));
                Toast.makeText(SetupActivity.this, R.string.setup_error_toast, Toast.LENGTH_LONG).show();
                return;
            }
            if (!alreadyImported) {
                Toast.makeText(SetupActivity.this,
                        getString(R.string.setup_done_fmt, totalCodesets, totalFunctions),
                        Toast.LENGTH_LONG).show();
            }
            goToNextScreen();
        }
    }
}
