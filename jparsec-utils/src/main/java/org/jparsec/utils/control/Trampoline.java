package org.jparsec.utils.control;

import java.util.function.Function;
import java.util.function.Supplier;

import org.jparsec.utils.data.Unit;
import static org.jparsec.utils.data.Unit.*;

import org.jparsec.utils.data.Either;
import static org.jparsec.utils.data.Either.*;
import org.jparsec.utils.data.Maybe;
import static org.jparsec.utils.data.Maybe.*;

public abstract class Trampoline<A> {
	static final class Done<A> extends Trampoline<A> {
		final A a;

		Done(A a) { this.a = a; }

		interface Case<A, R> { R caseDone(A a); }
		@Override final <R> R caseof(Done.Case<A, R> caseDone, More.Case<A, R> caseMore, FlatMap.Case<A, R> caseFlatMap) { return caseDone.caseDone(a); }
	}
	static final class More<A> extends Trampoline<A> {
		final Function<Unit, Trampoline<A>> ka;

		More(Function<Unit, Trampoline<A>> ka) { this.ka = ka; }

		interface Case<A, R> { R caseMore(Function<Unit, Trampoline<A>> ka); }
		@Override final <R> R caseof(Done.Case<A, R> caseDone, More.Case<A, R> caseMore, FlatMap.Case<A, R> caseFlatMap) { return caseMore.caseMore(ka); }
	}
	static final class FlatMap<X, A> extends Trampoline<A> {
		final Trampoline<X> tx;
		final Function<X, Trampoline<A>> ka;

		FlatMap(Trampoline<X> tx, Function<X, Trampoline<A>> ka) { this.tx = tx; this.ka = ka; }

		interface Case<A, R> { <X> R caseFlatMap(Trampoline<X> tx, Function<X, Trampoline<A>> ka); }
		@Override final <R> R caseof(Done.Case<A, R> caseDone, More.Case<A, R> caseMore, FlatMap.Case<A, R> caseFlatMap) { return caseFlatMap.caseFlatMap(tx, ka); }
	}

	Trampoline() {}

	interface Match<A, R> extends Done.Case<A, R>, More.Case<A, R>, FlatMap.Case<A, R> {}
	final <R> R match(Match<A, R> match) { return caseof(match, match, match); }
	abstract <R> R caseof(Done.Case<A, R> caseDone, More.Case<A, R> caseMore, FlatMap.Case<A, R> caseFlatMap);

	public static <A> Trampoline<A> done(A a) { return new Done<>(a); }
	public static <A> Trampoline<A> more(Function<Unit, Trampoline<A>> ka) { return new More<>(ka); }
	public static <A> Trampoline<A> more(Supplier<Trampoline<A>> ka) { return more(u -> ka.get()); }

	public final <B> Trampoline<B> map(Function<A, B> f) {
		return this.match(new Match<A, Trampoline<B>>() {
			@Override public final Trampoline<B>
				caseDone(A a) {
				return new FlatMap<>(Trampoline.this, f.andThen(Done::new));
			}
			@Override public final Trampoline<B>
				caseMore(Function<Unit, Trampoline<A>> ka) {
				return new FlatMap<>(Trampoline.this, f.andThen(Done::new));
			}
			@Override public final <X> Trampoline<B>
				caseFlatMap(Trampoline<X> tx, Function<X, Trampoline<A>> ka) {
				return new FlatMap<>(tx, x -> ka.apply(x).map(f));
			}
		});
	}
	public final <B> Trampoline<B> applyMap(Trampoline<Function<A, B>> fab) {
		return fab.match(new Match<Function<A, B>, Trampoline<B>>() {
			@Override public final Trampoline<B>
				caseDone(Function<A, B> f) {
				return new FlatMap<>(fab, Trampoline.this::map);
			}
			@Override public final Trampoline<B>
				caseMore(Function<Unit, Trampoline<Function<A, B>>> kf) {
				return new FlatMap<>(fab, Trampoline.this::map);
			}
			@Override public final <X> Trampoline<B>
				caseFlatMap(Trampoline<X> tx, Function<X, Trampoline<Function<A, B>>> kf) {
				return new FlatMap<>(tx, x -> kf.apply(x).flatMap(Trampoline.this::map));
			}
		});
	}
	public final <B> Trampoline<B> flatMap(Function<A, Trampoline<B>> f) {
		return this.match(new Match<A, Trampoline<B>>() {
			@Override public final Trampoline<B>
				caseDone(A a) {
				return new FlatMap<>(Trampoline.this, f);
			}
			@Override public final Trampoline<B>
				caseMore(Function<Unit, Trampoline<A>> ka) {
				return new FlatMap<>(Trampoline.this, f);
			}
			@Override public final <X> Trampoline<B>
				caseFlatMap(Trampoline<X> tx, Function<X, Trampoline<A>> ka) {
				return new FlatMap<>(tx, x -> ka.apply(x).flatMap(f));
			}
		});
	}

	public static <A> Trampoline<A> pure(A a) { return done(a); }
	public static <A, B> Trampoline<B> replace(Trampoline<A> fa, B b) { return fa.map(a -> b); }
	public static <A> Trampoline<Unit> discard(Trampoline<A> fa) { return fa.map(a -> unit()); }

	final <T extends Throwable> Either<Function<Unit, Trampoline<A>>, A> resume(Maybe<Thrower<T>> interrupter) throws T {
		Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>> tco = left(this);
		while (tco.isLeft()) tco = interrupter.isJust() && Thread.interrupted()
			? interrupter.coerceJust().apply()
			: tco.coerceLeft().match(new Match<A, Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>>() {
				@Override public final Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
					caseDone(A a) {
					return right(right(a));
				}
				@Override public final Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
					caseMore(Function<Unit, Trampoline<A>> ka) {
					return right(left(ka));
				}
				@Override public final <X> Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
					caseFlatMap(Trampoline<X> tx, Function<X, Trampoline<A>> ka) {
					return tx.match(new Match<X, Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>>() {
						@Override public final Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
							caseDone(X x) {
							return left(ka.apply(x));
						}
						@Override public final Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
							caseMore(Function<Unit, Trampoline<X>> kx) {
							return right(left(u -> kx.apply(unit()).flatMap(ka)));
						}
						@Override public final <Y> Either<Trampoline<A>, Either<Function<Unit, Trampoline<A>>, A>>
							caseFlatMap(Trampoline<Y> ty, Function<Y, Trampoline<X>> kx) {
							return left(ty.flatMap(y -> kx.apply(y).flatMap(ka)));
						}
					});
				}
			});
		return tco.coerceRight();
	}
	final <T extends Throwable> A run(Maybe<Thrower<T>> interrupter) throws T {
		Either<Trampoline<A>, A> tco = left(this);
		while (tco.isLeft()) tco = interrupter.isJust() && Thread.interrupted()
			? interrupter.coerceJust().apply()
			: tco.coerceLeft().resume(interrupter).caseof(
				ka -> left(ka.apply(unit())),
				a -> right(a)
			);
		return tco.coerceRight();
	}
	public final A run() { return run(nothing()); }
	public final A interruptibleRun() throws InterruptedException { return run(just(Thrower.of(InterruptedException::new))); }

	public static final class Notation {
		Notation() {}

		public static <A, B> Trampoline<B> $(Trampoline<A> fa, Function<A, Trampoline<B>> f) { return fa.flatMap(f); }
		public static <A, B> Trampoline<B> $(Trampoline<A> fa, Supplier<Trampoline<B>> fb) { return fa.flatMap(a -> fb.get()); }
	}
}
