package nl.vu.psy.relic;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Properties;
import java.util.StringTokenizer;

import joptsimple.OptionSet;
import nl.vu.psy.panter.csv.data.CsvEntry;
import nl.vu.psy.panter.csv.parser.CsvParser;
import nl.vu.psy.relic.persistence.mongo.MongoStore;
import nl.vu.psy.relic.resolvers.ResolverDescriptor;
import nl.vu.psy.relic.resolvers.implementations.GridSRMResolver;
import nl.vu.psy.relic.resolvers.implementations.LocalFileSystemResolver;

public class InitRelicDB {
	private static String PROPERTIES = "initdb.properties";

	public enum PropertyKeys {
		SEPARATOR("separator", ","), HOST("hostname", ""), PORT("port", ""), DBNAME("dbname", ""), AUTH("auth", "false"), USER("user", ""), PASS("pass", "");

		private final String key;
		private final String defaultValue;

		private PropertyKeys(String key, String defaultValue) {
			this.key = key;
			this.defaultValue = defaultValue;
		}

		public String getDefaultValue() {
			return defaultValue;
		}

		public String getKey() {
			return key;
		}

		public String getProperty(Properties properties) {
			return properties.getProperty(this.getKey(), this.getDefaultValue());
		}

	}

	private static Properties properties;
	private static String version;

