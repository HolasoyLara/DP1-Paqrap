package pe.pucp.paqrap.registro;

import pe.pucp.paqrap.comun.Coordenada;

/**
 * Cliente con su ubicacion. Es inmutable; su historial se obtiene filtrando los pedidos de
 * la corrida por idCliente.
 */
public class Cliente {
    private final String idCliente;
    private final String nombre;
    private final Coordenada coordenadas;

    public Cliente(String idCliente, String nombre, Coordenada coordenadas) {
        this.idCliente = idCliente;
        this.nombre = nombre;
        this.coordenadas = coordenadas;
    }

    public String getIdCliente() { return idCliente; }
    public String getNombre() { return nombre; }
    public Coordenada getCoordenadas() { return coordenadas; }

    @Override
    public String toString() {
        return "Cliente#" + idCliente + "[" + nombre + ", " + coordenadas + "]";
    }
}
