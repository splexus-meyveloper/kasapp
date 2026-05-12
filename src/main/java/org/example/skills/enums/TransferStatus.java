package org.example.skills.enums;

public enum TransferStatus {
    PENDING,    // Adapazarı oluşturdu, admin onay bekliyor
    APPROVED,   // Admin onayladı, işlemler gerçekleşti
    REJECTED    // Admin reddetti
}
