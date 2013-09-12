package util;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ValidateRunner implements IParameterValidator {
	public void validate(String name, String value) throws ParameterException {
		if (!value.equalsIgnoreCase("reporting") && !value.equalsIgnoreCase("tracing") && !value.equalsIgnoreCase("recovering")) {
			throw new ParameterException("Parameter " + name + " should either be \"reporting\", \"tracing\" or \"recovering\".");
		}
	}
}