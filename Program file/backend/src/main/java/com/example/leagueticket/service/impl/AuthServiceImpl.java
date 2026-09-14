package com.example.leagueticket.service.impl;

import com.example.leagueticket.dto.LoginRequest;
import com.example.leagueticket.dto.ManagementAccountProbeRequest;
import com.example.leagueticket.dto.ManagementActivationRequest;
import com.example.leagueticket.entity.SysUser;
import com.example.leagueticket.exception.BusinessException;
import com.example.leagueticket.mapper.SysUserMapper;
import com.example.leagueticket.security.AuthenticatedUser;
import com.example.leagueticket.security.JwtService;
import com.example.leagueticket.service.AuthService;
import com.example.leagueticket.service.SysUserService;
import com.example.leagueticket.vo.CurrentUserResponse;
import com.example.leagueticket.vo.LoginResponse;
import com.example.leagueticket.vo.ManagementAccountStateResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Profile("dev")
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final java.util.Set<String> MANAGEMENT_ROLES=java.util.Set.of("EVENT_ADMIN","ADMIN");

    private final SysUserService userService;
    private final SysUserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    @Override
    @Transactional
    public LoginResponse login(LoginRequest request) {
        String selectedRole = request.roleCode().trim().toUpperCase();
        String phone=request.phone().trim();
        if(!userService.phoneExists(phone))throw new BusinessException(HttpStatus.UNAUTHORIZED,"该账号未注册");
        SysUser user = userService.findByPhoneAndRole(phone,selectedRole);
        if(user==null)throw new BusinessException(HttpStatus.UNAUTHORIZED,"所选身份与账号不匹配");
        if(MANAGEMENT_ROLES.contains(selectedRole))validateManagementIdentity(user,selectedRole,request.employeeNo());
        requireLoginStatus(user);
        if (!passwordEncoder.matches(request.password(), user.getPasswordHash()))throw new BusinessException(HttpStatus.UNAUTHORIZED,"密码错误");
        return loginSuccess(user);
    }

    @Override
    public ManagementAccountStateResponse probeManagementAccount(ManagementAccountProbeRequest request){
        String role=normalizeManagementRole(request.roleCode()),phone=request.phone().trim();
        if(!userService.phoneExists(phone))throw new BusinessException(HttpStatus.UNAUTHORIZED,"该账号未注册");
        SysUser user=userService.findByPhoneAndRole(phone,role);
        if(user==null)throw new BusinessException(HttpStatus.UNAUTHORIZED,"所选身份与账号不匹配");
        validateManagementIdentity(user,role,request.employeeNo());
        if("DISABLED".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"该工号已被停用，请联系管理员");
        if("LOCKED".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"该工号已锁定，请联系管理员");
        if("CANCELLED".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"该账号已注销");
        if(!java.util.Set.of("PENDING_ACTIVATION","ENABLED").contains(user.getUserStatus()))throw new BusinessException(HttpStatus.CONFLICT,"管理账号状态异常");
        return new ManagementAccountStateResponse(role,user.getUserStatus(),"PENDING_ACTIVATION".equals(user.getUserStatus()));
    }

    @Override
    @Transactional
    public LoginResponse activateManagementAccount(ManagementActivationRequest request){
        if(!request.password().equals(request.confirmPassword()))throw new BusinessException("两次输入的密码不一致");
        String role=normalizeManagementRole(request.roleCode()),phone=request.phone().trim();
        if(!userService.phoneExists(phone))throw new BusinessException(HttpStatus.UNAUTHORIZED,"该账号未注册");
        SysUser user=userMapper.findByPhoneAndRoleForUpdate(phone,role);
        if(user==null)throw new BusinessException(HttpStatus.UNAUTHORIZED,"所选身份与账号不匹配");
        validateManagementIdentity(user,role,request.employeeNo());
        if("DISABLED".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"该工号已被停用，请联系管理员");
        if("CANCELLED".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.FORBIDDEN,"该账号已注销");
        if(!"PENDING_ACTIVATION".equals(user.getUserStatus()))throw new BusinessException(HttpStatus.CONFLICT,"该账号已完成首次启用");
        if(!user.getRealName().equals(request.realName().trim()))throw new BusinessException(HttpStatus.UNAUTHORIZED,"姓名与预登记信息不一致");
        if(userMapper.activateManagementAccount(user.getUserId(),passwordEncoder.encode(request.password()))!=1)throw new BusinessException(HttpStatus.CONFLICT,"首次启用已由其他请求完成");
        user=userMapper.findById(user.getUserId());
        return loginSuccess(user);
    }

    private LoginResponse loginSuccess(SysUser user){
        AuthenticatedUser principal = userService.loadAuthenticatedUser(user.getUserId());
        userMapper.updateLastLogin(user.getUserId());
        return new LoginResponse(jwtService.createToken(principal), user.getUserId(), user.getUsername(), user.getPhone(),
                user.getRealName(), user.getRoleCode(), user.getClubId());
    }

    private void requireLoginStatus(SysUser user){
        switch(user.getUserStatus()){
            case "ENABLED" -> { }
            case "PENDING_CLUB_APPROVAL" -> throw new BusinessException(HttpStatus.FORBIDDEN,"俱乐部审核中，请先以普通用户身份进入");
            case "PENDING_ACTIVATION" -> throw new BusinessException(HttpStatus.FORBIDDEN,"请先完成管理员首次启用");
            case "DISABLED" -> throw new BusinessException(HttpStatus.FORBIDDEN,MANAGEMENT_ROLES.contains(user.getRoleCode())?"该工号已被停用，请联系管理员":"该账号已被停用，请联系管理员");
            case "LOCKED" -> throw new BusinessException(HttpStatus.FORBIDDEN,"账号已锁定，请联系管理员");
            case "CANCELLED" -> throw new BusinessException(HttpStatus.FORBIDDEN,"该账号已注销");
            default -> throw new BusinessException(HttpStatus.FORBIDDEN,"账号状态异常");
        }
    }

    private void validateManagementIdentity(SysUser user,String role,String digits){
        if(digits==null||!digits.matches("\\d{4}"))throw new BusinessException(HttpStatus.BAD_REQUEST,"请输入4位工号数字");
        String full=("EVENT_ADMIN".equals(role)?"EA":"SA")+digits;
        SysUser employee=userMapper.findByEmployeeNo(full);
        if(employee==null)throw new BusinessException(HttpStatus.UNAUTHORIZED,"该工号不存在");
        if(!employee.getPhone().equals(user.getPhone()))throw new BusinessException(HttpStatus.UNAUTHORIZED,"工号与手机号不匹配");
        if(!employee.getRoleCode().equals(role))throw new BusinessException(HttpStatus.UNAUTHORIZED,"工号与所选身份不匹配");
    }

    private String normalizeManagementRole(String role){String value=role==null?"":role.trim().toUpperCase();if(!MANAGEMENT_ROLES.contains(value))throw new BusinessException("请选择管理身份");return value;}

    @Override
    public CurrentUserResponse currentUser(AuthenticatedUser principal) {
        SysUser user = userService.getById(principal.userId());
        return new CurrentUserResponse(user.getUserId(), user.getUsername(), user.getPhone(), user.getRealName(),
                user.getEmployeeNo(), user.getAvatarUrl(), user.getRoleCode(), user.getClubId(),
                user.getUserStatus(), principal.permissions());
    }
}
