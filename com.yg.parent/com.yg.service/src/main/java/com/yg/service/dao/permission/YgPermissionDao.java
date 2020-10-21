package com.yg.service.dao.permission;

import com.yg.service.bean.permission.YgPermission;
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
public interface YgPermissionDao {

	public YgPermission findYdPermissionById(Integer id);

	public Integer insert(YgPermission param);

	public Integer getYdPermissionCount(YgPermission param);

	public List<YgPermission> findYdPermissionsByPage(@Param("param") YgPermission param, @Param("offset") Integer offset, @Param("rows") Integer rows);

	//if not use,pls delete it
	public Integer deleteYdPermissionById(Integer id);

	public YgPermission findPermissionByAliasAndPid(@Param("alias") String alias, @Param("pid") int pid);

	public YgPermission findPermissionByAlias(@Param("alias") String alias);

	public void updateById(@Param("param") YgPermission item);

	public List<YgPermission> findYdPermissionByGroupCode(@Param("groupCode") String groupCode);
}