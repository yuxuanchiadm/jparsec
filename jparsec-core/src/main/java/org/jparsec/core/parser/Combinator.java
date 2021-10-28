package org.jparsec.core.parser;

import java.util.Arrays;
import java.util.function.BiFunction;
import java.util.function.Function;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Either;
import static org.monadium.core.data.Either.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

import static org.monadium.core.Notation.*;

public interface Combinator {
	@SafeVarargs static <S, U, E> Parser<S, U, E, Unit> sequence(Parser<S, U, E, ?>... ps) { return replace(Arrays.stream(ps).reduce(simple(unit()), (p1, p2) -> p1.flatMap(x -> p2)), unit()); }

	@SafeVarargs static <S, U, E, A> Parser<S, U, E, A> choice(Parser<S, U, E, A>... ps) { return $sum(ps); }
	static <S, U, E, A> Parser<S, U, E, A> option(Parser<S, U, E, A> p, A a) { return p.plus(simple(a)); }

	static <S, U, E, A> Parser<S, U, E, List<A>> replicate(int i, Parser<S, U, E, A> p) {
		return i <= 0 ? simple(nil()) : $do(
		$(	p									, a ->
		$(	recur(() -> replicate(i - 1, p))	, as ->
		$(	simple(cons(a, as))					)))
		);
	}
	static <S, U, E, A, B> Parser<S, U, E, B> loop(A a, Function<A, Parser<S, U, E, Either<A, B>>> f) {
		return $do(
		$(	f.apply(a)												, e ->
		$(	switch (e) {
				case Left<A, B> p1 -> recur(() -> loop(p1.a(), f));
				case Right<A, B> p1 -> simple(p1.b());
			}														))
		);
	}

	static <S, U, E, A> Parser<S, U, E, A> between(Parser<S, U, E, ?> begin, Parser<S, U, E, ?> end, Parser<S, U, E, A> p) {
		return $do(
		$(	begin		, o1 ->
		$(	p			, a ->
		$(	end			, o2 ->
	 	$(	simple(a)	))))
		);
	}

	static <S, U, E, A> Parser<S, U, E, List<A>> some(Parser<S, U, E, A> p) { return recur(() -> many(p)).plus(simple(nil())); }
	static <S, U, E, A> Parser<S, U, E, List<A>> many(Parser<S, U, E, A> p) {
		return $do(
		$(	p						, a ->
		$(	recur(() -> some(p))	, as ->
		$(	simple(cons(a, as))		)))
		);
	}

	static <S, U, E, A> Parser<S, U, E, A> iterateSome(A a, Function<A, Parser<S, U, E, A>> f) { return recur(() -> iterateMany(a, f)).plus(simple(a)); }
	static <S, U, E, A> Parser<S, U, E, A> iterateMany(A a, Function<A, Parser<S, U, E, A>> f) {
		return $do(
		$(	f.apply(a)						, a1 ->
		$(	recur(() -> iterateSome(a1, f))	))
		);
	}

	static <S, U, E, A, B> Parser<S, U, E, B> foldSome(BiFunction<B, A, Parser<S, U, E, B>> f, B b, Parser<S, U, E, A> p) { return recur(() -> foldMany(f, b, p)).plus(simple(b)); }
	static <S, U, E, A, B> Parser<S, U, E, B> foldMany(BiFunction<B, A, Parser<S, U, E, B>> f, B b, Parser<S, U, E, A> p) {
		return $do(
		$(	p								, a ->
		$(	f.apply(b, a)					, b1 ->
		$(	recur(() -> foldSome(f, b1, p))	)))
		);
	}

	static <S, U, E, A> Parser<S, U, E, Unit> skipSome(Parser<S, U, E, A> p) { return recur(() -> skipMany(p).plus(simple(unit()))); }
	static <S, U, E, A> Parser<S, U, E, Unit> skipMany(Parser<S, U, E, A> p) {
		return $do(
		$(	p							, () ->
		$(	recur(() -> skipSome(p))	))
		);
	}

	static <S, U, E, A> Parser<S, U, E, List<A>> someSep(Parser<S, U, E, ?> sep, Parser<S, U, E, A> p) { return recur(() -> manySep(sep, p)).plus(simple(nil())); }
	static <S, U, E, A> Parser<S, U, E, List<A>> manySep(Parser<S, U, E, ?> sep, Parser<S, U, E, A> p) {
		return $do(
		$(	p										, a ->
		$(	recur(() -> some(sep.flatMap(o -> p)))	, as ->
		$(	simple(cons(a, as))						)))
		);
	}

	static <S, U, E, A> Parser<S, U, E, A> iterateSomeSep(Parser<S, U, E, ?> sep, A a, Function<A, Parser<S, U, E, A>> f) { return recur(() -> iterateManySep(sep, a, f)).plus(simple(a)); }
	static <S, U, E, A> Parser<S, U, E, A> iterateManySep(Parser<S, U, E, ?> sep, A a, Function<A, Parser<S, U, E, A>> f) {
		return $do(
		$(	f.apply(a)														, a1 ->
		$(	recur(() -> iterateSome(a1, a2 -> sep.flatMap(o -> f.apply(a2))))	))
		);
	}

	static <S, U, E, A> Parser<S, U, E, Unit> skipSomeSep(Parser<S, U, E, ?> sep, Parser<S, U, E, A> p) { return recur(() -> skipManySep(sep, p).plus(simple(unit()))); }
	static <S, U, E, A> Parser<S, U, E, Unit> skipManySep(Parser<S, U, E, ?> sep, Parser<S, U, E, A> p) {
		return $do(
		$(	p											, () ->
		$(	recur(() -> skipSome(sep.flatMap(o -> p)))	))
		);
	}

	static <S, U, E, A, B> Parser<S, U, E, B> foldSomeSep(Parser<S, U, E, ?> sep, BiFunction<B, A, Parser<S, U, E, B>> f, B b, Parser<S, U, E, A> p) { return recur(() -> foldManySep(sep, f, b, p)).plus(simple(b)); }
	static <S, U, E, A, B> Parser<S, U, E, B> foldManySep(Parser<S, U, E, ?> sep, BiFunction<B, A, Parser<S, U, E, B>> f, B b, Parser<S, U, E, A> p) {
		return $do(
		$(	p													, a ->
		$(	f.apply(b, a)										, b1 ->
		$(	recur(() -> foldSome(f, b1, sep.flatMap(o -> p)))	)))
		);
	}
}
