package com.ecopoint.app.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

public enum BottleType {
	
	Plastic, Bottle;
	
	@JsonCreator
    public static BottleType from(String value) {
        if (value == null) return null;
        return switch (value.trim().toLowerCase()) {
            case "plastic" -> Plastic;
            case "bottle" -> Bottle;
            default -> throw new IllegalArgumentException("Unsupported bottle type: " + value);
        };
    }

    @JsonValue
    public String toValue() {
        return name().toLowerCase();
    }
	

}
