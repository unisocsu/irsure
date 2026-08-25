package kernel.unisocsu.irsure.db;

import android.content.Context;
import android.content.res.AssetManager;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.util.Xml;

import org.xmlpull.v1.XmlPullParser;

import java.io.IOException;
import java.io.InputStream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

/**
 * Streams assets/ac_codesets.zip -> XmlPullParser -> SQLite, without ever unzipping
 * to disk or holding the whole 60MB document in RAM. Designed to run once, off the
 * main thread (see SetupActivity), with an optional progress callback.
 */
public class DataImporter {

    private static final String ZIP_ASSET_NAME = "ac_codesets.zip";

    public interface ProgressListener {
        /** Called periodically as codesets are imported. codesetsSoFar has no fixed max (~226 expected). */
        void onProgress(int codesetsSoFar, String currentCodesetName);
        void onFinished(int totalCodesets, int totalFunctions);
        void onError(Exception e);
    }

    public static void importIfNeeded(Context context, ProgressListener listener) {
        DbHelper helper = DbHelper.getInstance(context);
        if (helper.hasData()) {
            if (listener != null) listener.onFinished(-1, -1);
            return;
        }
        try {
            doImport(context, helper, listener);
        } catch (Exception e) {
            if (listener != null) listener.onError(e);
        }
    }

    private static void doImport(Context context, DbHelper helper, ProgressListener listener)
            throws IOException, org.xmlpull.v1.XmlPullParserException {

        AssetManager assets = context.getAssets();
        SQLiteDatabase db = helper.getWritableDatabase();

        int codesetCount = 0;
        int functionCount = 0;

        try (InputStream assetStream = assets.open(ZIP_ASSET_NAME);
             ZipInputStream zis = new ZipInputStream(assetStream)) {

            ZipEntry entry;
            InputStream xmlStream = null;
            // Find the (single) XML entry inside the zip.
            while ((entry = zis.getNextEntry()) != null) {
                if (entry.getName().toLowerCase().endsWith(".xml")) {
                    xmlStream = zis; // ZipInputStream itself is now positioned at this entry's data
                    break;
                }
            }
            if (xmlStream == null) {
                throw new IOException("No .xml entry found inside " + ZIP_ASSET_NAME);
            }

            XmlPullParser parser = Xml.newPullParser();
            parser.setFeature(XmlPullParser.FEATURE_PROCESS_NAMESPACES, false);
            parser.setInput(xmlStream, "UTF-8");

            db.beginTransaction();
            SQLiteStatement insertCodeset = helper.compileCodesetInsert(db);
            SQLiteStatement insertFunction = helper.compileFunctionInsert(db);
            try {
                long currentCodesetId = -1;
                String currentCodesetName = null;

                int eventType = parser.getEventType();
                while (eventType != XmlPullParser.END_DOCUMENT) {
                    if (eventType == XmlPullParser.START_TAG) {
                        String tag = parser.getName();

                        if ("codeset".equals(tag)) {
                            String name = parser.getAttributeValue(null, "name");
                            String brands = parser.getAttributeValue(null, "brands");
                            String model = parser.getAttributeValue(null, "model");
                            String region = parser.getAttributeValue(null, "region");

                            insertCodeset.clearBindings();
                            bindStringOrNull(insertCodeset, 1, name);
                            bindStringOrNull(insertCodeset, 2, brands);
                            bindStringOrNull(insertCodeset, 3, model);
                            bindStringOrNull(insertCodeset, 4, region);
                            currentCodesetId = insertCodeset.executeInsert();
                            currentCodesetName = name;
                            codesetCount++;

                            if (listener != null && codesetCount % 5 == 0) {
                                listener.onProgress(codesetCount, currentCodesetName);
                            }

                        } else if ("function".equals(tag)) {
                            String funcName = parser.getAttributeValue(null, "name");
                            String power = parser.getAttributeValue(null, "power");
                            String mode = parser.getAttributeValue(null, "mode");
                            String tempStr = parser.getAttributeValue(null, "temp");
                            String fan = parser.getAttributeValue(null, "fan");
                            String swing = parser.getAttributeValue(null, "swing");
                            String freqStr = parser.getAttributeValue(null, "freq_hz");
                            String pattern = readText(parser);

                            insertFunction.clearBindings();
                            insertFunction.bindLong(1, currentCodesetId);
                            bindStringOrNull(insertFunction, 2, funcName);
                            bindStringOrNull(insertFunction, 3, power);
                            bindStringOrNull(insertFunction, 4, mode);
                            if (tempStr != null && !tempStr.isEmpty()) {
                                try {
                                    insertFunction.bindLong(5, Long.parseLong(tempStr));
                                } catch (NumberFormatException nfe) {
                                    insertFunction.bindNull(5);
                                }
                            } else {
                                insertFunction.bindNull(5);
                            }
                            bindStringOrNull(insertFunction, 6, fan);
                            bindStringOrNull(insertFunction, 7, swing);
                            int freq = 0;
                            if (freqStr != null && !freqStr.isEmpty()) {
                                try { freq = Integer.parseInt(freqStr); } catch (NumberFormatException ignored) {}
                            }
                            insertFunction.bindLong(8, freq);
                            bindStringOrNull(insertFunction, 9, pattern);
                            insertFunction.executeInsert();
                            functionCount++;
                        }
                    }
                    eventType = parser.next();
                }

                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        }

        if (listener != null) {
            listener.onFinished(codesetCount, functionCount);
        }
    }

    /** Reads the text content of the current element (parser must be on its START_TAG). */
    private static String readText(XmlPullParser parser) throws IOException, org.xmlpull.v1.XmlPullParserException {
        String text = "";
        if (parser.next() == XmlPullParser.TEXT) {
            text = parser.getText();
            parser.next(); // consume END_TAG
        }
        return text;
    }

    private static void bindStringOrNull(SQLiteStatement stmt, int index, String value) {
        if (value == null) {
            stmt.bindNull(index);
        } else {
            stmt.bindString(index, value);
        }
    }
}
