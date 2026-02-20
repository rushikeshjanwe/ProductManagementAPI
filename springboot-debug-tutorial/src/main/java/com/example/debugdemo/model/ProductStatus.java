package com.example.debugdemo.model;

/**
 * Product Status Enum
 * 
 * DEBUGGING TIP: Enums are great for type safety.
 * If you see "Unknown enum constant" errors, check for mismatches
 * between your code and database values.
 */
public enum ProductStatus {
    ACTIVE,
    INACTIVE,
    OUT_OF_STOCK,
    DISCONTINUED
}
