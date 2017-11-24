package com.wabao.autocreate;

/**
 * 用于存放创建表的字段信息
 * 
 * @since 2017年11月23日 上午11:04:35
 * @author Administrator
 */
public class CreateTableParam {

	/**
	 * 字段名
	 */
	private String fieldName;

	/**
	 * 字段类型
	 */
	private String fieldType;

	/**
	 * 类型长度
	 */
	private int fieldLength;

	/**
	 * 类型小数长度
	 */
	private int fieldDecimalLength;

	/**
	 * 字段是否非空
	 */
	private boolean fieldIsNull;

	/**
	 * 字段是否是主键
	 */
	private boolean fieldIsKey;

	/**
	 * 主键是否自增
	 */
	private boolean fieldIsAutoIncrement;

	/**
	 * 字段默认值
	 */
	private String fieldDefaultValue;

	/**
	 * 该类型需要几个长度（例如，需要小数位数的，那么总长度和小数长度就是2个长度）一版只有0、1、2三个可选值，自动从配置的类型中获取的
	 */
	private int fileTypeLength;

	/**
	 * 值是否唯一
	 */
	private boolean fieldIsUnique;

	/**
	 * 执行更新语句时是否需要加上PRIVATE KEY针对该字段属于主键时候使用
	 */
	private boolean needPrivateKeyOnAlterSql;

	/**
	 * 执行更新语句时是否需要加上UNIQUE KEY针对该字段属于唯一约束时候使用
	 */
	private boolean needUniqueKeyOnAlterSql;

	public String getFieldName() {
		return fieldName;
	}

	public void setFieldName(String fieldName) {
		this.fieldName = fieldName;
	}

	public String getFieldType() {
		return fieldType;
	}

	public void setFieldType(String fieldType) {
		this.fieldType = fieldType;
	}

	public int getFieldLength() {
		return fieldLength;
	}

	public void setFieldLength(int fieldLength) {
		this.fieldLength = fieldLength;
	}

	public int getFieldDecimalLength() {
		return fieldDecimalLength;
	}

	public void setFieldDecimalLength(int fieldDecimalLength) {
		this.fieldDecimalLength = fieldDecimalLength;
	}

	public boolean isFieldIsNull() {
		return fieldIsNull;
	}

	public void setFieldIsNull(boolean fieldIsNull) {
		this.fieldIsNull = fieldIsNull;
	}

	public boolean isFieldIsKey() {
		return fieldIsKey;
	}

	public void setFieldIsKey(boolean fieldIsKey) {
		this.fieldIsKey = fieldIsKey;
	}

	public boolean isFieldIsAutoIncrement() {
		return fieldIsAutoIncrement;
	}

	public void setFieldIsAutoIncrement(boolean fieldIsAutoIncrement) {
		this.fieldIsAutoIncrement = fieldIsAutoIncrement;
	}

	public String getFieldDefaultValue() {
		return fieldDefaultValue;
	}

	public void setFieldDefaultValue(String fieldDefaultValue) {
		this.fieldDefaultValue = fieldDefaultValue;
	}

	public int getFileTypeLength() {
		return fileTypeLength;
	}

	public void setFileTypeLength(int fileTypeLength) {
		this.fileTypeLength = fileTypeLength;
	}

	public boolean isFieldIsUnique() {
		return fieldIsUnique;
	}

	public void setFieldIsUnique(boolean fieldIsUnique) {
		this.fieldIsUnique = fieldIsUnique;
	}

	public boolean isNeedPrivateKeyOnAlterSql() {
		return needPrivateKeyOnAlterSql;
	}

	public void setNeedPrivateKeyOnAlterSql(boolean needPrivateKeyOnAlterSql) {
		this.needPrivateKeyOnAlterSql = needPrivateKeyOnAlterSql;
	}

	public boolean isNeedUniqueKeyOnAlterSql() {
		return needUniqueKeyOnAlterSql;
	}

	public void setNeedUniqueKeyOnAlterSql(boolean needUniqueKeyOnAlterSql) {
		this.needUniqueKeyOnAlterSql = needUniqueKeyOnAlterSql;
	}

	public boolean checkPass(Class<?> entity) {
		boolean flag = true;
		if (this.fieldIsKey && this.fieldIsNull) {// 字段属于主键情况下不允许设置为NULL
			flag = false;
			throw new RuntimeException("类" + entity.getName()
					+ "中主键字段与非空存在冲突,字段:" + this.fieldName);
		} else if (this.fieldIsAutoIncrement && this.fieldIsNull) {// 字段属于自增情况下不允许设置为NULL
			flag = false;
			throw new RuntimeException("类" + entity.getName()
					+ "中自增字段与非空存在冲突,字段:" + this.fieldName);
		} else if (!this.fieldIsKey && this.fieldIsAutoIncrement) {// 字段不是主键或者外键,但是定义了自增
			flag = false;
			throw new RuntimeException("类" + entity.getName()
					+ "中非主键或者外键却定义了自增,字段:" + this.fieldName);
		}
		return flag;
	}
}
