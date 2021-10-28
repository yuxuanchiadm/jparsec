package org.jparsec.core.parser;

import java.util.Objects;
import java.util.function.Function;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Either;
import static org.monadium.core.data.Either.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Maybe;
import static org.monadium.core.data.Maybe.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

import static org.monadium.core.Notation.*;
import static org.monadium.core.data.List.Notation.*;

public interface Term {
	enum Associativity { NONE, LEFT, RIGHT }
	record Definition<S, U, E, N, T>(
		Parser<S, U, E, ?> spacesP,
		Parser<S, U, E, ?> bracketBeginP,
		Parser<S, U, E, ?> bracketEndP,
		Function<N, Parser<S, U, E, ?>> notationP,
		Parser<S, U, E, T> scalarP,
		List<Level<N, T>> levels
	) {
		@SafeVarargs
		public static <S, U, E, N, T> Definition<S, U, E, N, T> definition(
			Parser<S, U, E, ?> spacesP,
			Parser<S, U, E, ?> bracketBeginP,
			Parser<S, U, E, ?> bracketEndP,
			Function<N, Parser<S, U, E, ?>> notationP,
			Parser<S, U, E, T> termP,
			Level<N, T>... levels
		) {
			return new Definition<>(spacesP, bracketBeginP, bracketEndP, notationP, termP, list(levels));
		}
	}
	record Level<N, T>(Associativity associativity, List<Operand<N, T>> operands) {
		@SafeVarargs
		public static <N, T> Level<N, T> level(Associativity associativity, Operand<N, T>... operands) {
			return new Level<>(associativity, list(operands));
		}
	}
	sealed interface Operand<N, T> {
		record Init<N, T>(N notation, Function<T, Operand<N, T>> next) implements Operand<N, T> {
			@Override public boolean isInit() { return true; }
			@Override public boolean isLast() { return false; }
			@Override public Maybe<N> safeNotation() { return just(notation); }
			@Override public Maybe<Function<T, Operand<N, T>>> init() { return just(next); }
			@Override public Maybe<Function<T, T>> last() { return nothing(); }
		}
		record Last<N, T>(Function<T, T> next) implements Operand<N, T> {
			@Override public boolean isInit() { return false; }
			@Override public boolean isLast() { return true; }
			@Override public Maybe<N> safeNotation() { return nothing(); }
			@Override public Maybe<Function<T, Operand<N, T>>> init() { return nothing(); }
			@Override public Maybe<Function<T, T>> last() { return just(next); }
		}

		static <N, T> Operand<N, T> operand(N notation, Function<T, Operand<N, T>> next) { return new Init<>(notation, next); }
		static <N, T> Operand<N, T> operand(Function<T, T> next) { return new Last<>(next); }

		boolean isInit();
		boolean isLast();
		Maybe<N> safeNotation();
		Maybe<Function<T, Operand<N, T>>> init();
		Maybe<Function<T, T>> last();
	}

	static <S, U, E, N, T> Parser<S, U, E, T> termP(Definition<S, U, E, N, T> definition) {
		return recur(() -> new Object() {
			Parser<S, U, E, T> levelP(List<Level<N, T>> current) {
				return switch (current) {
					case Nil<Level<N, T>> p1 -> recur(() -> bracketP());
					case Cons<Level<N, T>> p1 -> switch (p1.head().associativity()) {
						case NONE -> recur(() -> noneP(p1.head(), p1.tail()));
						case LEFT -> recur(() -> leftP(p1.head(), p1.tail()));
						case RIGHT -> recur(() -> rightP(p1.head(), p1.tail()));
					};
				};
			}
			Parser<S, U, E, T> noneP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))																				, term ->
				$(	option($do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term),
							recur(() -> levelP(higher))
						))																						, result ->
					$(	switch (result.second()) {
							case Left<List<Term.Operand<N, T>>, T> p1 -> loop(p1.a(), operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(higher))
								))																	, result1 ->
							$(	simple(result1.second())											))
							));
							case Right<List<Term.Operand<N, T>>, T> p1 -> simple(p1.b());
						}																						))
					), term)																								))
				);
			}
			Parser<S, U, E, T> leftP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))																				, term ->
				$(	iterateSome(term, term1 -> $do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term1),
							recur(() -> levelP(higher))
						))																						, result ->
					$(	switch (result.second()) {
							case Left<List<Term.Operand<N, T>>, T> p1 -> loop(p1.a(), operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(higher))
								))																	, result1 ->
							$(	simple(result1.second())											))
							));
							case Right<List<Term.Operand<N, T>>, T> p1 -> simple(p1.b());
						}																						))
					))																										))
				);
			}
			Parser<S, U, E, T> rightP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))																					, term ->
				$(	option($do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term),
							recur(() -> levelP(higher))
						))																							, result ->
					$(	switch (result.second()) {
							case Left<List<Term.Operand<N, T>>, T> p1 -> loop(p1.a(), operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(cons(level, higher)))
								))																	, result1 ->
							$(	simple(result1.second())											))
							));
							case Right<List<Term.Operand<N, T>>, T> p1 -> simple(p1.b());
						}																							))
					), term)																									))
				);
			}
			Parser<S, U, E, Tuple<T, Either<List<Operand<N, T>>, T>>> operandP(List<Operand<N, T>> operands, Parser<S, U, E, T> initTermP, Parser<S, U, E, T> lastTermP) {
				return switch (maybe(operands.stream().flatMap(operand -> operand.last().stream()).findFirst())) {
					case Nothing<Function<T, T>> p1 -> $do(
					$(	initTermP												, term ->
					$(	attempt($do(
						$(	definition.spacesP()				, () ->
						$(	recur(() -> notationP(operands))	, operands1 ->
						$(	definition.spacesP()				, () ->
						$(	simple(operands1)					))))
						))														, operands1 ->
					$(	simple(tuple(term, left($do(
						$(	operands1					, operand ->
						$(	operand.init().toList()		, init ->
						$(	singleton(init.apply(term))	)))
						))))													)))
					);
					case Just<Function<T, T>> p1 -> $do(
					$(	lastTermP										, term ->
					$(	simple(tuple(term, right(p1.a().apply(term))))	))
					);
				};
			}
			Parser<S, U, E, List<Operand<N, T>>> notationP(List<Operand<N, T>> operands) {
				return $do(
				$(	operands.stream().flatMap(operand -> operand.safeNotation().stream()).distinct().map(notation -> $do(
					$(	attempt(definition.notationP().apply(notation))	, () ->
					$(	simple(notation)								))
					)).reduce(ignore(), Parser::plus)																				, notation ->
				$(	simple(operands.filter(operand -> operand.safeNotation().all(notation1 -> Objects.equals(notation1, notation))))	))
				);
			}
			Parser<S, U, E, T> bracketP() {
				return choice($do(
				$(	definition.bracketBeginP()					, () ->
				$(	definition.spacesP()						, () ->
				$(	recur(() -> levelP(definition.levels()))	, expr ->
				$(	definition.spacesP()						, () ->
				$(	definition.bracketEndP()					, () ->
				$(	simple(expr)								))))))
				), definition.scalarP());
			}
		}.levelP(definition.levels()));
	}
}
