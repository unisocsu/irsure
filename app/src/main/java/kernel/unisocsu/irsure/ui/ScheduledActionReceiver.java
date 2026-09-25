package kernel.unisocsu.irsure.ui;

import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.hardware.ConsumerIrManager;
import android.os.Build;
import android.widget.Toast;
import kernel.unisocsu.irsure.R;
import kernel.unisocsu.irsure.db.DbHelper;
import kernel.unisocsu.irsure.models.AcFunction;

public class ScheduledActionReceiver extends BroadcastReceiver {
    public static final String EXTRA_TASK_ID = "task_id";
    public static final String EXTRA_CODESET_ID = "codeset_id";
    public static final String EXTRA_POWER = "power";
    public static final String EXTRA_MODE = "mode";
    public static final String EXTRA_TEMP = "temp";
    public static final String EXTRA_FAN = "fan";
    public static final String EXTRA_SWING = "swing";

    @Override public void onReceive(Context context, Intent intent) {
        long taskId = intent.getLongExtra(EXTRA_TASK_ID, -1);
        long codesetId = intent.getLongExtra(EXTRA_CODESET_ID, -1);
        if (codesetId == -1) return;

        String power = intent.getStringExtra(EXTRA_POWER);
        String mode = intent.getStringExtra(EXTRA_MODE);
        String fan = intent.getStringExtra(EXTRA_FAN);
        String swing = intent.getStringExtra(EXTRA_SWING);
        int tempValue = intent.getIntExtra(EXTRA_TEMP, -1);
        Integer temp = tempValue >= 0 ? tempValue : null;

        DbHelper db = DbHelper.getInstance(context);
        AcFunction function = db.findClosestFunction(codesetId, power, mode, temp, fan, swing);
        if (function == null) {
            Toast.makeText(context, R.string.scheduled_no_matching_code, Toast.LENGTH_SHORT).show();
            return;
        }

        ConsumerIrManager ir = (ConsumerIrManager) context.getSystemService(Context.CONSUMER_IR_SERVICE);
        if (ir == null || !ir.hasIrEmitter()) {
            Toast.makeText(context, R.string.remote_no_ir_blaster, Toast.LENGTH_SHORT).show();
            return;
        }

        try {
            int[] pattern = function.getPattern();
            if (pattern.length == 0) return;
            ir.transmit(function.getFreqHz(), pattern);
            Toast.makeText(context, R.string.scheduled_action_sent, Toast.LENGTH_SHORT).show();
            if (taskId > 0) db.deleteScheduledTask(taskId);
        } catch (Exception e) {
            Toast.makeText(context, R.string.scheduled_action_failed, Toast.LENGTH_SHORT).show();
        }
    }
}