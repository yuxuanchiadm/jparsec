package org.jparsec.utils.data;

import org.jparsec.utils.data.Bottom;
import static org.jparsec.utils.data.Bottom.*;
import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;
import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import java.util.Arrays;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Spliterator;
import java.util.Spliterators;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public abstract class List<A> implements Iterable<A> {
	public static final class Nil<A> extends List<A> {
		Nil() {}

		public interface Case<A, R> { R caseNil(); }
		@Override public <R> R caseof(Nil.Case<A, R> caseNil, Cons.Case<A, R> caseCons) { return caseNil.caseNil(); }

		@Override public boolean isNil() { return true; }
		@Override public boolean isCons() { return false; }
		@Override public A coerceHead() throws Undefined { return undefined(); }
		@Override public List<A> coerceTail() throws Undefined { return undefined(); }

		@Override public int length() { return 0; }
		@Override public List<A> filter(Predicate<A> p) { return nil(); }
		@Override public boolean any(Predicate<A> p) { return false; }
		@Override public boolean all(Predicate<A> p) { return false; }
		@Override public <B> B foldl(BiFunction<B, A, B> f, B b) { return b; }
		@Override public <B> B foldr(BiFunction<A, B, B> f, B b) { return b; }
		@Override public List<A> concat(List<A> list) { return list; }

		@Override public <B> List<B> map(Function<A, B> f) { return nil(); }
		@Override public <B> List<B> applyMap(List<Function<A, B>> fab) { return nil(); }
		@Override public <B> List<B> flatMap(Function<A, List<B>> f) { return nil(); }
		@Override public List<A> plus(List<A> fa) { return fa; }

		@Override public boolean equals(Object x) { return x instanceof Nil; }
		@Override public int hashCode() { return Objects.hash(1); }
	}
	public static final class Cons<A> extends List<A> {
		final A head;
		final List<A> tail;

		Cons(A head, List<A> tail) { this.head = head; this.tail = tail; }

		public interface Case<A, R> { R caseCons(A head, List<A> tail); }
		@Override public <R> R caseof(Nil.Case<A, R> caseNil, Cons.Case<A, R> caseCons) { return caseCons.caseCons(head, tail); }

		@Override public boolean isNil() { return false; }
		@Override public boolean isCons() { return true; }
		@Override public A coerceHead() throws Undefined { return head; }
		@Override public List<A> coerceTail() throws Undefined { return tail; }

		@Override public int length() { return 1 + tail.length(); }
		@Override public List<A> filter(Predicate<A> p) { return p.test(head) ? cons(head, tail.filter(p)) : tail.filter(p); }
		@Override public boolean any(Predicate<A> p) { return p.test(head) || tail.any(p); }
		@Override public boolean all(Predicate<A> p) { return p.test(head) && tail.all(p); }
		@Override public <B> B foldl(BiFunction<B, A, B> f, B b) { return tail.foldl(f, f.apply(b, head)); }
		@Override public <B> B foldr(BiFunction<A, B, B> f, B b) { return f.apply(head, tail.foldr(f, b)); }
		@Override public List<A> concat(List<A> list) { return cons(head, tail.concat(list)); }

		@Override public <B> List<B> map(Function<A, B> f) { return cons(f.apply(head), tail.map(f)); }
		@Override public <B> List<B> applyMap(List<Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
		@Override public <B> List<B> flatMap(Function<A, List<B>> f) { return f.apply(head).concat(tail.flatMap(f)); }
		@Override public List<A> plus(List<A> fa) { return concat(fa); }

		@Override public boolean equals(Object x) { return x instanceof Cons && Objects.equals(head, ((Cons<?>) x).head) && Objects.equals(tail, ((Cons<?>) x).tail); }
		@Override public int hashCode() { return Objects.hash(2, head, tail); }
	}

	List() {}

	public interface Match<A, R> extends Nil.Case<A, R>, Cons.Case<A, R> {}
	public final <R> R match(Match<A, R> match) { return caseof(match, match); }
	public abstract <R> R caseof(Nil.Case<A, R> caseNil, Cons.Case<A, R> caseCons);

	public static <A> List<A> nil() { return new Nil<>(); }
	public static <A> List<A> cons(A head, List<A> tail) { return new Cons<>(head, tail); }
	public static <A> List<A> singleton(A a) { return cons(a, nil()); }
	@SafeVarargs public static <A> List<A> list(A... as) {
		List<A> list = nil();
		for (int i = as.length - 1; i >= 0; i--)
			list = cons(as[i], list);
		return list;
	}
	public static <A> List<A> list(A[] as, int from, int to) throws IndexOutOfBoundsException, IllegalArgumentException {
		if (from < 0 || from > as.length)
			throw new IndexOutOfBoundsException();
		if (from > to)
			throw new IllegalArgumentException();
		List<A> list = nil();
		for (int i = to - 1; i >= from; i--)
			list = cons(as[i], list);
		return list;
	}

	public abstract boolean isNil();
	public abstract boolean isCons();
	public abstract A coerceHead() throws Undefined;
	public abstract List<A> coerceTail() throws Undefined;

	public abstract int length();
	public abstract List<A> filter(Predicate<A> p);
	public abstract boolean any(Predicate<A> p);
	public abstract boolean all(Predicate<A> p);
	public abstract <B> B foldl(BiFunction<B, A, B> f, B b);
	public abstract <B> B foldr(BiFunction<A, B, B> f, B b);
	public abstract List<A> concat(List<A> list);

	public abstract <B> List<B> map(Function<A, B> f);
	public abstract <B> List<B> applyMap(List<Function<A, B>> fab);
	public abstract <B> List<B> flatMap(Function<A, List<B>> f);
	public abstract List<A> plus(List<A> fa);

	public static <A> List<A> pure(A a) { return singleton(a); }
	public static <A> List<A> empty() { return nil(); }
	public static <A> List<Maybe<A>> optional(List<A> fa) { return fa.map(Maybe::just).plus(pure(nothing())); }
	public static <A, B> List<B> replace(List<A> fa, B b) { return fa.map(a -> b); }
	public static <A> List<Unit> discard(List<A> fa) { return fa.map(a -> unit()); }

	static final class ListIterator<A> implements Iterator<A> {
		List<A> current;
		ListIterator(List<A> current) { this.current = current; }
		@Override public boolean hasNext() { return current.isCons(); }
		@Override public A next() {
			if (current.isNil())
			throw new NoSuchElementException();
			A next = current.coerceHead();
			current = current.coerceTail();
			return next;
		}
	}
	@Override public final Iterator<A> iterator() { return new ListIterator<>(this); }
	@Override public Spliterator<A> spliterator() { return Spliterators.spliteratorUnknownSize(iterator(), Spliterator.ORDERED | Spliterator.IMMUTABLE); }
	public final Stream<A> stream() { return StreamSupport.stream(spliterator(), false); };

	@Override public abstract boolean equals(Object x);
	@Override public abstract int hashCode();

	public static final class Notation {
		Notation() {}

		public static <A, B> List<B> $(List<A> fa, Function<A, List<B>> f) { return fa.flatMap(f); }
		public static <A, B> List<B> $(List<A> fa, Supplier<List<B>> fb) { return fa.flatMap(a -> fb.get()); }
		@SafeVarargs public static <A> List<A> $sum(List<A>... fs) { return Arrays.stream(fs).reduce(empty(), List::plus); }
	}
}
