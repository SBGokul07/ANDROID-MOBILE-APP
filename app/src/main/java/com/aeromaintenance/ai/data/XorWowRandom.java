package com.aeromaintenance.ai.data;

/**
 * Small, fast, seedable pseudo-random generator (Marsaglia's xorwow, 2003).
 *
 * The simulator needs the same aircraft and data set to give exactly the same
 * numbers on every phone, so the faculty demo is repeatable. This generator is
 * bit-for-bit compatible with Kotlin's {@code kotlin.random.Random(seed)}, which
 * the first version of the prototype used, so the published sample data and the
 * documented demo numbers did not change when the app moved to Java.
 */
public final class XorWowRandom {

    private int x;
    private int y;
    private int z;
    private int w;
    private int v;
    private int addend;

    public XorWowRandom(int seed) {
        this(seed, seed >> 31);
    }

    private XorWowRandom(int seed1, int seed2) {
        x = seed1;
        y = seed2;
        z = 0;
        w = 0;
        v = ~seed1;
        addend = (seed1 << 10) ^ (seed2 >>> 4);
        if ((x | y | z | w | v) == 0) {
            throw new IllegalArgumentException("Initial state must have at least one non-zero element.");
        }
        // Some trivial seeds produce several values with zeroes in the upper bits, so discard the first 64.
        for (int i = 0; i < 64; i++) nextInt();
    }

    public int nextInt() {
        int t = x;
        t = t ^ (t >>> 2);
        x = y;
        y = z;
        z = w;
        int v0 = v;
        w = v0;
        t = (t ^ (t << 1)) ^ v0 ^ (v0 << 4);
        v = t;
        addend += 362437;
        return t + addend;
    }

    /** The upper {@code bitCount} bits of the next value. */
    public int nextBits(int bitCount) {
        return (nextInt() >>> (32 - bitCount)) & (-bitCount >> 31);
    }

    /** Uniform double in [0, 1) with 53 random bits. */
    public double nextDouble() {
        long hi = nextBits(26);
        long lo = nextBits(27);
        return ((hi << 27) + lo) / (double) (1L << 53);
    }

    /** Uniform double in [from, until). */
    public double nextDouble(double from, double until) {
        if (!(until > from)) throw new IllegalArgumentException("Random range is empty: [" + from + ", " + until + ").");
        double size = until - from;
        double r;
        if (Double.isInfinite(size) && !Double.isInfinite(from) && !Double.isInfinite(until)) {
            double r1 = nextDouble() * (until / 2 - from / 2);
            r = from + r1 + r1;
        } else {
            r = from + nextDouble() * size;
        }
        return r >= until ? Math.nextDown(until) : r;
    }

    /** Standard normal sample (Box–Muller). */
    public double nextGaussian() {
        double u1 = Math.max(nextDouble(), 1e-12);
        double u2 = nextDouble();
        return Math.sqrt(-2.0 * Math.log(u1)) * Math.cos(2.0 * Math.PI * u2);
    }
}
