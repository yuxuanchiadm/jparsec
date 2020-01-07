package org.jparsec.utils.data;

import java.util.Objects;

public final class Tuple<A, B> {
	final A a;
	final B b;

	Tuple(A a, B b) { this.a = a; this.b = b; }

	public interface Match<A, B, R> extends Tuple.Case<A, B, R> {}
	public final <R> R match(Match<A, B, R> match) { return caseof(match); }
	public interface Case<A, B, R> { R caseTuple(A a, B b); }
	public <R> R caseof(Tuple.Case<A, B, R> caseTuple) { return caseTuple.caseTuple(a, b); }

	public static <A, B> Tuple<A, B> tuple(A a, B b) { return new Tuple<>(a, b); }

	public A first() { return a; }
	public B second() { return b; }

	@Override public boolean equals(Object x) { return x instanceof Tuple && Objects.equals(a, ((Tuple<?, ?>) x).a) && Objects.equals(b, ((Tuple<?, ?>) x).b); }
	@Override public int hashCode() { return Objects.hash(a, b); }
}
