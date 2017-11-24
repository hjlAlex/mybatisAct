package com.wabao.autocreate;

/**
 * 用于配置Mysql数据库中类型，并且该类型需要设置几个长度 这里配置多少个类型决定了，创建表能使用多少类型 例如：varchar(1)
 * double(5,2) datetime
 * 
 * @since 2017年11月23日 上午11:06:47
 * @author Administrator
 */
public class MySqlTypeConstant {

	@TypeLength
	public static final String INT = "int";// int默认需要设置1个长度

	@TypeLength
	public static final String VARCHAR = "varchar";// varchar默认需要设置1个长度

	@TypeLength(value = 0)
	public static final String TEXT = "text";// text不需要配置长度

	@TypeLength(value = 0)
	public static final String DATETIME = "datetime";// datetime不需要配置长度

	@TypeLength(value = 2)
	public static final String DECIMAL = "decimal";// decimal需要配置2个长度

	@TypeLength(value = 2)
	public static final String DOUBLE = "double";// double需要配置2个长度

	@TypeLength
	public static final String CHAR = "char";// char默认需要设置1个长度

	/**
	 * 等于java中的long
	 */
	@TypeLength
	public static final String BIGINT = "bigint";// bigint默认需要设置1个长度
}
