package com.ecopoint.app.controller.output;

public record DepositResult(
		String status,
		String message,
		long added,
		Long newtotal
		) {
	
	public static DepositResult rejected(String msg) {
        return new DepositResult("rejected", msg, 0, null);
    }
    public static DepositResult success(long added, long newTotal) {
        return new DepositResult("success", "ok", added, newTotal);
    }

}
