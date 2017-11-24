/** 广州哇宝信息技术有限公司 */
package com.wabao.entity;

import org.apache.ibatis.type.Alias;

import com.wabao.autocreate.Column;
import com.wabao.autocreate.MySqlTypeConstant;
import com.wabao.autocreate.Table;

/**
 * 
 * @since 2017年11月23日 下午2:26:33
 * @author Administrator
 */
@Alias("TableOne")
@Table(name = "table_one")
public class TableOne {
	@Column(isNull = false, length = 12, name = "id", type = MySqlTypeConstant.INT, isKey = true)
	private int id;
	@Column(isNull = true, length = 250, name = "createTime", type = MySqlTypeConstant.VARCHAR)
	private String createTime;
	@Column(isNull = false, length = 11, name = "newField", type = MySqlTypeConstant.INT, isUnique = true)
	private int newField;

	public int getId() {
		return id;
	}

	public void setId(int id) {
		this.id = id;
	}

	public String getCreateTime() {
		return createTime;
	}

	public void setCreateTime(String createTime) {
		this.createTime = createTime;
	}

	public int getNewField() {
		return newField;
	}

	public void setNewField(int newField) {
		this.newField = newField;
	}

}
