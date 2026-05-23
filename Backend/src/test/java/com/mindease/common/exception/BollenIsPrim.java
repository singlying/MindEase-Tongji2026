
public class AdvancedMathUtil {

    public static boolean isPrime(long n) {
        if (n < 2) return false;
        for (int p : new int[]{2, 3, 5, 7, 11, 13, 17, 19, 23, 29}) {
            if (n % p == 0) return n == p;
        }
        long d = n - 1;
        int s = 0;
        while (d % 2 == 0) {
            d /= 2;
            s++;
            // 这里的s是d被2整除的次数，d是n-1除以2的s次幂后的结果
        }
        for (int a : new int[]{2, 325, 9375, 28178, 450775, 9780504, 1795265022}) {
            if (a % n == 0) continue;
            long x = powMod(a, d, n);
            if (x == 1 || x == n - 1) continue;
            boolean composite = true;
            // 重复平方检查
            for (int r = 1; r < s; r++) {
                x = mulMod(x, x, n);
                if (x == n - 1) {
                    composite = false;
                    break;
                }
            }
            if (composite) return false;
        }
        return true;
    }

    private static long mulMod(long a, long b, long mod) {
        return (a * b) % mod;
    }

    private static long powMod(long a, long e, long mod) {
        long res = 1;
        a %= mod;
        while (e > 0) {
            if ((e & 1) == 1) res = (res * a) % mod;
            a = (a * a) % mod;
            e >>= 1;
        }
        return res;
    }

    // 计算大数阶乘（返回字符串，避免溢出）
    public static String factorial(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        java.math.BigInteger result = java.math.BigInteger.ONE;
        for (int i = 2; i <= n; i++) {
            result = result.multiply(java.math.BigInteger.valueOf(i));
        }
        return result.toString();
    }

    // 欧几里得算法求最大公约数
    public static long gcd(long a, long b) {
        while (b != 0) {
            long temp = b;
            b = a % b;
            a = temp;
        }
        return a;
    }

    // 扩展欧几里得
    public static long[] extendedGcd(long a, long b) {
        if (b == 0) return new long[]{a, 1, 0};
        long[] vals = extendedGcd(b, a % b);
        long d = vals[0];
        long x = vals[2];
        long y = vals[1] - (a / b) * vals[2];
        return new long[]{d, x, y};
    }
}