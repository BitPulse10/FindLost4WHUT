package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.whut.lostandfoundforwhut.common.enums.ResponseCode;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.common.exception.AppException;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.service.IUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * @author DXR
 * @date 2026/01/31
 * @description User service implementation
 */
@Service
@RequiredArgsConstructor
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    @Override
    public User createUser(UserCreateDTO dto) {
        // validate create params
        if (dto == null || !StringUtils.hasText(dto.getEmail())
                || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // check duplicate email
        User existing = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, dto.getEmail()));
        if (existing != null) {
            throw new AppException(ResponseCode.DUPLICATE_OPERATION.getCode(), ResponseCode.DUPLICATE_OPERATION.getInfo());
        }

        // build and save user
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
        // query by id
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        return user;
    }

    @Override
    public Long getUserIdByEmail(String email) {
        // validate email
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // query by email
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
        return user.getId();
    }

    @Override
    public void requireUserByEmail(String email) {
        // validate email
        if (!StringUtils.hasText(email)) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // ensure user exists
        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }
    }

    @Override
    public User updateUser(Long userId, UserUpdateDTO dto) {
        // query user
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // update status only
        if (dto != null && dto.getStatus() != null) {
            user.setStatus(dto.getStatus());
        }
        userMapper.updateById(user);
        return user;
    }

    @Override
    public User updatePassword(Long userId, UserPasswordUpdateDTO dto) {
        // query user
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // validate password confirm
        if (dto == null || !StringUtils.hasText(dto.getPassword())
                || !StringUtils.hasText(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }
        if (!dto.getPassword().equals(dto.getConfirmPassword())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // update password
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        userMapper.updateById(user);
        return user;
    }

    @Override
    public User updateNickname(Long userId, UserNicknameUpdateDTO dto) {
        // query user
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // validate nickname
        if (dto == null || !StringUtils.hasText(dto.getNickname())) {
            throw new AppException(ResponseCode.ILLEGAL_PARAMETER.getCode(), ResponseCode.ILLEGAL_PARAMETER.getInfo());
        }

        // update nickname
        user.setNickname(dto.getNickname());
        userMapper.updateById(user);
        return user;
    }

    @Override
    public boolean deleteUser(Long userId) {
        // query user
        User user = userMapper.selectById(userId);
        if (user == null) {
            throw new AppException(ResponseCode.USER_NOT_FOUND.getCode(), ResponseCode.USER_NOT_FOUND.getInfo());
        }

        // logical deactivation
        user.setStatus(UserStatus.DEACTIVATED.getCode());
        return userMapper.updateById(user) > 0;
    }
}
