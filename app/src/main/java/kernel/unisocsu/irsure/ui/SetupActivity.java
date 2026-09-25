package kernel.unisocsu.irsure.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import kernel.unisocsu.irsure.MainActivity;
import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DataImporter;
import kernel.unisocsu.irsure.db.DbHelper;

public class SetupActivity extends AppCompatActivity {
    public static final String PREFS_NAME = "ac_remote_prefs";
    public static final String KEY_SELECTED_CODESET_ID = "selected_codeset_id";
    private ProgressBar progressBar;
    private TextView statusText;
    private Button retryButton;

    @Override protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        progressBar = findViewById(R.id.progress_setup);
        statusText = findViewById(R.id.text_setup_status);
        retryButton = findViewById(R.id.btn_setup_retry);
        retryButton.setOnClickListener(v -> startImport());
        startImport();
    }

    private void startImport() {
        retryButton.setVisibility(View.GONE);
        progressBar.setVisibility(View.VISIBLE);
        statusText.setText(R.string.setup_initial_status);
        new ImportTask().execute();
    }

    private void goToMainRouter() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    private class ImportTask extends AsyncTask<Void, String, Exception> {
        private int totalCodesets;
        private int totalFunctions;
        private boolean alreadyImported;

        @Override protected Exception doInBackground(Void... ignored) {
            final Exception[] error = new Exception[1];
            try {
                DataImporter.importIfNeeded(SetupActivity.this, new DataImporter.ProgressListener() {
                    @Override public void onProgress(int count, String name) {
                        publishProgress(getString(R.string.setup_importing_fmt, count, name));
                    }
                    @Override public void onFinished(int codesets, int functions) {
                        alreadyImported = codesets == -1;
                        totalCodesets = codesets;
                        totalFunctions = functions;
                    }
                    @Override public void onError(Exception e) { error[0] = e; }
                });
            } catch (Exception e) {
                error[0] = e;
            }
            return error[0];
        }

        @Override protected void onProgressUpdate(String... values) {
            if (!isFinishing() && values.length > 0) statusText.setText(values[0]);
        }

        @Override protected void onPostExecute(Exception error) {
            progressBar.setVisibility(View.GONE);
            if (error != null) {
                statusText.setText(getString(R.string.setup_error_fmt,
                        error.getMessage() == null ? "Unknown error" : error.getMessage()));
                retryButton.setVisibility(View.VISIBLE);
                Toast.makeText(SetupActivity.this, R.string.setup_error_toast, Toast.LENGTH_LONG).show();
                return;
            }

            DbHelper db = DbHelper.getInstance(SetupActivity.this);
            if (!db.hasData()) {
                statusText.setText(R.string.setup_error_toast);
                retryButton.setVisibility(View.VISIBLE);
                return;
            }

            if (!alreadyImported) {
                Toast.makeText(SetupActivity.this,
                        getString(R.string.setup_done_fmt, totalCodesets, totalFunctions),
                        Toast.LENGTH_LONG).show();
            }
            goToMainRouter();
        }
    }
}