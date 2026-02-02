package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.mail.EmailTemplate;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserLoginDTO;
import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateByCodeDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.AuthLoginResult;
import com.whut.lostandfoundforwhut.service.IAuthService;
import com.whut.lostandfoundforwhut.service.IRedisService;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;
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
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!StringUtils.hasText(mailFrom)) {
            throw new AppException(ResponseCode.MAIL_CONFIG_INVALID.getCode(), ResponseCode.MAIL_CONFIG_INVALID.getInfo());
        }

        // 已存在账号不允许重复注册
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (existing != null) {
            throw new AppException(ResponseCode.USER_EMAIL_EXISTS.getCode(), ResponseCode.USER_EMAIL_EXISTS.getInfo());
        }

        String rateKey = Constants.RedisKey.REGISTER_CODE_RATE + email;
        if (Boolean.TRUE.equals(redisService.isExists(rateKey))) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_RATE_LIMIT.getCode(),
                    ResponseCode.USER_EMAIL_CODE_RATE_LIMIT.getInfo());
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
            throw new AppException(ResponseCode.MAIL_SEND_FAILED.getCode(), ResponseCode.MAIL_SEND_FAILED.getInfo(), ex);
        }
    }

    @Override
    public User register(UserRegisterDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getEmail())
                || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())
                || !StringUtils.hasText(dto.getCode())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (existing != null) {
            throw new AppException(ResponseCode.USER_EMAIL_EXISTS.getCode(), ResponseCode.USER_EMAIL_EXISTS.getInfo());
        }

        String codeKey = Constants.RedisKey.REGISTER_CODE + dto.getEmail();
        Object cachedCode = redisService.getValue(codeKey);
        if (cachedCode == null) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_EXPIRED.getCode(),
                    ResponseCode.USER_EMAIL_CODE_EXPIRED.getInfo());
        }
        if (!cachedCode.toString().equals(dto.getCode())) {
            throw new AppException(ResponseCode.USER_EMAIL_CODE_INVALID.getCode(),
                    ResponseCode.USER_EMAIL_CODE_INVALID.getInfo());
        }
        // 验证码通过后立即失效
        redisService.remove(codeKey);

        UserCreateDTO createDTO = new UserCreateDTO();
        createDTO.setEmail(dto.getEmail());
        createDTO.setPassword(dto.getPassword());
        createDTO.setConfirmPassword(dto.getConfirmPassword());
        createDTO.setNickname(dto.getNickname());
        return userService.createUser(createDTO);
    }

    @Override
    public AuthLoginResult login(UserLoginDTO dto) {
        if (dto == null || !StringUtils.hasText(dto.getEmail()) || !StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        String email = dto.getEmail();
        String lockKey = Constants.RedisKey.LOGIN_LOCK + email;
        if (Boolean.TRUE.equals(redisService.isExists(lockKey))) {
            throw new AppException(ResponseCode.USER_LOGIN_LOCKED.getCode(), ResponseCode.USER_LOGIN_LOCKED.getInfo());
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            recordLoginFailure(email);
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        if (user.getStatus() != null && !user.getStatus().equals(com.whut.lostandfoundforwhut.common.enums.user.UserStatus.NORMAL.getCode())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), ResponseCode.USER_STATUS_INVALID.getInfo());
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(email, dto.getPassword()));
        } catch (BadCredentialsException ex) {
            recordLoginFailure(email);
            throw new AppException(ResponseCode.USER_PASSWORD_ERROR.getCode(), ResponseCode.USER_PASSWORD_ERROR.getInfo());
        }

        clearLoginFailure(email);
        String token = jwtUtil.generateToken(email);
        String refreshToken = issueRefreshToken(email);
        return new AuthLoginResult(user, token, refreshToken);
    }

    @Override
    public AuthLoginResult refresh(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        String refreshKey = Constants.RedisKey.REFRESH_TOKEN + refreshToken;
        Object cachedEmail = redisService.getValue(refreshKey);
        if (cachedEmail == null || !StringUtils.hasText(cachedEmail.toString())) {
            throw new AppException(ResponseCode.USER_REFRESH_TOKEN_INVALID.getCode(),
                    ResponseCode.USER_REFRESH_TOKEN_INVALID.getInfo());
        }

        String email = cachedEmail.toString();
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        if (user.getStatus() != null && !user.getStatus().equals(com.whut.lostandfoundforwhut.common.enums.user.UserStatus.NORMAL.getCode())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), ResponseCode.USER_STATUS_INVALID.getInfo());
        }

        // 刷新时轮换 refresh token

        String newRefreshToken = issueRefreshToken(email);
        String token = jwtUtil.generateToken(email);
        return new AuthLoginResult(user, token, newRefreshToken);
    }

    @Override
    public void logout(String refreshToken) {
        if (!StringUtils.hasText(refreshToken)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        String refreshKey = Constants.RedisKey.REFRESH_TOKEN + refreshToken;
        Object cachedEmail = redisService.getValue(refreshKey);
        if (cachedEmail == null || !StringUtils.hasText(cachedEmail.toString())) {
            throw new AppException(ResponseCode.USER_REFRESH_TOKEN_INVALID.getCode(),
                    ResponseCode.USER_REFRESH_TOKEN_INVALID.getInfo());
        }
        String email = cachedEmail.toString();
        redisService.remove(refreshKey);
        redisService.remove(Constants.RedisKey.REFRESH_TOKEN_BY_EMAIL + email);
    }


    @Override
    public void sendPasswordResetCode(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!StringUtils.hasText(mailFrom)) {
            throw new AppException(ResponseCode.MAIL_CONFIG_INVALID.getCode(), ResponseCode.MAIL_CONFIG_INVALID.getInfo());
        }
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (existing == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        String rateKey = Constants.RedisKey.PASSWORD_RESET_CODE_RATE + email;
        if (Boolean.TRUE.equals(redisService.isExists(rateKey))) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_RATE_LIMIT.getCode(), ResponseCode.USER_PASSWORD_CODE_RATE_LIMIT.getInfo());
        }
        String code = String.format("%04d", java.util.concurrent.ThreadLocalRandom.current().nextInt(0, 10000));
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
            throw new AppException(ResponseCode.MAIL_SEND_FAILED.getCode(), ResponseCode.MAIL_SEND_FAILED.getInfo(), ex);
        }
    }

    @Override
    public User resetPasswordByCode(String email, UserPasswordUpdateByCodeDTO dto) {
        if (!StringUtils.hasText(email) || dto == null || !StringUtils.hasText(dto.getPassword()) || !StringUtils.hasText(dto.getConfirmPassword()) || !StringUtils.hasText(dto.getCode())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        String codeKey = Constants.RedisKey.PASSWORD_RESET_CODE + email;
        Object cachedCode = redisService.getValue(codeKey);
        if (cachedCode == null) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_EXPIRED.getCode(), ResponseCode.USER_PASSWORD_CODE_EXPIRED.getInfo());
        }
        if (!cachedCode.toString().equals(dto.getCode())) {
            throw new AppException(ResponseCode.USER_PASSWORD_CODE_INVALID.getCode(), ResponseCode.USER_PASSWORD_CODE_INVALID.getInfo());
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
}
