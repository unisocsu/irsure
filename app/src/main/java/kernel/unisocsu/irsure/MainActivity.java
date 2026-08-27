package kernel.unisocsu.irsure;

import android.content.Intent;
import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import kernel.unisocsu.irsure.ui.DevicePickerActivity;

/** Entry router: show saved remotes when available, otherwise show the picker. */
public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        boolean hasSaved = !DevicePickerActivity.getSavedIds(this).isEmpty();
        Intent intent = new Intent(this, DevicePickerActivity.class);
        intent.putExtra(DevicePickerActivity.EXTRA_MODE,
                hasSaved ? DevicePickerActivity.MODE_SAVED : DevicePickerActivity.MODE_PICKER);
        startActivity(intent);
        finish();
    }
}
