package com.mindease.service.impl;

import com.mindease.common.constant.MessageConstant;
import com.mindease.common.constant.StatusConstant;
import com.mindease.common.exception.*;
import com.mindease.mapper.UserMapper;
import com.mindease.pojo.dto.UserLoginDTO;
import com.mindease.pojo.dto.UserRegisterDTO;
import com.mindease.pojo.dto.UserUpdateDTO;
import com.mindease.pojo.entity.User;
import com.mindease.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    /**
     * 登录
     *
     * @param userLoginDTO
     * @param needAdmin    是否需要管理员权限
     * @return
     */
    @Override
    public User login(UserLoginDTO userLoginDTO, boolean needAdmin) {
        String username = userLoginDTO.getUsername();
        String password = userLoginDTO.getPassword();

        User user = userMapper.getByUsername(username);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.USER_NOT_FOUND);
        }

        // 进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());
        if (!password.equals(user.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        // 检查账号状态
        if (user.getStatus() == StatusConstant.PENDING) {
            // 咨询师在待审核状态下仍可登录，用于提交/查看审核资料
            if ("COUNSELOR".equalsIgnoreCase(user.getRole())) {
                // 允许咨询师登录，但功能受限（只能访问审核相关接口）
                // 不抛出异常，继续登录流程
            } else {
                // 其他角色不允许待审核状态登录
            throw new AccountPendingException(MessageConstant.ACCOUNT_PENDING);
            }
        }

        if (user.getStatus() == StatusConstant.DISABLE) {
            //账号被禁用
            throw new AccountLockedException(MessageConstant.ACCOUNT_DISABLED);
        }

        // 防止用户使用管理端登录
        if (needAdmin) {
            if (!"ADMIN".equals(user.getRole()) && !"ROOT".equals(user.getRole())) {
                throw new PermissionDeniedException("权限不足，非管理员禁止访问");
            }
        }

        return user;
    }

    /**
     * 注册
     *
     * @param userRegisterDTO
     * @return
     */
    @Override
    @Transactional
    public User register(UserRegisterDTO userRegisterDTO) {
        String username = userRegisterDTO.getUsername();

        // 检查用户名是否已存在
        User existUser = userMapper.getByUsername(username);
        if (existUser != null) {
            throw new UsernameAlreadyExistsException(MessageConstant.USERNAME_ALREADY_EXISTS);
        }

        String role = userRegisterDTO.getRole();
        if (role == null || role.trim().isEmpty()) {
            role = "user";
        }
        role = role.toLowerCase();

        // 验证角色类型：只允许 user 和 counselor 注册，admin 不能通过注册接口创建
        if (!"user".equals(role) && !"counselor".equals(role)) {
            throw new BaseException(MessageConstant.INVALID_ROLE);
        }

        // 创建用户
        User user = User.builder()
                .username(username)
                .password(DigestUtils.md5DigestAsHex(userRegisterDTO.getPassword().getBytes()))
                .nickname(userRegisterDTO.getNickname())
                .phone(userRegisterDTO.getPhone())
                .role(role.toUpperCase())
                .status("user".equals(role) ? StatusConstant.ENABLE : StatusConstant.PENDING)
                .createTime(LocalDateTime.now())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.insert(user);

        return user;
    }

    /**
     * 根据ID查询用户
     *
     * @param userId
     * @return
     */
    @Override
    public User getById(Long userId) {
        User user = userMapper.getById(userId);
        if (user == null) {
            throw new AccountNotFoundException(MessageConstant.USER_NOT_FOUND);
        }
        return user;
    }

    /**
     * 更新用户信息
     *
     * @param userId
     * @param userUpdateDTO
     */
    @Override
    public void updateProfile(Long userId, UserUpdateDTO userUpdateDTO) {
        User user = User.builder()
                .id(userId)
                .nickname(userUpdateDTO.getNickname())
                .avatar(userUpdateDTO.getAvatar())
                .updateTime(LocalDateTime.now())
                .build();

        userMapper.update(user);
    }
}
