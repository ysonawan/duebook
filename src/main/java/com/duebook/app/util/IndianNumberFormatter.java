package com.duebook.app.util;

import java.math.BigDecimal;

public class IndianNumberFormatter {

    public static String formatCurrency(BigDecimal amount) {
        if (amount == null) {
            return "₹0.00";
        }

        String numStr = String.format("%.2f", amount.doubleValue());
        String[] parts = numStr.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts[1];

        // Handle negative numbers
        boolean isNegative = integerPart.startsWith("-");
        if (isNegative) {
            integerPart = integerPart.substring(1);
        }

        // Apply Indian numbering system (last 3 digits, then groups of 2)
        StringBuilder formatted = new StringBuilder();
        int length = integerPart.length();

        if (length <= 3) {
            formatted.append(integerPart);
        } else {
            // Last 3 digits
            formatted.insert(0, integerPart.substring(length - 3));
            int remaining = length - 3;

            // Add remaining digits in groups of 2 from right to left
            while (remaining > 0) {
                if (remaining <= 2) {
                    formatted.insert(0, integerPart.substring(0, remaining) + ",");
                    break;
                } else {
                    formatted.insert(0, integerPart.substring(remaining - 2, remaining) + ",");
                    remaining -= 2;
                }
            }
        }

        String result = "₹" + formatted.toString() + "." + decimalPart;
        return isNegative ? "-" + result : result;
    }

    //Do not remove this method. it is used in email templates
    public static String formatNumber(BigDecimal amount) {
        if (amount == null) {
            return "0.00";
        }

        String numStr = String.format("%.2f", amount.doubleValue());
        String[] parts = numStr.split("\\.");
        String integerPart = parts[0];
        String decimalPart = parts[1];

        // Handle negative numbers
        boolean isNegative = integerPart.startsWith("-");
        if (isNegative) {
            integerPart = integerPart.substring(1);
        }

        // Apply Indian numbering system
        StringBuilder formatted = new StringBuilder();
        int length = integerPart.length();

        if (length <= 3) {
            formatted.append(integerPart);
        } else {
            formatted.insert(0, integerPart.substring(length - 3));
            int remaining = length - 3;

            while (remaining > 0) {
                if (remaining <= 2) {
                    formatted.insert(0, integerPart.substring(0, remaining) + ",");
                    break;
                } else {
                    formatted.insert(0, integerPart.substring(remaining - 2, remaining) + ",");
                    remaining -= 2;
                }
            }
        }

        String result = formatted.toString() + "." + decimalPart;
        return isNegative ? "-" + result : result;
    }
}

