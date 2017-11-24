/** 广州哇宝信息技术有限公司 */
package com.wabao.autocreate;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.wabao.dao.AutoCreateTableDao;
import com.wabao.util.ClassTools;

/**
 * 核心方法,Mybatis自动更新表结构
 * 
 * @since 2017年11月23日 上午11:49:23
 * @author Administrator
 */
public class TableSchemaUpdate {
	private static final Logger log = LoggerFactory
			.getLogger(TableSchemaUpdate.class);
	private AutoCreateTableDao autoCreateTableDao;// 自动更新表结构相关接口

	private String entityPath;// 映射表的实体类所在包路径

	private String tableAuto;// 更新模式 "none" or "update" or "create"

	private String schema;// 实体类对应的数据库名称

	private Set<Class<?>> entityClasses;

	private Map<String, Integer> typeLengthMap;// MySQL自动类型与及其需要设置的长度个数

	// 当前需要创建的表名+结构
	private Map<String, List<CreateTableParam>> createTableMap = new HashMap<String, List<CreateTableParam>>();

	// 当前需要增加字段的表名+结构
	private Map<String, List<CreateTableParam>> addTableMap = new HashMap<String, List<CreateTableParam>>();

	// 当前需要删除字段的表名+删除的字段名
	private Map<String, List<String>> removeTableMap = new HashMap<String, List<String>>();

	// 当前需要更新字段的表名+结构
	private Map<String, List<CreateTableParam>> modifyTableMap = new HashMap<String, List<CreateTableParam>>();

	// 当前需要删除主键的表名+结构
	private Map<String, List<CreateTableParam>> dropKeyTableMap = new HashMap<String, List<CreateTableParam>>();

	// 当前需要删除唯一约束的表名+结构
	private Map<String, List<CreateTableParam>> dropUniqueTableMap = new HashMap<String, List<CreateTableParam>>();

	public TableSchemaUpdate(AutoCreateTableDao autoCreateTableDao,
			String entityPath, String tableAuto, String schema) {
		super();
		this.autoCreateTableDao = autoCreateTableDao;
		this.entityPath = entityPath;
		this.tableAuto = tableAuto;
		this.schema = schema;
		this.entityClasses = ClassTools.getClasses(this.entityPath);
		this.typeLengthMap = mySqlTypeAndLengthMap();
	}

	/**
	 * 获取定义的MySQL自动类型与及其需要设置的长度个数
	 * 
	 * @return
	 * @since 2017年11月23日 下午12:04:25
	 */
	private Map<String, Integer> mySqlTypeAndLengthMap() {
		Field[] fields = MySqlTypeConstant.class.getDeclaredFields();
		Map<String, Integer> map = new HashMap<String, Integer>();
		for (Field field : fields) {
			TypeLength typeLength = field.getAnnotation(TypeLength.class);
			map.put(field.getName().toLowerCase(), typeLength.value());
		}
		return map;
	}

