package org.jparsec.core.parser;

import java.math.BigDecimal;
import java.math.BigInteger;

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

import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

import static org.monadium.core.Notation.*;

public interface Read {
	static <U, E> Parser<Text, U, E, Boolean> readBoolean() {
		return conclude(choice(
			replace(string("false"), false),
			replace(string("true"), true)
		), expected("boolean"));
	}
	static <U, E> Parser<Text, U, E, Byte> readByte() {
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
	static <U, E> Parser<Text, U, E, Short> readShort() {
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
	static <U, E> Parser<Text, U, E, Integer> readInteger() {
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
	static <U, E> Parser<Text, U, E, Long> readLong() {
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
	static <U, E> Parser<Text, U, E, Float> readFloat() {
		return conclude(choice(
			replace(string("NaN"), Float.NaN),
			replace(string("Infinity"), Float.POSITIVE_INFINITY),
			replace(string("-Infinity"), Float.NEGATIVE_INFINITY),
			$do(
			$(	sign()															, sign ->
			$(	fraction()														, fraction ->
			$(	simple(sign ? -fraction.floatValue() : fraction.floatValue())	)))
			)
		), expected("float"));
	}
	static <U, E> Parser<Text, U, E, Double> readDouble() {
		return conclude(choice(
			replace(string("NaN"), Double.NaN),
			replace(string("Infinity"), Double.POSITIVE_INFINITY),
			replace(string("-Infinity"), Double.NEGATIVE_INFINITY),
			$do(
			$(	sign()															, sign ->
			$(	fraction()														, fraction ->
			$(	simple(sign ? -fraction.doubleValue() : fraction.doubleValue())	)))
			)
		), expected("double"));
	}
	static <U, E> Parser<Text, U, E, Character> readCharacter() {
		return conclude(between(character('\''), character('\''),
			choice(satisfy(c -> c != '\'' && c != '\\'), escape())
		), expected("character"));
	}
	static <U, E> Parser<Text, U, E, String> readString() {
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
			$(	simple(new BigDecimal(significand, fraction.first())		)))))
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
