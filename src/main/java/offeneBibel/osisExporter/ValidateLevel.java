package offeneBibel.osisExporter;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ValidateLevel implements IParameterValidator {
	public void validate(String name, String value) throws ParameterException {
		int level = Integer.parseInt(value);
		if (level < 0 || level > 7) {
			throw new ParameterException("Parameter " + name + " should be between \"0\" and \"7\".");
		}
	}
}