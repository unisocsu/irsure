package kernel.unisocsu.irsure.db;

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
    private static final int DB_VERSION = 1;

    public static final String TBL_CODESETS = "codesets";
    public static final String TBL_FUNCTIONS = "functions";

    private static DbHelper instance;

    public static synchronized DbHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DbHelper(context.getApplicationContext());
        }
        return instance;
    }

    private DbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + TBL_CODESETS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "name TEXT, " +
                "brands TEXT, " +
                "model TEXT, " +
                "region TEXT)");

        db.execSQL("CREATE TABLE " + TBL_FUNCTIONS + " (" +
                "id INTEGER PRIMARY KEY AUTOINCREMENT, " +
                "codeset_id INTEGER, " +
                "func_name TEXT, " +
                "power TEXT, " +
                "mode TEXT, " +
                "temp INTEGER, " +
                "fan TEXT, " +
                "swing TEXT, " +
                "freq INTEGER, " +
                "pattern TEXT, " +
                "FOREIGN KEY(codeset_id) REFERENCES " + TBL_CODESETS + "(id))");

        db.execSQL("CREATE INDEX idx_functions_codeset ON " + TBL_FUNCTIONS + "(codeset_id)");
        db.execSQL("CREATE INDEX idx_codesets_name ON " + TBL_CODESETS + "(name)");
        db.execSQL("CREATE INDEX idx_codesets_brands ON " + TBL_CODESETS + "(brands)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TBL_FUNCTIONS);
        db.execSQL("DROP TABLE IF EXISTS " + TBL_CODESETS);
        onCreate(db);
    }

    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_FUNCTIONS);
        db.execSQL("DELETE FROM " + TBL_CODESETS);
    }

    public SQLiteStatement compileCodesetInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO " + TBL_CODESETS +
                " (name, brands, model, region) VALUES (?,?,?,?)");
    }

    public SQLiteStatement compileFunctionInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO " + TBL_FUNCTIONS +
                " (codeset_id, func_name, power, mode, temp, fan, swing, freq, pattern) " +
                "VALUES (?,?,?,?,?,?,?,?,?)");
    }

    public boolean hasData() {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT COUNT(*) FROM " + TBL_CODESETS, null);
        boolean result = false;
        if (c.moveToFirst()) {
            result = c.getInt(0) > 0;
        }
        c.close();
        return result;
    }

    public List<AcCodeset> searchCodesets(String query) {
        SQLiteDatabase db = getReadableDatabase();
        List<AcCodeset> result = new ArrayList<>();
        Cursor c;
        if (query == null || query.trim().isEmpty()) {
            c = db.rawQuery("SELECT id, name, brands, model, region FROM " + TBL_CODESETS +
                    " ORDER BY brands, name", null);
        } else {
            String like = "%" + query.trim() + "%";
            c = db.rawQuery("SELECT id, name, brands, model, region FROM " + TBL_CODESETS +
                    " WHERE name LIKE ? OR brands LIKE ? ORDER BY brands, name", new String[]{like, like});
        }
        while (c.moveToNext()) {
            result.add(new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
        }
        c.close();
        return result;
    }

    public AcFunction findFunction(long codesetId, String power, String mode, Integer temp,
                                   String fan, String swing) {
        SQLiteDatabase db = getReadableDatabase();
        StringBuilder sql = new StringBuilder(
                "SELECT id, codeset_id, func_name, power, mode, temp, fan, swing, freq, pattern " +
                        "FROM " + TBL_FUNCTIONS + " WHERE codeset_id=?");
        List<String> args = new ArrayList<>();
        args.add(String.valueOf(codesetId));

        if (power != null) { sql.append(" AND power=?"); args.add(power); }
        if (mode != null) { sql.append(" AND mode=?"); args.add(mode); }
        if (temp != null) { sql.append(" AND temp=?"); args.add(String.valueOf(temp)); }
        if (fan != null) { sql.append(" AND fan=?"); args.add(fan); }
        if (swing != null) { sql.append(" AND swing=?"); args.add(swing); }
        sql.append(" LIMIT 1");

        Cursor c = db.rawQuery(sql.toString(), args.toArray(new String[0]));
        AcFunction result = null;
        if (c.moveToFirst()) {
            result = cursorToFunction(c);
        }
        c.close();
        return result;
    }

    private AcFunction cursorToFunction(Cursor c) {
        Integer temp = c.isNull(5) ? null : c.getInt(5);
        return new AcFunction(
                c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4),
                temp, c.getString(6), c.getString(7), c.getInt(8), c.getString(9));
    }
}