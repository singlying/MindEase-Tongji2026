package com.mindease.service;

import com.mindease.pojo.dto.UserLoginDTO;
import com.mindease.pojo.dto.UserRegisterDTO;
import com.mindease.pojo.dto.UserUpdateDTO;
import com.mindease.pojo.entity.User;

public interface UserService {

    /**
     * 登录
     *
     * @param userLoginDTO
     * @param needAdmin    是否需要管理员权限
     * @return
     */
    User login(UserLoginDTO userLoginDTO, boolean needAdmin);

    /**
     * 注册
     *
     * @param userRegisterDTO
     * @return
     */
    User register(UserRegisterDTO userRegisterDTO);

    /**
     * 根据ID查询用户
     *
     * @param userId
     * @return
     */
    User getById(Long userId);

    /**
     * 更新用户信息
     *
     * @param userId
     * @param userUpdateDTO
     */
    void updateProfile(Long userId, UserUpdateDTO userUpdateDTO);
}
