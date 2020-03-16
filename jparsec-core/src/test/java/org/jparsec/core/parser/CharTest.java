package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Maybe;
import static org.monadium.core.data.Maybe.*;
import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;

import static org.monadium.core.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class CharTest {
	@Test public void testEOF() {
		assertEquals(nothing(), evalParser(eof(), text("bar")));
		assertEquals(just(unit()), evalParser(eof(), text("")));
	}

	@Test public void testAny() {
		assertEquals(nothing(), evalParser(any(), text("")));
		assertEquals(just('x'), evalParser(any(), text("x")));
	}

	@Test public void testCharacter() {
		assertEquals(nothing(), evalParser(character('x'), text("")));
		assertEquals(nothing(), evalParser(character('x'), text("y")));
		assertEquals(just('x'), evalParser(character('x'), text("x")));
	}

	@Test public void testString() {
		assertEquals(nothing(), evalParser(string("bar"), text("")));
		assertEquals(nothing(), evalParser(string("bar"), text("foo")));
		assertEquals(just("bar"), evalParser(string("bar"), text("bar")));
	}
}
