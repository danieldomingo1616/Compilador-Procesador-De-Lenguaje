import java.io.*;
import java.util.*;

public class AnalizadorLexico {

    private BufferedReader br;
    private int car;
    private int linea = 1;
    private String lexema;
    private PrintWriter tokensWriter;
    private PrintWriter erroresWriter;
    private TS_Gestor gestor;
    private boolean zonaDecl;     // true = estamos declarando (let/param)
    private boolean dentroFuncion; // true = estamos dentro de una función (ámbito local)
    

   
    // COLUMNAS (clases de carácter)
    // 0: letra   1: dígito   2: '+'   3: '='   4: '"'   5: '/'   6: '*'
    // 7: blanco    8: ';'      9: '{'   10: '}'  11: '('  12: ')'  13: ','
    // 14: '<'      15: '_'     16: '&'  17: '.'  18: EOF  19: otro
    // ===========================================================

    // Aceptaciones (>100):
    // 101 id, 102 entero, 103 real, 104 +=, 105 +, 106 &&, 107 cadena, 108 comentario
    // 109 '=' 110 ';', 111 '{', 112 '}', 113 '(', 114 ')', 115 ',', 116 '<', 117 EOF

    private final int[][] trans = {
        //  0   1   2   3   4   5   6   7   8   9   10  11  12  13  14  15  16  17  18  19
        {  1,  2,   5,109,  7,  8, -1,  0,110,111, 112,113,114,115,116, 1,  6, -1, 117,-1}, //0
        {  1,  1, 101,101,101,101,101,101,101,101, 101,101,101,101,101, 1 ,101,101,101,101}, //1 id
        { 102, 2, 102,102,102,102,102,102,102,102, 102,102,102,102,102,102,102,  3,102,102}, //2 entero
        {  -2, 4,  -2, -2, -2, -2, -2, -2, -2, -2,  -2, -2, -2, -2, -2, -2, -2, -2, -2, -2}, //3 inicio real
        { 103, 4, 103, 103,103,103,103,103,103,103,103,103,103,103,103,103,103,103,103,103}, //4  real
        { 105,105,105,104, 105,105,105,105,105,105,105,105,105,105,105,105,105,105,105,105}, //5 += o +
        {  -5, -5, -5, -5, -5, -5, -5, -5, -5, -5,  -5, -5, -5, -5, -5, -5,106, -5, -5, -5}, //6 &&
        {  7,  7,   7,  7,  107,  7, 7, 7,  7,  7,   7,  7,  7,  7,  7,  7,  7,  7,  7,  7}, //7 cadena
        { -3, -3,  -3, -3, -3, -3,  9, -3, -3, -3,  -3, -3, -3, -3, -3, -3, -3, -3, -3, -3}, //8 inicio comentario
        {  9,  9,   9,  9,  9,  9, 10,  9,  9,  9,   9,  9,  9,  9,  9,  9,  9,  9,  9,  9}, //9 comentario
        { -3, -3,  -3, -3, -3,108, -3, -3, -3, -3,  -3, -3, -3, -3, -3, -3, -3, -3, -3, -3}, // fin comentario
    };

    private final int[][] acc = {
        //  0  1  2  3  4  5  6  7  8  9 10 11 12 13 14 15 16 17 18 19
          { 2, 2, 1, 6, 1, 1, 7, 1, 6, 6, 6, 6, 6, 6, 6, 2, 1, 4, 6, 4 }, //0
          { 2, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 3, 3, 3, 3 }, //1
          { 3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 2, 3, 3 }, //2
          { 4, 2, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 }, //3
          { 3, 2, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 }, //4
          { 3, 3, 3, 6, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3, 3 }, //5
          { 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 6, 4, 4, 4 }, //6
          { 2, 2, 2, 2, 6, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 2, 4, 2 }, //7
          { 4, 4, 4, 4, 4, 4, 1, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4, 4 }, //8
          { 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 1, 4, 1 }, //9
          { 7, 7, 7, 7, 7, 5, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 7, 4, 7 }, //10 
 
    };

    private static final Set<String> PALABRAS = new HashSet<>(Arrays.asList(
        "function","let","int","float","boolean","string","void",
        "if","while","return","read","write"
    ));

    public AnalizadorLexico(String fichero, TS_Gestor gestor) throws IOException {
        br = new BufferedReader(new FileReader(fichero));
        car = br.read();
        lexema = "";
        this.tokensWriter = new PrintWriter(new FileWriter("tokens.txt"));
        this.erroresWriter = new PrintWriter(new FileWriter("errores_lexico.txt"));
        this.gestor = gestor;
        zonaDecl = false;
        dentroFuncion = false;
    }

    private int claseCaracter(int c) {
        if (c == -1) return 18;
        char ch = (char) c;
        if (Character.isLetter(ch)) return 0;
        if (Character.isDigit(ch)) return 1;
        if (ch == '+') return 2;
        if (ch == '=') return 3;
        if (ch == '"') return 4;
        if (ch == '/') return 5;
        if (ch == '*') return 6;
        if (Character.isWhitespace(ch)) return 7;
        if (ch == ';') return 8;
        if (ch == '{') return 9;
        if (ch == '}') return 10;
        if (ch == '(') return 11;
        if (ch == ')') return 12;
        if (ch == ',') return 13;
        if (ch == '<') return 14;
        if (ch == '_') return 15;
        if (ch == '&') return 16;
        if (ch == '.') return 17;
        return 19;
    }

    private void leer() throws IOException {
        car = br.read();
        if (car == '\n') linea++;
    }

