package org.jparsec.core;

import org.jparsec.utils.data.Bottom;
import static org.jparsec.utils.data.Bottom.*;
import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;
import org.jparsec.utils.data.Tuple;
import static org.jparsec.utils.data.Tuple.*;

import java.util.function.Predicate;

public abstract class Text {
	public static final class Empty extends Text {
		Empty() {}

		public interface Case<R> { R caseEmpty(); }
		@Override public <R> R caseof(Empty.Case<R> caseEmpty, Nonempty.Case<R> caseNonempty) { return caseEmpty.caseEmpty(); };

		static final Text SINGLETON = new Empty();

		@Override public boolean isEmpty() { return true; }
		@Override public boolean isNonempty() { return false; }
		@Override public char fromHead(char other) { return other; }
		@Override public Text fromTail(Text other) { return other; }
		@Override public char coerceHead() throws Undefined { return undefined(); }
		@Override public Text coerceTail() throws Undefined { return undefined(); }

		@Override public Text concat(Text t) { return t; }
		@Override public Maybe<Character> head() { return nothing(); }
		@Override public Maybe<Text> tail() { return nothing(); }
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
	public static final class Nonempty extends Text {
		final String s;
		final int offset;

		Nonempty(String s, int offset) { this.s = s; this.offset = offset; }

		public interface Case<R> { R caseNonempty(char head, Text tail); }
		@Override public <R> R caseof(Empty.Case<R> caseEmpty, Nonempty.Case<R> caseNonempty) { return caseNonempty.caseNonempty(s.charAt(offset), text(s, offset + 1)); };

		@Override public boolean isEmpty() { return false; }
		@Override public boolean isNonempty() { return true; }
		@Override public char fromHead(char other) { return s.charAt(offset); }
		@Override public Text fromTail(Text other) { return text(s, offset + 1); }
		@Override public char coerceHead() throws Undefined { return s.charAt(offset); }
		@Override public Text coerceTail() throws Undefined { return text(s, offset + 1); }

		@Override public Text concat(Text t) { return text(toString() + t.toString()); }
		@Override public Maybe<Character> head() { return just(s.charAt(offset)); }
		@Override public Maybe<Text> tail() { return just(text(s, offset + 1)); }
		@Override public Maybe<Tuple<Character, Text>> uncons() { return just(tuple(s.charAt(offset), text(s, offset + 1))); }
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
		@Override public boolean equals(Object x) { return x instanceof Nonempty && ((Nonempty) x).length() == length() && ((Nonempty) x).s.regionMatches(((Nonempty) x).offset, s, offset, length()); }
		@Override public int hashCode() { return toString().hashCode(); }
	}

	Text() {}

	public interface Match<R> extends Empty.Case<R>, Nonempty.Case<R> {}
	public final <R> R match(Match<R> match) { return caseof(match, match); }
	public abstract <R> R caseof(Empty.Case<R> caseEmpty, Nonempty.Case<R> caseNonempty);

	public static Text emptyText() { return Empty.SINGLETON; }
	public static Text nonemptyText(char head, Text tail) { return new Nonempty(head + tail.toString(), 0); }
	public static Text text(String s) { return text(s, 0); }
	public static Text text(String s, int offset) { return Math.max(offset, 0) >= s.length() ? Empty.SINGLETON : new Nonempty(s, Math.max(offset, 0)); }

	public abstract boolean isEmpty();
	public abstract boolean isNonempty();
	public abstract char fromHead(char other);
	public abstract Text fromTail(Text other);
	public abstract char coerceHead() throws Undefined;
	public abstract Text coerceTail() throws Undefined;

	// Basic
	public abstract Text concat(Text t);
	public abstract Maybe<Character> head();
	public abstract Maybe<Text> tail();
	public abstract Maybe<Tuple<Character, Text>> uncons();
	public abstract int length();

	// Substring
	public abstract Text take(int i);
	public abstract Text drop(int i);
	public abstract Tuple<Text, Text> splitAt(int i);
	public abstract Text takeWhile(Predicate<Character> p);
	public abstract Text dropWhile(Predicate<Character> p);
	public abstract Tuple<Text, Text> span(Predicate<Character> p);

	// Predicate
	public abstract boolean isPrefixOf(Text t);
	public abstract boolean isSuffixOf(Text t);

	// Indexing
	public abstract Maybe<Character> index(int i);
	public abstract int count(Predicate<Character> p);

	@Override public abstract String toString();
	@Override public abstract boolean equals(Object x);
	@Override public abstract int hashCode();
}
