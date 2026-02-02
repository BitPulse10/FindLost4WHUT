package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.common.utils.security.jwt.JwtUtil;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

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

    @Value("${app.security.enabled:false}")
    private boolean securityEnabled;

    @Override
    public User createUser(UserCreateDTO dto) {
        // 校验创建参数
        if (dto == null || !StringUtils.hasText(dto.getEmail())
                || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 检查邮箱是否重复
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (existing != null) {
            throw new AppException(ResponseCode.DUPLICATE_OPERATION.getCode(), ResponseCode.DUPLICATE_OPERATION.getInfo());
        }

        // 构建并保存用户
        User user = new User();
        user.setEmail(dto.getEmail());
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setNickname(dto.getNickname());
        user.setStatus(UserStatus.NORMAL.getCode());
        userMapper.insert(user);
        return user;
    }

    @Override
    public User getUserById(Long userId) {
        // 根据ID查询
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        return user;
    }

    @Override
    public Long getUserIdByEmail(String email) {
        // 校验邮箱
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 根据邮箱查询
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        return user.getId();
    }

    @Override
    public void requireUserByEmail(String email) {
        // 校验邮箱
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 确保用户存在
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
    }

    @Override
    public User updatePassword(Long userId, UserPasswordUpdateDTO dto) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // 校验密码与确认密码
        if (dto == null || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 更新密码
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userMapper.updateById(user);
        return user;
    }

    @Override
    public User updatePasswordByEmail(String email, String newPassword) {
        if (!StringUtils.hasText(email) || !StringUtils.hasText(newPassword)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userMapper.updateById(user);
        return user;
    }

    @Override
    public String getNicknameByToken(String token) {
        if (!StringUtils.hasText(token)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (token.startsWith("Bearer ")) {
            token = token.substring(7);
        }
        if (!jwtUtil.isTokenValid(token)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        String email = jwtUtil.getEmail(token);
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
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
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        UserDetails userDetails = (UserDetails) authentication.getPrincipal();
        return userDetails.getUsername();
    }

    @Override
    public Long getCurrentUserId() {
        String email = getCurrentUserEmail();
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.NOT_LOGIN.getCode(), ResponseCode.NOT_LOGIN.getInfo());
        }
        return getUserIdByEmail(email);
    }

    @Override
    public User updateNickname(Long userId, UserNicknameUpdateDTO dto) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // 校验昵称
        if (dto == null || !StringUtils.hasText(dto.getNickname())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // 更新昵称
        user.setNickname(dto.getNickname());
        userMapper.updateById(user);
        return user;
    }

    @Override
    public boolean deleteUser(Long userId) {
        // 查询用户
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // 逻辑停用
        user.setStatus(UserStatus.DEACTIVATED.getCode());
        return userMapper.updateById(user) > 0;
    }
}