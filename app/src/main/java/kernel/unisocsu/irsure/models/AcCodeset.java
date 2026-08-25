package kernel.unisocsu.irsure.models;

/**
 * Metadata for a single AC remote model ("codeset").
 * Maps 1:1 to a row in the "codesets" SQLite table.
 */
public class AcCodeset {

    private final long id;
    private final String name;
    private final String brands;   // semicolon-separated list, e.g. "AKAI;VOLTAS"
    private final String model;
    private final String region;

    public AcCodeset(long id, String name, String brands, String model, String region) {
        this.id = id;
        this.name = name;
        this.brands = brands;
        this.model = model;
        this.region = region;
    }

    public long getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getBrands() {
        return brands;
    }

    public String getModel() {
        return model;
    }

    public String getRegion() {
        return region;
    }

    /** Human readable label for lists, e.g. "AKAI;VOLTAS — Re_GLOBAL_1092" */
    public String getDisplayLabel() {
        StringBuilder sb = new StringBuilder();
        if (brands != null && !brands.isEmpty()) {
            sb.append(brands);
        }
        if (name != null && !name.isEmpty()) {
            if (sb.length() > 0) sb.append(" — ");
            sb.append(name);
        }
        if (sb.length() == 0) {
            sb.append("Codeset #").append(id);
        }
        return sb.toString();
    }

    @Override
    public String toString() {
        return getDisplayLabel();
    }
}
