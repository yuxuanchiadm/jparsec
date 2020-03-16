package org.jparsec.core;

import org.jparsec.core.Parser.Location;
import static org.jparsec.core.Parser.Location.*;
import org.jparsec.core.Parser.Message;
import static org.jparsec.core.Parser.Message.*;
import org.jparsec.core.Parser.Logger;
import static org.jparsec.core.Parser.Logger.*;
import org.jparsec.core.Parser.Environment;
import static org.jparsec.core.Parser.Environment.*;
import org.jparsec.core.Parser.Result;
import static org.jparsec.core.Parser.Result.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.control.Trampoline;
import static org.monadium.core.control.Trampoline.*;
import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
import org.monadium.core.data.List;
import static org.monadium.core.data.List.*;
import org.monadium.core.data.Maybe;
import static org.monadium.core.data.Maybe.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

import static org.monadium.core.Notation.*;
import static org.monadium.core.control.Trampoline.Notation.*;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public final class Parser<S, U, E, A> {
	public static final class Location implements Comparable<Location> {
		final String tag;
		final int line;
		final int column;

		Location(String tag, int line, int column) { this.tag = tag; this.line = line; this.column = column; }

		public static Location location(String tag, int line, int column) { return new Location(tag, line, column); }
		public static Location location() { return location("<unknown>", 1, 1); }

		public String tag() { return tag; }
		public int line() { return line; }
		public int column() { return column; }

		public Location advanceCharacter(char c) { return c == '\n' ? location(tag, line + 1, 1) : location(tag, line, column + 1); }
		public Location advanceString(String s) { Location result = this; for (int i = 0; i < s.length(); i++) result = result.advanceCharacter(s.charAt(i)); return result; }

		@Override public int compareTo(Location location) {
			int ord;
			ord = tag.compareTo(location.tag);
			if (ord != 0) return ord;
			ord = Integer.compare(line, location.line);
			if (ord != 0) return ord;
			ord = Integer.compare(column, location.column);
			if (ord != 0) return ord;
			return 0;
		}
		@Override public String toString() { return "(tag: " + tag + ", line: " + line + ", column: " + column + ")"; }
	}
	public static abstract class Message<E> {
		public enum Type {
			INFO("Info", true),
			WARNING("Warning", true),
			ERROR("Error", true),
			INTERNAL("Internal", true),
			EXTERNAL("External", true),
			UNEXPECTED("Unexpected", false),
			EXPECTED("Expected", false);

			public final String key;
			public final boolean multiline;

			Type(String key, boolean multiline) { this.key = key; this.multiline = multiline; }
		}
		public static final class Info<E> extends Message<E> {
			final String message;

			Info(String message) { this.message = message; }

			public interface Case<E, R> { R caseInfo(String message); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseInfo.caseInfo(message); }

			@Override public Message.Type type() { return Message.Type.INFO; }

			@Override public String toString() { return message; }
		}
		public static final class Warning<E> extends Message<E> {
			final String message;

			Warning(String message) { this.message = message; }

			public interface Case<E, R> { R caseWarning(String message); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseWarning.caseWarning(message); }

			@Override public Message.Type type() { return Message.Type.WARNING; }

			@Override public String toString() { return message; }
		}
		public static final class Error<E> extends Message<E> {
			final String message;

			Error(String message) { this.message = message; }

			public interface Case<E, R> { R caseError(String message); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseError.caseError(message); }

			@Override public Message.Type type() { return Message.Type.ERROR; }

			@Override public String toString() { return message; }
		}
		public static final class Internal<E> extends Message<E> {
			final String message;

			Internal(String message) { this.message = message; }

			public interface Case<E, R> { R caseInternal(String message); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseInternal.caseInternal(message); }

			@Override public Message.Type type() { return Message.Type.INTERNAL; }

			@Override public String toString() { return message; }
		}
		public static final class External<E> extends Message<E> {
			final E external;

			External(E external) { this.external = external; }

			public interface Case<E, R> { R caseExternal(E external); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseExternal.caseExternal(external); }

			@Override public Message.Type type() { return Message.Type.EXTERNAL; }

			@Override public String toString() { return external.toString(); }
		}
		public static final class Unexpected<E> extends Message<E> {
			final String syntax;

			Unexpected(String syntax) { this.syntax = syntax; }

			public interface Case<E, R> { R caseUnexpected(String syntax); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseUnexpected.caseUnexpected(syntax); }

			@Override public Message.Type type() { return Message.Type.UNEXPECTED; }

			@Override public String toString() { return syntax; }
		}
		public static final class Expected<E> extends Message<E> {
			final String syntax;

			Expected(String syntax) { this.syntax = syntax; }

			public interface Case<E, R> { R caseExpected(String syntax); }
			@Override public <R> R caseof(
				Info.Case<E, R> caseInfo,
				Warning.Case<E, R> caseWarning,
				Error.Case<E, R> caseError,
				Internal.Case<E, R> caseInternal,
				External.Case<E, R> caseExternal,
				Unexpected.Case<E, R> caseUnexpected,
				Expected.Case<E, R> caseExpected
			) { return caseExpected.caseExpected(syntax); }

			@Override public Message.Type type() { return Message.Type.EXPECTED; }

			@Override public String toString() { return syntax; }
		}

		Message() {}

		public static <E> Message<E> info(String message) { return new Info<>(message); }
		public static <E> Message<E> warning(String message) { return new Warning<>(message); }
		public static <E> Message<E> error(String message) { return new Error<>(message); }
		public static <E> Message<E> internal(String message) { return new Internal<>(message); }
		public static <E> Message<E> external(E external) { return new External<>(external); }
		public static <E> Message<E> unexpected(String syntax) { return new Unexpected<>(syntax); }
		public static <E> Message<E> expected(String syntax) { return new Expected<>(syntax); }

		public interface Match<E, R> extends Info.Case<E, R>, Warning.Case<E, R>, Error.Case<E, R>, Internal.Case<E, R>, External.Case<E, R>, Unexpected.Case<E, R>, Expected.Case<E, R> {}
		public final <R> R match(Match<E, R> match) { return caseof(match, match, match, match, match, match, match); }
		public abstract <R> R caseof(
			Info.Case<E, R> caseInfo,
			Warning.Case<E, R> caseWarning,
			Error.Case<E, R> caseError,
			Internal.Case<E, R> caseInternal,
			External.Case<E, R> caseExternal,
			Unexpected.Case<E, R> caseUnexpected,
			Expected.Case<E, R> caseExpected
		);

		public abstract Message.Type type();

		@Override public abstract String toString();
	}
	public static final class Logger<E> {
		final SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap;

		Logger(SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap) { this.logMap = logMap; }

		@SafeVarargs public static <E> Logger<E> logger(Location location, Message<E>... messages) {
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap = new TreeMap<>();
			logMap.put(location, Arrays.stream(messages).collect(Collectors.toMap(
				Message::type,
				List::list,
				List::concat,
				() -> new EnumMap<>(Message.Type.class)
			)));
			return new Logger<>(logMap);
		}
		public static <E> Logger<E> logger() { return new Logger<>(new TreeMap<>()); }

		public Stream<Tuple<Location, Message<E>>> messages() {
			return logMap.entrySet().stream()
				.flatMap(entry -> entry.getValue().values().stream()
				.flatMap(messages -> messages.stream()
				.map(message -> tuple(entry.getKey(), message))));
		}
		public Logger<E> concat(Logger<E> logger) {
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap1 = this.logMap;
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap2 = logger.logMap;
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap3 = new TreeMap<>(logMap1);
			logMap2.forEach((location, messageMap) -> logMap3.merge(location, messageMap, (messageMap1, messageMap2) -> {
				EnumMap<Message.Type, List<Message<E>>> messageMap3 = new EnumMap<>(messageMap1);
				messageMap2.forEach((type, messages) -> messageMap3.merge(type, messages, List::concat));
				return messageMap3;
			}));
			return new Logger<>(logMap3);
		}
		@SafeVarargs public final Logger<E> log(Location location, Message<E>... messages) { return concat(logger(location, messages)); }

		static <E> String printSingleline(Message.Type type, List<Message<E>> messages, String prefix) {
			return prefix + type.key + ": " + String.join(", ", messages.stream()
				.map(Message::toString).distinct().sorted()
				.toArray(CharSequence[]::new)
			) + "\n";
		}
		static <E> String printMultiline(Message.Type type, List<Message<E>> messages, String prefix) {
			return messages.stream()
				.map(Message::toString).distinct().sorted()
				.map(message -> prefix + type.key + ": " + message + "\n")
				.reduce("", String::concat);
		}
		static <E> String printMessages(EnumMap<Message.Type, List<Message<E>>> messageMap, String prefix) {
			return messageMap.isEmpty() ? "Parser error occurred.\n" : Arrays.stream(Message.Type.values()).reduce("",
				(output, type) -> output + (!messageMap.containsKey(type) ? "" : !type.multiline
					? printSingleline(type, messageMap.get(type), prefix)
					: printMultiline(type, messageMap.get(type), prefix)),
				String::concat
			);
		}

		@Override public String toString() {
			return String.join("\n", logMap.entrySet().stream()
				.map(entry ->
					entry.getKey().tag() + ":" +
					entry.getKey().line() + ":" +
					entry.getKey().column() + ":\n" +
					printMessages(entry.getValue(), "    "))
				.toArray(CharSequence[]::new));
		}
	}
	public static final class Environment<S, U, E> {
		final S stream;
		final U user;
		final Location location;
		final Logger<E> logger;

		Environment(S stream, U user, Location location, Logger<E> logger) { this.stream = stream; this.user = user; this.location = location; this.logger = logger; }

		public static <S, U, E> Environment<S, U, E> environment(S stream, U user, Location location, Logger<E> logger) { return new Environment<>(stream, user, location, logger); }

		public S stream() { return stream; }
		public U user() { return user; }
		public Location location() { return location; }
		public Logger<E> logger() { return logger; }

		public Environment<S, U, E> updateStream(S stream) { return environment(stream, user, location, logger); }
		public Environment<S, U, E> mapStream(Function<S, S> f) { return environment(f.apply(stream), user, location, logger); }
		public Environment<S, U, E> updateUser(U user) { return environment(stream, user, location, logger); }
		public Environment<S, U, E> mapUser(Function<U, U> f) { return environment(stream, f.apply(user), location, logger); }
		public Environment<S, U, E> updateLocation(Location location) { return environment(stream, user, location, logger); }
		public Environment<S, U, E> mapLocation(Function<Location, Location> f) { return environment(stream, user, f.apply(location), logger); }
		public Environment<S, U, E> updateLogger(Logger<E> logger) { return environment(stream, user, location, logger); }
		public Environment<S, U, E> mapLogger(Function<Logger<E>, Logger<E>> f) { return environment(stream, user, location, f.apply(logger)); }

		@SafeVarargs public final Environment<S, U, E> log(Location location, Message<E>... messages) {
			return new Environment<>(this.stream, this.user, this.location, this.logger.log(location, messages));
		}
		@SafeVarargs public final Environment<S, U, E> log(Message<E>... messages) { return log(location, messages); }
	}
	public static abstract class Result<S, U, E, A> {
		public static final class Success<S, U, E, A> extends Result<S, U, E, A> {
			final Environment<S, U, E> environment;
			final boolean consumed;
			final A result;

			Success(Environment<S, U, E> environment, boolean consumed, A result) { this.environment = environment; this.consumed = consumed; this.result = result; }

			public interface Case<S, U, E, A, R> { R caseSuccess(Environment<S, U, E> environment, boolean consumed, A result); }
			@Override public <R> R caseof(Success.Case<S, U, E, A, R> caseSuccess, Fail.Case<S, U, E, A, R> caseFail) { return caseSuccess.caseSuccess(environment, consumed, result); }

			@Override public boolean isSuccess() { return true; }
			@Override public boolean isFail() { return false; }
			@Override public Environment<S, U, E> getEnvironment() { return environment; }
			@Override public boolean getConsumed() { return consumed; }
			@Override public Maybe<A> getResult() { return just(result); }
			@Override public A coerceResult() throws Undefined { return result; }
			@Override public boolean coerceAbort() throws Undefined { return undefined(); }
		}
		public static final class Fail<S, U, E, A> extends Result<S, U, E, A> {
			final Environment<S, U, E> environment;
			final boolean consumed;
			final boolean halt;

			Fail(Environment<S, U, E> environment, boolean consumed, boolean halt) { this.environment = environment; this.consumed = consumed; this.halt = halt; }

			public interface Case<S, U, E, A, R> { R caseFail(Environment<S, U, E> environment, boolean consumed, boolean halt); }
			@Override public <R> R caseof(Success.Case<S, U, E, A, R> caseSuccess, Fail.Case<S, U, E, A, R> caseFail) { return caseFail.caseFail(environment, consumed, halt); }

			@Override public boolean isSuccess() { return false; }
			@Override public boolean isFail() { return true; }
			@Override public Environment<S, U, E> getEnvironment() { return environment; }
			@Override public boolean getConsumed() { return consumed; }
			@Override public Maybe<A> getResult() { return nothing(); }
			@Override public A coerceResult() throws Undefined { return undefined(); }
			@Override public boolean coerceAbort() throws Undefined { return halt; }
		}

		Result() {}

		public interface Match<S, U, E, A, R> extends Success.Case<S, U, E, A, R>, Fail.Case<S, U, E, A, R> {}
		public final <R> R match(Match<S, U, E, A, R> match) { return caseof(match, match); }
		public abstract <R> R caseof(Success.Case<S, U, E, A, R> caseSuccess, Fail.Case<S, U, E, A, R> caseFail);

		public static <S, U, E, A> Result<S, U, E, A> success(Environment<S, U, E> environment, boolean consumed, A result) { return new Success<>(environment, consumed, result); }
		public static <S, U, E, A> Result<S, U, E, A> fail(Environment<S, U, E> environment, boolean consumed, boolean halt) { return new Fail<>(environment, consumed, halt); }

		public abstract boolean isSuccess();
		public abstract boolean isFail();
		public abstract Environment<S, U, E> getEnvironment();
		public abstract boolean getConsumed();
		public abstract Maybe<A> getResult();
		public abstract A coerceResult() throws Undefined;
		public abstract boolean coerceAbort() throws Undefined;
	}

	Parser(Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser) { this.parser = parser; }

	final Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser;

	public static <S, U, E, A> Parser<S, U, E, A> parser(Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser) { return new Parser<>(parser); }
	public static <S, U, E, A> Parser<S, U, E, A> simple(A a) { return parser(e -> done(success(e, false, a))); }
	public static <S, U, E, A> Parser<S, U, E, A> ignore() { return parser(e -> done(fail(e, false, false))); }
	public static <S, U, E, A> Parser<S, U, E, A> panic() { return halt(internal("Parser panicked")); }
	public static <S, U, E, A> Parser<S, U, E, A> recur(Function<Unit, Parser<S, U, E, A>> f) { return parser(e -> more(() -> f.apply(unit()).parser().apply(e))); }
	public static <S, U, E, A> Parser<S, U, E, A> recur(Supplier<Parser<S, U, E, A>> f) { return recur(u -> f.get()); }

	public Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser() { return parser; }

	public static <S, U, E> Parser<S, U, E, Environment<S, U, E>> getEnvironment() { return parser(e -> done(success(e, false, e))); }
	public static <S, U, E> Parser<S, U, E, Unit> setEnvironment(Environment<S, U, E> environment) { return parser(e -> done(success(environment, false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyEnvironment(Function<Environment<S, U, E>, Environment<S, U, E>> f) { return parser(e -> done(success(f.apply(e), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, S> getStream() { return parser(e -> done(success(e, false, e.stream()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setStream(S stream) { return parser(e -> done(success(environment(stream, e.user(), e.location(), e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyStream(Function<S, S> f) { return parser(e -> done(success(environment(f.apply(e.stream()), e.user(), e.location(), e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, U> getUser() { return parser(e -> done(success(e, false, e.user()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setUser(U user) { return parser(e -> done(success(environment(e.stream(), user, e.location(), e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyUser(Function<U, U> f) { return parser(e -> done(success(environment(e.stream(), f.apply(e.user()), e.location(), e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Location> getLocation() { return parser(e -> done(success(e, false, e.location()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setLocation(Location location) { return parser(e -> done(success(environment(e.stream(), e.user(), location, e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyLocation(Function<Location, Location> f) { return parser(e -> done(success(environment(e.stream(), e.user(), f.apply(e.location()), e.logger()), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Logger<E>> getLogger() { return parser(e -> done(success(e, false, e.logger()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setLogger(Logger<E> logger) { return parser(e -> done(success(environment(e.stream(), e.user(), e.location(), logger), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyLogger(Function<Logger<E>, Logger<E>> f) { return parser(e -> done(success(environment(e.stream(), e.user(), e.location(), f.apply(e.logger())), false, unit()))); }

	@SafeVarargs public static <S, U, E> Parser<S, U, E, Unit> log(Message<E>... messages) { return parser(e -> done(success(e.log(messages), false, unit()))); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> stop(Message<E>... messages) { return parser(e -> done(fail(e.log(messages), false, false))); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> halt(Message<E>... messages) { return parser(e -> done(fail(e.log(messages), false, true))); }
	@SafeVarargs public static <S, U, E> Parser<S, U, E, Unit> ensure(boolean condition, Message<E>... messages) { return condition ? simple(unit()) : stop(messages); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> conclude(Parser<S, U, E, A> parser, Message<E>... messages) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																		, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e1, c1, r1)),
				(e1, c1, h1) -> done(fail(e1.updateLogger(e.logger().log(e.location(), messages)), c1, h1))
			)																								))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> suppress(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)											, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e1, c1, r1)),
				(e1, c1, h1) -> done(fail(e1.updateLogger(e.logger()), c1, h1))
			)																	))
		));
	}

	public static <S, U, E, A> Parser<S, U, E, A> lookahead(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)													, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e.updateLogger(e1.logger()), false, r1)),
				(e1, c1, h1) -> done(fail(e1, c1, h1))
			)																			))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> attempt(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)												, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e1, c1, r1)),
				(e1, c1, h1) -> done(fail(e.updateLogger(e1.logger()), false, h1))
			)																		))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> advancing(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)														, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> c1
					? done(success(e1, true, r1))
					: done(fail(e1.log(internal("Parser not advancing")), false, false)),
				(e1, c1, h1) -> done(fail(e1, c1, h1))
			)																				))
		));
	}

	public static <S, U, E, A> Result<S, U, E, A> runParser(Parser<S, U, E, A> parser, Environment<S, U, E> environment) { return parser.parser().apply(environment).run(); }
	public static <S, U, E, A> Result<S, U, E, A> runParser(Parser<S, U, E, A> parser, S stream, U user, Location location, Logger<E> logger) { return runParser(parser, environment(stream, user, location, logger)); }
	public static <S, U, E, A> Result<S, U, E, A> runParser(Parser<S, U, E, A> parser, S stream, U user, Location location) { return runParser(parser, environment(stream, user, location, logger())); }
	public static <S, U, E, A> Result<S, U, E, A> runParser(Parser<S, U, E, A> parser, S stream, U user) { return runParser(parser, environment(stream, user, location(), logger())); }
	public static <S, E, A> Result<S, Unit, E, A> runParser(Parser<S, Unit, E, A> parser, S stream) { return runParser(parser, environment(stream, unit(), location(), logger())); }
	public static <S, U, E, A> Maybe<A> evalParser(Parser<S, U, E, A> parser, Environment<S, U, E> environment) { return runParser(parser, environment).getResult(); }
	public static <S, U, E, A> Maybe<A> evalParser(Parser<S, U, E, A> parser, S stream, U user, Location location, Logger<E> logger) { return evalParser(parser, environment(stream, user, location, logger)); }
	public static <S, U, E, A> Maybe<A> evalParser(Parser<S, U, E, A> parser, S stream, U user, Location location) { return evalParser(parser, environment(stream, user, location, logger())); }
	public static <S, U, E, A> Maybe<A> evalParser(Parser<S, U, E, A> parser, S stream, U user) { return evalParser(parser, environment(stream, user, location(), logger())); }
	public static <S, E, A> Maybe<A> evalParser(Parser<S, Unit, E, A> parser, S stream) { return evalParser(parser, environment(stream, unit(), location(), logger())); }
	public static <S, U, E, A> Environment<S, U, E> execParser(Parser<S, U, E, A> parser, Environment<S, U, E> environment) { return runParser(parser, environment).getEnvironment(); }
	public static <S, U, E, A> Environment<S, U, E> execParser(Parser<S, U, E, A> parser, S stream, U user, Location location, Logger<E> logger) { return execParser(parser, environment(stream, user, location, logger)); }
	public static <S, U, E, A> Environment<S, U, E> execParser(Parser<S, U, E, A> parser, S stream, U user, Location location) { return execParser(parser, environment(stream, user, location, logger())); }
	public static <S, U, E, A> Environment<S, U, E> execParser(Parser<S, U, E, A> parser, S stream, U user) { return execParser(parser, environment(stream, user, location(), logger())); }
	public static <S, E, A> Environment<S, Unit, E> execParser(Parser<S, Unit, E, A> parser, S stream) { return execParser(parser, environment(stream, unit(), location(), logger())); }

	public <B> Parser<S, U, E, B> map(Function<A, B> f) {
		return parser(e -> $do(
		$(	parser().apply(e)										, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e1, c1, f.apply(r1))),
				(e1, c1, h1) -> done(fail(e1, c1, h1))
			)														))
		));
	}
	public <B> Parser<S, U, E, B> applyMap(Parser<S, U, E, Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
	public <B> Parser<S, U, E, B> flatMap(Function<A, Parser<S, U, E, B>> f) {
		return parser(e -> $do(
		$(	parser().apply(e)														, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> more(() -> $do(
				$(	f.apply(r1).parser().apply(e1)						, result2 ->
				$(	result2.caseof(
						(e2, c2, r2) -> done(success(e2, c1 || c2, r2)),
						(e2, c2, h2) -> done(fail(e2, c1 || c2, h2))
					)													))
				)),
				(e1, c1, h1) -> done(fail(e1, c1, h1))
			)																		))
		));
	}
	public Parser<S, U, E, A> plus(Parser<S, U, E, A> fa) {
		return parser(e -> $do(
		$(	parser().apply(e)																					, result1 ->
		$(	result1.caseof(
				(e1, c1, r1) -> done(success(e1, c1, r1)),
				(e1, c1, h1) -> c1 || h1
					? done(fail(e1, c1, h1))
					: more(() -> $do(
					$(	fa.parser().apply(e)														, result2 ->
					$(	result2.caseof(
							(e2, c2, r2) -> done(success(e2, c2, r2)),
							(e2, c2, h2) -> done(fail(e2.mapLogger(e1.logger()::concat), c2, h2))
						)																			))
					))
			)																									))
		));
	}

	public static <S, U, E, A> Parser<S, U, E, A> pure(A a) { return simple(a); }
	public static <S, U, E, A> Parser<S, U, E, A> empty() { return ignore(); }
	public static <S, U, E, A> Parser<S, U, E, Maybe<A>> optional(Parser<S, U, E, A> fa) { return fa.map(Maybe::just).plus(pure(nothing())); }
	public static <S, U, E, A, B> Parser<S, U, E, B> replace(Parser<S, U, E, A> fa, B b) { return fa.map(a -> b); }
	public static <S, U, E, A> Parser<S, U, E, Unit> discard(Parser<S, U, E, A> fa) { return fa.map(a -> unit()); }

	public static final class Notation {
		Notation() {}

		public static <S, U, E, A, B> Parser<S, U, E, B> $(Parser<S, U, E, A> fa, Function<A, Parser<S, U, E, B>> f) { return fa.flatMap(f); }
		public static <S, U, E, A, B> Parser<S, U, E, B> $(Parser<S, U, E, A> fa, Supplier<Parser<S, U, E, B>> fb) { return fa.flatMap(a -> fb.get()); }
		@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> $sum(Parser<S, U, E, A>... fs) { return Arrays.stream(fs).reduce(empty(), Parser::plus); }
	}
}
