package pe.pucp.paqrap.simulacion;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import pe.pucp.paqrap.visualizador.EstadoVisual;

import java.util.Locale;
import java.util.function.BiConsumer;

/**
 * Corrida visual en marcha: un hilo que mueve el reloj de la simulacion al
 * ritmo del escenario y publica el estado para el visualizador.
 *
 * - Simulacion del periodo: 5 dias simulados en la duracion objetivo (30-60
 *   min reales, B22).
 * - Dia a dia: el reloj avanza 1:1 con el reloj real (D12).
 * - Colapso: reloj acelerado (factor configurable) sin limite; termina solo al
 *   detectar el colapso (C14, D14).
 *
 * En la simulacion del periodo y en el colapso el usuario puede cambiar el
 * ritmo durante la corrida: una velocidad relativa al ritmo base del escenario
 * (x0.25 a x8) y pausar/reanudar. El reloj avanza por pasos (tiempo real del
 * paso x factor base x velocidad), asi un cambio de ritmo aplica desde el paso
 * siguiente sin saltos. El dia a dia siempre corre 1:1.
 */
public final class Corrida implements Runnable {

    private static final Logger LOG = LoggerFactory.getLogger(Corrida.class);
    private static final long PASO_MS = 100;
    private static final long PUBLICACION_MS = 250;
    public static final double VELOCIDAD_MIN = 0.25;
    public static final double VELOCIDAD_MAX = 8.0;

    private final MotorSimulacion motor;
    private final double factor;
    private final BiConsumer<Corrida, CierreCorrida> alTerminar;
    private volatile boolean detenerSolicitado;
    private volatile String estado = "EN_CURSO";
    private volatile EstadoVisual ultimoEstado;
    private volatile CierreCorrida cierre;
    private volatile String error;
    private volatile double velocidad = 1.0;
    private volatile boolean pausada;
    private final long inicioRealMs;
    private volatile long finRealMs;
    private long ultimaPublicacion;

    public Corrida(MotorSimulacion motor, BiConsumer<Corrida, CierreCorrida> alTerminar) {
        this.motor = motor;
        this.factor = motor.getParametros().factorAceleracion();
        this.alTerminar = alTerminar;
        this.inicioRealMs = System.currentTimeMillis();
        publicar(true);
    }

    @Override
    public void run() {
        double horaSimulada = motor.getHoraInicio();
        long previo = System.nanoTime();
        try {
            while (!detenerSolicitado) {
                long inicioPaso = System.nanoTime();
                double transcurridoH = (inicioPaso - previo) / 3.6e12;
                previo = inicioPaso;
                if (!pausada) {
                    horaSimulada += transcurridoH * factor * velocidad;
                    motor.avanzarHasta(horaSimulada);
                }
                publicar(false);
                if (motor.estaFinalizado()) break;
                long dormir = PASO_MS - (System.nanoTime() - inicioPaso) / 1_000_000L;
                if (dormir > 0) Thread.sleep(dormir);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } catch (Throwable e) {
            LOG.error("La corrida {} se detuvo por un error", motor.getIdCorrida(), e);
            error = e.getClass().getSimpleName() + ": " + e.getMessage();
        }
        terminar();
    }

    private void terminar() {
        if (!motor.estaFinalizado()) motor.finalizar(error != null ? "ERROR" : "DETENIDA_MANUAL");
        String motivo = motor.getMotivoFin();
        estado = "DETENIDA_MANUAL".equals(motivo) || "ERROR".equals(motivo) ? "DETENIDA" : "FINALIZADA";
        finRealMs = System.currentTimeMillis();
        publicar(true);
        try {
            cierre = motor.generarCierre(estado, inicioRealMs, finRealMs);
        } catch (RuntimeException e) {
            LOG.error("No se pudo generar el cierre de la corrida {}", motor.getIdCorrida(), e);
        }
        if (alTerminar != null && cierre != null) alTerminar.accept(this, cierre);
    }

    private void publicar(boolean forzar) {
        long ahora = System.currentTimeMillis();
        if (!forzar && ahora - ultimaPublicacion < PUBLICACION_MS) return;
        ultimaPublicacion = ahora;
        boolean enCurso = "EN_CURSO".equals(estado) && !motor.estaFinalizado();
        boolean enPausa = enCurso && pausada;
        double factorEfectivo = enCurso && !enPausa ? factor * velocidad : 0;
        long realSeg = ((finRealMs > 0 ? finRealMs : ahora) - inicioRealMs) / 1000;
        EstadoVisual ev = motor.construirEstadoVisual(enCurso ? "EN_CURSO" : estado, factorEfectivo, realSeg);
        String mensaje = error != null ? "La corrida se detuvo por un error: " + error : null;
        ultimoEstado = ev.conRitmo(factorEfectivo, velocidad, enPausa, mensaje);
    }

    /**
     * Cambia el ritmo de la corrida: velocidad relativa al ritmo base del escenario (se acota a x0.25 - x8)
     * y/o pausa. Un valor nulo deja ese aspecto como esta.
     */
    public void cambiarRitmo(Double nuevaVelocidad, Boolean nuevaPausa) {
        if (nuevaVelocidad != null) {
            if (nuevaVelocidad.isNaN() || nuevaVelocidad <= 0) throw new IllegalArgumentException("La velocidad debe ser mayor que 0.");
            velocidad = Math.max(VELOCIDAD_MIN, Math.min(VELOCIDAD_MAX, nuevaVelocidad));
        }
        if (nuevaPausa != null) pausada = nuevaPausa;
        LOG.info("Corrida {}: ritmo x{} ({})", motor.getIdCorrida(), String.format(Locale.ROOT, "%.2f", velocidad),
                pausada ? "en pausa" : "en marcha");
        publicar(true);
    }

    /** Fuerza una nueva publicacion (despues de un comando del usuario). */
    public void refrescar() {
        publicar(true);
    }

    public void solicitarDetencion() { detenerSolicitado = true; }
    public boolean estaEnCurso() { return "EN_CURSO".equals(estado); }
    public MotorSimulacion getMotor() { return motor; }
    public EstadoVisual getUltimoEstado() { return ultimoEstado; }
    public CierreCorrida getCierre() { return cierre; }
    public String getEstado() { return estado; }
    public long getInicioRealMs() { return inicioRealMs; }
    public long getFinRealMs() { return finRealMs; }
    public String getError() { return error; }
    public double getVelocidad() { return velocidad; }
    public boolean estaPausada() { return pausada; }
}
