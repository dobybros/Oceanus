package core.storage.adapters.data.zset;

/**
 * Created by lick on 2020/10/28.
 * Description：
 */

import java.util.Arrays;

public class Tuple implements Comparable<Tuple> {
    private byte[] element;
    private Double score;

    public Tuple(byte[] element, Double score) {
        super();
        this.element = element;
        this.score = score;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result;
        if (null != element) {
            for (final byte b : element) {
                result = prime * result + b;
            }
        }
        long temp;
        temp = Double.doubleToLongBits(score);
        result = prime * result + (int) (temp ^ (temp >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) return true;
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;
        Tuple other = (Tuple) obj;
        if (element == null) {
            if (other.element != null) return false;
        } else if (!Arrays.equals(element, other.element)) return false;
        return true;
    }

    @Override
    public int compareTo(Tuple other) {
        if (this.score == other.getScore() || Arrays.equals(this.element, other.element)) return 0;
        else return this.score < other.getScore() ? -1 : 1;
    }

    public byte[] getElement() {
        return element;
    }

    public byte[] getBinaryElement() {
        return element;
    }

    public double getScore() {
        return score;
    }

    @Override
    public String toString() {
        return '[' + Arrays.toString(element) + ',' + score + ']';
    }
}

