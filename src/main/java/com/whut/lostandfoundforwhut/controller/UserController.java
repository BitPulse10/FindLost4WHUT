package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.UserVO;
import com.whut.lostandfoundforwhut.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
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
    private final JwtUtil jwtUtil;

    // 新增：获取当前登录用户的邮箱（从Security上下文，替代原requireEmail方法）
    private String getCurrentUserEmail() {
        // 从Security上下文获取已认证的用户信息（过滤器已完成Token校验）
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        // 未认证的情况会被过滤器拦截，这里无需额外判断
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername(); // 注意：这里的username实际是用户邮箱（和过滤器逻辑一致）
    }

    // 新增：校验用户归属权（提取为公共方法，避免重复）
    private void checkUserOwnership(Long userId, String currentUserEmail) {
        User target = userService.getUserById(userId);
        if (!currentUserEmail.equals(target.getEmail())) {
            throw new AppException(ResponseCode.NO_PERMISSION.getCode(), ResponseCode.NO_PERMISSION.getInfo());
        }
    }

    @PostMapping
    @Operation(summary = "Create user", description = "Create user and return token")
    public Result<UserVO> createUser(@RequestBody UserCreateDTO dto) {
        // 保留原有逻辑（注册接口无需认证）
        User user = userService.createUser(dto);
        String token = jwtUtil.generateToken(user.getEmail());
        return Result.success(UserVO.from(user, token));
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

    @PutMapping("/{userId}")
    @Operation(summary = "Update user status", description = "Update user status by id")
    public Result<UserVO> updateUser(
            @Parameter(description = "User id", required = true)
            @PathVariable Long userId,
            @RequestBody UserUpdateDTO dto) {
        // 1. 获取当前登录用户邮箱
        String currentUserEmail = getCurrentUserEmail();
        // 2. 校验归属权
        checkUserOwnership(userId, currentUserEmail);
        // 3. 核心业务逻辑
        User updated = userService.updateUser(userId, dto);
        return Result.success(UserVO.from(updated, null));
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