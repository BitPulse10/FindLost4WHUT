package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.dto.UserRegisterDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.dto.UserLoginDTO;
import com.whut.lostandfoundforwhut.model.vo.AuthLoginResult;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateByCodeDTO;

/**
 * @author DXR
 * @date 2026/02/02
 * @description 认证与注册服务接口
 */
public interface IAuthService {

    /**
     * 发送注册邮箱验证码（4位）
     * @param email 邮箱
     */
    void sendRegisterCode(String email);

    /**
     * 注册用户（邮箱验证码校验）
     * @param dto 注册参数
     * @return 创建后的用户
     */
    User register(UserRegisterDTO dto);

    /**
     * 登录
     * @param dto 登录参数
     * @return 登录结果（用户 + token）
     */
    AuthLoginResult login(UserLoginDTO dto);

    /**
     * 刷新登录（使用 refresh token）
     * @param refreshToken refresh token
     * @return 登录结果（用户 + token + refresh token）
     */
    AuthLoginResult refresh(String refreshToken);

    /**
     * 退出登录（删除 refresh token）
     * @param refreshToken refresh token
     */
    void logout(String refreshToken);

    /**
     * 发送找回密码验证码（4位）
     * @param email 邮箱
     */
    void sendPasswordResetCode(String email);

    /**
     * 通过验证码重置密码
     * @param email 邮箱
     * @param dto 重置参数
     * @return 更新后的用户
     */
    User resetPasswordByCode(String email, UserPasswordUpdateByCodeDTO dto);
}