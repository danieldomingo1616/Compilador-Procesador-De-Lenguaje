import java.io.IOException;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.Iterator;

public class AnalizadorSintacticoSemantico {
    private AnalizadorLexico lexico;
    private Token tokenActual;
    private FileWriter parseWriter;
    private FileWriter erroresSintWriter;
    private FileWriter erroresSemWriter;
    private TS_Gestor gestor;
    private int desplG = 0;
    private int desplL = 0;

    // Declaramos los tipos con un nombre fijo para evitar confusiones y faltas de
    // ortografia
    private final String TIPO_ENTERO = "entero";
    private final String TIPO_REAL = "real";
    private final String TIPO_BOOL = "l�gico";
    private final String TIPO_CADENA = "cadena";
    private final String TIPO_VOID = "void";
    private final String CLASE_VAR = "VAR";
    private final String CLASE_FUNC = "FUNC";
    private final String CLASE_PARAM = "PARAM"; 
    private final String TIPO_ERROR = "tipo_error";
    private final String TIPO_OK = "tipo_ok";

    // Clase auxiliar para poder devolver tipo y listaTipos a la vez en un función
    private class Atributos {
        String tipo;
        ArrayList<String> listaTipos;

        public Atributos() {
            this.tipo = TIPO_OK;
            this.listaTipos = new ArrayList<>();
        }
    }

