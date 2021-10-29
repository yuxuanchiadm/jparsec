package org.jparsec.core;

import java.util.function.Predicate;

import org.monadium.core.data.Bottom;
import static org.monadium.core.data.Bottom.*;
import org.monadium.core.data.Maybe;
import static org.monadium.core.data.Maybe.*;
import org.monadium.core.data.Tuple;
import static org.monadium.core.data.Tuple.*;

public sealed interface Text {
	record Empty() implements Text {
		static final Text SINGLETON = new Empty();

		@Override public boolean isEmpty() { return true; }
		@Override public boolean isNonempty() { return false; }
		@Override public char fromHead(char other) { return other; }
		@Override public Text fromTail(Text other) { return other; }
		@Override public char coerceHead() throws Undefined { return undefined(); }
		@Override public Text coerceTail() throws Undefined { return undefined(); }

		@Override public Text concat(Text t) { return t; }
		@Override public Maybe<Character> safeHead() { return nothing(); }
		@Override public Maybe<Text> safeTail() { return nothing(); }
		@Override public Maybe<Tuple<Character, Text>> uncons() { return nothing(); }
		@Override public int length() { return 0; }

		@Override public Text take(int i) { return emptyText(); }
		@Override public Text drop(int i) { return emptyText(); }
		@Override public Tuple<Text, Text> splitAt(int i) { return tuple(emptyText(), emptyText()); }
		@Override public Text takeWhile(Predicate<Character> p) { return emptyText(); }
		@Override public Text dropWhile(Predicate<Character> p) { return emptyText(); }
		@Override public Tuple<Text, Text> span(Predicate<Character> p) { return tuple(emptyText(), emptyText()); }

		@Override public boolean isPrefixOf(Text t) { return true; }
		@Override public boolean isSuffixOf(Text t) { return true; }

		@Override public Maybe<Character> index(int i) { return nothing(); }
		@Override public int count(Predicate<Character> p) { return 0; }

		@Override public String toString() { return ""; }
		@Override public boolean equals(Object x) { return x instanceof Empty; }
		@Override public int hashCode() { return "".hashCode(); }
	}
	record Nonempty(String s, int offset) implements Text {
		public char head() { return s.charAt(offset); }
		public Text tail() { return text(s, offset + 1); }

		@Override public boolean isEmpty() { return false; }
		@Override public boolean isNonempty() { return true; }
		@Override public char fromHead(char other) { return head(); }
		@Override public Text fromTail(Text other) { return tail(); }
		@Override public char coerceHead() throws Undefined { return head(); }
		@Override public Text coerceTail() throws Undefined { return tail(); }

		@Override public Text concat(Text t) { return text(toString() + t.toString()); }
		@Override public Maybe<Character> safeHead() { return just(head()); }
		@Override public Maybe<Text> safeTail() { return just(tail()); }
		@Override public Maybe<Tuple<Character, Text>> uncons() { return just(tuple(head(), tail())); }
		@Override public int length() { return s.length() - offset; }

		@Override public Text take(int i) { return text(s.substring(offset, Math.min(offset + Math.max(i, 0), s.length()))); }
		@Override public Text drop(int i) { return text(s, offset + Math.max(i, 0)); }
		@Override public Tuple<Text, Text> splitAt(int i) { return tuple(take(i), drop(i)); }
		@Override public Text takeWhile(Predicate<Character> p) { return text(s.substring(offset, offset + count(p))); }
		@Override public Text dropWhile(Predicate<Character> p) { return text(s, offset + count(p)); }
		@Override public Tuple<Text, Text> span(Predicate<Character> p) { return tuple(takeWhile(p), dropWhile(p)); }

		@Override public boolean isPrefixOf(Text t) { return t.toString().regionMatches(0, s, offset, length()); }
		@Override public boolean isSuffixOf(Text t) { return t.toString().regionMatches(t.length() - length(), s, offset, length()); }

		@Override public Maybe<Character> index(int i) { return i < 0 || i >= length() ? nothing() : just(s.charAt(offset + i)); }
		@Override public int count(Predicate<Character> p) { int c; for (c = 0; offset + c < s.length() && p.test(s.charAt(offset + c)); c++); return c; }

		@Override public String toString() { return s.substring(offset); }
		@Override public boolean equals(Object x) { return x instanceof Nonempty x0 && x0.length() == length() && x0.s.regionMatches(x0.offset, s, offset, length()); }
		@Override public int hashCode() { return toString().hashCode(); }
	}

	static Text emptyText() { return Empty.SINGLETON; }
	static Text nonemptyText(char head, Text tail) { return new Nonempty(head + tail.toString(), 0); }
	static Text text(String s) { return text(s, 0); }
	static Text text(String s, int offset) { return Math.max(offset, 0) >= s.length() ? Empty.SINGLETON : new Nonempty(s, Math.max(offset, 0)); }

	boolean isEmpty();
	boolean isNonempty();
	char fromHead(char other);
	Text fromTail(Text other);
	char coerceHead() throws Undefined;
	Text coerceTail() throws Undefined;

	// Basic
	Text concat(Text t);
	Maybe<Character> safeHead();
	Maybe<Text> safeTail();
	Maybe<Tuple<Character, Text>> uncons();
	int length();

	// Substring
	Text take(int i);
	Text drop(int i);
	Tuple<Text, Text> splitAt(int i);
	Text takeWhile(Predicate<Character> p);
	Text dropWhile(Predicate<Character> p);
	Tuple<Text, Text> span(Predicate<Character> p);

	// Predicate
	boolean isPrefixOf(Text t);
	boolean isSuffixOf(Text t);

	// Indexing
	Maybe<Character> index(int i);
	int count(Predicate<Character> p);

	@Override String toString();
	@Override boolean equals(Object x);
	@Override int hashCode();
}
