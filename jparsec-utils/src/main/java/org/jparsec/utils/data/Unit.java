package org.jparsec.utils.data;

import java.util.Objects;

public final class Unit {
	Unit() {}

	public interface Match<R> extends Unit.Case<R> {}
	public final <R> R match(Match<R> match) { return caseof(match); }
	public interface Case<R> { R caseUnit(); }
	public <R> R caseof(Unit.Case<R> caseUnit) { return caseUnit.caseUnit(); }

	static final Unit SINGLETON = new Unit();
	public static Unit unit() { return SINGLETON; }

	@Override public boolean equals(Object x) { return x instanceof Unit; }
	@Override public int hashCode() { return Objects.hash(); }
}
