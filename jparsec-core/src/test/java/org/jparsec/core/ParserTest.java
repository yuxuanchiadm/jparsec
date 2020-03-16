package org.jparsec.core;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;

import static org.jparsec.core.Parser.Notation.*;

import org.monadium.core.data.Unit;
import static org.monadium.core.data.Unit.*;
import org.monadium.core.data.Maybe;
import static org.monadium.core.data.Maybe.*;

import static org.monadium.core.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ParserTest {
	@Test public void testBasic() {
		assertEquals(just(1024), evalParser(simple(1024), unit()));
		assertEquals(nothing(), evalParser(panic(), unit()));
	}

	@Test public void testMonadic() {
		assertEquals(just(3), evalParser($do(
		$(	simple(1)		, x ->
		$(	simple(2)		, y ->
		$(	simple(x + y)	)))
		), unit()));
		assertEquals(nothing(), evalParser($do(
		$(	panic()			, () ->
		$(	simple(unit())	))
		), unit()));
	}
}
