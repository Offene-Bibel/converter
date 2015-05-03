package util;

import offeneBibel.parser.BookNameHelper;

import com.beust.jcommander.IParameterValidator;
import com.beust.jcommander.ParameterException;

public class ValidateBook implements IParameterValidator {
    @Override
    public void validate(String name, String value) throws ParameterException {
        if (value.length() == 0) return;
        for(String bookName : value.split(",", -1)) {
            if (!BookNameHelper.getInstance().isValid(bookName)) {
                throw new ParameterException("Parameter "+name+" refers to unknown book \""+bookName+"\".");
            }
        }
    }
}
