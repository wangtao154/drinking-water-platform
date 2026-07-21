package com.platform.auth.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.platform.auth.entity.SysRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface SysRoleMapper extends BaseMapper<SysRole> {

    /**
     * 根据用户 ID 查询关联角色（通过 sys_user.role_id）
     */
    @Select("SELECT r.* FROM sys_role r INNER JOIN sys_user u ON u.role_id = r.id WHERE u.id = #{userId} AND r.deleted = 0")
    SysRole selectByUserId(@Param("userId") Long userId);
}
