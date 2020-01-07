package org.jparsec.utils.data;

import org.jparsec.utils.data.Bottom;
import static org.jparsec.utils.data.Bottom.*;
import org.jparsec.utils.data.List;
import static org.jparsec.utils.data.List.*;
import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class Maybe<A> implements Iterable<A> {
	public static final class Nothing<A> extends Maybe<A> {
		Nothing() {}

		public interface Case<A, R> { R caseNothing(); }
		@Override public <R> R caseof(Nothing.Case<A, R> caseNothing, Just.Case<A, R> caseJust) { return caseNothing.caseNothing(); }

		@Override public boolean isNothing() { return true; }
		@Override public boolean isJust() { return false; }
		@Override public A fromJust(A other) { return other; }
		@Override public A coerceJust() throws Undefined { return undefined(); }

		@Override public boolean any(Predicate<A> p) { return false; }
		@Override public boolean all(Predicate<A> p) { return false; }
		@Override public List<A> toList() { return nil(); }

		@Override public <B> Maybe<B> map(Function<A, B> f) { return nothing(); }
		@Override public <B> Maybe<B> applyMap(Maybe<Function<A, B>> fab) { return nothing(); }
		@Override public <B> Maybe<B> flatMap(Function<A, Maybe<B>> f) { return nothing(); }
		@Override public Maybe<A> plus(Maybe<A> fa) { return fa; }

		@Override public boolean equals(Object x) { return x instanceof Nothing; }
		@Override public int hashCode() { return Objects.hash(1); }
	}
	public static final class Just<A> extends Maybe<A> {
		final A a;

		Just(A a) { this.a = a; }

		public interface Case<A, R> { R caseJust(A a); }
		@Override public <R> R caseof(Nothing.Case<A, R> caseNothing, Just.Case<A, R> caseJust) { return caseJust.caseJust(a); }

		@Override public boolean isNothing() { return false; }
		@Override public boolean isJust() { return true; }
		@Override public A fromJust(A other) { return a; }
		@Override public A coerceJust() throws Undefined { return a; }

		@Override public boolean any(Predicate<A> p) { return p.test(a); }
		@Override public boolean all(Predicate<A> p) { return p.test(a); }
		@Override public List<A> toList() { return singleton(a); }

		@Override public <B> Maybe<B> map(Function<A, B> f) { return just(f.apply(a)); }
		@Override public <B> Maybe<B> applyMap(Maybe<Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
		@Override public <B> Maybe<B> flatMap(Function<A, Maybe<B>> f) { return f.apply(a); }
		@Override public Maybe<A> plus(Maybe<A> fa) { return this; }

		@Override public boolean equals(Object x) { return x instanceof Just && Objects.equals(a, ((Just<?>) x).a); }
		@Override public int hashCode() { return Objects.hash(2, a); }
	}

	Maybe() {}

	public interface Match<A, R> extends Nothing.Case<A, R>, Just.Case<A, R> {}
	public final <R> R match(Match<A, R> match) { return caseof(match, match); }
	public abstract <R> R caseof(Nothing.Case<A, R> caseNothing, Just.Case<A, R> caseJust);

	public static <A> Maybe<A> nothing() { return new Nothing<>(); }
	public static <A> Maybe<A> just(A a) { return new Just<>(a); }
	public static <A> Maybe<A> maybe(Optional<A> optional) { return optional.map(a -> just(a)).orElse(nothing()); }

	public abstract boolean isNothing();
	public abstract boolean isJust();
	public abstract A fromJust(A other);
	public abstract A coerceJust() throws Undefined;

	public abstract boolean any(Predicate<A> p);
	public abstract boolean all(Predicate<A> p);
	public abstract List<A> toList();

	public abstract <B> Maybe<B> map(Function<A, B> f);
	public abstract <B> Maybe<B> applyMap(Maybe<Function<A, B>> fab);
	public abstract <B> Maybe<B> flatMap(Function<A, Maybe<B>> f);
	public abstract Maybe<A> plus(Maybe<A> fa);

	public static <A> Maybe<A> pure(A a) { return just(a); }
	public static <A> Maybe<A> empty() { return nothing(); }
	public static <A> Maybe<Maybe<A>> optional(Maybe<A> fa) { return fa.map(Maybe::just).plus(pure(nothing())); }
	public static <A, B> List<B> replace(List<A> fa, B b) { return fa.map(a -> b); }
	public static <A> List<Unit> discard(List<A> fa) { return fa.map(a -> unit()); }

	static final class MaybeIterator<A> implements Iterator<A> {
		Maybe<A> current;
		MaybeIterator(Maybe<A> current) { this.current = current; }
		@Override public boolean hasNext() { return current.isJust(); }
		@Override public A next() {
			if (current.isNothing())
				throw new NoSuchElementException();
			A next = current.coerceJust();
			current = nothing();
			return next;
		}
	}
	@Override public final Iterator<A> iterator() { return new MaybeIterator<>(this); }
	@Override public Spliterator<A> spliterator() { return Spliterators.spliterator(iterator(), 1, Spliterator.ORDERED | Spliterator.IMMUTABLE); }
	public final Stream<A> stream() { return StreamSupport.stream(spliterator(), false); };

	@Override public abstract boolean equals(Object x);
	@Override public abstract int hashCode();

	public static final class Notation {
		Notation() {}

		public static <A, B> Maybe<B> $(Maybe<A> fa, Function<A, Maybe<B>> f) { return fa.flatMap(f); }
		public static <A, B> Maybe<B> $(Maybe<A> fa, Supplier<Maybe<B>> fb) { return fa.flatMap(a -> fb.get()); }
		@SafeVarargs public static <A> Maybe<A> $sum(Maybe<A>... fs) { return Arrays.stream(fs).reduce(empty(), Maybe::plus); }
	}
}
