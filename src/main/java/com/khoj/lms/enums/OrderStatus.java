package com.khoj.lms.enums;

public enum OrderStatus {
    PENDING,        // created, awaiting payment / processing
    COMPLETED,      // payment success OR free → enrollment issued
    FAILED,         // payment failed
    CANCELLED,      // student cancelled
    REFUNDED        // admin refunded
}