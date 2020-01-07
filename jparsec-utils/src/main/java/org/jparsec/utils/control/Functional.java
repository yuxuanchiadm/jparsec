package org.jparsec.utils.control;

import java.util.Objects;
import java.util.function.Function;

public final class Functional {
	Functional() {}

	public static <A, B> Function<A, B> fix(Function<Function<A, B>, Function<A, B>> f) {
		return new Function<A, B>() {
			volatile Function<A, B> self;

			@Override public B apply(A a) {
				Function<A, B> local;
				if ((local = self) == null) synchronized (this) { if ((local = self) == null) local = self = Objects.requireNonNull(f.apply(this)); }
				return local.apply(a);
			}
		};
	}
}
