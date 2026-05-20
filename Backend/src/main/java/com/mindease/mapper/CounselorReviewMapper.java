package com.mindease.mapper;

import com.mindease.pojo.entity.CounselorReview;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface CounselorReviewMapper {

    /**
     * 插入评价
     *
     * @param review
     */
    @Insert("insert into counselor_review(appointment_id, counselor_id, user_id, rating, content, create_time) " +
            "values(#{appointmentId}, #{counselorId}, #{userId}, #{rating}, #{content}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CounselorReview review);

    /**
     * 查询咨询师的评价列表
     *
     * @param counselorId
     * @param limit
     * @param offset
     * @return
     */
    @Select("select * from counselor_review where counselor_id = #{counselorId} order by create_time desc limit #{limit} offset #{offset}")
    List<CounselorReview> getByCounselorId(@Param("counselorId") Long counselorId,
                                            @Param("limit") Integer limit,
                                            @Param("offset") Integer offset);

    /**
     * 查询咨询师的评价总数
     *
     * @param counselorId
     * @return
     */
    @Select("select count(*) from counselor_review where counselor_id = #{counselorId}")
    int countByCounselorId(Long counselorId);

    /**
     * 查询咨询师的平均评分
     *
     * @param counselorId
     * @return
     */
    @Select("select avg(rating) from counselor_review where counselor_id = #{counselorId}")
    Double getAvgRatingByCounselorId(Long counselorId);

    /**
     * 根据预约ID查询评价数量（用于检查是否已评价）
     *
     * @param appointmentId
     * @return
     */
    @Select("select count(*) from counselor_review where appointment_id = #{appointmentId}")
    int countByAppointmentId(Long appointmentId);

    /**
     * 查询咨询师的所有评价（用于计算评分）
     *
     * @param counselorId
     * @return
     */
    @Select("select * from counselor_review where counselor_id = #{counselorId}")
    List<CounselorReview> listByCounselorId(Long counselorId);
}

