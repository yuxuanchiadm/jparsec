package org.jparsec.core.parser;

import org.jparsec.core.Escaper;
import static org.jparsec.core.Escaper.*;
import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Message;
import static org.jparsec.core.Parser.Message.*;
import org.jparsec.core.Parser.Result;
import static org.jparsec.core.Parser.Result.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import org.jparsec.utils.control.Trampoline;
import static org.jparsec.utils.control.Trampoline.*;
import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import java.util.Arrays;
import java.util.function.Predicate;

public final class Char {
	Char() {}

	public static <U, E> Parser<Text, U, E, Unit> eof() {
		return parser(e -> e.stream().caseof(
			() -> done(success(e, false, unit())),
			(h, t) -> done(fail(e.log(expected("eof")), false, false))
		));
	}
	public static <U, E> Parser<Text, U, E, Character> any() {
		return parser(e -> e.stream().caseof(
			() -> done(fail(e.log(unexpected("eof")), false, false)),
			(h, t) -> done(success(e.updateStream(t).mapLocation(l -> l.advanceCharacter(h)), true, h))
		));
	}
	public static <U, E> Parser<Text, U, E, Character> character(char c) {
		return parser(e -> e.stream().caseof(
			() -> done(fail(e.log(expected(escapeCharacter(c))), false, false)),
			(h, t) -> h == c
				? done(success(e.updateStream(t).mapLocation(l -> l.advanceCharacter(h)), true, h))
				: done(fail(e.log(expected(escapeCharacter(c))), false, false))
		));
	}
	public static <U, E> Parser<Text, U, E, String> string(String str) {
		return parser(e -> text(str).isPrefixOf(e.stream())
			? done(success(e.mapStream(s -> s.drop(str.length())).mapLocation(l -> l.advanceString(str)), !str.isEmpty(), str))
			: done(fail(e.log(expected(escapeString(str))), false, false))
		);
	}

	public static <U, E> Parser<Text, U, E, Character> satisfy(Predicate<Character> p) {
		return parser(e -> e.stream().caseof(
			() -> done(fail(e.log(unexpected("eof")), false, false)),
			(h, t) -> p.test(h)
				? done(success(e.updateStream(t).mapLocation(l -> l.advanceCharacter(h)), true, h))
				: done(fail(e.log(unexpected(escapeCharacter(h))), false, false))
		));
	}
	public static <U, E> Parser<Text, U, E, Character> dissatisfy(Predicate<Character> p) { return satisfy(p.negate()); }
	public static <U, E> Parser<Text, U, E, String> stringSatisfy(Predicate<Character> p) {
		return parser(e -> e.stream().span(p).caseof(
			(t, d) -> done(success(e.updateStream(d).mapLocation(l -> l.advanceString(t.toString())), t.isNonempty(), t.toString()))
		));
	}
	public static <U, E> Parser<Text, U, E, String> stringDissatisfy(Predicate<Character> p) { return stringSatisfy(p.negate()); }

	public static <U, E> Parser<Text, U, E, Character> oneOf(Character... cs) { return satisfy(Arrays.asList(cs)::contains); }
	public static <U, E> Parser<Text, U, E, Character> noneOf(Character... cs) { return dissatisfy(Arrays.asList(cs)::contains); }

	public static <U, E> Parser<Text, U, E, Character> space() { return conclude(satisfy(Character::isWhitespace), expected("space")); }
	public static <U, E> Parser<Text, U, E, Unit> spaces() { return skipSome(space()); }
	public static <U, E> Parser<Text, U, E, String> newline() { return conclude(choice(string("\r\n"), string("\n")), expected("newline")); }
	public static <U, E> Parser<Text, U, E, Character> upper() { return conclude(satisfy(Character::isUpperCase), expected("upper")); }
	public static <U, E> Parser<Text, U, E, Character> lower() { return conclude(satisfy(Character::isLowerCase), expected("lower")); }
	public static <U, E> Parser<Text, U, E, Character> letter() { return conclude(satisfy(Character::isLetter), expected("letter")); }
	public static <U, E> Parser<Text, U, E, Character> digit() { return conclude(satisfy(Character::isDigit), expected("digit")); }
}
