package kernel.unisocsu.irsure.ui;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import kernel.unisocsu.irsure.MainActivity;
import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DataImporter;

/** First-run database import screen. */
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

    private void goToMainRouter() {
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

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
                    publishProgress(getString(
                            R.string.setup_importing_fmt,
                            codesetsSoFar,
                            currentCodesetName));
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
            progressBar.setVisibility(View.GONE);

            if (error != null) {
                statusText.setText(getString(R.string.setup_error_fmt, error.getMessage()));
                Toast.makeText(
                        SetupActivity.this,
                        R.string.setup_error_toast,
                        Toast.LENGTH_LONG).show();
                return;
            }

            if (!alreadyImported) {
                Toast.makeText(
                        SetupActivity.this,
                        getString(R.string.setup_done_fmt, totalCodesets, totalFunctions),
                        Toast.LENGTH_LONG).show();
            }

            goToMainRouter();
        }
    }
}
