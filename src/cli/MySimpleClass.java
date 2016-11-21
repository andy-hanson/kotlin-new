package cli;

class MySimpleClass {
    int x, y;

    MySimpleClass(int x) {
        this.x = x; this.y = y;
    }

    public String toString() {
        StringBuilder b = new StringBuilder("MySimpleClass(");
        b.append(x);
        b.append(", ");
        b.append(y);
        b.append(")");
        return b.toString();
    }
}
