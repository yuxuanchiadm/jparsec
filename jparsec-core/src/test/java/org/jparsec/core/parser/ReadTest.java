package org.jparsec.core.parser;

import org.jparsec.core.Parser;
import static org.jparsec.core.Parser.*;
import org.jparsec.core.Text;
import static org.jparsec.core.Text.*;
import org.jparsec.core.parser.Read;
import static org.jparsec.core.parser.Read.*;

import static org.jparsec.core.Parser.Notation.*;

import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;

import static org.jparsec.utils.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ReadTest {
	@Test public void testReadBoolean() {
		assertEquals(nothing(), evalParser(readBoolean(), text("")));
		assertEquals(nothing(), evalParser(readBoolean(), text("bar")));
		assertEquals(just(false), evalParser(readBoolean(), text("false")));
		assertEquals(just(true), evalParser(readBoolean(), text("true")));
	}

	@Test public void testReadInteger() {
		assertEquals(nothing(), evalParser(readInteger(), text("")));
		assertEquals(nothing(), evalParser(readInteger(), text("bar")));
		assertEquals(nothing(), evalParser(readInteger(), text("9999999999999999999")));
		assertEquals(just(1024), evalParser(readInteger(), text("1024")));
		assertEquals(just(-1024), evalParser(readInteger(), text("-1024")));
	}

	@Test public void testReadDouble() {
		assertEquals(nothing(), evalParser(readDouble(), text("")));
		assertEquals(nothing(), evalParser(readDouble(), text("bar")));
		assertEquals(nothing(), evalParser(readDouble(), text("-NaN")));
		assertEquals(just(Double.NaN), evalParser(readDouble(), text("NaN")));
		assertEquals(just(Double.POSITIVE_INFINITY), evalParser(readDouble(), text("Infinity")));
		assertEquals(just(Double.NEGATIVE_INFINITY), evalParser(readDouble(), text("-Infinity")));
		assertEquals(just(0.0D), evalParser(readDouble(), text("0.0")));
		assertEquals(just(-0.0D), evalParser(readDouble(), text("-0.0")));
		assertEquals(just(1024.0D), evalParser(readDouble(), text("1024")));
		assertEquals(just(-1024.0D), evalParser(readDouble(), text("-1024")));
		assertEquals(just(-12345.6789D), evalParser(readDouble(), text("-12345.6789")));
	}

	@Test public void testReadCharacter() {
		assertEquals(nothing(), evalParser(readCharacter(), text("")));
		assertEquals(nothing(), evalParser(readCharacter(), text("a")));
		assertEquals(nothing(), evalParser(readCharacter(), text("\'a")));
		assertEquals(nothing(), evalParser(readCharacter(), text("a\'")));
		assertEquals(nothing(), evalParser(readCharacter(), text("\'bar\'")));
		assertEquals(just('a'), evalParser(readCharacter(), text("\'a\'")));
	}

	@Test public void testReadString() {
		assertEquals(nothing(), evalParser(readString(), text("")));
		assertEquals(nothing(), evalParser(readString(), text("bar")));
		assertEquals(nothing(), evalParser(readString(), text("bar\"")));
		assertEquals(nothing(), evalParser(readString(), text("\"bar")));
		assertEquals(just(""), evalParser(readString(), text("\"\"")));
		assertEquals(just("bar"), evalParser(readString(), text("\"bar\"")));
	}
}
