package org.vaadin.cored;

import java.util.regex.Pattern;

import com.vaadin.data.validator.AbstractValidator;

public class JavaUtils {
	
	private static final Pattern validClass = Pattern.compile("^[A-Z][A-Za-z1-9_]+$");
	
	public static boolean isValidJavaClass(String s) {
		System.out.println("isValidClass(\""+s+"\");");
		return validClass.matcher(s).matches();
	}

	@SuppressWarnings("serial")
	public static class JavaClassNameValidator extends AbstractValidator {

		public JavaClassNameValidator() {
			super("Class names should start with a capital letter and contain letters, numbers, _");
		}

		public boolean isValid(Object value) {
			return value instanceof String && isValidJavaClass((String) value);
		}
		
	}
	
}
