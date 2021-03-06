
import cn.org.rapid_framework.generator.*;
import cn.org.rapid_framework.generator.util.*;
import cn.org.rapid_framework.generator.ext.tableconfig.model.*;
import cn.org.rapid_framework.generator.ext.tableconfig.builder.TableConfigXmlBuilder;
import cn.org.rapid_framework.generator.provider.db.sql.model.Sql;
main();
def main(){
//		freemarker.log.Logger.selectLoggerLibrary(freemarker.log.Logger.LIBRARY_NONE);

		loadDefaultGeneratorProperties();

		String executeTarget = System.getProperty("executeTarget");
		new Targets(this)."${executeTarget}"();

		println "---------------------Generator executed SUCCESS---------------------"
	}
def loadDefaultGeneratorProperties() {
	GeneratorProperties.properties.put("generator_tools_class","cn.org.rapid_framework.generator.util.StringHelper,org.apache.commons.lang.StringUtils");
	GeneratorProperties.properties.put("gg_isOverride","true");

	GeneratorProperties.properties.put("generator_sourceEncoding","UTF-8");
	GeneratorProperties.properties.put("generator_outputEncoding","UTF-8");
	GeneratorProperties.properties.put("gg_isOverride","true");
	//将表名从复数转换为单数
	GeneratorProperties.properties.put("tableNameSingularize","true");
	if(pom != null) {
		GeneratorProperties.load("${pom.basedir}/"+System.getProperty("generatorConfigFile")); //加载配置文件
		GeneratorProperties.properties.put("basedir",pom.basedir);
		GeneratorProperties.properties.putAll(pom.properties);
	}else {
		GeneratorProperties.load("db.xml","gen_config.xml");
		GeneratorProperties.properties.put("basedir",new File("."));
	}
}
public class Targets extends HashMap{
	public  Object pom;
	public  Object settings;
	public  Object log;
	public  fail

	public Targets(Object global) {
		this.pom = global.pom;
		this.settings = global.settings;
		this.log = global.log;
		this.fail = global.fail;

		for(e in System.properties) {
			put(e.key,e.value)
		}
		for(e in GeneratorProperties.properties) {
			put(e.key,e.value)
		}
		// GeneratorProperties.properties.each { k,v -> println "${k}=${v}"}
	}

	def dal() {
		println "dal_package:${dal_package} basedir:${basedir} dir_table_configs:${dir_table_configs} dir_templates_root:${dir_templates_root}";
		TableConfigSet tableConfigSet = new TableConfigXmlBuilder().parseFromXML(new File(basedir,dir_table_configs),dal_package,Helper.getTableConfigFiles(new File(basedir,dir_table_configs)));
		GenUtils.genByTableConfigSet(Helper.createGeneratorFacade(dir_dal_output_root,"${dir_templates_root}/table_config_set/dal","${dir_templates_root}/share/dal"),tableConfigSet);
		GenUtils.genByTableConfig(Helper.createGeneratorFacade(dir_dal_output_root,"${dir_templates_root}/table_config/dal","${dir_templates_root}/share/dal"),tableConfigSet,genInputCmd);
		GenUtils.genByOperation(Helper.createGeneratorFacade(dir_dal_output_root,"${dir_templates_root}/operation/dal","${dir_templates_root}/share/dal"),tableConfigSet,genInputCmd);
	}

	def table() {
		GenUtils.genByTable(Helper.createGeneratorFacade(dir_dal_output_root,"${dir_templates_root}/table/dalgen_config","${dir_templates_root}/share/dal"),genInputCmd)
	}

	def seq() {
		TableConfigSet tableConfigSet = new TableConfigXmlBuilder().parseFromXML(new File(basedir,dir_table_configs),dal_package,Helper.getTableConfigFiles(new File(basedir,dir_table_configs)));
		GeneratorProperties.properties.put("basepackage",dal_package);
		GenUtils.genByTableConfigSet(Helper.createGeneratorFacade(dir_dal_output_root,"${dir_templates_root}/table_config_set/sequence","${dir_templates_root}/share/dal"),tableConfigSet);
	}

