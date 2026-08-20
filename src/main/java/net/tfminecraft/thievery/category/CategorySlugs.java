package net.tfminecraft.thievery.category;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CategorySlugs {

    private static final Pattern MATERIAL = Pattern.compile(
            "^ac_(metal|wood|crystal|leather|feather|wool)_tier_(\\d+)$", Pattern.CASE_INSENSITIVE);

    private CategorySlugs() {
    }

    public static boolean isMaterialSlug(String slug) {
        return slug != null && MATERIAL.matcher(slug.trim()).matches();
    }

    public static boolean isAcSlug(String slug) {
        return slug != null && AcCraftRef.parse(slug.trim()).isPresent();
    }

    public static boolean isCraftSlug(String slug) {
        return isAcSlug(slug) && !isMaterialSlug(slug);
    }

    public static boolean isPathSlug(String slug) {
        return slug != null && !slug.isBlank() && !isAcSlug(slug);
    }

    public static Optional<AcCraftRef> parseCraftRef(String slug) {
        if (slug == null || slug.isBlank()) {
            return Optional.empty();
        }
        return AcCraftRef.parse(slug.trim());
    }

    public static String materialType(String slug) {
        Matcher matcher = MATERIAL.matcher(slug.trim());
        return matcher.matches() ? matcher.group(1) : null;
    }

    public static int materialTier(String slug) {
        Matcher matcher = MATERIAL.matcher(slug.trim());
        return matcher.matches() ? Integer.parseInt(matcher.group(2)) : 0;
    }
}
