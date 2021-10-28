package org.jparsec.core;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.Objects;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Function;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

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

public record Parser<S, U, E, A>(Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser) {
	public record Location(String tag, int offset, int line, int column) implements Comparable<Location> {
		public static Location location(String tag, int offset, int line, int column) { return new Location(tag, offset, line, column); }
		public static Location location() { return location("<unknown>", 0, 1, 1); }

		public Location advanceCharacter(char c) { return c == '\n' ? location(tag(), offset() + 1, line() + 1, 1) : location(tag(), offset() + 1, line(), column() + 1); }
		public Location advanceString(String s) { Location result = this; for (int i = 0; i < s.length(); i++) result = result.advanceCharacter(s.charAt(i)); return result; }

		public String compact() { return tag() + ":" + offset() + ":(" + line() + "," + column() + ")"; }

		@Override public int compareTo(Location location) {
			int ord;
			ord = tag().compareTo(location.tag());
			if (ord != 0) return ord;
			ord = Integer.compare(offset(), location.offset());
			if (ord != 0) return ord;
			ord = Integer.compare(line(), location.line());
			if (ord != 0) return ord;
			ord = Integer.compare(column(), location.column());
			if (ord != 0) return ord;
			return 0;
		}
		@Override public String toString() { return "(tag: " + tag() + ", offset: " + offset() + ", line: " + line() + ", column: " + column() + ")"; }
	}
	public sealed interface Message<E> {
		enum Type {
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
		record Info<E>(String message) implements Message<E> {
			@Override public Message.Type type() { return Message.Type.INFO; }

			@Override public String toString() { return message(); }
		}
		record Warning<E>(String message) implements Message<E> {
			@Override public Message.Type type() { return Type.WARNING; }

			@Override public String toString() { return message(); }
		}
		record Error<E>(String message) implements Message<E> {
			@Override public Message.Type type() { return Type.ERROR; }

			@Override public String toString() { return message(); }
		}
		record Internal<E>(String message) implements Message<E> {
			@Override public Message.Type type() { return Type.INTERNAL; }

			@Override public String toString() { return message(); }
		}
		record External<E>(E external) implements Message<E> {
			@Override public Message.Type type() { return Message.Type.EXTERNAL; }

			@Override public String toString() { return external().toString(); }
		}
		record Unexpected<E>(String syntax) implements Message<E> {
			@Override public Message.Type type() { return Message.Type.UNEXPECTED; }

			@Override public String toString() { return syntax(); }
		}
		record Expected<E>(String syntax) implements Message<E> {
			@Override public Message.Type type() { return Message.Type.EXPECTED; }

			@Override public String toString() { return syntax(); }
		}

		static <E> Message<E> info(String message) { return new Info<>(message); }
		static <E> Message<E> warning(String message) { return new Warning<>(message); }
		static <E> Message<E> error(String message) { return new Error<>(message); }
		static <E> Message<E> internal(String message) { return new Internal<>(message); }
		static <E> Message<E> external(E external) { return new External<>(external); }
		static <E> Message<E> unexpected(String syntax) { return new Unexpected<>(syntax); }
		static <E> Message<E> expected(String syntax) { return new Expected<>(syntax); }

		Message.Type type();

		@Override String toString();
	}
	public record Logger<E>(SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap) {
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
			return logMap().entrySet().stream()
				.flatMap(entry -> entry.getValue().values().stream()
				.flatMap(messages -> messages.stream()
				.map(message -> tuple(entry.getKey(), message))));
		}
		public Logger<E> concat(Logger<E> logger) {
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap1 = this.logMap();
			SortedMap<Location, EnumMap<Message.Type, List<Message<E>>>> logMap2 = logger.logMap();
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
			return String.join("\n", logMap().entrySet().stream()
				.map(entry -> entry.getKey().compact() + ":\n" + printMessages(entry.getValue(), "    "))
				.toArray(CharSequence[]::new));
		}
	}
	public record Environment<S, U, E>(S stream, U user, Location location, Logger<E> logger) {
		public static <S, U, E> Environment<S, U, E> environment(S stream, U user, Location location, Logger<E> logger) { return new Environment<>(stream, user, location, logger); }

