package de.lumen.home;

import android.graphics.Bitmap;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tiny self-contained QR generator: byte mode, ECC level M, versions 1-10.
 * Enough for a short URL and needs no external library. Standard Reed-Solomon /
 * matrix construction (Nayuki's public-domain algorithm). The QR is drawn fresh
 * every time, so a changing IP always ends up with the right code.
 */
final class Qr {
    private Qr() {}

    // ECC level M, indexed by version (index 0 unused).
    private static final int[] ECC_PER_BLOCK = {-1, 10, 16, 26, 18, 24, 16, 18, 22, 22, 26};
    private static final int[] NUM_BLOCKS    = {-1,  1,  1,  1,  2,  2,  4,  4,  4,  5,  5};

    /** QR as a crisp bitmap: scale px per module, border modules of quiet zone. Returns null on failure. */
    static Bitmap bitmap(String text, int scale, int border, int darkColor, int lightColor) {
        try {
            boolean[][] m = encode(text);
            int n = m.length;
            int px = (n + border * 2) * scale;
            Bitmap bmp = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
            int[] row = new int[px];
            for (int y = 0; y < px; y++) {
                int my = y / scale - border;
                for (int x = 0; x < px; x++) {
                    int mx = x / scale - border;
                    boolean dark = my >= 0 && my < n && mx >= 0 && mx < n && m[my][mx];
                    row[x] = dark ? darkColor : lightColor;
                }
                bmp.setPixels(row, 0, px, 0, y, px, 1);
            }
            return bmp;
        } catch (Exception e) {
            return null;
        }
    }

    // ---- matrix state (single-threaded, built on the UI thread) ----
    private static boolean[][] modules;
    private static boolean[][] isFn;
    private static int size, version;

    private static boolean[][] encode(String text) {
        byte[] data = text.getBytes(StandardCharsets.UTF_8);
        version = chooseVersion(data.length);
        size = version * 4 + 17;
        modules = new boolean[size][size];
        isFn = new boolean[size][size];

        byte[] codewords = addEccAndInterleave(makeDataCodewords(data));

        drawFunctionPatterns();
        drawCodewords(codewords);

        int bestMask = 0, bestPenalty = Integer.MAX_VALUE;
        for (int mask = 0; mask < 8; mask++) {
            applyMask(mask);
            drawFormatBits(mask);
            int p = penalty();
            if (p < bestPenalty) { bestPenalty = p; bestMask = mask; }
            applyMask(mask); // undo
        }
        applyMask(bestMask);
        drawFormatBits(bestMask);
        return modules;
    }

    private static int chooseVersion(int len) {
        for (int v = 1; v <= 10; v++) {
            int ccBits = v <= 9 ? 8 : 16;
            int needed = (4 + ccBits + 8 * len + 7) / 8; // data bytes required
            if (needed <= numDataCodewords(v)) return v;
        }
        return 10; // callers keep URLs short, so this never truncates in practice
    }

    private static int rawDataModules(int v) {
        int r = (16 * v + 128) * v + 64;
        if (v >= 2) {
            int na = v / 7 + 2;
            r -= (25 * na - 10) * na - 55;
            if (v >= 7) r -= 36;
        }
        return r;
    }

    private static int numDataCodewords(int v) {
        return rawDataModules(v) / 8 - ECC_PER_BLOCK[v] * NUM_BLOCKS[v];
    }

    private static byte[] makeDataCodewords(byte[] data) {
        int capBits = numDataCodewords(version) * 8;
        List<Boolean> bb = new ArrayList<>();
        appendBits(bb, 0x4, 4);                       // byte mode
        appendBits(bb, data.length, version <= 9 ? 8 : 16);
        for (byte b : data) appendBits(bb, b & 0xFF, 8);
        appendBits(bb, 0, Math.max(0, Math.min(4, capBits - bb.size())));
        while (bb.size() % 8 != 0) bb.add(false);
        for (int pad = 0xEC; bb.size() < capBits; pad ^= 0xEC ^ 0x11) appendBits(bb, pad, 8);
        byte[] out = new byte[bb.size() / 8];
        for (int i = 0; i < bb.size(); i++)
            if (bb.get(i)) out[i >>> 3] |= 1 << (7 - (i & 7));
        return out;
    }

    private static void appendBits(List<Boolean> bb, int val, int len) {
        for (int i = len - 1; i >= 0; i--) bb.add(((val >>> i) & 1) != 0);
    }

    private static byte[] addEccAndInterleave(byte[] data) {
        int numBlocks = NUM_BLOCKS[version];
        int eccLen = ECC_PER_BLOCK[version];
        int rawCw = rawDataModules(version) / 8;
        int numShort = numBlocks - rawCw % numBlocks;
        int shortLen = rawCw / numBlocks;
        byte[][] blocks = new byte[numBlocks][];
        byte[] div = rsDivisor(eccLen);
        for (int i = 0, k = 0; i < numBlocks; i++) {
            int datLen = shortLen - eccLen + (i < numShort ? 0 : 1);
            byte[] dat = Arrays.copyOfRange(data, k, k + datLen);
            k += datLen;
            byte[] block = Arrays.copyOf(dat, shortLen + 1);
            byte[] ecc = rsRemainder(dat, div);
            System.arraycopy(ecc, 0, block, block.length - eccLen, eccLen);
            blocks[i] = block;
        }
        byte[] result = new byte[rawCw];
        for (int i = 0, k = 0; i < blocks[0].length; i++)
            for (int j = 0; j < blocks.length; j++)
                if (i != shortLen - eccLen || j >= numShort) result[k++] = blocks[j][i];
        return result;
    }

    // ---- Reed-Solomon over GF(256), primitive 0x11D ----
    private static byte[] rsDivisor(int degree) {
        byte[] result = new byte[degree];
        result[degree - 1] = 1;
        int root = 1;
        for (int i = 0; i < degree; i++) {
            for (int j = 0; j < result.length; j++) {
                result[j] = (byte) rsMul(result[j] & 0xFF, root);
                if (j + 1 < result.length) result[j] ^= result[j + 1];
            }
            root = rsMul(root, 0x02);
        }
        return result;
    }

    private static byte[] rsRemainder(byte[] data, byte[] divisor) {
        byte[] result = new byte[divisor.length];
        for (byte b : data) {
            int factor = (b ^ result[0]) & 0xFF;
            System.arraycopy(result, 1, result, 0, result.length - 1);
            result[result.length - 1] = 0;
            for (int i = 0; i < result.length; i++)
                result[i] ^= (byte) rsMul(divisor[i] & 0xFF, factor);
        }
        return result;
    }

    private static int rsMul(int x, int y) {
        int z = 0;
        for (int i = 7; i >= 0; i--) {
            z = (z << 1) ^ ((z >>> 7) * 0x11D);
            z ^= ((y >>> i) & 1) * x;
        }
        return z & 0xFF;
    }

    // ---- matrix drawing ----
    private static void setFn(int x, int y, boolean dark) {
        if (x < 0 || x >= size || y < 0 || y >= size) return;
        modules[y][x] = dark;
        isFn[y][x] = true;
    }

    private static void drawFunctionPatterns() {
        for (int i = 0; i < size; i++) {
            setFn(6, i, i % 2 == 0);
            setFn(i, 6, i % 2 == 0);
        }
        drawFinder(3, 3);
        drawFinder(size - 4, 3);
        drawFinder(3, size - 4);
        int[] align = alignPositions();
        int n = align.length;
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++) {
                if ((i == 0 && j == 0) || (i == 0 && j == n - 1) || (i == n - 1 && j == 0)) continue;
                drawAlign(align[i], align[j]);
            }
        drawFormatBits(0);
        drawVersion();
    }

    private static void drawFinder(int cx, int cy) {
        for (int dy = -4; dy <= 4; dy++)
            for (int dx = -4; dx <= 4; dx++) {
                int dist = Math.max(Math.abs(dx), Math.abs(dy));
                setFn(cx + dx, cy + dy, dist != 2 && dist != 4);
            }
    }

    private static void drawAlign(int cx, int cy) {
        for (int dy = -2; dy <= 2; dy++)
            for (int dx = -2; dx <= 2; dx++)
                setFn(cx + dx, cy + dy, Math.max(Math.abs(dx), Math.abs(dy)) != 1);
    }

    private static int[] alignPositions() {
        if (version == 1) return new int[0];
        int na = version / 7 + 2;
        int step = (version * 4 + na * 2 + 1) / (na * 2 - 2) * 2;
        int[] r = new int[na];
        r[0] = 6;
        for (int i = na - 1, pos = size - 7; i >= 1; i--, pos -= step) r[i] = pos;
        return r;
    }

    private static void drawFormatBits(int mask) {
        int data = mask; // ECC level M = 0b00, so (0 << 3) | mask
        int rem = data;
        for (int i = 0; i < 10; i++) rem = (rem << 1) ^ ((rem >>> 9) * 0x537);
        int bits = ((data << 10) | rem) ^ 0x5412;
        for (int i = 0; i <= 5; i++) setFn(8, i, bit(bits, i));
        setFn(8, 7, bit(bits, 6));
        setFn(8, 8, bit(bits, 7));
        setFn(7, 8, bit(bits, 8));
        for (int i = 9; i < 15; i++) setFn(14 - i, 8, bit(bits, i));
        for (int i = 0; i < 8; i++) setFn(size - 1 - i, 8, bit(bits, i));
        for (int i = 8; i < 15; i++) setFn(8, size - 15 + i, bit(bits, i));
        setFn(8, size - 8, true);
    }

    private static void drawVersion() {
        if (version < 7) return;
        int rem = version;
        for (int i = 0; i < 12; i++) rem = (rem << 1) ^ ((rem >>> 11) * 0x1F25);
        int bits = version << 12 | rem;
        for (int i = 0; i < 18; i++) {
            boolean b = bit(bits, i);
            int a = size - 11 + i % 3, d = i / 3;
            setFn(a, d, b);
            setFn(d, a, b);
        }
    }

    private static void drawCodewords(byte[] data) {
        int i = 0;
        for (int right = size - 1; right >= 1; right -= 2) {
            if (right == 6) right = 5;
            for (int vert = 0; vert < size; vert++)
                for (int j = 0; j < 2; j++) {
                    int x = right - j;
                    boolean upward = ((right + 1) & 2) == 0;
                    int y = upward ? size - 1 - vert : vert;
                    if (!isFn[y][x] && i < data.length * 8) {
                        modules[y][x] = bit(data[i >>> 3] & 0xFF, 7 - (i & 7));
                        i++;
                    }
                }
        }
    }

    private static void applyMask(int mask) {
        for (int y = 0; y < size; y++)
            for (int x = 0; x < size; x++) {
                if (isFn[y][x]) continue;
                boolean inv;
                switch (mask) {
                    case 0:  inv = (x + y) % 2 == 0; break;
                    case 1:  inv = y % 2 == 0; break;
                    case 2:  inv = x % 3 == 0; break;
                    case 3:  inv = (x + y) % 3 == 0; break;
                    case 4:  inv = (x / 3 + y / 2) % 2 == 0; break;
                    case 5:  inv = x * y % 2 + x * y % 3 == 0; break;
                    case 6:  inv = (x * y % 2 + x * y % 3) % 2 == 0; break;
                    default: inv = ((x + y) % 2 + x * y % 3) % 2 == 0; break;
                }
                modules[y][x] ^= inv;
            }
    }

    /** Penalty rules 1, 2 and 4 — a safe subset that still picks a good mask (any valid mask scans). */
    private static int penalty() {
        int result = 0;
        for (int y = 0; y < size; y++) {
            boolean c = modules[y][0]; int run = 1;
            for (int x = 1; x < size; x++) {
                if (modules[y][x] == c) { run++; if (run == 5) result += 3; else if (run > 5) result++; }
                else { c = modules[y][x]; run = 1; }
            }
        }
        for (int x = 0; x < size; x++) {
            boolean c = modules[0][x]; int run = 1;
            for (int y = 1; y < size; y++) {
                if (modules[y][x] == c) { run++; if (run == 5) result += 3; else if (run > 5) result++; }
                else { c = modules[y][x]; run = 1; }
            }
        }
        for (int y = 0; y < size - 1; y++)
            for (int x = 0; x < size - 1; x++) {
                boolean c = modules[y][x];
                if (c == modules[y][x + 1] && c == modules[y + 1][x] && c == modules[y + 1][x + 1]) result += 3;
            }
        int dark = 0;
        for (int y = 0; y < size; y++) for (int x = 0; x < size; x++) if (modules[y][x]) dark++;
        int total = size * size;
        int k = (Math.abs(dark * 20 - total * 10) + total - 1) / total - 1;
        result += k * 10;
        return result;
    }

    private static boolean bit(int x, int i) { return ((x >>> i) & 1) != 0; }
}
