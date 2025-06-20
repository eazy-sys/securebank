package com.webapp.bankingportal.dto;

import java.math.BigDecimal;

public record FundTransferRequest(String targetAccountNumber, String pin, BigDecimal amount) {
}
