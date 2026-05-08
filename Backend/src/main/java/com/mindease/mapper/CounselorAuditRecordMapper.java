package com.mindease.mapper;

import com.mindease.pojo.entity.CounselorAuditRecord;
import org.apache.ibatis.annotations.*;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CounselorAuditRecordMapper {

    /**
     * 插入审核记录
     *
     * @param record
     */
    @Insert("insert into counselor_audit_record(user_id, real_name, qualification_url, id_card_url, status, create_time) " +
            "values(#{userId}, #{realName}, #{qualificationUrl}, #{idCardUrl}, #{status}, #{createTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insert(CounselorAuditRecord record);

    /**
     * 根据用户ID查询最新的审核记录
     *
     * @param userId
     * @return
     */
    @Select("select * from counselor_audit_record where user_id = #{userId} order by create_time desc limit 1")
    CounselorAuditRecord getLatestByUserId(Long userId);

    /**
     * 根据ID查询审核记录
     *
     * @param id
     * @return
     */
    @Select("select * from counselor_audit_record where id = #{id}")
    CounselorAuditRecord getById(Long id);

    /**
     * 查询待审核列表（分页）
     *
     * @param limit
     * @param offset
     * @return
     */
    @Select("select * from counselor_audit_record where status = 'PENDING' order by create_time asc limit #{limit} offset #{offset}")
    List<CounselorAuditRecord> getPendingList(@Param("limit") Integer limit, @Param("offset") Integer offset);

    /**
     * 查询待审核总数
     *
     * @return
     */
    @Select("select count(*) from counselor_audit_record where status = 'PENDING'")
    int countPending();

    /**
     * 更新审核状态
     *
     * @param id
     * @param status
     * @param auditorId
     * @param auditRemark
     * @param auditTime
     */
    @Update("update counselor_audit_record set status = #{status}, auditor_id = #{auditorId}, " +
            "audit_remark = #{auditRemark}, audit_time = #{auditTime} where id = #{id}")
    void updateAuditStatus(@Param("id") Long id,
                           @Param("status") String status,
                           @Param("auditorId") Long auditorId,
                           @Param("auditRemark") String auditRemark,
                           @Param("auditTime") LocalDateTime auditTime);
}