	/**
	 * 开始执行扫描和更新表结构
	 * 
	 * @since 2017年11月23日 下午1:44:13
	 */
	public void run() {
		log.info("当前表的更新模式值tableAuto:" + this.tableAuto);
		if ("none".equals(this.tableAuto)) {
			log.info("更新模式值tableAuto:" + this.tableAuto + ",不做任何处理...");
			return;
		}
		if (null == this.entityClasses || 0 == this.entityClasses.size()) {
			log.info("没有@Table需要处理的实体类");
			return;
		}
		for (Class<?> entity : this.entityClasses) {
			Table table = entity.getAnnotation(Table.class);
			// 没有打注解不需要创建变
			if (null == table) {
				continue;
			}
			// 如果配置文件配置的是create，表示将所有的表删掉重新创建
			if ("create".equals(tableAuto)) {
				// this.autoCreateTableDao.dorpTableByName(table.name());
			}
			// 用于存新增表的字段
			List<CreateTableParam> entityTableFieldList = getEntityTableFieldList(
					this.typeLengthMap, entity);
			// 检测单个表中是否存在多个主键和自增字段
			checkTableCreateParams(entity, entityTableFieldList);

			// 先查该表是否以存在
			int exist = this.autoCreateTableDao
					.findTableCountByTableName(this.schema, table.name());

			if (exist == 0) {// 不存在,需要新增该表
				this.createTableMap.put(table.name(), entityTableFieldList);
			} else {// 存在需要作出更新
				// 已存在时理论上做修改的操作，这里查出该表的结构
				List<SysMysqlColumns> tableExistColumnList = this.autoCreateTableDao
						.findTableEnsembleByTableName(this.schema,
								table.name());
				// 从sysColumns中取出我们需要比较的列的List
				// 将entityTableFieldList转成Map类型，字段名作为主键
				Map<String, CreateTableParam> entityTableFieldMap = new HashMap<String, CreateTableParam>();
				for (CreateTableParam createTableParam : entityTableFieldList) {
					entityTableFieldMap.put(createTableParam.getFieldName(),
							createTableParam);
				}

				// 先取出name用来筛选出增加和删除的字段
				List<String> existColumnNames = ClassTools.getPropertyValueList(
						tableExistColumnList, SysMysqlColumns.COLUMN_NAME);
				// 1,提取需要新增的字段并放到对应的map中
				List<CreateTableParam> entityTableAddFieldList = getEntityTableAddFieldList(
						entityTableFieldList, existColumnNames);
				if (entityTableAddFieldList.size() > 0) {
					this.addTableMap.put(table.name(), entityTableAddFieldList);
				}
				// 2,提取需要删除的字段并放到对应的map中
				List<String> entityTableRemoveFieldList = getEntityTableRemoveFieldList(
						entityTableFieldMap, existColumnNames);
				if (entityTableRemoveFieldList.size() > 0) {
					this.removeTableMap.put(table.name(),
							entityTableRemoveFieldList);
				}
				// 3,更新字段操作
				buildModifyFields(table, tableExistColumnList,
						entityTableFieldMap);
			}
		}
		// 务必按照以下步骤进行
		// 1.开始创建表
		createTableByMap();
		// 2.删除那些原来是主键，但现在不是了的表字段(必须在这一步)
		dropFieldsKeyByMap();
		// 3.删除那些原来是唯一约束，但现在不是了的表字段(必须在这一步)
		dropFieldsUniqueByMap();
		// 4.新增表字段
		addFieldsByMap();
		// 5.删除表字段
		removeFieldsByMap();
		// 6.最后更新字段
		modifyFieldsByMap();
	}

	private void checkTableCreateParams(Class<?> entity,
			List<CreateTableParam> entityTableFieldList) {
		int privateKeyCount = 0;
		int autoIncrementCount = 0;
		for (CreateTableParam createTableParam : entityTableFieldList) {
			if (createTableParam.isFieldIsKey()) {
				privateKeyCount++;
			}
			if (privateKeyCount > 1) {
				throw new RuntimeException(
						"类" + entity.getName() + "中主键字段个数多于1");
			}
			if (createTableParam.isFieldIsAutoIncrement()) {
				autoIncrementCount++;
			}
			if (autoIncrementCount > 1) {
				throw new RuntimeException(
						"类" + entity.getName() + "中自增字段个数多于1");
			}
		}
	}

	/**
	 * 根据map结构创建表
	 * 
	 * @param createTableMap
	 */
	private void createTableByMap() {
		// 做创建表操作
		if (this.createTableMap.size() > 0) {
			for (Entry<String, List<CreateTableParam>> entry : this.createTableMap
					.entrySet()) {
				Map<String, List<CreateTableParam>> map = new HashMap<String, List<CreateTableParam>>();
				map.put(entry.getKey(), entry.getValue());
				log.info("开始创建表：" + entry.getKey());
				this.autoCreateTableDao.createTable(map);
				log.info("完成创建表：" + entry.getKey());
			}
		}
	}

	/**
	 * 删除那些原来是主键，但现在不是了的表字段
	 * 
	 * @since 2017年11月23日 下午5:53:22
	 */
	private void dropFieldsKeyByMap() {
		// 先去做删除主键的操作，这步操作必须在增加和修改字段之前！
		if (this.dropKeyTableMap.size() > 0) {
			for (Entry<String, List<CreateTableParam>> entry : this.dropKeyTableMap
					.entrySet()) {
				for (CreateTableParam field : entry.getValue()) {
					Map<String, CreateTableParam> map = new HashMap<String, CreateTableParam>();
					map.put(entry.getKey(), field);
					log.info("开始为表" + entry.getKey() + "删除主键"
							+ field.getFieldName());
					this.autoCreateTableDao.dropKeyTableField(map);
					log.info("完成为表" + entry.getKey() + "删除主键"
							+ field.getFieldName());
				}
			}
		}
	}

