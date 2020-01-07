package org.jparsec.core;

public final class Escaper {
	Escaper() {}

	public static String escapeCharacter(char c) {
		StringBuilder builder = new StringBuilder();
		builder.append("\'");
		builder.append(
			c == '\b' ? "\\b" :
			c == '\t' ? "\\t" :
			c == '\n' ? "\\n" :
			c == '\f' ? "\\f" :
			c == '\r' ? "\\r" :
			c == '\"' ? "\\\"" :
			c == '\'' ? "\\\'" :
			c == '\\' ? "\\\\" :
			Character.toString(c)
		);
		builder.append("\'");
		return builder.toString();
	}
	public static String escapeString(String s) {
		StringBuilder builder = new StringBuilder();
		builder.append("\"");
		for (int i = 0; i < s.length(); i++) {
			char c = s.charAt(i);
			builder.append(
				c == '\b' ? "\\b" :
				c == '\t' ? "\\t" :
				c == '\n' ? "\\n" :
				c == '\f' ? "\\f" :
				c == '\r' ? "\\r" :
				c == '\"' ? "\\\"" :
				c == '\'' ? "\\\'" :
				c == '\\' ? "\\\\" :
				Character.toString(c)
			);
		}
		builder.append("\"");
		return builder.toString();
	}
}
