package com.telehealth.auth.service;

import java.util.UUID;

public interface GdprErasureService {
    void executeRightToErasure(String otpCode);
    void executeErasureByAdmin(UUID targetUserId, String adminOtpCode);
}
