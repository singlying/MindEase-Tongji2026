package com.mindease.mapper;

import com.mindease.pojo.entity.CounselorProfile;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CounselorProfileMapper {

    /**
     * 根据ID查询咨询师资料
     *
     * @param userId
     * @return
     */
    @Select("select * from counselor_profile where user_id = #{userId}")
    CounselorProfile getByUserId(Long userId);

    /**
     * 更新咨询师排班配置
     *
     * @param userId
     * @param workSchedule
     */
    @Update("update counselor_profile set work_schedule = #{workSchedule} where user_id = #{userId}")
    void updateWorkSchedule(@Param("userId") Long userId, @Param("workSchedule") String workSchedule);

    /**
     * 智能推荐咨询师
     *
     * @param keywords 关键词列表
     * @param sort 排序方式
     * @return
     */
    List<CounselorProfile> recommendCounselors(@Param("keywords") List<String> keywords, 
                                                @Param("sort") String sort);

    /**
     * 查询所有正常状态的咨询师
     *
     * @return
     */
    List<CounselorProfile> getAllActiveCounselors();

    /**
     * 插入咨询师资料
     *
     * @param profile
     */
    @Insert("insert into counselor_profile(user_id, real_name, title, experience_years, specialty, bio, " +
            "qualification_url, location, price_per_hour, rating, review_count) " +
            "values(#{userId}, #{realName}, #{title}, #{experienceYears}, #{specialty}, #{bio}, " +
            "#{qualificationUrl}, #{location}, #{pricePerHour}, #{rating}, #{reviewCount})")
    void insert(CounselorProfile profile);

    /**
     * 更新咨询师资料
     *
     * @param profile
     */
    @Update("update counselor_profile set real_name = #{realName}, title = #{title}, " +
            "experience_years = #{experienceYears}, specialty = #{specialty}, bio = #{bio}, " +
            "qualification_url = #{qualificationUrl}, location = #{location}, " +
            "price_per_hour = #{pricePerHour}, rating = #{rating}, review_count = #{reviewCount} " +
            "where user_id = #{userId}")
    void update(CounselorProfile profile);

    /**
     * 根据咨询师ID列表批量查询咨询师资料（协同过滤用）
     *
     * @param counselorIds
     * @return
     */
    List<CounselorProfile> getByCounselorIds(@Param("counselorIds") List<Long> counselorIds);
}

