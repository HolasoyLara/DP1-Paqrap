package pe.pucp.paqrap.planificador;

import java.util.List;

/**
 * Traslado de carga en transito hacia el cliente mas critico. Como el producto es unico,
 * una unidad en camino puede entregar a un cliente sin unidad (por ejemplo, por una averia)
 * los paquetes que llevaba para otro con mas holgura; ese segundo pedido se vuelve a
 * planificar desde un almacen dentro de su plazo. El motor ejecuta el cambio y esta clase
 * deja la constancia.
 */
public class ReasignacionCarga extends EventoReplanificacion {
    private final String pedidoOrigenTransito;
    private final String pedidoDestinoCritico;
    private final String motivo;

    public ReasignacionCarga(double fechaHora, String pedidoOrigenTransito, String pedidoDestinoCritico,
                             String vehiculo, String motivo, String detalle) {
        super(fechaHora, List.of(vehiculo), List.of(pedidoOrigenTransito, pedidoDestinoCritico), detalle);
        this.pedidoOrigenTransito = pedidoOrigenTransito;
        this.pedidoDestinoCritico = pedidoDestinoCritico;
        this.motivo = motivo;
    }

    @Override
    public String getCausa() { return "REASIGNACION"; }

    public String dejarConstanciaAuditable() {
        return String.format("Reasignacion de carga: %s -> %s (motivo: %s)", pedidoOrigenTransito, pedidoDestinoCritico, motivo);
    }

    public String getPedidoOrigenTransito() { return pedidoOrigenTransito; }
    public String getPedidoDestinoCritico() { return pedidoDestinoCritico; }
    public String getMotivo() { return motivo; }
}