		public <A> Environment<A, U, E> updateStream(A stream) { return environment(stream, user(), location(), logger()); }
		public <A> Environment<A, U, E> mapStream(Function<S, A> f) { return environment(f.apply(stream()), user(), location(), logger()); }
		public <A> Environment<S, A, E> updateUser(A user) { return environment(stream(), user, location(), logger()); }
		public <A> Environment<S, A, E> mapUser(Function<U, A> f) { return environment(stream(), f.apply(user()), location(), logger()); }
		public Environment<S, U, E> updateLocation(Location location) { return environment(stream(), user(), location, logger()); }
		public Environment<S, U, E> mapLocation(Function<Location, Location> f) { return environment(stream(), user(), f.apply(location()), logger()); }
		public Environment<S, U, E> updateLogger(Logger<E> logger) { return environment(stream(), user(), location(), logger); }
		public Environment<S, U, E> mapLogger(Function<Logger<E>, Logger<E>> f) { return environment(stream(), user(), location(), f.apply(logger())); }

		@SafeVarargs public final Environment<S, U, E> log(Location location, Message<E>... messages) {
			return new Environment<>(stream(), user(), location(), logger().log(location, messages));
		}
		@SafeVarargs public final Environment<S, U, E> log(Message<E>... messages) { return log(location(), messages); }
	}
	public sealed interface Result<S, U, E, A> {
		record Success<S, U, E, A>(Environment<S, U, E> environment, boolean consumed, A result) implements Result<S, U, E, A> {
			@Override public boolean isSuccess() { return true; }
			@Override public boolean isFail() { return false; }
			@Override public Environment<S, U, E> getEnvironment() { return environment(); }
			@Override public boolean getConsumed() { return consumed(); }
			@Override public Maybe<A> getResult() { return just(result()); }
			@Override public A coerceResult() throws Undefined { return result(); }
			@Override public boolean coerceAbort() throws Undefined { return undefined(); }
		}
		record Fail<S, U, E, A>(Environment<S, U, E> environment, boolean consumed, boolean halt) implements Result<S, U, E, A> {
			@Override public boolean isSuccess() { return false; }
			@Override public boolean isFail() { return true; }
			@Override public Environment<S, U, E> getEnvironment() { return environment(); }
			@Override public boolean getConsumed() { return consumed(); }
			@Override public Maybe<A> getResult() { return nothing(); }
			@Override public A coerceResult() throws Undefined { return undefined(); }
			@Override public boolean coerceAbort() throws Undefined { return halt(); }
		}

		static <S, U, E, A> Result<S, U, E, A> success(Environment<S, U, E> environment, boolean consumed, A result) { return new Success<>(environment, consumed, result); }
		static <S, U, E, A> Result<S, U, E, A> fail(Environment<S, U, E> environment, boolean consumed, boolean halt) { return new Fail<>(environment, consumed, halt); }

		boolean isSuccess();
		boolean isFail();
		Environment<S, U, E> getEnvironment();
		boolean getConsumed();
		Maybe<A> getResult();
		A coerceResult() throws Undefined;
		boolean coerceAbort() throws Undefined;
	}

	public static <S, U, E, A> Parser<S, U, E, A> parser(Function<Environment<S, U, E>, Trampoline<Result<S, U, E, A>>> parser) { return new Parser<>(parser); }
	public static <S, U, E, A> Parser<S, U, E, A> simple(A a) { return parser(e -> done(success(e, false, a))); }
	public static <S, U, E, A> Parser<S, U, E, A> ignore() { return parser(e -> done(fail(e, false, false))); }
	public static <S, U, E, A> Parser<S, U, E, A> panic() { return halt(internal("Parser panicked")); }
	public static <S, U, E, A> Parser<S, U, E, A> recur(Function<Unit, Parser<S, U, E, A>> f) { return parser(e -> more(() -> f.apply(unit()).parser().apply(e))); }
	public static <S, U, E, A> Parser<S, U, E, A> recur(Supplier<Parser<S, U, E, A>> f) { return recur(u -> f.get()); }