    public AnalizadorSintacticoSemantico(String fuente) {
        this.gestor = new TS_Gestor("tabla_simbolos.txt");
        try {
            this.lexico = new AnalizadorLexico(fuente, gestor);
        } catch (IOException e) {
            System.out.println("Error en el fichero");
        }
        try {
            this.parseWriter = new FileWriter("parse.txt");
            parseWriter.write("Descendiente ");
            this.erroresSintWriter = new FileWriter("errores_sintactico.txt");
            this.erroresSemWriter = new FileWriter("errores_semantico.txt");
            this.tokenActual = lexico.siguienteToken();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void inicializarTabla() {
        // Guardamos las palabras reservadas en la tabla de simbolos
        gestor.createTPalabrasReservadas();
        Iterator<String> it = lexico.palabrasReservadas();
        while (it.hasNext()) {
            gestor.addEntradaTPalabrasReservadas(it.next());
        }

        // Creamos los atributos necesarios
        gestor.createAtributo("dirección", TS_Gestor.DescripcionAtributo.DIR, TS_Gestor.TipoDatoAtributo.ENTERO);
        gestor.createAtributo("dimensión", TS_Gestor.DescripcionAtributo.OTROS, TS_Gestor.TipoDatoAtributo.ENTERO);
        // El atributo clase sirve para diferenciar si se trata de una variable o una
        // función
        gestor.createAtributo("clase", TS_Gestor.DescripcionAtributo.OTROS, TS_Gestor.TipoDatoAtributo.CADENA);
        gestor.createAtributo("param", TS_Gestor.DescripcionAtributo.PARAM, TS_Gestor.TipoDatoAtributo.LISTA);

        gestor.createTSGlobal();
    }

    // metodo auxiliar para comparar dos tokens
    private void equipara(String codigoEsperado) {
        if (tokenActual != null && tokenActual.getCodigo().equals(codigoEsperado)) {
            try {
                if (!codigoEsperado.equals("EOF")) {
                    tokenActual = lexico.siguienteToken();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        } else {
            String encontrado = (tokenActual != null) ? tokenActual.getCodigo() : "null";
            errorSint("Se esperaba '" + codigoEsperado + "' pero se encontró '" + encontrado + "'");
            escribir(erroresSintWriter, "En la línea " + lexico.getLinea() + " Se esperaba '" + codigoEsperado
                    + "' pero se encontró '" + encontrado + "'");
        }
    }

    // Método auxiliar para devolver errores sintacticos
    private void errorSint(String mensaje) {
        System.err.println("ERROR SINTÁCTICO en línea " + lexico.getLinea() + ": " + mensaje);
    }

    // Método auxiliar para devolver errores semanticos
    private void errorSem(String mensaje) {
        System.err.println("ERROR Semantico en línea " + lexico.getLinea() + ": " + mensaje);
    }

    // Método para escribir en cualquier fichero
    private void escribir(FileWriter escritor, String texto) {
        try {
            if (escritor == parseWriter) {
                escritor.write(texto + " ");
            } else {
                escritor.write(texto + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void cerrar() {
        try {
            parseWriter.close();
            erroresSintWriter.close();
            erroresSemWriter.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Función auxiliar para saber el espacio que ocupan en memoria cada tipo de
    // variable
    private int getAncho(String tipo) {
        if (tipo.equals(TIPO_ENTERO))
            return 2;
        if (tipo.equals(TIPO_REAL))
            return 8;
        if (tipo.equals(TIPO_BOOL))
            return 1;
        if (tipo.equals(TIPO_CADENA))
            return 64;
        return 0;
    }

    public void parseA() {
        Atributos A = new Atributos();

        // Regla: A -> B C eof
        escribir(parseWriter, "1");

        Atributos B = parseB();
        Atributos C = parseC();

        // Si B y C son tipo_ok todo ha funcionado bien
        if (B.tipo.equals(TIPO_OK) && C.tipo.equals(TIPO_OK)) {
            A.tipo = TIPO_OK;
            System.out.println("El análisis semántico se ha relizado correctamente");

            gestor.write(TS_Gestor.Tabla.GLOBAL);
            gestor.destroy(TS_Gestor.Tabla.GLOBAL);

            System.out.println("Fichero generado: tabla_simbolos.txt");
        } else {
            A.tipo = TIPO_ERROR;
            System.err.println("El analisis semántico se ha detenido, se encontraron errores semánticos");
        }

        if (tokenActual != null && !tokenActual.getCodigo().equals("EOF")) {
            errorSint("Fin de fichero inesperado");
        }
    }

    public Atributos parseB() {
        Atributos B = new Atributos();

        // Regla: B -> D B
        if (tokenActual.getCodigo().equals("let")) {
            escribir(parseWriter, "2");

            Atributos D = parseD();
            // Utilizamos B1 para poder tener dos atributos de B a la vez
            Atributos B1 = parseB();

            if (D.tipo.equals(TIPO_OK) && B1.tipo.equals(TIPO_OK)) {
                B.tipo = TIPO_OK;
            } else {
                B.tipo = TIPO_ERROR;
            }
        }
        // Regla: B -> Lambda
        else {
            escribir(parseWriter, "3");
            B.tipo = TIPO_OK;
        }
        return B;
    }

    public Atributos parseD() {
        Atributos D = new Atributos();

        // Regla: D -> let T id ;
        escribir(parseWriter, "4");
        equipara("let");

        // Indicamos al analizador léxico que empieza la zona de declaracion de
        // variables
        lexico.setZonaDecl(true);

        Atributos T = parseT();

        Token id = tokenActual;
        equipara("id");
        // Cerramos la zona de declaración de variables
        lexico.setZonaDecl(false);

        // Obtenemos la posición del token actual en la tabla de simbolos
        int pos = (Integer) id.getAtributo();

        // Si la posición es 0 indica que no se encuentra en la tabla de simbolos
        if (pos == 0) {
            D.tipo = TIPO_ERROR;
        } 
        else {
            gestor.setTipo(pos, T.tipo);
            gestor.setValorAtributoCad(pos, "clase", CLASE_VAR);
            int ancho = getAncho(T.tipo);
            // Comprobamos si estamos dentro de una función para saber donde añadir el
            // desplazamiento, en la tabla local o en la global
            if (lexico.getDentroFuncion()) {
                // Tabla local
                gestor.setValorAtributoEnt(pos, "dirección", desplL);
                desplL += ancho;
            } else {
                // Tabla global
                gestor.setValorAtributoEnt(pos, "dirección", desplG);
                desplG += ancho;
            }
            D.tipo = TIPO_OK;
        }
        equipara("puntoComa");
        return D;
    }

    public Atributos parseC() {
        Atributos C = new Atributos();

        // Regla: C -> F C
        if (tokenActual.getCodigo().equals("function")) {
            escribir(parseWriter, "5");

            Atributos F = parseF();
            Atributos C1 = parseC();

            if (F.tipo.equals(TIPO_OK) && C1.tipo.equals(TIPO_OK)) {
                C.tipo = TIPO_OK;
            } else {
                C.tipo = TIPO_ERROR;
            }
        }
        // Regla: C -> Lambda
        else {
            escribir(parseWriter, "6");
            C.tipo = TIPO_OK;
        }
        return C;
    }

    public Atributos parseF() {
        Atributos F = new Atributos();

        // Regla: F -> function X id ( P ) { S }
        escribir(parseWriter, "7");

        equipara("function");

        lexico.setZonaDecl(true);

        Atributos X = parseX();

        Token idFuncToken = tokenActual;
        equipara("id");

        lexico.setZonaDecl(false);

        int pos = (Integer) idFuncToken.getAtributo();
        if (pos != 0) {
            gestor.setTipo(pos, X.tipo);
            gestor.setValorAtributoCad(pos, "clase", CLASE_FUNC);
        }

        this.desplL = 0;

        // Indicamos al analizador lexico que hemos entrado dentro de una funcion
        lexico.setDentroFuncion(true);
        gestor.createTSLocal();

        equipara("parentesisIzquierdo");
        Atributos P = parseP();
        equipara("parentesisDerecho");

        if (pos != 0) {
            // Transformamos el arrayList de P a un array
            String[] params = P.listaTipos.toArray(new String[0]);
            // Añadimos los parametros a la tabla global
            gestor.setValorAtributoLista(pos, "param", params);
        }

        equipara("llaveIzquierda");
        parseS(X.tipo);
        equipara("llaveDerecha");

        // Escribimos la tabla antes de borrarla
        gestor.write(TS_Gestor.Tabla.LOCAL);
        gestor.destroy(TS_Gestor.Tabla.LOCAL);

        lexico.setDentroFuncion(false);
        F.tipo = TIPO_OK;
        return F;
    }

    public Atributos parseX() {
        Atributos X = new Atributos();

        // Regla: X -> void
        if (tokenActual.getCodigo().equals("void")) {
            escribir(parseWriter, "9");

            equipara("void");
            X.tipo = TIPO_VOID;
        }
        // Regla: X -> T
        else {
            escribir(parseWriter, "8");

            Atributos T = parseT();
            X.tipo = T.tipo;
        }
        return X;
    }

    // P -> T id G (10) | void (11)
    public Atributos parseP() {
        Atributos P = new Atributos();
        String t = tokenActual.getCodigo();

        // Regla: P -> T id G
        // Comprobamos si el token pertenece al First de T
        if (t.equals("int") || t.equals("float") || t.equals("boolean") || t.equals("string")) {
            escribir(parseWriter, "10");

            lexico.setZonaDecl(true);

            Atributos T = parseT();

            Token id = tokenActual;
            equipara("id");

            lexico.setZonaDecl(false);

            int pos = (Integer) id.getAtributo();
            if (pos != 0) {
                gestor.setTipo(pos, T.tipo);
                gestor.setValorAtributoCad(pos, "clase", CLASE_PARAM);

                int ancho = getAncho(T.tipo);
                gestor.setValorAtributoEnt(pos, "dirección", desplL);
                desplL += ancho;
            }
            P.listaTipos.add(T.tipo);

            Atributos G = parseG();
            P.listaTipos.addAll(G.listaTipos);
        }
        // Regla: P -> void
        else if (tokenActual.getCodigo().equals("void")) {
            escribir(parseWriter, "11");
            equipara("void");
        } else {
            errorSem("Se esperaba un tipo...");
            escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " Se esperaba algun tipo");
            P.tipo = TIPO_ERROR;
        }
        return P;
    }

    public Atributos parseG() {
        Atributos G = new Atributos();

        // Regla: G -> , T id G
        if (tokenActual.getCodigo().equals("coma")) {
            escribir(parseWriter, "12");

            equipara("coma");

            lexico.setZonaDecl(true);

            Atributos T = parseT();

            Token id = tokenActual;
            equipara("id");

            lexico.setZonaDecl(false);

            int pos = (Integer) id.getAtributo();
            if (pos != 0) {
                gestor.setTipo(pos, T.tipo);
                gestor.setValorAtributoCad(pos, "clase", CLASE_PARAM);

                int ancho = getAncho(T.tipo);
                gestor.setValorAtributoEnt(pos, "dirección", desplL);
                desplL += ancho;
            }
            G.listaTipos.add(T.tipo);

            Atributos G1 = parseG();
            // Añadimos todos los nuevos tipos a la lista de G
            G.listaTipos.addAll(G1.listaTipos);
        }
        // Regla: G -> Lambda
        else {
            escribir(parseWriter, "13");
        }
        return G;
    }

    public Atributos parseS(String tipoRetornoEsperado) {
        Atributos S = new Atributos();
        String t = tokenActual.getCodigo();

        // Regla: S -> H S
        // Comprobamos si el token pertenece al First de H
        if (t.equals("if") || t.equals("while") || t.equals("let") || t.equals("id") ||
                t.equals("write") || t.equals("read") || t.equals("return") ||
                t.equals("llaveIzquierda")) {
            escribir(parseWriter, "14");

            Atributos H = parseH(tipoRetornoEsperado);
            Atributos S1 = parseS(tipoRetornoEsperado);

            if (H.tipo.equals(TIPO_OK) && S1.tipo.equals(TIPO_OK)) {
                S.tipo = TIPO_OK;
            } else {
                S.tipo = TIPO_ERROR;
            }
        }
        // Regla: S -> Lambda
        else {
            escribir(parseWriter, "15");
            S.tipo = TIPO_OK;
        }
        return S;
    }

    public Atributos parseH(String tipoRetornoEsperado) {
        Atributos H = new Atributos();
        String token = tokenActual.getCodigo();

        switch (token) {
            // Regla: H -> if ( E ) { S }
            case "if":
                escribir(parseWriter, "16");
                equipara("if");
                equipara("parentesisIzquierdo");
                Atributos E = parseE();
                if (E.tipo != null && !E.tipo.equals(TIPO_BOOL) && !E.tipo.equals(TIPO_ERROR)) {
                    errorSem("La condición del IF debe ser de tipo boolean.");
                    escribir(erroresSemWriter,
                            "En la línea " + lexico.getLinea() + " La condición del if debe ser de tipo boolean" +
                                    "pero se encontro" + E.tipo);
                }
                equipara("parentesisDerecho");
                equipara("llaveIzquierda");
                parseS(tipoRetornoEsperado);
                equipara("llaveDerecha");
                H.tipo = TIPO_OK;
                break;

            // Regla: H -> while ( E ) { S }
            case "while":
                escribir(parseWriter, "17");
                equipara("while");
                equipara("parentesisIzquierdo");
                Atributos E1 = parseE();
                if (!E1.tipo.equals(TIPO_BOOL) && !E1.tipo.equals(TIPO_ERROR)) {
                    errorSem("La condición del WHILE debe ser de tipo boolean.");
                    escribir(erroresSemWriter,
                            "En la línea " + lexico.getLinea() + " La condición del while debe ser de tipo boolean" +
                                    "pero se encontro" + E1.tipo);
                }
                equipara("parentesisDerecho");
                equipara("llaveIzquierda");
                parseS(tipoRetornoEsperado);
                equipara("llaveDerecha");
                H.tipo = TIPO_OK;
                break;

            // Regla: H -> let T id ; (o H -> D)
            case "let":
                escribir(parseWriter, "18");
                Atributos D = parseD();
                H.tipo = D.tipo;
                break;

            // Regla: H -> I ;
            case "id":
                escribir(parseWriter, "19");
                Atributos I = parseI();
                H.tipo = I.tipo;
                equipara("puntoComa");
                break;

            // Regla: H -> write E ;
            case "write":
                escribir(parseWriter, "20");
                equipara("write");
                Atributos E2 = parseE();
                if (E2.tipo.equals(TIPO_ERROR))
                    H.tipo = TIPO_ERROR;
                else
                    H.tipo = TIPO_OK;
                equipara("puntoComa");
                break;

            // Regla: H -> read id ;
            case "read":
                escribir(parseWriter, "21");
                equipara("read");
                Token id = tokenActual;
                equipara("id");
                if ((Integer) id.getAtributo() == 0)
                    H.tipo = TIPO_ERROR;
                else
                    H.tipo = TIPO_OK;
                equipara("puntoComa");
                break;

            // Regla 22: H -> return J ;
            case "return":
                escribir(parseWriter, "22");
                equipara("return");
                Atributos J = parseJ();
                if (!J.tipo.equals(tipoRetornoEsperado) && !J.tipo.equals(TIPO_ERROR)) {
                    errorSem("Tipo de retorno incorrecto. Se esperaba " + tipoRetornoEsperado + " y se encontró "
                            + J.tipo);
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " El tipo de retorno deberia ser " +
                            tipoRetornoEsperado + "pero se encontro" + J.tipo);
                    H.tipo = TIPO_ERROR;
                } else {
                    H.tipo = TIPO_OK;
                }
                equipara("puntoComa");
                break;

            // Regla: H -> { S }
            case "llaveIzquierda":
                escribir(parseWriter, "23");
                equipara("llaveIzquierda");
                parseS(tipoRetornoEsperado);
                equipara("llaveDerecha");
                H.tipo = TIPO_OK;
                break;

            default:
                errorSint("Sentencia no válida: " + token);
                escribir(erroresSintWriter, "En la línea " + lexico.getLinea() + " Sentencia no valida" + token);
                H.tipo = TIPO_ERROR;
        }
        return H;
    }

    public Atributos parseI() {
        // Regla: I -> id K
        escribir(parseWriter, "24");

        Atributos I = new Atributos();
        Token id = tokenActual;
        equipara("id");

        int pos = (Integer) id.getAtributo();
        // Por defecto asumimos que es un error
        String t = TIPO_ERROR;
        // Comprobamos si la variable esta guardada en la tabla de símbolos
        if (pos != 0) {
            String tipoRecuperado = gestor.getTipo(pos);
            
            // Comprobamos si se encuentra en la TS
            if (tipoRecuperado != null) {
                t = tipoRecuperado;
            } 
            else {
                // Como el tipo void no se le puede asignar a traves de la libreria, asumimos que si una función no tiene 
                // tipo es void automaticamente
                String clase = gestor.getValorAtributoCad(pos, "clase");

                // Aqui lo comprobamos y si funcion lo asignamos
                if (clase != null && clase.equals(CLASE_FUNC)) {
                    t = TIPO_VOID; 
                }
                // Si no la encontramos y no cumple el caso anterior devolvemos un error
                else {
                    errorSem("La variable no ha sido declarada correctamente.");
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " Variable sin tipo o no declarada.");
                    // t se queda como TIPO_ERROR para que el programa no explote
                }
            }    
        }
        Atributos K = parseK(t, pos);
        I.tipo = K.tipo;
        return I;
    }

    public Atributos parseK(String t, int pos) {
        Atributos K = new Atributos();

        // Regla: K -> = E
        if (tokenActual.getCodigo().equals("asignacion")) {
            escribir(parseWriter, "25");
            equipara("asignacion");
            Atributos E = parseE();

            // Comprobamos si los tipos son correctos
            if (t != null && E.tipo != null && !t.equals(E.tipo) && !t.equals(TIPO_ERROR) && !E.tipo.equals(TIPO_ERROR)) {
                errorSem("Asignación incorrecta, variable de tipo " + t + " no admite valor " + E.tipo);
                escribir(erroresSemWriter,
                        "En la línea " + lexico.getLinea() + " Asignación incorrecta, se esperaba tipo "
                                + t + " y se encontro tipo " + E.tipo);
                K.tipo = TIPO_ERROR;
            } else {
                K.tipo = TIPO_OK;
            }
        }
        // Regla: K -> += E
        else if (tokenActual.getCodigo().equals("asignacionSuma")) {
            escribir(parseWriter, "26");
            equipara("asignacionSuma");
            Atributos E = parseE();

            // Comprobamos si los tipos son correctos
            if ((!t.equals(TIPO_ENTERO) || !E.tipo.equals(TIPO_ENTERO))
                    && (!t.equals(TIPO_REAL) || !E.tipo.equals(TIPO_REAL))) {
                errorSem("Operador += solo válido para enteros y reales");
                escribir(erroresSemWriter,
                        "En la línea " + lexico.getLinea() + " El operador += solo sirve para reales y " +
                                "enteros pero se encontro " + t + " y " + E.tipo);
                K.tipo = TIPO_ERROR;
            } else {
                K.tipo = TIPO_OK;
            }
        }
        // Regla: K -> ( L )
        else if (tokenActual.getCodigo().equals("parentesisIzquierdo")) {
            escribir(parseWriter, "27");
            equipara("parentesisIzquierdo");
            if (pos != 0) {
                String clase = gestor.getValorAtributoCad(pos, "clase");

                // Comprobamos por si acaso es una variable y no una función
                if (clase != null && !"FUNC".equals(clase)) {
                    errorSem("Se está intentando llamar a una variable como si fuera una función.");
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                            " Se está intentando llamar a la variable como si fuera una función.");
                }

                // Recuperamos los parametros de la función
                String[] paramsEsperados = gestor.getValorAtributoLista(pos, "param");

                // Le pasamos a L los parametros esperados para que los compruebe
                parseL(paramsEsperados);
            }
            // No esta guardada la función en la TS
            else {
                parseL(null);
            }
            equipara("parentesisDerecho");
            K.tipo = TIPO_OK;
        }
        return K;
    }

    public Atributos parseJ() {
        String t = tokenActual.getCodigo();

        // Regla: J -> E
        // Comprobamos que el token se encuentre en el First de E
        if (t.equals("id") || t.equals("constanteEntera") || t.equals("constanteReal") ||
                t.equals("cadena") || t.equals("parentesisIzquierdo")) {
            escribir(parseWriter, "28");
            return parseE();
        }
        // Regla: J -> Lambda
        else {
            escribir(parseWriter, "29");
            Atributos J = new Atributos();
            J.tipo = TIPO_VOID;
            return J;
        }
    }

    public Atributos parseE() {
        Atributos E = new Atributos();

        // Regla: E -> R N
        escribir(parseWriter, "30");
        Atributos R = parseR();
        Atributos N = parseN(R.tipo);
        E.tipo = N.tipo;
        return E;
    }

    // N -> && R N (31) | lambda (32)
    public Atributos parseN(String Rtipo) {
        Atributos N = new Atributos();

        // Regla 31: N -> && R N
        if (tokenActual.getCodigo().equals("and")) {
            escribir(parseWriter, "31");
            if (!Rtipo.equals(TIPO_BOOL) && !Rtipo.equals(TIPO_ERROR)) {
                errorSem("El operador '&&' requiere un operando izquierdo booleano.");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " El operador de la izquierda de && tiene que ser un booleano");
            }
            equipara("and");
            Atributos R = parseR();
            if (!R.tipo.equals(TIPO_BOOL) && !R.tipo.equals(TIPO_ERROR)) {
                errorSem("El operador '&&' requiere un operando derecho booleano.");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " El operador de la derecha de && tiene que ser un booleano");
            }
            return parseN(TIPO_BOOL);
        }
        // Regla: N -> Lambda
        else {
            escribir(parseWriter, "32");
            N.tipo = Rtipo;
        }
        return N;
    }

    public Atributos parseR() {
        // Regla: R -> U O
        escribir(parseWriter, "33");
        Atributos U = parseU();
        Atributos O = parseO(U.tipo);
        Atributos R = new Atributos();
        R.tipo = O.tipo;
        return R;
    }

    public Atributos parseO(String tipo) {
        Atributos O = new Atributos();

        // Regla 34: O -> < U O
        if (tokenActual.getCodigo().equals("menor")) {
            escribir(parseWriter, "34");
            // Comprobamos que los operandos sean numéricos (int o real)
            if (!tipo.equals(TIPO_ENTERO) && !tipo.equals(TIPO_REAL) && !tipo.equals(TIPO_ERROR)) {
                errorSem("El operador '<' solo admite operandos numéricos.");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        "El operador < solo admite operandos numericos");
            }

            equipara("menor");
            Atributos U = parseU();
            if (!U.tipo.equals(tipo) && !U.tipo.equals(TIPO_ERROR)) {
                errorSem("Tipos incompatibles en comparación '<'.");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " Los dos tipos de los operandos de < tienen que ser el mismo");
            }
            return parseO(TIPO_BOOL);
        }
        // Regla: O -> Lambda
        else {
            escribir(parseWriter, "35");
            O.tipo = tipo;
        }
        return O;
    }

    public Atributos parseU() {
        Atributos U = new Atributos();

        // Regla: U -> V Q
        escribir(parseWriter, "36");
        Atributos V = parseV();
        Atributos Q = parseQ(V.tipo);
        U.tipo = Q.tipo;
        return U;
    }

    public Atributos parseQ(String tipo) {
        Atributos Q = new Atributos();

        // Regla 37: Q -> + V Q
        if (tokenActual.getCodigo().equals("suma")) {
            escribir(parseWriter, "37");

            String tipoEsperado = "";
            if (tipo.equals(TIPO_ERROR)) {
                tipoEsperado = TIPO_ERROR;
            } else if (tipo.equals(TIPO_ENTERO)) {
                tipoEsperado = TIPO_ENTERO;
            } else if (tipo.equals(TIPO_REAL)) {
                tipoEsperado = TIPO_REAL;
            } else {
                errorSem("El operando izquierdo debe ser real o entero");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " El operando izquierdo de + debe ser real o entero");
                tipoEsperado = TIPO_ERROR;
            }

            equipara("suma");
            Atributos V = parseV();

            // Si el tipo del operando izquierdo no era correcto lo transmitimos
            if (tipoEsperado.equals(TIPO_ERROR)) {
                return parseQ(TIPO_ERROR);
            }

            // Comprobamos que los dos operandos son del mismo tipo
            if (tipoEsperado != null && V.tipo != null && tipoEsperado.equals(TIPO_ENTERO) && !V.tipo.equals(TIPO_ENTERO) && !V.tipo.equals(TIPO_ERROR)) {
                errorSem("El operando derecho de '+' debe ser entero");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " Los dos tipos de los operandos de + tienen que ser el mismo");
                tipoEsperado = TIPO_ERROR;
            } else if (tipoEsperado.equals(TIPO_REAL) && !V.tipo.equals(TIPO_REAL) && !V.tipo.equals(TIPO_ERROR)) {
                errorSem("El operando derecho de '+' debe ser real");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " Los dos tipos de los operandos de + tienen que ser el mismo");
                tipoEsperado = TIPO_ERROR;
            } else if (V.tipo != null && V.tipo.equals(TIPO_ERROR)) {
                tipoEsperado = TIPO_ERROR;
            }

            return parseQ(tipoEsperado);

        }
        // Regla: Q -> Lambda
        else {
            escribir(parseWriter, "38");
            Q.tipo = tipo;
        }
        return Q;
    }

    public Atributos parseV() {
        Atributos V = new Atributos();
        String t = tokenActual.getCodigo();

        // Regla 39: V -> id W
        if (t.equals("id")) {
            escribir(parseWriter, "39");

            // Nos guardamos el token para no perderlo
            Token id = tokenActual;
            equipara("id");

            int pos = (Integer) id.getAtributo();
            String tipo = (pos != 0) ? gestor.getTipo(pos) : TIPO_ERROR;

            if (tipo == null) {
                    errorSem("Variable no declarada o sin tipo");
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " Variable no declarada o sin tipo");
                    tipo = TIPO_ERROR;
                }
            
            Atributos W = parseW(tipo, pos);
            V.tipo = W.tipo;
        }
        // Regla: V -> ( E )
        else if (t.equals("parentesisIzquierdo")) {
            escribir(parseWriter, "40");

            equipara("parentesisIzquierdo");
            V = parseE();
            equipara("parentesisDerecho");
        }
        // Regla: V -> constanteEntera
        else if (t.equals("constanteEntera")) {
            escribir(parseWriter, "41");

            V.tipo = TIPO_ENTERO;
            equipara("constanteEntera");
        }
        // Regla: V -> constanteReal
        else if (t.equals("constanteReal")) {
            escribir(parseWriter, "42");

            V.tipo = TIPO_REAL;
            equipara("constanteReal");
        }
        // Regla: V -> cadena
        else if (t.equals("cadena")) {
            escribir(parseWriter, "43");
            V.tipo = TIPO_CADENA;
            equipara("cadena");
        } else {
            errorSem("Se esperaba un operando (id, número, cadena...)");
            escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " Se esperaba un operando");
            V.tipo = TIPO_ERROR;
        }
        return V;
    }

    public Atributos parseW(String tipo, int pos) {
        Atributos W = new Atributos();

        // Regla: W -> ( L )
        if (tokenActual.getCodigo().equals("parentesisIzquierdo")) {
            escribir(parseWriter, "44");

            equipara("parentesisIzquierdo");

            if (pos != 0) {
                String clase = gestor.getValorAtributoCad(pos, "clase");
                if (clase != null && !"FUNC".equals(clase)) {
                    errorSem("Se está intentando llamar a una variable como si fuera una función");
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                            " Se está intentando llamar a una variable como si fuera una función");
                }

                // Obtenemos los parametros esperados y los transmitimos
                String[] parametrosEsperados = gestor.getValorAtributoLista(pos, "param");
                parseL(parametrosEsperados);
            } else {
                parseL(null);
            }

            equipara("parentesisDerecho");
            W.tipo = tipo;
        }
        // Regla: W -> Lambda
        else {
            escribir(parseWriter, "45");
            W.tipo = tipo;
        }
        return W;
    }

    public void parseL(String[] tiposEsperados) {
        String t = tokenActual.getCodigo();

        // Regla 46: L -> E M
        // Comprobamos que el token se encuentra en el first de E
        if (t.equals("id") || t.equals("constanteEntera") || t.equals("constanteReal") ||
                t.equals("cadena") || t.equals("parentesisIzquierdo")) {
            escribir(parseWriter, "46");

            Atributos E = parseE();

            // Empezamos a comparar tipos, el primer argumento se compara aqui en L y el
            // resto en M
            if (tiposEsperados != null && tiposEsperados.length >= 1) {
                String tipoEsperado = tiposEsperados[0];
                if (E.tipo != null && !E.tipo.equals(tipoEsperado) && !E.tipo.equals(TIPO_ERROR)) {
                    errorSem("Argumento 1 incorrecto. Se esperaba " + tipoEsperado + " y se encontró " + E.tipo);
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                            " Argumento 1 incorrecto. Se esperaba " + tipoEsperado + " y se encontró " + E.tipo);
                }
            }
            parseM(tiposEsperados, 1);
        }
        // Regla: L -> Lambda
        else {
            escribir(parseWriter, "47");
            if (tiposEsperados != null && tiposEsperados.length > 0) {
                errorSem("Faltan argumentos en la llamada");
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " Faltan argumentos en la llamada a la función");
            }
        }
    }

    public void parseM(String[] tiposEsperados, int indiceActual) {
        // Regla: M -> , E M
        if (tokenActual.getCodigo().equals("coma")) {
            escribir(parseWriter, "48");

            equipara("coma");
            Atributos E = parseE();
            // Comparamos si los tipos de todos los parametros son correctos de forma
            // recursiva
            if (tiposEsperados != null && indiceActual < tiposEsperados.length) {

                String tipoEsperado = tiposEsperados[indiceActual];

                if (!E.tipo.equals(tipoEsperado) && !E.tipo.equals(TIPO_ERROR)) {
                    errorSem("Argumento " + (indiceActual + 1) + " incorrecto. Se esperaba " + tipoEsperado +
                            " y se encontró " + E.tipo);
                    escribir(erroresSemWriter, "En la línea " + lexico.getLinea() + " Argumento " + (indiceActual + 1) +
                            " incorrecto. Se esperaba " + tipoEsperado + " y se encontró " + E.tipo);
                }
            }
            // Volvemos a llamar a M siguiendo por el siguiente argumento
            parseM(tiposEsperados, indiceActual + 1);
        }
        // Regla: M -> Lambda
        else {
            escribir(parseWriter, "49");

            if (tiposEsperados != null && indiceActual < tiposEsperados.length) {
                errorSem("Faltan argumentos. Se esperaban " + tiposEsperados.length);
                escribir(erroresSemWriter, "En la línea " + lexico.getLinea() +
                        " Faltan argumentos en la llamada a la función");
            }
        }
    }

    public Atributos parseT() {
        Atributos T = new Atributos();
        String t = tokenActual.getCodigo();

        // Regla: T -> int
        if (t.equals("int")) {
            escribir(parseWriter, "50");
            equipara("int");
            T.tipo = TIPO_ENTERO;
        }
        // Regla: T -> float
        else if (t.equals("float")) {
            escribir(parseWriter, "51");
            equipara("float");
            T.tipo = TIPO_REAL;
        }
        // Regla: T -> boolean
        else if (t.equals("boolean")) {
            escribir(parseWriter, "52");
            equipara("boolean");
            T.tipo = TIPO_BOOL;
        }
        // Regla: T -> string
        else if (t.equals("string")) {
            escribir(parseWriter, "53");
            equipara("string");
            T.tipo = TIPO_CADENA;
        } else {
            errorSint("Se esperaba un tipo (int, float, boolean, string)");
            escribir(erroresSintWriter, "En la línea " + lexico.getLinea() +
                    "  Se esperaba un tipo (int, float, boolean, string)");
            T.tipo = TIPO_ERROR;
        }
        return T;
    }
}