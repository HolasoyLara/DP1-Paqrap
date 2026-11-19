package pe.pucp.paqrap.planificador;

import java.util.List;

/**
 * Replanificacion por una calle bloqueada: se registra cuando un bloqueo empieza con
 * unidades en camino que iban a pasar por el, o cuando el usuario cierra un tramo desde el
 * mapa. La unidad recalcula su recorrido desde donde esta, esquivando el bloqueo.
 */
public class ReplanificacionPorBloqueo extends EventoReplanificacion {
    private final String bloqueoOrigen;

    public ReplanificacionPorBloqueo(double fechaHora, String bloqueoOrigen, List<String> vehiculosAfectados,
                                     List<String> pedidosAfectados, String detalle) {
        super(fechaHora, vehiculosAfectados, pedidosAfectados, detalle);
        this.bloqueoOrigen = bloqueoOrigen;
    }

    @Override
    public String getCausa() { return "BLOQUEO"; }

    public String getBloqueoOrigen() { return bloqueoOrigen; }
}
