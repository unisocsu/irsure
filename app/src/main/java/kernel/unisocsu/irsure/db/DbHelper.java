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

    /** Wipes both tables so a fresh import can run (used if the ZIP asset ever changes). */
    public void clearAll() {
        SQLiteDatabase db = getWritableDatabase();
        db.execSQL("DELETE FROM " + TBL_FUNCTIONS);
        db.execSQL("DELETE FROM " + TBL_CODESETS);
    }

    // ---- Insert helpers used by DataImporter (compiled statements for speed) ----

    public SQLiteStatement compileCodesetInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO " + TBL_CODESETS +
                " (name, brands, model, region) VALUES (?,?,?,?)");
    }

    public SQLiteStatement compileFunctionInsert(SQLiteDatabase db) {
        return db.compileStatement("INSERT INTO " + TBL_FUNCTIONS +
                " (codeset_id, func_name, power, mode, temp, fan, swing, freq, pattern) " +
                "VALUES (?,?,?,?,?,?,?,?,?)");
    }

    // ---- Read queries used by the UI ----

    /** Returns true if the import has already populated at least one codeset. */
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

    /** Search codesets by name or brand substring. Pass null/empty for all 226. */
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
            result.add(new AcCodeset(
                    c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4)));
        }
        c.close();
        return result;
    }

    public AcCodeset getCodeset(long codesetId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor c = db.rawQuery("SELECT id, name, brands, model, region FROM " + TBL_CODESETS +
                " WHERE id=?", new String[]{String.valueOf(codesetId)});
        AcCodeset result = null;
        if (c.moveToFirst()) {
            result = new AcCodeset(c.getLong(0), c.getString(1), c.getString(2), c.getString(3), c.getString(4));
        }
        c.close();
        return result;
    }

    /**
     * Finds the function row that exactly matches the requested state for a codeset.
     * Any of the state params can be null to mean "don't care" (matches any value in that column).
     */
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

    /**
     * Fallback search: same as findFunction but ignores temp, then progressively relaxes
     * mode/fan/swing if still no match. Used when the exact combination doesn't exist
     * for this codeset (not every remote has every combination).
     */
    public AcFunction findClosestFunction(long codesetId, String power, String mode, Integer temp,
                                           String fan, String swing) {
        AcFunction exact = findFunction(codesetId, power, mode, temp, fan, swing);
        if (exact != null) return exact;

        // Relax temp first (find nearest available temp for this mode/fan/swing).
        if (temp != null) {
            SQLiteDatabase db = getReadableDatabase();
            Cursor c = db.rawQuery(
                    "SELECT id, codeset_id, func_name, power, mode, temp, fan, swing, freq, pattern " +
                            "FROM " + TBL_FUNCTIONS + " WHERE codeset_id=? AND power=? AND mode=? " +
                            "AND fan=? AND swing=? AND temp IS NOT NULL ORDER BY ABS(temp - ?) LIMIT 1",
                    new String[]{String.valueOf(codesetId), power, mode, fan, swing, String.valueOf(temp)});
            if (c.moveToFirst()) {
                AcFunction f = cursorToFunction(c);
                c.close();
                return f;
            }
            c.close();
        }

        // Relax fan/swing next, keep power+mode+temp.
        AcFunction relaxed = findFunction(codesetId, power, mode, temp, null, null);
        if (relaxed != null) return relaxed;

        // Last resort: match only on power (covers ON/OFF toggle functions).
        return findFunction(codesetId, power, null, null, null, null);
    }

    private AcFunction cursorToFunction(Cursor c) {
        Integer temp = c.isNull(5) ? null : c.getInt(5);
        return new AcFunction(
                c.getLong(0), c.getLong(1), c.getString(2), c.getString(3), c.getString(4),
                temp, c.getString(6), c.getString(7), c.getInt(8), c.getString(9));
    }
}
