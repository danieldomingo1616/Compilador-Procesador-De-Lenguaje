/* PRUEBA 3: Complejidad Sintáctica */

let int a;
let int b;
let boolean resultado;

function int duplicar(int v) {
    return v + v;
}

function int cuadrado(int v) {
    return v + v; 
}

function int sumarTres(int v1, int v2, int v3) {
    return v1 + v2 + v3;
}

function boolean complejo(int v) {
    /* Retorna true si v está entre 10 y 20 */
    return (9 < v) && (v < 21);
}

function void main(void) {
    a = 5;
    b = 10;

    /* Llamadas ANIDADAS: sumarTres( duplicar(5), 10, duplicar(10) ) */
    /* Debe dar: 10 + 10 + 20 = 40 */
    a = sumarTres(duplicar(a), b, duplicar(b)); 
    
    write "Resultado anidado (40):";
    write a;

    /* Lógica booleana compleja */
    /* (40 > 0) AND (40 < 100) AND (true) */
    resultado = (0 < a) && (a < 100) && (1 < 2);

    if (resultado) {
        write "La lógica booleana funciona";
    }

    /* Bucle con expresión compleja */
    /* Mientras a (40) sea mayor que 0 */
    while (0 < a) {
        /* a = a + 1 (usando sumas negativas si tienes resta, o ajusta según tus ops) */
        a += 1; 
    }
    write "Terminado bucle";
}