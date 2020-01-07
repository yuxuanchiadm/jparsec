package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Message;
import static org.jparsec.core.Parser.Message.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.jparsec.utils.data.Either;
import static org.jparsec.utils.data.Either.*;
import org.jparsec.utils.data.List;
import static org.jparsec.utils.data.List.*;
import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;
import org.jparsec.utils.data.Tuple;
import static org.jparsec.utils.data.Tuple.*;
import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import static org.jparsec.utils.Notation.*;
import static org.jparsec.utils.data.List.Notation.*;

import java.util.Objects;
import java.util.function.Function;

public final class Term {
	public enum Associativity { NONE, LEFT, RIGHT }
	public static final class Definition<S, U, E, N, T> {
		final Parser<S, U, E, ?> spacesP;
		final Parser<S, U, E, ?> bracketBeginP;
		final Parser<S, U, E, ?> bracketEndP;
		final Function<N, Parser<S, U, E, ?>> notationP;
		final Parser<S, U, E, T> scalarP;
		final List<Level<N, T>> levels;

		Definition(Parser<S, U, E, ?> spacesP, Parser<S, U, E, ?> bracketBeginP, Parser<S, U, E, ?> bracketEndP, Function<N, Parser<S, U, E, ?>> notationP, Parser<S, U, E, T> scalarP, List<Level<N, T>> levels) {
			this.spacesP = spacesP;
			this.bracketBeginP = bracketBeginP;
			this.bracketEndP = bracketEndP;
			this.notationP = notationP;
			this.scalarP = scalarP;
			this.levels = levels;
		}

		@SafeVarargs public static <S, U, E, N, T> Definition<S, U, E, N, T> definition(Parser<S, U, E, ?> spacesP, Parser<S, U, E, ?> bracketBeginP, Parser<S, U, E, ?> bracketEndP, Function<N, Parser<S, U, E, ?>> notationP, Parser<S, U, E, T> termP, Level<N, T>... levels) {
			return new Definition<>(spacesP, bracketBeginP, bracketEndP, notationP, termP, list(levels));
		}

		public Parser<S, U, E, ?> spacesP() { return spacesP; }
		public Parser<S, U, E, ?> bracketBeginP() { return bracketBeginP; }
		public Parser<S, U, E, ?> bracketEndP() { return bracketEndP; }
		public Function<N, Parser<S, U, E, ?>> notationP() { return notationP; }
		public Parser<S, U, E, T> scalarP() { return scalarP; }
		public List<Level<N, T>> levels() { return levels; }
	}
	public static final class Level<N, T> {
		final Associativity associativity;
		final List<Operand<N, T>> operands;

		Level(Associativity associativity, List<Operand<N, T>> operands) { this.associativity = associativity; this.operands = operands; }

		@SafeVarargs public static <N, T> Level<N, T> level(Associativity associativity, Operand<N, T>... operands) { return new Level<>(associativity, list(operands)); }

		public Associativity associativity() { return associativity; }
		public List<Operand<N, T>> operands() { return operands; }
	}
	public static abstract class Operand<N, T> {
		public static final class Init<N, T> extends Operand<N, T> {
			final N notation;
			final Function<T, Operand<N, T>> next;

			Init(N notation, Function<T, Operand<N, T>> next) { this.notation = notation; this.next = next; }

			public interface Case<N, T, R> { R caseInit(N notation, Function<T, Operand<N, T>> next); }
			@Override public <R> R caseof(Init.Case<N, T, R> caseInit, Last.Case<N, T, R> caseLast) { return caseInit.caseInit(notation, next); }

			@Override public boolean isInit() { return true; }
			@Override public boolean isLast() { return false; }
			@Override public Maybe<N> notation() { return just(notation); }
			@Override public Maybe<Function<T, Operand<N, T>>> init() { return just(next); }
			@Override public Maybe<Function<T, T>> last() { return nothing(); }
		}
		public static final class Last<N, T> extends Operand<N, T> {
			final Function<T, T> next;

			Last(Function<T, T> next) { this.next = next; }

			public interface Case<N, T, R> { R caseLast(Function<T, T> last); }
			@Override public <R> R caseof(Init.Case<N, T, R> caseInit, Last.Case<N, T, R> caseLast) { return caseLast.caseLast(next); }

