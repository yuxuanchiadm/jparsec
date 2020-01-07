package org.jparsec.utils;

import java.util.function.Supplier;

public final class Notation {
	Notation() {}

	public static <A> A $do(A a) { return a; }
	public static <A> A $(A a) { return a; }
}
