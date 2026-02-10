package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.mail.EmailTemplate;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserLoginDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateByCodeDTO;
import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.AuthLoginResult;
import com.whut.lostandfoundforwhut.service.IAuthService;
import com.whut.lostandfoundforwhut.service.IRedisService;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 认证与注册服务实现
 */
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Duration REGISTER_CODE_TTL = Duration.ofSeconds(90);
    private static final Duration REGISTER_CODE_RATE_TTL = Duration.ofSeconds(60);
    private static final Duration PASSWORD_RESET_CODE_TTL = Duration.ofSeconds(90);
    private static final Duration PASSWORD_RESET_CODE_RATE_TTL = Duration.ofSeconds(60);
    private static final Duration LOGIN_FAIL_WINDOW = Duration.ofMinutes(5);
    private static final Duration LOGIN_LOCK_TTL = Duration.ofMinutes(5);
    private static final Duration RE_REGISTER_COOLDOWN = Duration.ofDays(10);
    private static final int LOGIN_MAX_FAILS = 5;

    private final IRedisService redisService;
    private final JavaMailSender mailSender;
    private final IUserService userService;
    private final UserMapper userMapper;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;

    @Value("${app.mail.from}")
    private String mailFrom;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    @Override
    public void sendRegisterCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(mailFrom)) {
            throw new AppException(ResponseCode.MAIL_CONFIG_INVALID.getCode(), "邮件发送配置无效");
        }

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        assertRegisterAllowed(existing);

        String rateKey = Constants.RedisKey.REGISTER_CODE_RATE + email;
        if (Boolean.TRUE.equals(redisService.isExists(rateKey))) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_RATE_LIMIT.getCode(), "验证码发送过于频繁，请稍后再试");
        }

        String code = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        String codeKey = Constants.RedisKey.REGISTER_CODE + email;
        redisService.setValue(codeKey, code, REGISTER_CODE_TTL);
        redisService.setValue(rateKey, "1", REGISTER_CODE_RATE_TTL);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(EmailTemplate.registerCodeSubject());
        message.setText(EmailTemplate.registerCodeBody(code, REGISTER_CODE_TTL.getSeconds()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new AppException(ResponseCode.MAIL_SEND_FAILED.getCode(), "邮件发送失败，请稍后重试", ex);
        }
    }

    @Override
    public User register(UserRegisterDTO dto) {
        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "注册参数不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "确认密码不能为空");
        }
        if (!StringUtils.hasText(dto.getCode())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "验证码不能为空");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码与确认密码不一致");
        }

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        boolean shouldReactivate = assertRegisterAllowed(existing);

        String codeKey = Constants.RedisKey.REGISTER_CODE + dto.getEmail();
        Object cachedCode = redisService.getValue(codeKey);
        if (cachedCode == null) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_EXPIRED.getCode(), "邮箱验证码已过期");
        }
        if (!cachedCode.toString().equals(dto.getCode())) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_INVALID.getCode(), "邮箱验证码错误");
        }
        redisService.remove(codeKey);

        if (shouldReactivate) {
            return userService.reactivateUser(dto.getEmail(), dto.getPassword(), dto.getNickname());
        }

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(dto.getEmail());
        createDTO.setPassword(dto.getPassword());
        createDTO.setConfirmPassword(dto.getConfirmPassword());
        createDTO.setNickname(dto.getNickname());
        return userService.createUser(createDTO);
    }

    @Override
    public AuthLoginResult login(UserLoginDTO dto) {
        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "登录参数不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码不能为空");
        }

        String email = dto.getEmail();
        String lockKey = Constants.RedisKey.LOGIN_LOCK + email;
        if (Boolean.TRUE.equals(redisService.isExists(lockKey))) {
            throw new AppException(ResponseCode.USER_LOGIN_LOCKED.getCode(), "登录失败次数过多，请5分钟后再试");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            recordLoginFailure(email);
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        if (user.getStatus() != null && !user.getStatus().equals(UserStatus.NORMAL.getCode())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "用户状态异常，无法登录");
        }

        try {
            authenticationManager.authenticate(new UsernamePasswordAuthenticationToken(email, dto.getPassword()));
        } catch (BadCredentialsException ex) {
            recordLoginFailure(email);
            throw new AppException(ResponseCode.USER_PASSWORD_ERROR.getCode(), "邮箱或密码错误");
        }

        clearLoginFailure(email);
        String token = jwtUtil.generateToken(email);
        String refreshToken = issueRefreshToken(email);
        return new AuthLoginResult(user, token, refreshToken);
    }

    @Override
    public AuthLoginResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "RefreshToken不能为空");
        }

        String refreshKey = Constants.RedisKey.REFRESH_TOKEN + refreshToken;
        Object cachedEmail = redisService.getValue(refreshKey);
        if (cachedEmail == null || !StringUtils.hasText(cachedEmail.toString())) {
            throw new AppException(ResponseCode.USER_REFRESH_TOKEN_INVALID.getCode(), "RefreshToken无效或已过期");
        }

        String email = cachedEmail.toString();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        if (user.getStatus() != null && !user.getStatus().equals(UserStatus.NORMAL.getCode())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "用户状态异常，无法刷新登录态");
        }

        String newRefreshToken = issueRefreshToken(email);
        String token = jwtUtil.generateToken(email);
        return new AuthLoginResult(user, token, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "RefreshToken不能为空");
        }
        String refreshKey = Constants.RedisKey.REFRESH_TOKEN + refreshToken;
        Object cachedEmail = redisService.getValue(refreshKey);
        if (cachedEmail == null || !StringUtils.hasText(cachedEmail.toString())) {
            throw new AppException(ResponseCode.USER_REFRESH_TOKEN_INVALID.getCode(), "RefreshToken无效或已过期");
        }
        String email = cachedEmail.toString();
        redisService.remove(refreshKey);
        redisService.remove(Constants.RedisKey.REFRESH_TOKEN_BY_EMAIL + email);
    }

    @Override
    public void sendPasswordResetCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(mailFrom)) {
            throw new AppException(ResponseCode.MAIL_CONFIG_INVALID.getCode(), "邮件发送配置无效");
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (existing == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        String rateKey = Constants.RedisKey.PASSWORD_RESET_CODE_RATE + email;
        if (Boolean.TRUE.equals(redisService.isExists(rateKey))) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_RATE_LIMIT.getCode(), "重置密码验证码发送过于频繁，请稍后再试");
        }
        String code = String.format("%04d", ThreadLocalRandom.current().nextInt(0, 10000));
        String codeKey = Constants.RedisKey.PASSWORD_RESET_CODE + email;
        redisService.setValue(codeKey, code, PASSWORD_RESET_CODE_TTL);
        redisService.setValue(rateKey, "1", PASSWORD_RESET_CODE_RATE_TTL);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject(EmailTemplate.passwordResetSubject());
        message.setText(EmailTemplate.passwordResetBody(code, PASSWORD_RESET_CODE_TTL.getSeconds()));
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            throw new AppException(ResponseCode.MAIL_SEND_FAILED.getCode(), "邮件发送失败，请稍后重试", ex);
        }
    }

    @Override
    public User resetPasswordByCode(String email, UserPasswordUpdateByCodeDTO dto) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "重置密码参数不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "新密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "确认密码不能为空");
        }
        if (!StringUtils.hasText(dto.getCode())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "验证码不能为空");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码与确认密码不一致");
        }
        String codeKey = Constants.RedisKey.PASSWORD_RESET_CODE + email;
        Object cachedCode = redisService.getValue(codeKey);
        if (cachedCode == null) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_EXPIRED.getCode(), "重置密码验证码已过期");
        }
        if (!cachedCode.toString().equals(dto.getCode())) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_INVALID.getCode(), "重置密码验证码错误");
        }
        redisService.remove(codeKey);
        return userService.updatePasswordByEmail(email, dto.getPassword());
    }

    private void recordLoginFailure(String email) {
        String failKey = Constants.RedisKey.LOGIN_FAIL_COUNT + email;
        Long count = redisService.increment(failKey);
        if (count != null && count == 1) {
            redisService.expire(failKey, LOGIN_FAIL_WINDOW);
        }
        if (count != null && count >= LOGIN_MAX_FAILS) {
            String lockKey = Constants.RedisKey.LOGIN_LOCK + email;
            redisService.setValue(lockKey, "1", LOGIN_LOCK_TTL);
            redisService.remove(failKey);
        }
    }

    private void clearLoginFailure(String email) {
        String failKey = Constants.RedisKey.LOGIN_FAIL_COUNT + email;
        String lockKey = Constants.RedisKey.LOGIN_LOCK + email;
        redisService.remove(failKey);
        redisService.remove(lockKey);
    }

    private String issueRefreshToken(String email) {
        String oldTokenKey = Constants.RedisKey.REFRESH_TOKEN_BY_EMAIL + email;
        Object oldToken = redisService.getValue(oldTokenKey);
        if (oldToken != null && StringUtils.hasText(oldToken.toString())) {
            redisService.remove(Constants.RedisKey.REFRESH_TOKEN + oldToken.toString());
        }
        String refreshToken = UUID.randomUUID().toString().replace("-", "");
        String refreshKey = Constants.RedisKey.REFRESH_TOKEN + refreshToken;
        redisService.setValue(refreshKey, email, Duration.ofMillis(refreshExpirationMs));
        redisService.setValue(oldTokenKey, refreshToken, Duration.ofMillis(refreshExpirationMs));
        return refreshToken;
    }

    private boolean assertRegisterAllowed(User existing) {
        if (existing == null) {
            return false;
        }
        if (UserStatus.BANNED.getCode().equals(existing.getStatus())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "账号已封禁，无法注册");
        }
        if (UserStatus.DEACTIVATED.getCode().equals(existing.getStatus())) {
            LocalDateTime updatedAt = existing.getUpdatedAt();
            if (updatedAt == null) {
                throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "账号注销后需等待10天才能重新注册");
            }
            LocalDateTime allowAt = updatedAt.plus(RE_REGISTER_COOLDOWN);
            if (LocalDateTime.now().isBefore(allowAt)) {
                throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "账号注销后需等待10天才能重新注册");
            }
            return true;
        }
        throw new AppException(ResponseCode.USER_EMAIL_EXISTS.getCode(), "邮箱已存在，无法重复注册");
    }
}