	def crud() {
		GeneratorFacade gf = Helper.createGeneratorFacade(dir_dal_output_root,
				"${dir_templates_root}/share/basic",
				"${dir_templates_root}/table/dao_hibernate",
				"${dir_templates_root}/table/dao_hibernate_annotation",
				"${dir_templates_root}/table/service_no_interface",
				"${dir_templates_root}/table/web_struts2");
		GenUtils.genByTable(gf,genInputCmd)
	}
}
	public class GenUtils {
		/**
		 * 生成ibatis sql相关的配置以及sql文件
		 * @param generatorFacade
		 * @param tableConfigSet
		 * @throws Exception
		 */
		public static void genByTableConfigSet(GeneratorFacade generatorFacade,TableConfigSet tableConfigSet) throws Exception {
			Map map = new HashMap();
			map.putAll(BeanHelper.describe(tableConfigSet));
			map.put("tableConfigSet", tableConfigSet);
			println "++++++++++++++++++tableConfigSet"+ tableConfigSet;
			generatorFacade.generateByMap(map);
		}

		/**
		 * 生成DAO接口及实现类和返回契约
		 * @param generatorFacade
		 * @param tableConfigSet
		 * @param tableSqlName
		 * @throws Exception
		 */
		public static void genByTableConfig(GeneratorFacade generatorFacade,TableConfigSet tableConfigSet,String tableSqlName) throws Exception {

			Collection<TableConfig> tableConfigs = Helper.getTableConfigs(tableConfigSet,tableSqlName);
			for(TableConfig tableConfig : tableConfigs) {
				Map map = new HashMap();
				String[] ignoreProperties = {"sqls"};
				map.putAll(BeanHelper.describe(tableConfig,ignoreProperties));
				map.put("tableConfig", tableConfig);
				println "================tableConfig";
				generatorFacade.generateByMap(map);
			}
		}

		/**
		 * 生成相关的入参契约
		 * @param generatorFacade
		 * @param tableConfigSet
		 * @param tableSqlName
		 * @throws Exception
		 */
		public static void genByOperation(GeneratorFacade generatorFacade,TableConfigSet tableConfigSet,String tableSqlName) throws Exception {
			Collection<TableConfig> tableConfigs = Helper.getTableConfigs(tableConfigSet,tableSqlName);
			for(TableConfig tableConfig : tableConfigs) {
				for(Sql sql : tableConfig.getSqls()) {
					Map operationMap = new HashMap();
					operationMap.putAll(BeanHelper.describe(sql));
					operationMap.put("sql", sql);
					println "================sql"+ sql.getSql();
					operationMap.put("tableConfig", tableConfig);
					operationMap.put("basepackage", tableConfig.getBasepackage());
					generatorFacade.generateByMap(operationMap);
				}
			}
		}

		public static void genByTable(GeneratorFacade generatorFacade,String tableSqlName) throws Exception {
			generatorFacade.generateByTable(tableSqlName);
		}

	}
	public class Helper {
		public static List<String> getTableConfigFiles(File basedir) {
			String[] tableConfigFilesArray = basedir.list();
			List<String> result = new ArrayList();
			for(String str : tableConfigFilesArray) {
				if(str.endsWith(".xml")) {
					result.add(str);
				}
			}
			return result;
		}
		public static Collection<TableConfig> getTableConfigs(TableConfigSet tableConfigSet,String tableSqlName) {
			if("*".equals(tableSqlName)) {
				return tableConfigSet.getTableConfigs();
			}else {
				TableConfig tableConfig = tableConfigSet.getBySqlName(tableSqlName);
				if(tableConfig == null) {
					throw new RuntimeException("根据表名:${tableSqlName}没有找到配置文件");
				}
				return Arrays.asList(tableConfig);
			}
		}

		public static GeneratorFacade createGeneratorFacade(String outRootDir,String... templateRootDirs) {
			if(templateRootDirs == null) throw new IllegalArgumentException("templateRootDirs must be not null");
			if(outRootDir == null) throw new IllegalArgumentException("outRootDir must be not null");

			GeneratorFacade gf = new GeneratorFacade();
			gf.getGenerator().setTemplateRootDirs(templateRootDirs);
			gf.getGenerator().setOutRootDir(outRootDir);
			return gf;
		}

	}