	/**
	 * 删除那些原来是唯一约束，但现在不是了的表字段
	 * 
	 * @since 2017年11月23日 下午5:59:57
	 */
	private void dropFieldsUniqueByMap() {
		// 先去做删除唯一约束的操作，这步操作必须在增加和修改字段之前！
		if (this.dropUniqueTableMap.size() > 0) {
			for (Entry<String, List<CreateTableParam>> entry : this.dropUniqueTableMap
					.entrySet()) {
				for (CreateTableParam field : entry.getValue()) {
					Map<String, CreateTableParam> map = new HashMap<String, CreateTableParam>();
					map.put(entry.getKey(), field);
					log.info("开始为表" + entry.getKey() + "删除唯一约束"
							+ field.getFieldName());
					this.autoCreateTableDao.dropUniqueTableField(map);
					log.info("完成为表" + entry.getKey() + "删除唯一约束"
							+ field.getFieldName());
				}
			}
		}
	}

	/**
	 * 更新操作
	 * 
	 * @since 2017年11月23日 下午6:24:45
	 */
	private void modifyFieldsByMap() {
		// 做修改字段操作
		if (this.modifyTableMap.size() > 0) {

			for (Entry<String, List<CreateTableParam>> entry : this.modifyTableMap
					.entrySet()) {
				for (CreateTableParam field : entry.getValue()) {
					Map<String, CreateTableParam> map = new HashMap<String, CreateTableParam>();
					map.put(entry.getKey(), field);
					log.info("开始修改表" + entry.getKey() + "中的字段"
							+ field.getFieldName());
					this.autoCreateTableDao.modifyTableField(map);
					log.info("完成修改表" + entry.getKey() + "中的字段"
							+ field.getFieldName());
				}
			}
		}
	}

	/**
	 * 根据map结构新增表字段
	 * 
	 * @since 2017年11月23日 下午4:11:28
	 */
	private void addFieldsByMap() {
		// 做增加字段操作
		if (this.addTableMap.size() > 0) {
			for (Entry<String, List<CreateTableParam>> entry : this.addTableMap
					.entrySet()) {
				for (CreateTableParam field : entry.getValue()) {
					Map<String, CreateTableParam> map = new HashMap<String, CreateTableParam>();
					map.put(entry.getKey(), field);
					log.info("开始为表" + entry.getKey() + "增加字段"
							+ field.getFieldName());
					this.autoCreateTableDao.addTableField(map);
					log.info("完成为表" + entry.getKey() + "增加字段"
							+ field.getFieldName());
				}
			}
		}
	}

	private void removeFieldsByMap() {
		// 做删除字段操作
		if (this.removeTableMap.size() > 0) {
			for (Entry<String, List<String>> entry : this.removeTableMap
					.entrySet()) {
				for (String removeField : entry.getValue()) {
					Map<String, String> map = new HashMap<String, String>();
					map.put(entry.getKey(), removeField);
					log.info("开始删除表" + entry.getKey() + "中的字段" + removeField);
					this.autoCreateTableDao.removeTableField(map);
					log.info("完成删除表" + entry.getKey() + "中的字段" + removeField);
				}
			}
		}
	}

	/**
	 * 获取当前model实体类对应的mysql表结构
	 * 
	 * @param mySqlTypeAndLengthMap
	 * @param entity
	 * @return
	 * @since 2017年11月23日 下午3:41:50
	 */
	private List<CreateTableParam> getEntityTableFieldList(
			Map<String, Integer> mySqlTypeAndLengthMap, Class<?> entity) {
		List<CreateTableParam> entityTableFieldList = new ArrayList<CreateTableParam>();
		Field[] childFields = entity.getDeclaredFields();
		Field[] superFields = null;
		// 判断是否有父类，如果有拉取父类的field，这里只支持一层继承
		if (entity.getSuperclass() != null) {
			Class<?> clsSup = entity.getSuperclass();
			superFields = clsSup.getDeclaredFields();
		}

		List<Field> fields = new ArrayList<Field>();

		if (null != childFields && 0 != childFields.length) {
			fields.addAll(Arrays.asList(childFields));
		}

		if (null != superFields && 0 != superFields.length) {
			fields.addAll(Arrays.asList(superFields));
		}

		if (null == fields || 0 == fields.size()) {
			log.info("@Table注解下的类" + entity.getName() + "下,没有任何声明的字段");
			return entityTableFieldList;
		}

		for (Field field : fields) {
			// 判断方法中是否有指定注解类型的注解
			boolean hasAnnotation = field.isAnnotationPresent(Column.class);
			if (hasAnnotation) {
				// 根据注解类型返回方法的指定类型注解
				Column column = field.getAnnotation(Column.class);
				CreateTableParam param = new CreateTableParam();
				param.setFieldName(column.name());
				param.setFieldType(column.type().toLowerCase());
				param.setFieldLength(column.length());
				param.setFieldDecimalLength(column.decimalLength());
				param.setFieldIsNull(column.isNull());
				param.setFieldIsKey(column.isKey());
				param.setFieldIsAutoIncrement(column.isAutoIncrement());
				param.setFieldDefaultValue(column.defaultValue());
				param.setFieldIsUnique(column.isUnique());
				int length = mySqlTypeAndLengthMap
						.get(column.type().toLowerCase());
				param.setFileTypeLength(length);
				if (param.checkPass(entity)) {
					entityTableFieldList.add(param);
				}
			}
		}
		return entityTableFieldList;
	}

