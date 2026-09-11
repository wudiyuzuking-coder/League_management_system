package com.example.leagueticket.service;

import com.example.leagueticket.vo.AvatarResponse;
import org.springframework.web.multipart.MultipartFile;

public interface ClubLogoService { AvatarResponse upload(Long clubId, MultipartFile file); }
