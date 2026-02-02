package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.UserVO;
import com.whut.lostandfoundforwhut.service.IUserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 用户相关接口
 */
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Tag(name = "User Management", description = "用户相关接口")
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    @Operation(summary = "获取当前用户", description = "获取当前登录用户信息")
    public Result<UserVO> getCurrentUser() {
        Long userId = userService.getCurrentUserId();
        User target = userService.getUserById(userId);
        return Result.success(UserVO.from(target, null));
    }

    @PutMapping("/me/nickname")
    @Operation(summary = "修改当前昵称", description = "修改当前用户昵称")
    public Result<UserVO> updateCurrentNickname(@RequestBody UserNicknameUpdateDTO dto) {
        Long userId = userService.getCurrentUserId();
        User updated = userService.updateNickname(userId, dto);
        return Result.success(UserVO.from(updated, null));
    }

    @DeleteMapping("/me")
    @Operation(summary = "注销当前用户", description = "逻辑停用当前用户")
    public Result<Boolean> deleteCurrentUser() {
        Long userId = userService.getCurrentUserId();
        boolean success = userService.deleteUser(userId);
        return Result.success(success);
    }

}