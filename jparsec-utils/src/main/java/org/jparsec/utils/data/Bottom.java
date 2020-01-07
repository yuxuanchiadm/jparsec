package org.jparsec.utils.data;

public abstract class Bottom {
	Bottom() {}

	public interface Match<R> {}
	public final <R> R match(Match<R> match) { return caseof(); }
	public abstract <R> R caseof();

	public static <A> A absurd(Bottom b) { return b.caseof(); };

	public static <A, B> B unsafeCoerce(A a) { @SuppressWarnings("unchecked") B b = (B) a; return b; }
	public static class Undefined extends RuntimeException {}
	public static <A> A undefined() throws Undefined { throw new Undefined(); }

	@Override public abstract boolean equals(Object x);
	@Override public abstract int hashCode();
}
