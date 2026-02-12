package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;
import com.whut.lostandfoundforwhut.model.vo.UserPublicVO;

/**
 * @author DXR
 * @date 2026/01/31
 * @description 用户服务接口
 */
public interface IUserService {

    /**
     * 创建用户
     * @param dto 创建参数
     * @return 用户实体
     */
    User createUser(UserCreateDTO dto);

    /**
     * 根据ID获取用户
     * @param userId 用户ID
     * @return 用户实体
     */
    User getUserById(Long userId);

    /**
     * 根据ID获取用户公开信息（用于对外展示）
     * @param userId 用户ID
     * @return 用户公开信息
     */
    UserPublicVO getPublicUserById(Long userId);

    /**
     * 根据邮箱获取用户ID
     * @param email 邮箱
     * @return 用户ID
     */
    Long getUserIdByEmail(String email);

    /**
     * 校验邮箱对应用户存在
     * @param email 邮箱
     */
    void requireUserByEmail(String email);

    /**
     * 更新密码
     * @param userId 用户ID
     * @param dto 密码更新参数
     * @return 更新后的用户
     */
    User updatePassword(Long userId, UserPasswordUpdateDTO dto);

    /**
     * 通过邮箱更新密码
     * @param email 邮箱
     * @param newPassword 新密码
     * @return 更新后的用户
     */
    User updatePasswordByEmail(String email, String newPassword);

    /**
     * 通过 Token 解析邮箱并获取昵称
     * @param token JWT Token
     * @return 昵称（可能为空）
     */
    String getNicknameByToken(String token);

    /**
     * 获取当前登录用户邮箱（从 Security 上下文）
     * @return 邮箱
     */
    String getCurrentUserEmail();

    /**
     * 获取当前登录用户ID（从 Security 上下文）
     * @return 用户ID
     */
    Long getCurrentUserId();

    /**
     * 更新昵称
     * @param userId 用户ID
     * @param dto 昵称更新参数
     * @return 更新后的用户
     */
    User updateNickname(Long userId, UserNicknameUpdateDTO dto);

    /**
     * 删除用户（逻辑停用）
     * @param userId 用户ID
     * @return 是否成功
     */
    boolean deleteUser(Long userId);

    /**
     * 重新激活已注销账号
     * @param email 邮箱
     * @param password 新密码
     * @param nickname 新昵称（可为空）
     * @return 更新后的用户
     */
    User reactivateUser(String email, String password, String nickname);
}
