package pe.pucp.paqrap.visualizador;

import java.util.List;

/**
 * Detalle completo de un vehiculo, pedido o almacen: es
 * lo que muestra el modal "ampliado" del visualizador, con su historial de eventos.
 */
public final class DetalleVisual {

    private DetalleVisual() {
    }

    public record Evento(String fecha, double minuto, String tipo, String detalle) {
    }

    public record ParadaDetalle(int secuencia, String pedido, String cliente, double x, double y, int cantidad,
                                String llegada, String salida, String limite, String estado, boolean aTiempo) {
    }

    public record RutaDetalle(String id, String causa, String salida, String regresoEstimado, String almacenSalida,
                              String almacenRetorno, int carga, List<ParadaDetalle> paradas) {
    }

    /**
     * Ruta ya recorrida, para dibujarla a demanda en el mapa con sus escalas. La trayectoria es plana
     * [x, y, minuto, x, y, minuto, ...] (minutos simulados desde el inicio de la corrida) y llega solo hasta
     * donde la unidad la recorrio (cierre de la ruta o regreso al almacen).
     */
    public record RutaAnterior(String id, String vehiculo, String causa, String salida, String fin, String almacenSalida,
                               String almacenRetorno, double km, List<ParadaDetalle> paradas, double[] trayectoria) {
    }

    public record Vehiculo(String id, String tipo, String estado, double x, double y, String almacenBase,
                           int capacidad, int carga, double velocidad, double costoKm, List<String> horasAlimentacion,
                           String turnoActual, String proximoMantenimiento, String averia, RutaDetalle ruta,
                           List<String> pedidosABordo, String proximaParada, String eta, int rutasRealizadas,
                           int entregas, double km, double costo, List<Evento> eventos, List<RutaAnterior> rutasAnteriores) {
    }

    public record Parte(String id, int cantidad, String estado) {
    }

    public record Pedido(String id, String cliente, double x, double y, int cantidad, String tipoEntrega, double plazo,
                         String registro, String limite, String estado, double holgura, List<String> vehiculos,
                         List<String> almacenes, List<String> rutas, String entregaReal, Boolean aTiempo,
                         boolean reprogramado, String canal, String observaciones, List<Parte> partes,
                         boolean modificable, List<Evento> eventos, List<RutaAnterior> rutasAnteriores) {
    }

    public record Almacen(String id, String nombre, double x, double y, boolean central, Integer capacidad,
                          Integer stock, Double porcentaje, int despachadoHoy, String recarga,
                          List<String> vehiculosEnAlmacen, List<String> vehiculosEnCamino) {
    }
}
