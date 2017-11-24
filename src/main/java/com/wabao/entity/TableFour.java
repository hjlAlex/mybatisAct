/** 广州哇宝信息技术有限公司 */
package com.wabao.entity;

import java.util.Date;

import org.apache.ibatis.type.Alias;

import com.wabao.autocreate.Column;
import com.wabao.autocreate.MySqlTypeConstant;
import com.wabao.autocreate.Table;

/**
 * 
 * @since 2017年11月23日 下午2:26:33
 * @author Administrator
 */
@Alias("TableFour")
@Table(name = "table_four")
public class TableFour {
	@Column(isNull = false, length = 11, name = "id", type = MySqlTypeConstant.INT, isKey = true, isAutoIncrement = true)
	private int id;
	@Column(isNull = true, length = 255, name = "name", type = MySqlTypeConstant.VARCHAR)
	private String name;
	@Column(isNull = false, length = 0, name = "createTime", type = MySqlTypeConstant.DATETIME)
	private Date createTime;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public Date getCreateTime() {
		return createTime;
	}

	public void setCreateTime(Date createTime) {
		this.createTime = createTime;
	}

}