			@Override public boolean isInit() { return false; }
			@Override public boolean isLast() { return true; }
			@Override public Maybe<N> notation() { return nothing(); }
			@Override public Maybe<Function<T, Operand<N, T>>> init() { return nothing(); }
			@Override public Maybe<Function<T, T>> last() { return just(next); }
		}

		Operand() {}

		public interface Match<N, T, R> extends Init.Case<N, T, R>, Last.Case<N, T, R> {}
		public final <R> R match(Match<N, T, R> match) { return caseof(match, match); }
		public abstract <R> R caseof(Init.Case<N, T, R> caseInit, Last.Case<N, T, R> caseLast);

		public static <N, T> Operand<N, T> operand(N notation, Function<T, Operand<N, T>> next) { return new Init<>(notation, next); }
		public static <N, T> Operand<N, T> operand(Function<T, T> next) { return new Last<>(next); }

		public abstract boolean isInit();
		public abstract boolean isLast();
		public abstract Maybe<N> notation();
		public abstract Maybe<Function<T, Operand<N, T>>> init();
		public abstract Maybe<Function<T, T>> last();
	}

	Term() {}

	public static <S, U, E, N, T> Parser<S, U, E, T> termP(Definition<S, U, E, N, T> definition) {
		return recur(() -> new Object() {
			Parser<S, U, E, T> levelP(List<Level<N, T>> current) {
				return current.caseof(
					() -> recur(() -> bracketP()),
					(level, higher) -> {
						switch (level.associativity()) {
						case NONE: return recur(() -> noneP(level, higher));
						case LEFT: return recur(() -> leftP(level, higher));
						case RIGHT: return recur(() -> rightP(level, higher));
						default: throw new IllegalStateException();
						}
					}
				);
			}
			Parser<S, U, E, T> noneP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))													, term ->
				$(	option($do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term),
							recur(() -> levelP(higher))
						))															, result ->
					$(	result.second().caseof(
							operands -> loop(operands, operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(higher))
								))									, result1 ->
							$(	simple(result1.second())			))
							)),
							expr -> simple(expr)
						)															))
					), term)																	))
				);
			}
			Parser<S, U, E, T> leftP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))													, term ->
				$(	iterateSome(term, term1 -> $do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term1),
							recur(() -> levelP(higher))
						))															, result ->
					$(	result.second().caseof(
							operands -> loop(operands, operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(higher))
								))									, result1 ->
							$(	simple(result1.second())			))
							)),
							expr -> simple(expr)
						)															))
					))																			))
				);
			}
			Parser<S, U, E, T> rightP(Level<N, T> level, List<Level<N, T>> higher) {
				return $do(
				$(	recur(() -> levelP(higher))																, term ->
				$(	option($do(
					$(	recur(() -> operandP(
							level.operands(),
							simple(term),
							recur(() -> levelP(higher))
						))																		, result ->
					$(	result.second().caseof(
							operands -> loop(operands, operands1 -> $do(
							$(	recur(() -> operandP(
									operands1,
									recur(() -> levelP(higher)),
									recur(() -> levelP(cons(level, higher)))
								))												, result1 ->
							$(	simple(result1.second())						))
							)),
							expr -> simple(expr)
						)																		))
					), term)																				))
				);
			}
			Parser<S, U, E, Tuple<T, Either<List<Operand<N, T>>, T>>> operandP(List<Operand<N, T>> operands, Parser<S, U, E, T> initTermP, Parser<S, U, E, T> lastTermP) {
				return maybe(operands.stream().flatMap(operand -> operand.last().stream()).findFirst()).caseof(
					() -> $do(
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
					),
					last -> $do(
					$(	lastTermP										, term ->
					$(	simple(tuple(term, right(last.apply(term))))	))
					)
				);
			}
			Parser<S, U, E, List<Operand<N, T>>> notationP(List<Operand<N, T>> operands) {
				return $do(
				$(	operands.stream().flatMap(operand -> operand.notation().stream()).distinct().map(notation -> $do(
					$(	attempt(definition.notationP().apply(notation))	, () ->
					$(	simple(notation)								))
					)).reduce(ignore(), Parser::plus)																				, notation ->
				$(	simple(operands.filter(operand -> operand.notation().all(notation1 -> Objects.equals(notation1, notation))))	))
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
