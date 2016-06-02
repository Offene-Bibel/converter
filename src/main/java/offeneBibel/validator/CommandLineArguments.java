package offeneBibel.validator;

import util.ValidateRunner;

import com.beust.jcommander.Parameter;

class CommandLineArguments {
    @Parameter(names = { "-r", "--runner" }, description = "Runner to use for error reporting. One of: \"reporting\", \"tracing\" " +
                                    "or \"recovering\"", validateWith=ValidateRunner.class)
    String parseRunner = "reporting";

    @Parameter(names = { "-d", "--debugFile" }, description = "Where to save the debug file.")
    String debugFile = null;

    @Parameter(names = { "-i", "--inputFile" }, description = "File to validate.")
    String inputFile = null;

    @Parameter(names = { "--json" }, description = "Output a longer status report in JSON format.")
    boolean json = false;

    @Parameter(names = { "-u", "--url" }, description = "URL to download content from to validate.")
    String inputUrl = null;

    @Parameter(names = { "-t", "--timeout" }, description = "Number of milliseconds to wait for the parser before aborting.")
    long timeout = 10000;

    @Parameter(names = {"-h", "--help"})
    boolean help;
}