    private Token error(String msg) throws IOException {
        if (car != -1) leer();
        return new Token("ERROR(Lexico,L" + linea + ")", msg);
    }

    private Token ejecutarAccion(int a, int estado) throws IOException {
        char c = (char) car;
        switch (a) {
            case 1:
                leer();
                return null;
            case 2:
                lexema += c;
                leer();
                return null; 
            // case 6 y 3 hacen lo mismo pero el 6 además lee, por eso los ponemos juntos
            case 6:  
                leer();
            case 3:
                String lexemaAux = lexema;
                lexema = "";
                switch (estado) {
                    // Caso del id, lo añadimos también a la tabla de simbolos
                    case 101: 
                        // Compramos antes si es una palabra reservada
                        if (PALABRAS.contains(lexemaAux)) {
                            return new Token(lexemaAux, null);
                        } 
                        else {
                            Integer idPos = null;
                        // Comprobamos que estamos en zona de declaración de variables
                        if (this.zonaDecl) { 
                            // Comprobamos que estamos si estamos o no dentro de una función
                            if (this.dentroFuncion) {
                                idPos = gestor.addEntradaTSLocal(lexemaAux);
                            } 
                            else {
                                idPos = gestor.addEntradaTSGlobal(lexemaAux);
                            }
                            // Si la posicion sigue siendo 0 no se ha introducido en la tabla porque ya estaba
                            if (idPos == 0) {
                                System.err.println("Error semántico línea " + linea + ": Variable '" + lexemaAux + "' duplicada");
                                erroresWriter.println("Error semántico: Variable '" + lexemaAux + "' duplicada.");
                                // Devolvemos la posición real para que el semantico pueda continuar
                                idPos = gestor.getEntradaTS(lexemaAux);
                            }
                            return new Token("id", idPos);
                        }    
                        // Si no estamos declarando variables
                        else {
                            int pos = gestor.getEntradaTS(lexemaAux);
                        
                            // Buscamos la variable en la tabla global si no la hemos encontrado en la local
                            if (pos == 0 && this.dentroFuncion) {
                                    Integer posGlobal = gestor.getEntradaTSGlobal(lexemaAux);
                                    if (posGlobal != null && posGlobal != 0) {
                                        pos = posGlobal;
                                    }
                                }
                            // En el caso de que la variable no haya sido declarada seguimos las reglas de MYJS y la añadimos
                            // en la TS global
                            if(pos == 0) {
                                pos = gestor.addEntradaTSGlobal(lexemaAux);
                                gestor.setTipo(pos, "entero");
                                gestor.setValorAtributoCad(pos, "clase", "VAR");
                            }
                            return new Token("id", pos); 
                        }
                    }
                            
                    case 102: return new Token("constanteEntera", lexemaAux);
                    case 103: return new Token("constanteReal", lexemaAux);
                    case 104: return new Token("asignacionSuma", null);
                    case 105: return new Token("suma", null);
                    case 106: return new Token("and", null);
                    case 107: return new Token("cadena", "\"" + lexemaAux.toString() + "\"");
                    case 109: return new Token("asignacion", null);
                    case 110: return new Token("puntoComa", null);
                    case 111: return new Token("llaveIzquierda", null);
                    case 112: return new Token("llaveDerecha", null);
                    case 113: return new Token("parentesisIzquierdo", null);
                    case 114: return new Token("parentesisDerecho", null);
                    case 115: return new Token("coma", null);
                    case 116: return new Token("menor", null);
                    case 117: return new Token("EOF", null);
                        
                }
            // case 7 y 4 también hacen lo mismo con la excepción de que 7 lee
            case 7:  
                leer();
            case 4:  
                lexemaAux = lexema;
                lexema = "";
                switch(estado){
                    case -1: return error("carácter no valido: '" + c + "'");
                    case -2: return error("real mal formado: " + lexemaAux);
                    case -3: return error("carácter no valido: '" + c + "'");
                    case -4: return error("Comentario sin cerrar");
                    case -5: return error("Carácter no valido: '" + c + "'");

                }
            case 5:
                leer();
                return null;
        }
        return null;
    }

    public Token siguienteToken() throws IOException {

        int estado = 0;
        lexema = "";
        int accion = 0;
        while (true) {
            int col = claseCaracter(car);

            accion = acc[estado][col];
            estado = trans[estado][col];
            
            Token t = ejecutarAccion(accion, estado);
            // En el caso de que sea un comentario volvemos a empezar
            if (accion == 5) {
                estado = 0;
                lexema = "";
            }
            if (t != null) {
                    if (t.getCodigo().startsWith("ERROR")) {
                        erroresWriter.println(t.toString());
                        erroresWriter.flush();
                    }
                    else{
                        tokensWriter.println(t.toString());
                        tokensWriter.flush(); 
                    }

                    // Cerramos los ficheros
                    if (t.getCodigo().equals("EOF")) {
                        tokensWriter.close();
                        System.out.println("Fichero generado: tokens.txt");
                        erroresWriter.close();
                        System.out.println("Fichero generado: errores_lexico.txt");
                    }

                return t;
            }
        }
    }

    public void setZonaDecl(boolean decl){
        this.zonaDecl = decl;
    }
    
    public void setDentroFuncion(boolean dentro){
        this.dentroFuncion = dentro;
    }
    public boolean getDentroFuncion() {
    	return dentroFuncion;
    }
    public Iterator<String> palabrasReservadas(){
        return PALABRAS.iterator();
    }

    public int getLinea(){
        return this.linea;
    }
    
}