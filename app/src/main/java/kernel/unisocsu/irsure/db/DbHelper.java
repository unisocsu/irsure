package kernel.unisocsu.irsure.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;
import kernel.unisocsu.irsure.models.AcCodeset;
import kernel.unisocsu.irsure.models.AcFunction;

public class DbHelper extends SQLiteOpenHelper {
    private static final String DB_NAME = "ac_codes.db";
    private static final int DB_VERSION = 1;
    private static DbHelper instance;

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) instance = new DbHelper(context.getApplicationContext());
        return instance;
    }

    private DbHelper(Context context) { super(context, DB_NAME, null, DB_VERSION); }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE codesets (id INTEGER PRIMARY KEY, name TEXT, brands TEXT, model TEXT, region TEXT)");
        db.execSQL("CREATE TABLE functions (id INTEGER PRIMARY KEY, codeset_id INTEGER, func_name TEXT, power TEXT, mode TEXT, temp INTEGER, fan TEXT, swing TEXT, freq INTEGER, pattern TEXT)");
    }

    @Override public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {}

    // פונקציה חסרה 1
    public AcCodeset getCodeset(long id) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM codesets WHERE id=?", new String[]{String.valueOf(id)});
        if (c.moveToFirst()) {
            AcCodeset res = new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
            c.close();
            return res;
        }
        return null;
    }

    public List<AcCodeset> searchCodesets(String query) {
        List<AcCodeset> list = new ArrayList<>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT * FROM codesets", null);
        while (c.moveToNext()) {
            list.add(new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
        }
        c.close();
        return list;
    }

    public AcFunction findFunction(long codesetId, String power, String mode, Integer temp, String fan, String swing) {
        SQLiteDatabase db = getReadableDatabase();
        String sql = "SELECT * FROM functions WHERE codeset_id=? AND power=?";
        Cursor c = db.rawQuery(sql, new String[]{String.valueOf(codesetId), power});
        if (c.moveToFirst()) {
            AcFunction f = new AcFunction(c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4), c.getInt(5), c.getString(6), c.getString(7), c.getInt(8), c.getString(9));
            c.close();
            return f;
        }
        return null;
    }

    // פונקציה חסרה 2
    public AcFunction findClosestFunction(long codesetId, String power, String mode, Integer temp, String fan, String swing) {
        return findFunction(codesetId, power, mode, temp, fan, swing); // מימוש בסיסי ל-Fallback
    }
}