package com.ecopoint.app.controller.output;

public record WalletDetails(
		long currentBalance,
		long earnedThisMonth,
		long totalRedeemed,
		long totalPointEarned
		) {

}
