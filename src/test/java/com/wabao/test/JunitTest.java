/** 广州哇宝信息技术有限公司 */
package com.wabao.test;

import java.io.InputStream;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;
import org.apache.ibatis.session.SqlSessionFactoryBuilder;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabao.autocreate.TableSchemaUpdate;
import com.wabao.dao.AutoCreateTableDao;

/**
 * 
 * @since 2017年11月20日 下午2:18:28
 * @author Administrator
 */
public class JunitTest {
	private static final Logger log = LoggerFactory.getLogger(JunitTest.class);

	private static String entityPath = "com.wabao.entity";
	private static String tableAuto = "update";
	private static String schema = "mybatis";

	@Test
	public void testMybatis() {
		InputStream inputStream = JunitTest.class.getClassLoader()
				.getResourceAsStream("mybatis.xml");
		SqlSessionFactory sqlSessionFactory = new SqlSessionFactoryBuilder()
				.build(inputStream);
		SqlSession session = sqlSessionFactory.openSession();

		try {
			AutoCreateTableDao autoCreateTableDao = session
					.getMapper(AutoCreateTableDao.class);
			TableSchemaUpdate tsu = new TableSchemaUpdate(autoCreateTableDao,
					entityPath, tableAuto, schema);
			log.info("开始更新表结构...");
			long s = System.currentTimeMillis();
			tsu.run();
			long e = System.currentTimeMillis();
			log.info("更新表结构结束...用时:" + (e - s));
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			System.out.println(session);
			session.close();
		}
	}

}