	/**
	 * 获取当前model中是否存在需要新增的字段集合
	 * 
	 * @param entityTableFieldList
	 * @param existColumnNames
	 * @return
	 * @since 2017年11月23日 下午3:53:19
	 */
	private List<CreateTableParam> getEntityTableAddFieldList(
			List<CreateTableParam> entityTableFieldList,
			List<String> existColumnNames) {

		List<CreateTableParam> resultList = new ArrayList<CreateTableParam>();

		if (null == entityTableFieldList || null == existColumnNames) {
			return resultList;
		}
		for (CreateTableParam createTableParam : entityTableFieldList) {
			// 循环新的model中的字段，判断是否在数据库中已经存在
			if (!existColumnNames.contains(createTableParam.getFieldName())) {
				// 不存在，表示要在数据库中增加该字段
				resultList.add(createTableParam);
			}
		}
		return resultList;
	}

	/**
	 * 获取当前model中是否存在需要删除的字段集合
	 * 
	 * @param entityTableFieldList
	 * @param existColumnNames
	 * @return
	 * @since 2017年11月23日 下午3:53:19
	 */
	private List<String> getEntityTableRemoveFieldList(
			Map<String, CreateTableParam> entityTableFieldMap,
			List<String> existColumnNames) {

		List<String> resultList = new ArrayList<String>();

		if (null == entityTableFieldMap || null == existColumnNames) {
			return resultList;
		}
		for (String existColumnName : existColumnNames) {
			// 判断该字段是否需要删除
			if (null == entityTableFieldMap.get(existColumnName)) {
				// 不存在，表示要在数据库中增加该字段
				resultList.add(existColumnName);
			}
		}
		return resultList;
	}

