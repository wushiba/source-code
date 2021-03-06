package com.yg.service.dao.permission;

import com.yg.service.bean.permission.YgRlRolePermission;
import org.apache.ibatis.annotations.Param;
import java.util.List;

/**
 *
 * This tools just a simple useful tools, gen table to javabean
 *
 * "I hope this tools can save your time"
 *
 * Generated by <tt>Generate</tt> 
 *
 * @author com.ypn.mapi.generate
 * @version v1.0
 */
public interface YgRlRolePermissionDao {

	public YgRlRolePermission findYdRlRolePermissionById(Integer id);

	public Integer insert(YgRlRolePermission param);

	public Integer getYdRlRolePermissionCount(YgRlRolePermission param);

	public List<YgRlRolePermission> findYdRlRolePermissionsByPage(@Param("param") YgRlRolePermission param, @Param("offset") Integer offset, @Param("rows") Integer rows);

	//if not use,pls delete it
	public Integer deleteYdRlRolePermissionById(Integer id);

	public void deleteYdRlRolePermissionByRoleId(@Param("roleId") Integer roleId);
}