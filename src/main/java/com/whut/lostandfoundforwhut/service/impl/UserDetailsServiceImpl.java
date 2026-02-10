package com.whut.lostandfoundforwhut.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.whut.lostandfoundforwhut.common.enums.user.UserStatus;
import com.whut.lostandfoundforwhut.mapper.UserMapper;
import com.whut.lostandfoundforwhut.model.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;

/**
 * @author DXR
 * @date 2026/02/01
 * @description UserDetailsService implementation backed by users table
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserMapper userMapper;

    @Override
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (!StringUtils.hasText(email)) {
            throw new UsernameNotFoundException("Email is blank");
        }

        User user = userMapper.selectOne(new LambdaQueryWrapper<User>().eq(User::getEmail, email));
        if (user == null) {
            throw new UsernameNotFoundException("User not found: " + email);
        }
        if (user.getStatus() != null && !user.getStatus().equals(UserStatus.NORMAL.getCode())) {
            throw new UsernameNotFoundException("User disabled: " + email);
        }

        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail())
                .password(user.getPasswordHash())
                .authorities(Collections.emptyList())
                .build();
    }
}
