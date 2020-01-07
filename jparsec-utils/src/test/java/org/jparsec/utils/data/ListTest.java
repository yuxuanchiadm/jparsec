package org.jparsec.utils.data;

import org.jparsec.utils.data.List;
import static org.jparsec.utils.data.List.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

public class ListTest {
	@Test public void testBasic() {
		assertEquals(nil(), list());
		assertEquals(cons(0, nil()), list(0));
		assertEquals(cons(0, cons(1, nil())), list(0, 1));
		assertEquals(cons(0, cons(1, cons(2, nil()))), list(0, 1, 2));

		assertThrows(IndexOutOfBoundsException.class, () -> list(new Integer[] { 0, 1, 2 }, -1, -1));
		assertThrows(IndexOutOfBoundsException.class, () -> list(new Integer[] { 0, 1, 2 }, 4, 4));
		assertThrows(IllegalArgumentException.class, () -> list(new Integer[] { 0, 1, 2 }, 3, 0));
		assertEquals(nil(), list(new Integer[] { 0, 1, 2 }, 0, 0));
		assertEquals(cons(0, nil()), list(new Integer[] { 0, 1, 2 }, 0, 1));
		assertEquals(cons(0, cons(1, nil())), list(new Integer[] { 0, 1, 2 }, 0, 2));
		assertEquals(cons(0, cons(1, cons(2, nil()))), list(new Integer[] { 0, 1, 2 }, 0, 3));
		assertEquals(nil(), list(new Integer[] { 0, 1, 2 }, 1, 1));
		assertEquals(cons(1, nil()), list(new Integer[] { 0, 1, 2 }, 1, 2));
		assertEquals(cons(1, cons(2, nil())), list(new Integer[] { 0, 1, 2 }, 1, 3));
		assertEquals(nil(), list(new Integer[] { 0, 1, 2 }, 2, 2));
		assertEquals(cons(2, nil()), list(new Integer[] { 0, 1, 2 }, 2, 3));
		assertEquals(nil(), list(new Integer[] { 0, 1, 2 }, 3, 3));
	}
}
