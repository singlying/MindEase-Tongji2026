package com.mindease.mapper;

import com.mindease.pojo.entity.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {

    /**
     * 根据用户名查询用户
     *
     * @param username 用户名
     * @return 用户
     */
    @Select("select * from sys_user where username = #{username}")
    User getByUsername(String username);

    /**
     * 插入用户
     *
     * @param user 用户
     */
    @Insert("insert into sys_user(username, password, nickname, phone, avatar, role, status, create_time, update_time) " +
            "values(#{username}, #{password}, #{nickname}, #{phone}, #{avatar}, #{role}, #{status}, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(User user);

    /**
     * 根据ID查询用户
     *
     * @param id 用户ID
     * @return 用户
     */
    @Select("select * from sys_user where id = #{id}")
    User getById(Long id);

    /**
     * 更新用户基本信息（昵称、头像）
     *
     * @param user 用户
     */
    @Update("update sys_user set nickname = #{nickname}, avatar = #{avatar}, update_time = #{updateTime} where id = #{id}")
    void update(User user);

    /**
     * 更新用户状态
     *
     * @param user 用户（需要包含id、status、updateTime）
     */
    @Update("update sys_user set status = #{status}, update_time = #{updateTime} where id = #{id}")
    void updateStatus(User user);
}
