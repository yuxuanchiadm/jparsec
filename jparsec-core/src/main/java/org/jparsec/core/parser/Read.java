package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Parser.Message;
import static org.jparsec.core.Parser.Message.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.jparsec.utils.data.Tuple;
import static org.jparsec.utils.data.Tuple.*;

import static org.jparsec.utils.Notation.*;

import java.math.BigDecimal;
import java.math.BigInteger;

public final class Read {
	Read() {}

	public static <U, E> Parser<Text, U, E, Boolean> readBoolean() {
		return conclude(choice(
			replace(string("false"), false),
			replace(string("true"), true)
		), expected("boolean"));
	}
	public static <U, E> Parser<Text, U, E, Byte> readByte() {
		return $do(
		$(	conclude(integer(), expected("byte"))								, number ->
		$(	ensure(
				number.compareTo(BigInteger.valueOf(Byte.MIN_VALUE)) >= 0 &&
				number.compareTo(BigInteger.valueOf(Byte.MAX_VALUE)) <= 0,
				error("Literal out of range")
			)																	, () ->
		$(	simple(number.byteValueExact())										)))
		);
	}
	public static <U, E> Parser<Text, U, E, Short> readShort() {
		return $do(
		$(	conclude(integer(), expected("short"))								, number ->
		$(	ensure(
				number.compareTo(BigInteger.valueOf(Short.MIN_VALUE)) >= 0 &&
				number.compareTo(BigInteger.valueOf(Short.MAX_VALUE)) <= 0,
				error("Literal out of range")
			)																	, () ->
		$(	simple(number.shortValueExact())									)))
		);
	}
	public static <U, E> Parser<Text, U, E, Integer> readInteger() {
		return $do(
		$(	conclude(integer(), expected("integer"))							, number ->
		$(	ensure(
				number.compareTo(BigInteger.valueOf(Integer.MIN_VALUE)) >= 0 &&
				number.compareTo(BigInteger.valueOf(Integer.MAX_VALUE)) <= 0,
				error("Literal out of range")
			)																	, () ->
		$(	simple(number.intValueExact())										)))
		);
	}
	public static <U, E> Parser<Text, U, E, Long> readLong() {
		return $do(
		$(	conclude(integer(), expected("long"))								, number ->
		$(	ensure(
				number.compareTo(BigInteger.valueOf(Long.MIN_VALUE)) >= 0 &&
				number.compareTo(BigInteger.valueOf(Long.MAX_VALUE)) <= 0,
				error("Literal out of range")
			)																	, () ->
		$(	simple(number.longValueExact())										)))
		);
	}
	public static <U, E> Parser<Text, U, E, Float> readFloat() {
		return $do(
		$(	conclude(fraction(), expected("float"))	, number ->
		$(	simple((Float) number.floatValue())		))
		);
	}
	public static <U, E> Parser<Text, U, E, Double> readDouble() {
		return $do(
		$(	conclude(fraction(), expected("double"))	, number ->
		$(	simple((Double) number.doubleValue())		))
		);
	}
	public static <U, E> Parser<Text, U, E, Character> readCharacter() {
		return conclude(between(character('\''), character('\''),
			choice(satisfy(c -> c != '\'' && c != '\\'), escape())
		), expected("character"));
	}
	public static <U, E> Parser<Text, U, E, String> readString() {
		return conclude(between(character('\"'), character('\"'), $(
		$(	some(choice(satisfy(c -> c != '\"' && c != '\\'), escape()))					, characters ->
		$(	simple(characters.foldl(StringBuilder::append, new StringBuilder()).toString())	))
		)), expected("string"));
	}

	static <U, E> Parser<Text, U, E, Boolean> sign() {
		return choice(
			replace(character('-'), true),
			replace(character('+'), false),
			simple(false)
		);
	}
	static <U, E> Parser<Text, U, E, Tuple<Integer, BigInteger>> digits(BigInteger base, Parser<Text, U, E, BigInteger> digitP) {
		return $do(
		$(	many(digitP)																, digits ->
		$(	simple(digits.foldl(
				(t, d) -> tuple(t.first() + 1, t.second().multiply(base).add(d)),
				tuple(0, BigInteger.ZERO)
			))																			))
		);
	}
	static <U, E> Parser<Text, U, E, BigInteger> integer() {
		return $do(
			$(	sign()														, sign ->
			$(	digits(BigInteger.TEN, $do(
				$(	satisfy(c -> c >= '0' && c <= '9')	, c ->
				$(	simple(BigInteger.valueOf(c - '0'))	))
			))																, integer ->
			$(	simple(sign ? integer.second().negate() : integer.second())	)))
		);
	}
	static <U, E> Parser<Text, U, E, BigDecimal> fraction() {
		return $do(
			$(	sign()														, sign ->
			$(	digits(BigInteger.TEN, $do(
				$(	satisfy(c -> c >= '0' && c <= '9')	, c ->
				$(	simple(BigInteger.valueOf(c - '0'))	))
			))																, integer ->
			$(	choice($do(
				$(	character('.')									, () ->
				$(	digits(BigInteger.TEN, $do(
					$(	satisfy(c -> c >= '0' && c <= '9')	, c ->
					$(	simple(BigInteger.valueOf(c - '0'))	))
				))													))
			), simple(tuple(0, BigInteger.ZERO)))							, fraction ->
			$(	simple(integer.second()
					.multiply(BigInteger.TEN.pow(fraction.first()))
					.add(fraction.second()))								, significand ->
			$(	simple(sign ? significand.negate() : significand)			, unscaled ->
			$(	simple(new BigDecimal(unscaled, fraction.first())			)))))))
		);
	}
	static <U, E> Parser<Text, U, E, Character> escape() {
		return $do(
		$(	character('\\')						, () ->
		$(	choice(
				replace(character('b'), '\b'),
				replace(character('t'), '\t'),
				replace(character('n'), '\n'),
				replace(character('f'), '\f'),
				replace(character('r'), '\r'),
				replace(character('\"'), '\"'),
				replace(character('\''), '\''),
				replace(character('\\'), '\\')
			)									))
		);
	}
}
