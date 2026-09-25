package kernel.unisocsu.irsure.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteStatement;
import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.irsure.models.AcCodeset;
import kernel.unisocsu.irsure.models.AcFunction;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ac_codes.db";
    private static final int DB_VERSION = 2;
    private static DbHelper instance;

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) instance = new DbHelper(context.getApplicationContext());
        return instance;
    }
    private DbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE codesets (id INTEGER PRIMARY KEY, name TEXT, brands TEXT, model TEXT, region TEXT)");
        db.execSQL("CREATE TABLE functions (id INTEGER PRIMARY KEY, codeset_id INTEGER, func_name TEXT, power TEXT, mode TEXT, temp INTEGER, fan TEXT, swing TEXT, freq INTEGER, pattern TEXT)");
        db.execSQL("CREATE TABLE scheduled_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, codeset_id INTEGER, time_millis INTEGER, action_type INTEGER, power TEXT, mode TEXT, temp INTEGER, fan TEXT, swing TEXT, is_enabled INTEGER)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        if (oldV < 2) db.execSQL("CREATE TABLE scheduled_tasks (id INTEGER PRIMARY KEY AUTOINCREMENT, codeset_id INTEGER, time_millis INTEGER, action_type INTEGER, power TEXT, mode TEXT, temp INTEGER, fan TEXT, swing TEXT, is_enabled INTEGER)");
    }

    public boolean hasData() {
        Cursor c = getReadableDatabase().rawQuery("SELECT 1 FROM codesets LIMIT 1", null);
        try { return c.moveToFirst(); } finally { c.close(); }
    }

    public SQLiteStatement compileCodesetInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO codesets (name, brands, model, region) VALUES (?, ?, ?, ?)");
    }
    public SQLiteStatement compileFunctionInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO functions (codeset_id, func_name, power, mode, temp, fan, swing, freq, pattern) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
    }

    public AcCodeset getCodeset(long id) {
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM codesets WHERE id=?", new String[]{String.valueOf(id)});
        try {
            if (!c.moveToFirst()) return null;
            return new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
        } finally { c.close(); }
    }

    public List<AcCodeset> searchCodesets(String query) {
        List<AcCodeset> list = new ArrayList<>();
        Cursor c = getReadableDatabase().rawQuery("SELECT * FROM codesets", null);
        try {
            while (c.moveToNext()) list.add(new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
        } finally { c.close(); }
        return list;
    }

    public AcFunction findFunction(long codesetId, String power, String mode, Integer temp, String fan, String swing) {
        Cursor c = getReadableDatabase().rawQuery(
                "SELECT * FROM functions WHERE codeset_id=? AND power=? AND (mode=? OR mode IS NULL) AND (temp=? OR temp IS NULL) AND (fan=? OR fan IS NULL) AND (swing=? OR swing IS NULL)",
                new String[]{String.valueOf(codesetId), power, mode, String.valueOf(temp), fan, swing});
        try {
            if (!c.moveToFirst()) return null;
            return new AcFunction(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getInt(5), c.getString(6), c.getString(7), c.getInt(8), c.getString(9));
        } finally { c.close(); }
    }
    public AcFunction findClosestFunction(long codesetId, String power, String mode, Integer temp, String fan, String swing) {
        return findFunction(codesetId, power, mode, temp, fan, swing);
    }

    public long insertScheduledTask(long codesetId, long timeMillis, int actionType, String power, String mode, Integer temp, String fan, String swing) {
        ContentValues values = new ContentValues();
        values.put("codeset_id", codesetId);
        values.put("time_millis", timeMillis);
        values.put("action_type", actionType);
        values.put("power", power);
        if (mode != null) values.put("mode", mode); else values.putNull("mode");
        if (temp != null) values.put("temp", temp); else values.putNull("temp");
        if (fan != null) values.put("fan", fan); else values.putNull("fan");
        if (swing != null) values.put("swing", swing); else values.putNull("swing");
        values.put("is_enabled", 1);
        return getWritableDatabase().insert("scheduled_tasks", null, values);
    }

    public void deleteScheduledTask(long id) {
        getWritableDatabase().delete("scheduled_tasks", "id=?", new String[]{String.valueOf(id)});
    }
}