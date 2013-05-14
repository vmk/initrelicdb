package nl.vu.psy.relic;

import java.io.File;
import java.util.ArrayList;

import joptsimple.OptionParser;
import joptsimple.OptionSpec;

public class InitRelicDBOptionParser extends OptionParser {
	public OptionSpec<File> relicListFile;
	@SuppressWarnings("rawtypes")
	public OptionSpec help;
	
	public InitRelicDBOptionParser() {
		super();
		
		ArrayList<String> relicListFileOption = new ArrayList<String>();
		relicListFileOption.add("r");
		relicListFileOption.add("relics");
		
		ArrayList<String> helpCommand = new ArrayList<String>();
		helpCommand.add("h");
		helpCommand.add("?");
		helpCommand.add("help");
		
		// accept rules:
		
		relicListFile = acceptsAll(relicListFileOption, "Required. A csv file listing relic ids and resolver properties").withRequiredArg().ofType(File.class).describedAs("file");
		help = acceptsAll(helpCommand, "Prints usage information.");
	}



}
