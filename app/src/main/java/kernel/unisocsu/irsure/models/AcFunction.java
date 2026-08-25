package kernel.unisocsu.irsure.models;

import java.util.Locale;

/**
 * A single IR function/button for a given codeset: a specific combination of
 * power/mode/temp/fan/swing and the raw IR pulse/space pattern that produces it.
 * Maps 1:1 to a row in the "functions" SQLite table.
 */
public class AcFunction {

    private final long id;
    private final long codesetId;
    private final String name;     // e.g. "POWER ON"
    private final String power;    // "ON" / "OFF" / ""
    private final String mode;     // "COOL" / "HEAT" / "DRY" / "FAN" / "AUTO" / ""
    private final Integer temp;    // null if not applicable
    private final String fan;      // "AUTO" / "LOW" / "MED" / "HIGH" / ""
    private final String swing;    // "ON" / "OFF" / ""
    private final int freqHz;
    private final String patternCsv; // raw "9000,4500,560,..." string as stored in DB

    public AcFunction(long id, long codesetId, String name, String power, String mode,
                       Integer temp, String fan, String swing, int freqHz, String patternCsv) {
        this.id = id;
        this.codesetId = codesetId;
        this.name = name;
        this.power = power;
        this.mode = mode;
        this.temp = temp;
        this.fan = fan;
        this.swing = swing;
        this.freqHz = freqHz;
        this.patternCsv = patternCsv;
    }

    public long getId() {
        return id;
    }

    public long getCodesetId() {
        return codesetId;
    }

    public String getName() {
        return name;
    }

    public String getPower() {
        return power;
    }

    public String getMode() {
        return mode;
    }

    public Integer getTemp() {
        return temp;
    }

    public String getFan() {
        return fan;
    }

    public String getSwing() {
        return swing;
    }

    public int getFreqHz() {
        return freqHz;
    }

    public String getPatternCsv() {
        return patternCsv;
    }

    /** Parses the stored CSV string into an int[] pattern ready for ConsumerIrManager.transmit(). */
    public int[] getPattern() {
        if (patternCsv == null || patternCsv.isEmpty()) {
            return new int[0];
        }
        String[] parts = patternCsv.split(",");
        int[] result = new int[parts.length];
        for (int i = 0; i < parts.length; i++) {
            try {
                result[i] = Integer.parseInt(parts[i].trim());
            } catch (NumberFormatException e) {
                result[i] = 0;
            }
        }
        return result;
    }

    @Override
    public String toString() {
        return String.format(Locale.US, "AcFunction{name=%s power=%s mode=%s temp=%s fan=%s swing=%s}",
                name, power, mode, temp, fan, swing);
    }
}
