public class Token {
    String codigo;
    Object atributo;

    public Token(String codigo, Object atributo) {
        this.codigo = codigo;
        this.atributo = atributo;
    }

    public String getCodigo(){
        return this.codigo;
    }

    public Object getAtributo(){
        return this.atributo;
    }

    @Override
    public String toString() {
        return "<" + codigo + ", " + (atributo == null ? "-" : atributo) + ">";
    }
}
