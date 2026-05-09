package tn.edu.esprit.entities;

/**
 * InventoryStatus enum represents the current stock status of a blood type
 * at a hospital based on quantity thresholds.
 *
 * Status is automatically computed by the database trigger based on quantity_units:
 * - CRITICAL: quantity <= 5 (urgent need for donations)
 * - LOW: 6 <= quantity <= 10 (below optimal but acceptable)
 * - OPTIMAL: quantity > 10 (healthy stock level)
 */
public enum InventoryStatus {
    CRITICAL("Critical", "Urgent - Stock Below 5 Units"),
    LOW("Low", "Low Stock - 6 to 10 Units"),
    OPTIMAL("Optimal", "Optimal - More than 10 Units");

    private final String displayName;
    private final String description;

    InventoryStatus(String displayName, String description) {
        this.displayName = displayName;
        this.description = description;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getDescription() {
        return description;
    }

    /**
     * Gets the status based on quantity of units.
     *
     * @param quantity the number of units in stock
     * @return the corresponding InventoryStatus
     */
    public static InventoryStatus fromQuantity(Integer quantity) {
        if (quantity == null || quantity <= 5) {
            return CRITICAL;
        } else if (quantity <= 10) {
            return LOW;
        } else {
            return OPTIMAL;
        }
    }
}
