public class Main {
    public static void main(String[] args) {
        try {
            String fuente = "prueba.javascript";

           AnalizadorSintacticoSemantico analizador = new AnalizadorSintacticoSemantico(fuente);

            analizador.inicializarTabla();

            // Llamamos al méteodo del Axioma y empezamos la creación del fichero del parse
            analizador.parseA();
            
            // Cerramos el fichero del parse
            analizador.cerrar();

        } catch (Exception e) {
            System.out.println("El análisis se detuvo por un error.");
            e.printStackTrace();
        }
    }
}