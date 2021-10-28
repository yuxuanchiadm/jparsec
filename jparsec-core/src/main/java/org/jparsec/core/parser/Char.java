package org.jparsec.core.parser;

import java.util.Arrays;
import java.util.function.Predicate;

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

import org.monadium.core.control.Trampoline;
import static org.monadium.core.control.Trampoline.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

public interface Char {
	static <U, E> Parser<Text, U, E, Unit> eof() {
		return parser(e -> switch (e.stream()) {
			case Empty p1 -> done(success(e, false, unit()));
			case Nonempty p1 -> done(fail(e.log(expected("eof")), false, false));
		});
	}
	static <U, E> Parser<Text, U, E, Character> any() {
		return parser(e -> switch (e.stream()) {
			case Empty p1 -> done(fail(e.log(unexpected("eof")), false, false));
			case Nonempty p1 -> done(success(e.updateStream(p1.tail()).mapLocation(l -> l.advanceCharacter(p1.head())), true, p1.head()));
		});
	}
	static <U, E> Parser<Text, U, E, Character> character(char c) {
		return parser(e -> switch (e.stream()) {
			case Empty p1 -> done(fail(e.log(expected(escapeCharacter(c))), false, false));
			case Nonempty p1 -> p1.head() == c
				? done(success(e.updateStream(p1.tail()).mapLocation(l -> l.advanceCharacter(p1.head())), true, p1.head()))
				: done(fail(e.log(expected(escapeCharacter(c))), false, false));
		});
	}
	static <U, E> Parser<Text, U, E, String> string(String str) {
		return parser(e -> text(str).isPrefixOf(e.stream())
			? done(success(e.mapStream(s -> s.drop(str.length())).mapLocation(l -> l.advanceString(str)), !str.isEmpty(), str))
			: done(fail(e.log(expected(escapeString(str))), false, false))
		);
	}

	static <U, E> Parser<Text, U, E, Character> satisfy(Predicate<Character> p) {
		return parser(e -> switch (e.stream()) {
			case Empty p1 -> done(fail(e.log(unexpected("eof")), false, false));
			case Nonempty p1 -> p.test(p1.head())
				? done(success(e.updateStream(p1.tail()).mapLocation(l -> l.advanceCharacter(p1.head())), true, p1.head()))
				: done(fail(e.log(unexpected(escapeCharacter(p1.head()))), false, false));
		});
	}
	static <U, E> Parser<Text, U, E, Character> dissatisfy(Predicate<Character> p) { return satisfy(p.negate()); }
	static <U, E> Parser<Text, U, E, String> stringSatisfy(Predicate<Character> p) {
		return parser(e -> switch (e.stream().span(p)) {
			case Tuple<Text, Text> p1 -> done(success(e.updateStream(p1.b()).mapLocation(l -> l.advanceString(p1.a().toString())), p1.a().isNonempty(), p1.a().toString()));
		});
	}
	static <U, E> Parser<Text, U, E, String> stringDissatisfy(Predicate<Character> p) { return stringSatisfy(p.negate()); }

	static <U, E> Parser<Text, U, E, Character> oneOf(Character... cs) { return satisfy(Arrays.asList(cs)::contains); }
	static <U, E> Parser<Text, U, E, Character> noneOf(Character... cs) { return dissatisfy(Arrays.asList(cs)::contains); }

	static <U, E> Parser<Text, U, E, Character> space() { return conclude(satisfy(Character::isWhitespace), expected("space")); }
	static <U, E> Parser<Text, U, E, Unit> spaces() { return skipSome(space()); }
	static <U, E> Parser<Text, U, E, String> newline() { return conclude(choice(string("\r\n"), string("\n")), expected("newline")); }
	static <U, E> Parser<Text, U, E, Character> upper() { return conclude(satisfy(Character::isUpperCase), expected("upper")); }
	static <U, E> Parser<Text, U, E, Character> lower() { return conclude(satisfy(Character::isLowerCase), expected("lower")); }
	static <U, E> Parser<Text, U, E, Character> letter() { return conclude(satisfy(Character::isLetter), expected("letter")); }
	static <U, E> Parser<Text, U, E, Character> digit() { return conclude(satisfy(Character::isDigit), expected("digit")); }
}
