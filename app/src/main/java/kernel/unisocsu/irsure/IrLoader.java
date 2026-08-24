package kernel.unisocsu.irsure;

import android.content.Context;
import android.content.res.XmlResourceParser;
import java.util.ArrayList;
import java.util.List;
import org.xmlpull.v1.XmlPullParser;

public class IrLoader {

    public static class Protocol {
        public String name;
        public int[] pattern;

        public Protocol(String name, String patternStr) {
            this.name = name;
            // הפיכת הטקסט מה-XML למערך מספרים (int[])
            String[] parts = patternStr.split(",");
            this.pattern = new int[parts.length];
            for (int i = 0; i < parts.length; i++) {
                this.pattern[i] = Integer.parseInt(parts[i].trim());
            }
        }
    }

    public static List<Protocol> loadCodesFromXml(Context context) {
        List<Protocol> protocols = new ArrayList<>();
        XmlResourceParser parser = context.getResources().getXml(R.xml.ir_codes);

        try {
            int eventType = parser.getEventType();
            while (eventType != XmlPullParser.END_DOCUMENT) {
                if (eventType == XmlPullParser.START_TAG && parser.getName().equals("protocol")) {
                    String name = parser.getAttributeValue(null, "name");
                    String patternStr = parser.nextText();
                    protocols.add(new Protocol(name, patternStr));
                }
                eventType = parser.next();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return protocols;
    }
}
