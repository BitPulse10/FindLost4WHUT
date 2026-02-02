package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.UserVO;
import com.whut.lostandfoundforwhut.service.IAuthService;
import com.whut.lostandfoundforwhut.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;

/**
 * @author DXR
 * @date 2026/01/31
 * @description User controller
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "User related endpoints")
public class UserController {

    private final IUserService userService;
    private final IAuthService authService;

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    // 新增：获取当前登录用户的邮箱（从Security上下文，替代原requireEmail方法）
    private String getCurrentUserEmail() {
        if (!securityEnabled) {
            return null;
        }
        // 从Security上下文获取已认证的用户信息（过滤器已完成Token校验）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证的情况会被过滤器拦截，这里无需额外判断
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername(); // 注意：这里的username实际是用户邮箱（和过滤器逻辑一致）
    }

    private String requireCurrentUserEmail() {
        String email = getCurrentUserEmail();
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        return email;
    }

    private Long getCurrentUserId() {
        String email = requireCurrentUserEmail();
        return userService.getUserIdByEmail(email);
    }


    // 新增：校验用户归属权（提取为公共方法，避免重复）
    private void checkUserOwnership(Long userId, String currentUserEmail) {
        if (!securityEnabled) {
            return;
        }
        User target = userService.getUserById(userId);
        if (!currentUserEmail.equals(target.getEmail())) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }
    }

    @PostMapping
    @Operation(summary = "Register user", description = "Register user with email verification code")
    public Result<UserVO> createUser(@RequestBody UserRegisterDTO dto) {
        // 保留原有逻辑（注册接口无需认证）
        User user = authService.register(dto);
        return Result.success(UserVO.from(user, null));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current user", description = "Get current user info")
    public Result<UserVO> getCurrentUser() {
        Long userId = getCurrentUserId();
        User target = userService.getUserById(userId);
        return Result.success(UserVO.from(target, null));
    }

    @PutMapping("/me/password")
    @Operation(summary = "Update current password", description = "Update current user password")
    public Result<UserVO> updateCurrentPassword(@RequestBody UserPasswordUpdateDTO dto) {
        Long userId = getCurrentUserId();
        User updated = userService.updatePassword(userId, dto);
        return Result.success(UserVO.from(updated, null));
    }

    @PutMapping("/me/nickname")
    @Operation(summary = "Update current nickname", description = "Update current user nickname")
    public Result<UserVO> updateCurrentNickname(@RequestBody UserNicknameUpdateDTO dto) {
        Long userId = getCurrentUserId();
        User updated = userService.updateNickname(userId, dto);
        return Result.success(UserVO.from(updated, null));
    }

    @DeleteMapping("/me")
    @Operation(summary = "Delete current user", description = "Deactivate current user")
    public Result<Boolean> deleteCurrentUser() {
        Long userId = getCurrentUserId();
        boolean success = userService.deleteUser(userId);
        return Result.success(success);
    }

    @GetMapping("/{userId}")
    @Operation(summary = "Get user", description = "Get user info by id")
    public Result<UserVO> getUser(
            @Parameter(description = "User id", required = true)
            @PathVariable Long userId) {
        // 1. 获取当前登录用户邮箱（过滤器已校验Token，无需手动传Authorization）
        String currentUserEmail = getCurrentUserEmail();
        // 2. 校验归属权（提取为公共方法）
        checkUserOwnership(userId, currentUserEmail);
        // 3. 核心业务逻辑
        User target = userService.getUserById(userId);
        return Result.success(UserVO.from(target, null));
    }

    @PutMapping("/{userId}/password")
    @Operation(summary = "Update password", description = "Update user password by id")
    public Result<UserVO> updatePassword(
            @Parameter(description = "User id", required = true)
            @PathVariable Long userId,
            @RequestBody UserPasswordUpdateDTO dto) {
        // 1. 获取当前登录用户邮箱
        String currentUserEmail = getCurrentUserEmail();
        // 2. 校验归属权
        checkUserOwnership(userId, currentUserEmail);
        // 3. 核心业务逻辑
        User updated = userService.updatePassword(userId, dto);
        return Result.success(UserVO.from(updated, null));
    }

    @PutMapping("/{userId}/nickname")
    @Operation(summary = "Update nickname", description = "Update user nickname by id")
    public Result<UserVO> updateNickname(
            @Parameter(description = "User id", required = true)
            @PathVariable Long userId,
            @RequestBody UserNicknameUpdateDTO dto) {
        // 1. 获取当前登录用户邮箱
        String currentUserEmail = getCurrentUserEmail();
        // 2. 校验归属权
        checkUserOwnership(userId, currentUserEmail);
        // 3. 核心业务逻辑
        User updated = userService.updateNickname(userId, dto);
        return Result.success(UserVO.from(updated, null));
    }

    @DeleteMapping("/{userId}")
    @Operation(summary = "Delete user", description = "Deactivate user by id")
    public Result<Boolean> deleteUser(
            @Parameter(description = "User id", required = true)
            @PathVariable Long userId) {
        // 1. 获取当前登录用户邮箱
        String currentUserEmail = getCurrentUserEmail();
        // 2. 校验归属权
        checkUserOwnership(userId, currentUserEmail);
        // 3. 核心业务逻辑
        boolean success = userService.deleteUser(userId);
        return Result.success(success);
    }
}