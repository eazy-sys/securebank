package com.webapp.bankingportal.dto;

import java.math.BigDecimal;

public record AmountRequest(BigDecimal amount, String pin) {
}
