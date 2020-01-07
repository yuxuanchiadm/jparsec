package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Term;
import static org.jparsec.core.parser.Term.*;
import org.jparsec.core.parser.Term.Associativity;
import static org.jparsec.core.parser.Term.Associativity.*;
import org.jparsec.core.parser.Term.Definition;
import static org.jparsec.core.parser.Term.Definition.*;
import org.jparsec.core.parser.Term.Level;
import static org.jparsec.core.parser.Term.Level.*;
import org.jparsec.core.parser.Term.Operand;
import static org.jparsec.core.parser.Term.Operand.*;

import static org.jparsec.core.Parser.Notation.*;

import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;

import static org.jparsec.utils.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class TermTest {
	@Test public void testBasic() {
		assertEquals(nothing(), evalParser(termP(definition(
			ignore(),
			ignore(),
			ignore(),
			n -> ignore(),
			ignore())
		), text("")));
		assertEquals(nothing(), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("")));
		assertEquals(nothing(), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("+")));
		assertEquals(nothing(), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("a +")));
		assertEquals(nothing(), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("+ a")));
		assertEquals(just("a"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("a")));
		assertEquals(just("(a + b)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("a + b")));
	}
	@Test public void testTernary() {
		assertEquals(just("(a ? b : c)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("?", t1 -> operand(":", t2 -> operand(t3 ->
					"(" + t1 + " ? " + t2 + " : " + t3 + ")"
				)))
			))
		), text("a ? b : c")));
	}
	@Test public void testBracket() {
		assertEquals(just("(a + ((b + c) + d))"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("(a + ((b + c) + d))")));
	}
	@Test public void testLevel() {
		assertEquals(just("(a + (b * c))"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			),
			level(NONE,
				operand("*", t1 -> operand(t2 ->
					"(" + t1 + " * " + t2 + ")"
				))
			))
		), text("a + b * c")));
		assertEquals(just("((a * b) + c)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			),
			level(NONE,
				operand("*", t1 -> operand(t2 ->
					"(" + t1 + " * " + t2 + ")"
				))
			))
		), text("a * b + c")));
	}
	@Test public void testAssociativity() {
		assertEquals(just("((a + b) + c)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(LEFT,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("a + b + c")));
		assertEquals(just("(a + (b + c))"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(RIGHT,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				))
			))
		), text("a + b + c")));
		assertEquals(just("((a ? b : c) ? d : e)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(LEFT,
				operand("?", t1 -> operand(":", t2 -> operand(t3 ->
					"(" + t1 + " ? " + t2 + " : " + t3 + ")"
				)))
			))
		), text("a ? b : c ? d : e")));
		assertEquals(just("(a ? b : (c ? d : e))"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(RIGHT,
				operand("?", t1 -> operand(":", t2 -> operand(t3 ->
					"(" + t1 + " ? " + t2 + " : " + t3 + ")"
				)))
			))
		), text("a ? b : c ? d : e")));
	}
	@Test public void testOperands() {
		assertEquals(just("(a ~ b @ c)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("~", t1 -> operand("@", t2 -> operand(t3 ->
					"(" + t1 + " ~ " + t2 + " @ " + t3 + ")"
				))),
				operand("~", t1 -> operand("#", t2 -> operand(t3 ->
					"(" + t1 + " ~ " + t2 + " # " + t3 + ")"
				)))
			))
		), text("a ~ b @ c")));
		assertEquals(just("(a ~ b # c)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(NONE,
				operand("~", t1 -> operand("@", t2 -> operand(t3 ->
					"(" + t1 + " ~ " + t2 + " @ " + t3 + ")"
				))),
				operand("~", t1 -> operand("#", t2 -> operand(t3 ->
					"(" + t1 + " ~ " + t2 + " # " + t3 + ")"
				)))
			))
		), text("a ~ b # c")));
		assertEquals(just("(((a + b) - c) + d)"), evalParser(termP(definition(
			spaces(),
			character('('),
			character(')'),
			n -> string(n),
			advancing(stringSatisfy(c -> c >= 'A' && c <= 'Z' || c >= 'a' && c <= 'z')),
			level(LEFT,
				operand("+", t1 -> operand(t2 ->
					"(" + t1 + " + " + t2 + ")"
				)),
				operand("-", t1 -> operand(t2 ->
					"(" + t1 + " - " + t2 + ")"
				))
			))
		), text("a + b - c + d")));
	}
}
