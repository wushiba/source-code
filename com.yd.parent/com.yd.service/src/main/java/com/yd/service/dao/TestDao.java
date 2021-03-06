package com.yd.service.dao;

import java.util.List;
import java.lang.Integer;
import org.apache.ibatis.annotations.Param;
import com.yd.service.bean.Test;
import com.yd.api.req.TestReq;

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
public interface TestDao {

	public Test findTestById(Integer id);
	
	public Integer insert(Test param);
	
	public Integer getTestCount(TestReq req);
	
	public List<Test> findTestsByPage(@Param("req")TestReq req,@Param("offset")Integer offset,@Param("rows")Integer rows);
	
	//if not use,pls delete it
	public Integer deleteTestById(Integer id);
}