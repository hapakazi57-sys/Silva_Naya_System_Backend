package com.silvanaya.service;

import com.silvanaya.model.ActivityLog;
import com.silvanaya.model.User;
import com.silvanaya.repository.ActivityLogRepository;
import com.silvanaya.util.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service @RequiredArgsConstructor
public class AuditService {
    private final ActivityLogRepository repo;

    public void log(String action, String entity, String details) {
        User u = CurrentUser.get();
        save(u == null ? null : u.getId(), u == null ? "system" : u.getFullName(), action, entity, details);
    }

    public void save(Long userId, String userName, String action, String entity, String details) {
        ActivityLog l = new ActivityLog();
        l.setUserId(userId); l.setUserName(userName); l.setAction(action); l.setEntity(entity);
        l.setDetails(details != null && details.length() > 590 ? details.substring(0, 590) : details);
        repo.save(l);
    }
}
