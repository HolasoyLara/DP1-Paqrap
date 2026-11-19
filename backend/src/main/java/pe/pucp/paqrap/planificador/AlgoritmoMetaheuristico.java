package pe.pucp.paqrap.planificador;

/**
 * Contrato comun de los algoritmos de planificacion (patron Strategy).
 */
public interface AlgoritmoMetaheuristico {

    Plan resolver(ModeloRuteo modelo);

    /** Nombre corto del algoritmo ("GA" o "ACO") para reportes y bitacora. */
    String getNombre();
}
