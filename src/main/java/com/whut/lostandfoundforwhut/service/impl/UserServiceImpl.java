package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.constant.Constants;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.UserPublicVO;
import com.whut.lostandfoundforwhut.service.IRedisService;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.Duration;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 用户服务实现
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final IRedisService redisService;

    private static final Duration USER_CACHE_TTL = Duration.ofMinutes(30);
    private static final String WHUT_EMAIL_SUFFIX = "@whut.edu.cn";

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    public User createUser(UserCreateDTO dto) {
        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "创建用户参数不能为空");
        }
        if (!StringUtils.hasText(dto.getEmail())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        String normalizedEmail = normalizeAndValidateWhutEmail(dto.getEmail());
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "确认密码不能为空");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码与确认密码不一致");
        }

        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, normalizedEmail));
        if (existing != null) {
            throw new AppException(ResponseCode.DUPLICATE_OPERATION.getCode(), "用户邮箱已存在");
        }

        User user = new User();
        user.setEmail(normalizedEmail);
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.insert(user);
        writeUserCache(user);
        return user;
    }

    @Override
    public User getUserById(Long userId) {
        if (userId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }
        String cachedEmailKey = buildUserEmailByIdKey(userId);
        Object cachedEmail = redisService.getValue(cachedEmailKey);
        if (cachedEmail instanceof String email && StringUtils.hasText(email)) {
            User cachedUser = readUserProfileCache(email);
            if (cachedUser != null) {
                return cachedUser;
            }
        }

        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        writeUserCache(user);
        return user;
    }

    @Override
    public UserPublicVO getPublicUserById(Long userId) {
        User user = getUserById(userId);
        return UserPublicVO.from(user);
    }

    @Override
    public Long getUserIdByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }

        Object cachedUserId = redisService.getValue(buildUserIdByEmailKey(email));
        if (cachedUserId instanceof Number number) {
            return number.longValue();
        }
        if (cachedUserId instanceof String text && StringUtils.hasText(text)) {
            try {
                return Long.parseLong(text);
            } catch (NumberFormatException ex) {
                redisService.remove(buildUserIdByEmailKey(email));
            }
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        writeUserCache(user);
        return user.getId();
    }

    @Override
    public void requireUserByEmail(String email) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        Long userId = getUserIdByEmail(email);
        if (userId == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
    }

    @Override
    public User updatePassword(Long userId, UserPasswordUpdateDTO dto) {
        if (userId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "修改密码参数不能为空");
        }
        if (!StringUtils.hasText(dto.getPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "新密码不能为空");
        }
        if (!StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "确认密码不能为空");
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码与确认密码不一致");
        }

        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userMapper.updateById(user);
        evictUserCache(user.getEmail(), user.getId());
        return user;
    }

    @Override
    public User updatePasswordByEmail(String email, String newPassword) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(newPassword)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "新密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        evictUserCache(user.getEmail(), user.getId());
        return user;
    }

    @Override
    public String getNicknameByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "Token不能为空");
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.isTokenValid(token)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), "登录状态无效");
        }

        String email;
        try {
            email = jwtUtil.getEmail(token);
        } catch (Exception ex) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), "Token无效或解析失败");
        }
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), "Token中邮箱信息无效");
        }
        User user = readUserProfileCache(email);
        if (user == null) {
            user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
            if (user != null) {
                writeUserCache(user);
            }
        }
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        return user.getNickname();
    }

    @Override
    public String getCurrentUserEmail() {
        if (!securityEnabled) {
            return null;
        }
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || authentication.getPrincipal() == null) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), "未登录或登录已过期");
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @Override
    public Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), "未登录或登录已过期");
        }
        return getUserIdByEmail(email);
    }

    @Override
    public User updateNickname(Long userId, UserNicknameUpdateDTO dto) {
        if (userId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        if (dto == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "昵称修改参数不能为空");
        }
        if (!StringUtils.hasText(dto.getNickname())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "昵称不能为空");
        }

        user.setNickname(dto.getNickname());
        userMapper.updateById(user);
        evictUserCache(user.getEmail(), user.getId());
        return user;
    }

    @Override
    public boolean deleteUser(Long userId) {
        if (userId == null) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "用户ID不能为空");
        }
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }

        user.setStatus(UserStatus.DEACTIVATED.getCode());
        boolean success = userMapper.updateById(user) > 0;
        if (success) {
            evictUserCache(user.getEmail(), user.getId());
        }
        return success;
    }

    @Override
    public User reactivateUser(String email, String password, String nickname) {
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "邮箱不能为空");
        }
        if (!StringUtils.hasText(password)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "密码不能为空");
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), "用户不存在");
        }
        if (!UserStatus.DEACTIVATED.getCode().equals(user.getStatus())) {
            throw new AppException(ResponseCode.USER_STATUS_INVALID.getCode(), "用户状态不允许该操作");
        }

        user.setPasswordHash(passwordEncoder.encode(password));
        if (StringUtils.hasText(nickname)) {
            user.setNickname(nickname);
        }
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.updateById(user);
        writeUserCache(user);
        return user;
    }

    private User readUserProfileCache(String email) {
        if (!StringUtils.hasText(email)) {
            return null;
        }
        Object cachedProfile = redisService.getValue(buildUserProfileByEmailKey(email));
        if (cachedProfile instanceof User user) {
            return user;
        }
        return null;
    }

    private void writeUserCache(User user) {
        if (user == null || user.getId() == null || !StringUtils.hasText(user.getEmail())) {
            return;
        }
        redisService.setValue(buildUserProfileByEmailKey(user.getEmail()), user, USER_CACHE_TTL);
        redisService.setValue(buildUserIdByEmailKey(user.getEmail()), user.getId(), USER_CACHE_TTL);
        redisService.setValue(buildUserEmailByIdKey(user.getId()), user.getEmail(), USER_CACHE_TTL);
    }

    private void evictUserCache(String email, Long userId) {
        if (StringUtils.hasText(email)) {
            redisService.remove(buildUserProfileByEmailKey(email));
            redisService.remove(buildUserIdByEmailKey(email));
        }
        if (userId != null) {
            redisService.remove(buildUserEmailByIdKey(userId));
        }
    }

    private String buildUserProfileByEmailKey(String email) {
        return Constants.RedisKey.USER_PROFILE_BY_EMAIL + email;
    }

    private String buildUserIdByEmailKey(String email) {
        return Constants.RedisKey.USER_ID_BY_EMAIL + email;
    }

    private String buildUserEmailByIdKey(Long userId) {
        return Constants.RedisKey.USER_EMAIL_BY_ID + userId;
    }

    private String normalizeAndValidateWhutEmail(String email) {
        String normalized = email == null ? "" : email.trim().toLowerCase();
        if (!normalized.endsWith(WHUT_EMAIL_SUFFIX)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), "仅支持武汉理工大学邮箱（@whut.edu.cn）注册");
        }
        return normalized;
    }
}
