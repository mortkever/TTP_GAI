package Masterprobleem;

public class Triple<A, B, C> {
    public final A first;
    public final B second;
    public final C third;

    public Triple(A a, B b, C c) {
        this.first = a;
        this.second = b;
        this.third = c;
    }

    @Override
    public boolean equals(Object o) {
        if (!(o instanceof Triple)) return false;
        Triple<?, ?, ?> other = (Triple<?, ?, ?>) o;
        return first.equals(other.first) && second.equals(other.second) && third.equals(other.third);
    }

    @Override
    public int hashCode() {
        return 31 * first.hashCode() + 17 * second.hashCode() + third.hashCode();
    }

    @Override
    public String toString() {
        return "(" + first + ", " + second + ", " + third + ")";
    }
}