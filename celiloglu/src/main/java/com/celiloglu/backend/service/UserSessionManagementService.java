package com.celiloglu.backend.service;

import com.celiloglu.backend.security.CustomUserDetails;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.session.SessionInformation;
import org.springframework.security.core.session.SessionRegistry;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserSessionManagementService {

    private static final Logger log = LoggerFactory.getLogger(UserSessionManagementService.class);

    private final SessionRegistry sessionRegistry;

    public UserSessionManagementService(SessionRegistry sessionRegistry) {
        this.sessionRegistry = sessionRegistry;
    }

    /**
     * Hədəf istifadəçinin bütün aktiv sessiyalarını tapır və expireNow() ilə etibarsız edir.
     * Növbəti istənilən sorğuda ConcurrentSessionFilter sessiyanı ləğv edir və yeni giriş tələb edir.
     */
    public void invalidateUserSessions(Long userId, String username) {
        if (userId == null && username == null) {
            return;
        }

        int expiredCount = 0;
        List<Object> allPrincipals = sessionRegistry.getAllPrincipals();
        for (Object principal : allPrincipals) {
            boolean matches = false;
            if (principal instanceof CustomUserDetails cud) {
                if (userId != null && userId.equals(cud.getId())) {
                    matches = true;
                } else if (username != null && username.equalsIgnoreCase(cud.getUsername())) {
                    matches = true;
                }
            } else if (principal instanceof UserDetails ud) {
                if (username != null && username.equalsIgnoreCase(ud.getUsername())) {
                    matches = true;
                }
            } else if (principal instanceof String principalUsername) {
                if (username != null && username.equalsIgnoreCase(principalUsername)) {
                    matches = true;
                }
            }

            if (matches) {
                List<SessionInformation> sessions = sessionRegistry.getAllSessions(principal, false);
                if (sessions != null) {
                    for (SessionInformation sessionInfo : sessions) {
                        sessionInfo.expireNow();
                        expiredCount++;
                    }
                }
            }
        }
        log.info("İstifadəçi sessiyaları etibarsız edildi (UserId: {}, Username: {}): cəmi {} aktiv sessiya sonlandırıldı.",
                userId, username, expiredCount);
    }
}

