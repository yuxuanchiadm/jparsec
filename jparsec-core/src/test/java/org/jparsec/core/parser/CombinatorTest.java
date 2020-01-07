package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Char;
import static org.jparsec.core.parser.Char.*;
import org.jparsec.core.parser.Combinator;
import static org.jparsec.core.parser.Combinator.*;

import static org.jparsec.core.Parser.Notation.*;

import org.jparsec.utils.data.List;
import static org.jparsec.utils.data.List.*;
import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;
import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import static org.jparsec.utils.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class CombinatorTest {
	@Test public void testSequence() {
		assertEquals(nothing(), evalParser(sequence(panic()), text("")));
		assertEquals(just(unit()), evalParser(sequence(), text("")));
		assertEquals(just(unit()), evalParser(sequence(string("foo")), text("foo")));
		assertEquals(just(unit()), evalParser(sequence(string("foo"), string("bar")), text("foobar")));
	}

	@Test public void testChoice() {
		assertEquals(nothing(), evalParser(choice(), text("")));

		assertEquals(nothing(), evalParser(choice(panic(), simple(1)), text("")));
		assertEquals(just(1), evalParser(choice(simple(1), panic()), text("")));

		assertEquals(just(1), evalParser(choice(simple(1)), text("")));
		assertEquals(just(1), evalParser(choice(simple(1), ignore()), text("")));
		assertEquals(just(1), evalParser(choice(ignore(), simple(1)), text("")));
		assertEquals(just(1), evalParser(choice(simple(1), simple(2)), text("")));

		assertEquals(just("foo"), evalParser(choice(string("foo"), string("bar")), text("foo")));
		assertEquals(just("bar"), evalParser(choice(string("foo"), string("bar")), text("bar")));

		assertEquals(nothing(), evalParser(choice(sequence(string("abc"), string("foo")), sequence(string("abc"), string("bar"))), text("abcbar")));
		assertEquals(just(unit()), evalParser(choice(sequence(string("abc"), string("foo")), sequence(string("abc"), string("bar"))), text("abcfoo")));
	}

	@Test public void testReplicate() {
		assertEquals(just(list()), evalParser(replicate(0, string("bar")), text("")));
		assertEquals(just(list("bar")), evalParser(replicate(1, string("bar")), text("bar")));
		assertEquals(just(list("bar", "bar")), evalParser(replicate(2, string("bar")), text("barbar")));
	}

	@Test public void testSome() {
		assertEquals(just(list()), evalParser(some(string("bar")), text("")));
		assertEquals(just(list("bar")), evalParser(some(string("bar")), text("bar")));
		assertEquals(just(list("bar", "bar")), evalParser(some(string("bar")), text("barbar")));
	}

	@Test public void testMany() {
		assertEquals(nothing(), evalParser(many(string("bar")), text("")));
		assertEquals(just(list("bar")), evalParser(many(string("bar")), text("bar")));
		assertEquals(just(list("bar", "bar")), evalParser(many(string("bar")), text("barbar")));
	}
}