	public static <S, U, E> Parser<S, U, E, Environment<S, U, E>> getEnvironment() { return parser(e -> done(success(e, false, e))); }
	public static <S, U, E> Parser<S, U, E, Unit> setEnvironment(Environment<S, U, E> environment) { return parser(e -> done(success(environment, false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyEnvironment(Function<Environment<S, U, E>, Environment<S, U, E>> f) { return parser(e -> done(success(f.apply(e), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, S> getStream() { return parser(e -> done(success(e, false, e.stream()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setStream(S stream) { return parser(e -> done(success(e.updateStream(stream), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyStream(Function<S, S> f) { return parser(e -> done(success(e.mapStream(f), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, U> getUser() { return parser(e -> done(success(e, false, e.user()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setUser(U user) { return parser(e -> done(success(e.updateUser(user), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyUser(Function<U, U> f) { return parser(e -> done(success(e.mapUser(f), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Location> getLocation() { return parser(e -> done(success(e, false, e.location()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setLocation(Location location) { return parser(e -> done(success(e.updateLocation(location), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyLocation(Function<Location, Location> f) { return parser(e -> done(success(e.mapLocation(f), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Logger<E>> getLogger() { return parser(e -> done(success(e, false, e.logger()))); }
	public static <S, U, E> Parser<S, U, E, Unit> setLogger(Logger<E> logger) { return parser(e -> done(success(e.updateLogger(logger), false, unit()))); }
	public static <S, U, E> Parser<S, U, E, Unit> modifyLogger(Function<Logger<E>, Logger<E>> f) { return parser(e -> done(success(e.mapLogger(f), false, unit()))); }

	public static <S, U, E, A, L> Parser<S, U, E, A> localStream(Parser<L, U, E, A> parser, Function<S, L> f) {
		return parser(e -> $do(
		$(	parser.parser().apply(e.mapStream(f))																					, result1 ->
		$(	switch (result1) {
				case Success<L, U, E, A> p1 -> done(success(p1.environment().updateStream(e.stream()), p1.consumed(), p1.result()));
				case Fail<L, U, E, A> p1 -> done(fail(p1.environment().updateStream(e.stream()), p1.consumed(), p1.halt()));
			}																														))
		));
	}
	public static <S, U, E, A, L> Parser<S, U, E, A> localUser(Parser<S, L, E, A> parser, Function<U, L> f) {
		return parser(e -> $do(
		$(	parser.parser().apply(e.mapUser(f))																					, result1 ->
		$(	switch (result1) {
				case Success<S, L, E, A> p1 -> done(success(p1.environment().updateUser(e.user()), p1.consumed(), p1.result()));
				case Fail<S, L, E, A> p1  -> done(fail(p1.environment().updateUser(e.user()), p1.consumed(), p1.halt()));
			}																													))
		));
	}

	@SafeVarargs public static <S, U, E> Parser<S, U, E, Unit> log(Message<E>... messages) { return parser(e -> done(success(e.log(messages), false, unit()))); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> stop(Message<E>... messages) { return parser(e -> done(fail(e.log(messages), false, false))); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> halt(Message<E>... messages) { return parser(e -> done(fail(e.log(messages), false, true))); }
	@SafeVarargs public static <S, U, E> Parser<S, U, E, Unit> ensure(boolean condition, Message<E>... messages) { return condition ? simple(unit()) : stop(messages); }
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> conclude(Parser<S, U, E, A> parser, Message<E>... messages) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																													, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), p1.result()));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment().updateLogger(e.logger().log(e.location(), messages)), p1.consumed(), p1.halt()));
			}																																			))
		));
	}
	@SafeVarargs public static <S, U, E, A> Parser<S, U, E, A> supplement(Parser<S, U, E, A> parser, Message<E>... messages) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																							, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), p1.result()));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment().log(e.location(), messages), p1.consumed(), p1.halt()));
			}																													))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> suppress(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																						, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), p1.result()));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment().updateLogger(e.logger()), p1.consumed(), p1.halt()));
			}																												))
		));
	}

	public static <S, U, E, A> Parser<S, U, E, A> lookahead(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																						, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(e.updateLogger(p1.environment().logger()), false, p1.result()));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment(), p1.consumed(), p1.halt()));
			}																												))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> attempt(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																				, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), p1.result()));
				case Fail<S, U, E, A> p1 -> done(fail(e.updateLogger(p1.environment().logger()), false, p1.halt()));
			}																										))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, A> advancing(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																	, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> p1.consumed()
					? done(success(p1.environment(), true, p1.result()))
					: done(fail(p1.environment().log(internal("Parser not advancing")), false, false));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment(), p1.consumed(), p1.halt()));
			}																							))
		));
	}
	public static <S, U, E, A> Parser<S, U, E, Tuple<Boolean, A>> inspect(Parser<S, U, E, A> parser) {
		return parser(e -> $do(
		$(	parser.parser().apply(e)																								, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), tuple(p1.consumed(), p1.result())));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment(), p1.consumed(), p1.halt()));
			}																														))
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
		$(	parser().apply(e)																						, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), f.apply(p1.result())));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment(), p1.consumed(), p1.halt()));
			}																										))
		));
	}
	public <B> Parser<S, U, E, B> applyMap(Parser<S, U, E, Function<A, B>> fab) { return fab.flatMap(f -> map(f)); }
	public <B> Parser<S, U, E, B> flatMap(Function<A, Parser<S, U, E, B>> f) {
		return parser(e -> $do(
		$(	parser().apply(e)																													, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> more(() -> $do(
				$(	f.apply(p1.result()).parser().apply(p1.environment())															, result2 ->
				$(	switch (result2) {
						case Success<S, U, E, B> p2 -> done(success(p2.environment(), p1.consumed() || p2.consumed(), p2.result()));
						case Fail<S, U, E, B> p2 -> done(fail(p2.environment(), p1.consumed() || p2.consumed(), p2.halt()));
					}																												))
				));
				case Fail<S, U, E, A> p1 -> done(fail(p1.environment(), p1.consumed(), p1.halt()));
			}																																	))
		));
	}
	public Parser<S, U, E, A> plus(Parser<S, U, E, A> fa) {
		return parser(e -> $do(
		$(	parser().apply(e)																																												, result1 ->
		$(	switch (result1) {
				case Success<S, U, E, A> p1 -> done(success(p1.environment(), p1.consumed(), p1.result()));
				case Fail<S, U, E, A> p1 -> p1.consumed() || p1.halt()
					? done(fail(p1.environment(), p1.consumed(), p1.halt()))
					: more(() -> $do(
					$(	fa.parser().apply(e)																																					, result2 ->
					$(	switch (result2) {
							case Success<S, U, E, A> p2 -> done(success(p2.environment(), p2.consumed(), p2.result()));
							case Fail<S, U, E, A> p2 -> done(fail(p2.consumed() ? p2.environment() : p2.environment().mapLogger(p1.environment().logger()::concat), p2.consumed(), p2.halt()));
						}																																										))
					));
			}																																																))
		));
	}

	public static <S, U, E, A> Parser<S, U, E, A> pure(A a) { return simple(a); }
	public static <S, U, E, A> Parser<S, U, E, A> empty() { return ignore(); }
	public static <S, U, E, A> Parser<S, U, E, Maybe<A>> optional(Parser<S, U, E, A> fa) { return fa.map(Maybe::just).plus(pure(nothing())); }
	public static <S, U, E, A, B> Parser<S, U, E, B> replace(Parser<S, U, E, A> fa, B b) { return fa.map(a -> b); }
	public static <S, U, E, A> Parser<S, U, E, Unit> discard(Parser<S, U, E, A> fa) { return fa.map(a -> unit()); }

	public interface Notation {
		static <S, U, E, A, B> Parser<S, U, E, B> $(Parser<S, U, E, A> fa, Function<A, Parser<S, U, E, B>> f) { return fa.flatMap(f); }
		static <S, U, E, A, B> Parser<S, U, E, B> $(Parser<S, U, E, A> fa, Supplier<Parser<S, U, E, B>> fb) { return fa.flatMap(a -> fb.get()); }
		@SafeVarargs static <S, U, E, A> Parser<S, U, E, A> $sum(Parser<S, U, E, A>... fs) { return Arrays.stream(fs).reduce(empty(), Parser::plus); }
	}
}
