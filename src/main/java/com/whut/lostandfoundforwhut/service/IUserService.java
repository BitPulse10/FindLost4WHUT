package com.whut.lostandfoundforwhut.service;

import com.whut.lostandfoundforwhut.model.dto.UserCreateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserNicknameUpdateDTO;
import com.whut.lostandfoundforwhut.model.dto.UserPasswordUpdateDTO;
import com.whut.lostandfoundforwhut.model.entity.User;

/**
 * @author DXR
 * @date 2026/01/31
 * @description User service interface
 */
public interface IUserService {

    /**
     * create user
     *
     * @param dto create params
     * @return user entity
     */
    User createUser(UserCreateDTO dto);

    /**
     * get user by id
     *
     * @param userId user id
     * @return user entity
     */
    User getUserById(Long userId);

    /**
     * get user id by email
     *
     * @param email user email
     * @return user id
     */
    Long getUserIdByEmail(String email);

    /**
     * verify user exists by email
     *
     * @param email user email
     */
    void requireUserByEmail(String email);

    /**
     * update user password
     *
     * @param userId user id
     * @param dto    password update params
     * @return updated user
     */
    User updatePassword(Long userId, UserPasswordUpdateDTO dto);

    /**
     * update user nickname
     *
     * @param userId user id
     * @param dto    nickname update params
     * @return updated user
     */
    User updateNickname(Long userId, UserNicknameUpdateDTO dto);

    /**
     * delete user (logical deactivation)
     *
     * @param userId user id
     * @return whether success
     */
    boolean deleteUser(Long userId);

}