	public static void main(String[] args) {
		version = InitRelicDB.class.getPackage().getImplementationVersion();
		StringTokenizer st = null;
		if (version != null) {
			st = new StringTokenizer(version, "_");
		} else {
			st = new StringTokenizer("");
			version = "undetermined";
		}
		System.out.println();
		System.out.println("+-++-++-++-++-++-++-++-++-++-++-+");
		System.out.println("|I||n||i||t||R||e||l||i||c||D||B|");
		System.out.println("+-++-++-++-++-++-++-++-++-++-++-+");
		if (version != null && st.countTokens() >= 2) {
			System.out.print("version: " + st.nextToken() + " build " + st.nextToken() + "\n");
		} else {
			System.out.print("version: " + version + "\n");
		}
		System.out.println();

		InitRelicDBOptionParser optionParser = new InitRelicDBOptionParser();
		OptionSet optionsInEffect = null;

		try {
			optionsInEffect = optionParser.parse(args);
			boolean showUsage = false;
			if (optionsInEffect.has(optionParser.help) || args.length == 0) {
				showUsage = true;
			}
			if (optionsInEffect.has(optionParser.relicListFile)) {
				if (showUsage) {
					showUsage(optionParser);
					showUsage = false;
				}
				System.out.println("Initializing relics...");
				System.out.println("-------------------------------------------------------------------------------");
				System.out.println("Using the following properties: ");
				properties = new Properties();
				try {
					properties.load(new FileInputStream(new File(PROPERTIES)));
				} catch (Exception e1) {
					// Absorb
					System.out.println("Could not read properties files. Assuming programmed default values for client settings.");

				}
				for (PropertyKeys p : PropertyKeys.values()) {
					System.out.println("\t " + p.getKey() + ": " + getProperty(p));
				}
				System.out.println("-------------------------------------------------------------------------------");
				if(optionsInEffect.has(optionParser.clean)){
					MongoStore ms;
					System.out.println("Cleaning database...");
					if (Boolean.parseBoolean(getProperty(PropertyKeys.AUTH))) {
						ms = new MongoStore(getProperty(PropertyKeys.HOST), Integer.parseInt(getProperty(PropertyKeys.PORT)), getProperty(PropertyKeys.DBNAME), "relics", getProperty(PropertyKeys.USER), getProperty(PropertyKeys.PASS));
						ms.clearRelicDB();
					} else {
						ms = new MongoStore(getProperty(PropertyKeys.HOST), Integer.parseInt(getProperty(PropertyKeys.PORT)), getProperty(PropertyKeys.DBNAME), "relics");
						ms.clearRelicDB();
					}
					System.out.println("-------------------------------------------------------------------------------");
				}
				File listFile = optionsInEffect.valueOf(optionParser.relicListFile);
				System.out.println("Using " + listFile.getAbsolutePath() + " as input...");
				ArrayList<CsvEntry> listEntries = CsvParser.parse(listFile, getProperty(PropertyKeys.SEPARATOR).charAt(0));
				System.out.println("Read " + listEntries.size() + " entries...");
				int ctotal = 0, crelics = 0, cresolvers = 0;
				if (listEntries.size() > 0) {
					MongoStore ms;
					if (Boolean.parseBoolean(getProperty(PropertyKeys.AUTH))) {
						ms = new MongoStore(getProperty(PropertyKeys.HOST), Integer.parseInt(getProperty(PropertyKeys.PORT)), getProperty(PropertyKeys.DBNAME), "relics", getProperty(PropertyKeys.USER), getProperty(PropertyKeys.PASS));
					} else {
						ms = new MongoStore(getProperty(PropertyKeys.HOST), Integer.parseInt(getProperty(PropertyKeys.PORT)), getProperty(PropertyKeys.DBNAME), "relics");
					}
					for (CsvEntry entry : listEntries) {
						String relicId = entry.getEntryValue("id");
						String fileName = entry.getEntryValue("filename");
						String localfsPath = entry.getEntryValue("localfs");
						String srmurl = entry.getEntryValue("srmurl");
						if (isNullOrEmpty(relicId)) {
							showErrorAndExit(optionParser, new Exception("The input file must contain values for the id and filename fields!"));
						}
						
						Relic r = new Relic(relicId);
						r.setFileName(fileName);
						ms.putRelic(r);
						ctotal++;
						crelics++;
						System.out.println("Added relic: " + r.getIdentifier());
						
						if(!isNullOrEmpty(localfsPath)) {
							ResolverDescriptor rd = new ResolverDescriptor(r.getIdentifier(), LocalFileSystemResolver.ENVIRONMENT);
							rd.setProperty(LocalFileSystemResolver.DescriptorKeys.ABSOLUTEPATH.getKey(), localfsPath);
							ms.putResolverDescriptor(rd);
							cresolvers++;
							ctotal++;
							System.out.println("\t With local filesystem path: " + localfsPath);
						}
						
						if(!isNullOrEmpty(srmurl)) {
							ResolverDescriptor rd = new ResolverDescriptor(r.getIdentifier(), GridSRMResolver.ENVIRONMENT);
							rd.setProperty(GridSRMResolver.DescriptorKeys.SRMURL.getKey(), srmurl);
							ms.putResolverDescriptor(rd);
							cresolvers++;
							ctotal++;
							System.out.println("\t With grid storage URL: " + srmurl);
						}
					}
				}
				System.out.println("-------------------------------------------------------------------------------");
				System.out.println("Done!");
				System.out.println("Added:");
				System.out.println(" Relics: " + crelics);
				System.out.println(" Resolvers: " + cresolvers);
				System.out.println(" Total: " + ctotal);
				System.out.println("-------------------------------------------------------------------------------");
			} else {
				showUsage = true;
			}
			if (showUsage) {
				showUsage(optionParser);
			}
		} catch (Exception e) {
			showErrorAndExit(optionParser, e);
		}
	}

	private static void showErrorAndExit(InitRelicDBOptionParser optionParser, Exception e) {
		System.out.println("Something didn't quite work like expected: [" + e.getMessage() + "]");
		showUsage(optionParser);
		System.exit(1);
	}

	private static void showUsage(InitRelicDBOptionParser optionParser) {
		try {
			optionParser.printHelpOn(System.out);
		} catch (IOException e) {
			// Should never happen in this case. I wonder how the sysout below
			// would fare..
			System.out.println("Yikes, could not print to System.out");
			e.printStackTrace();
		}
	}

	private static String getProperty(PropertyKeys prop) {
		return prop.getProperty(properties);
	}

	private static boolean isNullOrEmpty(String s) {
		if (s == null || "".equals(s)) {
			return true;
		}
		return false;
	}
}
