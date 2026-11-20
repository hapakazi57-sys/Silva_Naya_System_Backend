package com.silvanaya.service;

import com.silvanaya.model.ActivityLog;
import com.silvanaya.model.Enums.Role;
import com.silvanaya.model.User;
import com.silvanaya.repository.ActivityLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;


@Service
@RequiredArgsConstructor
public class ActivityLogService {

    private final ActivityLogRepository repo;

    public List<ActivityLog> latest10(User viewer) {
        return latest(viewer, 10);
    }

    public List<ActivityLog> latest200(User viewer) {
        return latest(viewer, 200);
    }

    public long count(User viewer) {
        if (viewer.getRole() == Role.ADMIN) return repo.count();
        if (viewer.getRole() == Role.MANAGER) return repo.countExcludingRole(Role.ADMIN);
        return repo.countByUserId(viewer.getId());
    }

    private List<ActivityLog> latest(User viewer, int limit) {
        boolean small = limit <= 10;
        Role role = viewer.getRole();

        if (role == Role.ADMIN) {
            return small ? repo.findTop10ByOrderByIdDesc() : repo.findTop200ByOrderByIdDesc();
        }
        if (role == Role.MANAGER) {
            return repo.latestExcludingRole(Role.ADMIN, PageRequest.of(0, limit));
        }
        // EMPLOYEE na wengine: shughuli zao tu
        return small
                ? repo.findTop10ByUserIdOrderByIdDesc(viewer.getId())
                : repo.findTop200ByUserIdOrderByIdDesc(viewer.getId());
    }
}