	/**
	 * 更新字段操作
	 * 
	 * @param table
	 * @param tableColumnList
	 * @param fieldMap
	 * @since 2017年11月23日 下午5:46:22
	 */
	private void buildModifyFields(Table table,
			List<SysMysqlColumns> tableColumnList,
			Map<String, CreateTableParam> fieldMap) {
		List<CreateTableParam> dropKeyFieldList = new ArrayList<CreateTableParam>();
		List<CreateTableParam> dropUniqueFieldList = new ArrayList<CreateTableParam>();
		List<CreateTableParam> modifyFieldList = new ArrayList<CreateTableParam>();
		for (SysMysqlColumns sysColumn : tableColumnList) {
			// 数据库中有该字段时
			CreateTableParam createTableParam = fieldMap
					.get(sysColumn.getColumn_name());
			if (createTableParam != null) {
				// 验证是否有更新

				// 注意下如果当前字段需要进行更新,并且更新的字段是主键时
				// (但不是新增一个主键),更新的sql语句不能带上private key在后面
				if (createTableParam.isFieldIsKey()) {
					createTableParam.setNeedPrivateKeyOnAlterSql(false);
				}
				// 注意下如果当前字段需要进行更新,并且更新的字段是唯一约束时
				// (但不是新增一个唯一约束),更新的sql语句不能带上unique key在后面
				if (createTableParam.isFieldIsUnique()) {
					createTableParam.setNeedUniqueKeyOnAlterSql(false);
				}
				// 检查是否要删除已有主键和是否要删除已有唯一约束的代码必须放在其他检查的最前面
				// 原本是主键，现在不是了，那么要去做删除主键的操作
				if ("PRI".equals(sysColumn.getColumn_key().toUpperCase())
						&& !createTableParam.isFieldIsKey()) {
					dropKeyFieldList.add(createTableParam);// 该字段虽然属于删除主键字段,但是后面的逻辑还要执行(参考如下业务场景:我需要把这个字段的某个主键删了,同时字段长度也变更)
				}

				// 原本不是主键，现在变成了主键，那么要去做更新
				if (!"PRI".equals(sysColumn.getColumn_key())
						&& createTableParam.isFieldIsKey()) {
					if (createTableParam.isFieldIsKey()) {
						createTableParam.setNeedPrivateKeyOnAlterSql(true);// 这种情况更新sql语句后面要带上private
																			// key
					}
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 原本是唯一，现在不是了，那么要去做删除唯一的操作
				if ("UNI".equals(sysColumn.getColumn_key().toUpperCase())
						&& !createTableParam.isFieldIsUnique()) {
					dropUniqueFieldList.add(createTableParam);// 该字段虽然属于删除唯一约束字段,但是后面的逻辑还要执行(参考如下业务场景:我需要把这个字段的某个唯一约束删了,同时字段长度也变更)
				}

				// 原本不是唯一，现在变成了唯一，那么要去做更新
				if (!"UNI".equals(sysColumn.getColumn_key().toUpperCase())
						&& createTableParam.isFieldIsUnique()) {
					if (createTableParam.isFieldIsUnique()) {
						createTableParam.setNeedUniqueKeyOnAlterSql(true);// 这种情况更新sql语句后面要带上unique
																			// key
					}
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 验证自增
				// 原本数据库是自增状态,现在不属于自增状态
				if ("auto_increment".equals(sysColumn.getExtra().toLowerCase())
						&& !createTableParam.isFieldIsAutoIncrement()) {
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 原本数据库不是自增状态,现在属于自增状态
				if (!"auto_increment".equals(sysColumn.getExtra().toLowerCase())
						&& createTableParam.isFieldIsAutoIncrement()) {
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 验证类型
				if (!sysColumn.getData_type().toLowerCase().equals(
						createTableParam.getFieldType().toLowerCase())) {
					modifyFieldList.add(createTableParam);
					continue;
				}
				// 验证长度和验证小数点位数
				int typeLength = this.typeLengthMap
						.get(createTableParam.getFieldType().toLowerCase());
				StringBuilder typeAndLength = new StringBuilder(
						createTableParam.getFieldType().toLowerCase());
				if (typeLength == 1) {
					// 拼接出类型加长度，比如varchar(1)
					typeAndLength.append("(")
							.append(createTableParam.getFieldLength())
							.append(")");
				} else if (typeLength == 2) {
					// 拼接出类型加长度，比如varchar(1)
					typeAndLength.append("(")
							.append(createTableParam.getFieldLength())
							.append(",")
							.append(createTableParam.getFieldDecimalLength())
							.append(")");
				}
				// 判断类型+长度是否相同
				if (!sysColumn.getColumn_type().toLowerCase()
						.equals(typeAndLength.toString())) {
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 验证默认值
				if (sysColumn.getColumn_default() == null
						|| sysColumn.getColumn_default().equals("")) {
					// 数据库默认值是null，model中注解设置的默认值不为NULL时，那么需要更新该字段
					if (!"NULL"
							.equals(createTableParam.getFieldDefaultValue())) {
						modifyFieldList.add(createTableParam);
						continue;
					}
				} else if (!sysColumn.getColumn_default()
						.equals(createTableParam.getFieldDefaultValue())) {
					// 两者不相等时，需要更新该字段
					modifyFieldList.add(createTableParam);
					continue;
				}

				// 验证是否可以为null
				if (sysColumn.getIs_nullable().toUpperCase().equals("NO")) {
					if (createTableParam.isFieldIsNull()) {
						// 原本数据库不允许为null，但是现在注解设置可以允许了，所以需要更新该字段
						modifyFieldList.add(createTableParam);
						continue;
					}
				} else if (sysColumn.getIs_nullable().toUpperCase()
						.equals("YES")) {
					if (!createTableParam.isFieldIsNull()) {
						// 原本数据库允许为null，但是现在注解设置不可以允许了，所以需要更新该字段
						modifyFieldList.add(createTableParam);
						continue;
					}
				}

			}
		}

		if (dropKeyFieldList.size() > 0) {
			this.dropKeyTableMap.put(table.name(), dropKeyFieldList);
		}

		if (dropUniqueFieldList.size() > 0) {
			this.dropUniqueTableMap.put(table.name(), dropUniqueFieldList);
		}

		if (modifyFieldList.size() > 0) {
			this.modifyTableMap.put(table.name(), modifyFieldList);
		}

	}
}
