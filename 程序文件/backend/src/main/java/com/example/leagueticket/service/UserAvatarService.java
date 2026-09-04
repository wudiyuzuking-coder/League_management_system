package com.example.leagueticket.service;

import com.example.leagueticket.vo.AvatarResponse;
import org.springframework.web.multipart.MultipartFile;

public interface UserAvatarService {
    AvatarResponse upload(Long userId, MultipartFile file);
    void remove(Long userId);
}
