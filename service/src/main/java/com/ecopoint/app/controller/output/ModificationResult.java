package com.ecopoint.app.controller.output;

public record ModificationResult<T>(
		T id,
		String message
		) {
}
