package net.tfminecraft.thievery.category;

public final class CategoryMatch {

    private final CategoryMatchType type;
    private final String acType;
    private final int acTier;

    private CategoryMatch(CategoryMatchType type, String acType, int acTier) {
        this.type = type;
        this.acType = acType;
        this.acTier = acTier;
    }

    public static CategoryMatch path() {
        return new CategoryMatch(CategoryMatchType.PATH, null, 0);
    }

    public static CategoryMatch acMaterial(String acType, int acTier) {
        return new CategoryMatch(CategoryMatchType.AC_MATERIAL, acType, acTier);
    }

    public static CategoryMatch composite() {
        return new CategoryMatch(CategoryMatchType.COMPOSITE, null, 0);
    }

    public CategoryMatchType getType() {
        return type;
    }

    public String getAcType() {
        return acType;
    }

    public int getAcTier() {
        return acTier;
    }
}
