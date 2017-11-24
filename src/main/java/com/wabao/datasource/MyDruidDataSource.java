/** 广州哇宝信息技术有限公司 */
package com.wabao.datasource;

import org.apache.ibatis.datasource.pooled.PooledDataSourceFactory;

import com.alibaba.druid.pool.DruidDataSource;

/**
 * 
 * @since 2017年11月20日 下午5:03:07
 * @author Administrator
 */
public class MyDruidDataSource extends PooledDataSourceFactory {
	public MyDruidDataSource() {
		this.dataSource = new DruidDataSource();
	}
}
