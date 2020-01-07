package org.jparsec.utils.control;

import org.jparsec.utils.control.Trampoline;
import static org.jparsec.utils.control.Trampoline.*;
import org.jparsec.utils.data.Bottom;
import static org.jparsec.utils.data.Bottom.*;

import static org.jparsec.utils.Notation.*;
import static org.jparsec.utils.control.Trampoline.Notation.*;

import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

import java.time.Duration;

public class TrampolineTest {
	@Test public void testBasic() {
		assertEquals(1, new Object() {
			Trampoline<Integer> one() {
				return done(1);
			}
		}.one().run());
		assertEquals(2, new Object() {
			Trampoline<Integer> two() {
				return more(() -> done(2));
			}
		}.two().run());
	}
	@Test public void testRecurrsion() {
		assertEquals(832040, new Object() {
			Trampoline<Integer> fib(int i) {
				return i <= 1 ? done(i) : $do(
				$(	more(() -> fib(i - 1))	, x ->
				$(	more(() -> fib(i - 2))	, y ->
				$(	done(x + y)				)))
				);
			}
		}.fib(30).run());
		assertEquals(2147450880, new Object() {
			Trampoline<Integer> sum(int i) {
				return i <= 0 ? done(i) : $do(
				$(	more(() -> sum(i - 1))	, x ->
				$(	done(i + x)				))
				);
			}
		}.sum(65535).run());
		assertTimeout(Duration.ofSeconds(1), () -> new Object() {
			Trampoline<Integer> fibtail(int i, int a, int b) {
				return i <= 0 ? done(a)
					: i <= 1 ? done(b)
					: more(() -> fibtail(i - 1, b, a + b));
			}
		}.fibtail(65535, 0, 1).run());
	}
	@Test public void testInterrupt() {
		Thread.currentThread().interrupt();
		assertThrows(InterruptedException.class, () -> new Object() {
			Trampoline<Bottom> loop() {
				return more(() -> loop());
			}
		}.loop().interruptibleRun());
	}
}
