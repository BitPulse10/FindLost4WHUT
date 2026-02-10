package com.whut.lostandfoundforwhut.controller;

import com.whut.lostandfoundforwhut.common.result.Result;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.model.dto.PasswordResetCodeSendDTO;
import com.whut.lostandfoundforwhut.model.dto.PasswordResetDTO;
import com.whut.lostandfoundforwhut.model.dto.RefreshTokenDTO;
import com.whut.lostandfoundforwhut.model.dto.RegisterCodeSendDTO;
import com.whut.lostandfoundforwhut.model.dto.UserLoginDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateByCodeDTO;
import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.AuthLoginResult;
import com.whut.lostandfoundforwhut.model.vo.UserVO;
import com.whut.lostandfoundforwhut.service.IAuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "认证与注册相关接口")
public class AuthController {

    private final IAuthService authService;
    private final JwtUtil jwtUtil;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @PostMapping("/register/code")
    @Operation(summary = "发送注册验证码", description = "发送 4 位邮箱验证码用于注册")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_003", description = "邮箱已存在"),
            @ApiResponse(responseCode = "USR_006", description = "验证码发送过于频繁"),
            @ApiResponse(responseCode = "MAIL_001", description = "邮箱配置缺失或无效"),
            @ApiResponse(responseCode = "MAIL_002", description = "邮件发送失败")
    })
    public Result<Boolean> sendRegisterCode(@RequestBody RegisterCodeSendDTO dto) {
        authService.sendRegisterCode(dto == null ? null : dto.getEmail());
        return Result.success(true);
    }

    @PostMapping("/register")
    @Operation(summary = "注册", description = "使用邮箱验证码注册用户")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_003", description = "邮箱已存在"),
            @ApiResponse(responseCode = "USR_004", description = "邮箱验证码无效"),
            @ApiResponse(responseCode = "USR_005", description = "邮箱验证码已过期")
    })
    public Result<UserVO> register(@RequestBody UserRegisterDTO dto) {
        User user = authService.register(dto);
        return Result.success(UserVO.from(user, null));
    }

    @PostMapping("/login")
    @Operation(summary = "登录", description = "使用邮箱和密码登录")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_001", description = "用户不存在"),
            @ApiResponse(responseCode = "USR_007", description = "邮箱或密码错误"),
            @ApiResponse(responseCode = "USR_008", description = "登录失败次数过多，请5分钟后再试")
    })
    public Result<UserVO> login(@RequestBody UserLoginDTO dto) {
        AuthLoginResult result = authService.login(dto);
        UserVO vo = UserVO.from(result.getUser(), result.getToken());
        vo.setExpiresIn(jwtUtil.getExpirationMs());
        vo.setRefreshToken(result.getRefreshToken());
        vo.setRefreshExpiresIn(refreshExpirationMs);
        return Result.success(vo);
    }

    @PostMapping("/password/code")
    @Operation(summary = "发送找回密码验证码", description = "发送邮箱验证码用于找回密码")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_001", description = "用户不存在"),
            @ApiResponse(responseCode = "USR_012", description = "验证码发送过于频繁"),
            @ApiResponse(responseCode = "MAIL_001", description = "邮箱配置缺失或无效"),
            @ApiResponse(responseCode = "MAIL_002", description = "邮件发送失败")
    })
    public Result<Boolean> sendPasswordResetCode(@RequestBody PasswordResetCodeSendDTO dto) {
        authService.sendPasswordResetCode(dto == null ? null : dto.getEmail());
        return Result.success(true);
    }

    @PostMapping("/password/reset")
    @Operation(summary = "重置密码", description = "使用邮箱验证码重置密码")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_001", description = "用户不存在"),
            @ApiResponse(responseCode = "USR_010", description = "验证码无效"),
            @ApiResponse(responseCode = "USR_011", description = "验证码已过期")
    })
    public Result<UserVO> resetPassword(@RequestBody PasswordResetDTO dto) {
        UserPasswordUpdateByCodeDTO mapped = new UserPasswordUpdateByCodeDTO();
        mapped.setPassword(dto == null ? null : dto.getPassword());
        mapped.setConfirmPassword(dto == null ? null : dto.getConfirmPassword());
        mapped.setCode(dto == null ? null : dto.getCode());
        User user = authService.resetPasswordByCode(dto == null ? null : dto.getEmail(), mapped);
        return Result.success(UserVO.from(user, null));
    }

    @PostMapping("/refresh")
    @Operation(summary = "刷新令牌", description = "使用 refresh token 刷新 access token")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_009", description = "Refresh token 无效")
    })
    public Result<UserVO> refresh(@RequestBody RefreshTokenDTO dto) {
        AuthLoginResult result = authService.refresh(dto == null ? null : dto.getRefreshToken());
        UserVO vo = UserVO.from(result.getUser(), result.getToken());
        vo.setExpiresIn(jwtUtil.getExpirationMs());
        vo.setRefreshToken(result.getRefreshToken());
        vo.setRefreshExpiresIn(refreshExpirationMs);
        return Result.success(vo);
    }

    @PostMapping("/logout")
    @Operation(summary = "退出登录", description = "删除 refresh token 退出登录")
    @ApiResponses({
            @ApiResponse(responseCode = "0000", description = "成功"),
            @ApiResponse(responseCode = "0002", description = "非法参数"),
            @ApiResponse(responseCode = "USR_009", description = "Refresh token 无效")
    })
    public Result<Boolean> logout(@RequestBody RefreshTokenDTO dto) {
        authService.logout(dto == null ? null : dto.getRefreshToken());
        return Result.success(true);
    }
}