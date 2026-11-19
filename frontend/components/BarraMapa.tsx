'use client';

import { useEffect, useState } from 'react';
import type { EstadoVisual } from '@/lib/tipos';
import {
  ahoraLimaTexto, BORDE_VACIO, COLOR_OCUPACION, ETIQUETA_OCUPACION, fechaHoraCompleta, numero, semaforoOcupacion, transcurridoReal,
  transcurridoSimulado,
} from '@/lib/formato';
import { ocupacionesGlobales } from './TopBar';
import ControlRitmo, { type CambioRitmo } from './ControlRitmo';

interface Props {
  estado: EstadoVisual | null;
  minutoAhora: () => number;
  puedeIniciar: boolean;
  ocupado: boolean;
  onIniciar: () => void;
  onDetener: () => void;
  onRitmo: (cambio: CambioRitmo) => void;
  infoAbierta: boolean;
  onInfo: () => void;
}

const ESTADO_TEXTO: Record<string, string> = {
  SIN_CORRIDA: 'Sin corrida',
  EN_CURSO: 'En curso',
  FINALIZADA: 'Finalizada',
  DETENIDA: 'Detenida',
};

/**
 * Barra minima sobre el mapa a pantalla completa: estado, los 4 tiempos (fecha-hora simulada,
 * transcurrido simulado, fecha-hora actual y transcurrido real) siempre visibles, y los controles de la corrida.
 */
export default function BarraMapa({ estado, minutoAhora, puedeIniciar, ocupado, onIniciar, onDetener, onRitmo, infoAbierta, onInfo }: Props) {
  const [, setTic] = useState(0);
  // la hora actual solo existe en el navegador: se muestra despues de montar para no desajustar la pagina estatica
  const [montado, setMontado] = useState(false);
  const [base, setBase] = useState({ seg: 0, desde: 0 });
  useEffect(() => {
    setMontado(true);
    const t = setInterval(() => setTic((x) => x + 1), 500);
    return () => clearInterval(t);
  }, []);
  useEffect(() => {
    if (estado) setBase({ seg: estado.tiempoRealSeg, desde: performance.now() });
  }, [estado]);
  const enCurso = estado?.estadoCorrida === 'EN_CURSO';
  const hay = !!estado && estado.estadoCorrida !== 'SIN_CORRIDA';
  const minuto = hay ? minutoAhora() : 0;
  const segundosReales = estado ? (enCurso ? base.seg + (performance.now() - base.desde) / 1000 : estado.tiempoRealSeg) : 0;
  // Ocupacion global de la flota y de los almacenes, siempre visible con su semaforo
  const ocup = estado && hay ? ocupacionesGlobales(estado) : null;
  const pildora = (etiqueta: string, pct: number, detalle: string) => {
    const sem = semaforoOcupacion(pct, estado!.umbralVerde, estado!.umbralAmbar);
    return (
      <span className="barra-ocup" title={`${etiqueta}: ${detalle} · ${ETIQUETA_OCUPACION[sem]}`}>
        <i style={{ background: COLOR_OCUPACION[sem], borderColor: sem === 'vacio' ? BORDE_VACIO : COLOR_OCUPACION[sem] }} />
        <em>{etiqueta}</em><b>{numero(pct, 0)} %</b>
      </span>
    );
  };
  return (
    <div className="barra-mapa" onPointerDown={(e) => e.stopPropagation()}>
      <span className={'barra-estado' + (enCurso ? ' en-curso' : '')}>{estado ? (enCurso && estado.pausada ? 'En pausa' : ESTADO_TEXTO[estado.estadoCorrida]) : 'Conectando…'}</span>
      {ocup && (
        <div className="barra-ocupaciones">
          {pildora('Flota', ocup.flota.pct, `${numero(ocup.flota.carga)} / ${numero(ocup.flota.cap)} paquetes en unidades operativas`)}
          {pildora('Almacenes', ocup.almacenes.pct, `${numero(ocup.almacenes.stock)} / ${numero(ocup.almacenes.cap)} en intermedios (central sin límite)`)}
        </div>
      )}
      {/* Los 4 tiempos se muestran en 5 días y colapso, no en la operación día a día */}
      {estado?.escenario !== 'TIEMPO_REAL' && <div className="barra-tiempos" aria-live="polite">
        <span><em>Simulado</em><b>{hay && estado ? fechaHoraCompleta(estado.inicioMs + minuto * 60000) : '—'}</b></span>
        <span><em>Transcurrido sim.</em><b>{hay ? transcurridoSimulado(minuto) : '—'}</b></span>
        <span><em>Actual</em><b suppressHydrationWarning>{montado ? ahoraLimaTexto() : '—'}</b></span>
        <span><em>Transcurrido real</em><b>{hay ? transcurridoReal(segundosReales) : '—'}</b></span>
      </div>}
      <button className={'btn-flot' + (infoAbierta ? ' activo' : '')} onClick={onInfo} title="Ocupación global, escenario e información de la corrida">Información</button>
      <ControlRitmo estado={estado} ocupado={ocupado} onCambiar={onRitmo} compacto />
      <button className="btn btn-sm btn-start" disabled={!puedeIniciar || ocupado || enCurso} onClick={onIniciar}>Iniciar</button>
      <button className="btn btn-sm btn-stop" disabled={!enCurso || ocupado} onClick={onDetener}>Detener</button>
    </div>
  );
}

/**
 * Panel «Información» del mapa: solo lo que no esta en la barra (escenario, algoritmo, semilla, progreso, ritmo y
 * el detalle en paquetes de la ocupacion global).
 */
export function InfoCorrida({ estado, escenarioTexto, algoritmoTexto, onCerrar }: {
  estado: EstadoVisual | null; escenarioTexto: string; algoritmoTexto: string; onCerrar: () => void;
}) {
  const hay = !!estado && estado.estadoCorrida !== 'SIN_CORRIDA';
  const enCurso = estado?.estadoCorrida === 'EN_CURSO';
  const ocup = estado && hay ? ocupacionesGlobales(estado) : null;
  const mayus = (s: string) => (s ? s.charAt(0).toUpperCase() + s.slice(1) : '—');
  const filas: [string, string][] = [['Escenario', mayus(escenarioTexto)], ['Algoritmo', algoritmoTexto || '—']];
  if (estado && hay) {
    filas.push(['Semilla', String(estado.semilla)]);
    filas.push(['Progreso', estado.totalDias ? `Día ${estado.diaActual} de ${estado.totalDias}` : `Día ${estado.diaActual}`]);
    filas.push(['Inicio simulado', fechaHoraCompleta(estado.inicioMs)]);
    if (estado.escenario !== 'TIEMPO_REAL') {
      const base = estado.velocidad > 0 ? estado.factor / estado.velocidad : 0;
      filas.push(['Ritmo', !enCurso ? '—' : estado.pausada ? 'En pausa'
        : estado.velocidad === 1 ? `${formatoNumero(estado.factor)} h simuladas por h real (velocidad base)`
          : `×${formatoNumero(estado.velocidad)} · ${formatoNumero(estado.factor)} h simuladas por h real (base ${formatoNumero(base)})`]);
    }
  }
  if (ocup && estado) {
    const sf = semaforoOcupacion(ocup.flota.pct, estado.umbralVerde, estado.umbralAmbar);
    const sa = semaforoOcupacion(ocup.almacenes.pct, estado.umbralVerde, estado.umbralAmbar);
    filas.push(['Carga de la flota', `${numero(ocup.flota.carga)} / ${numero(ocup.flota.cap)} paquetes · ${ETIQUETA_OCUPACION[sf]}`]);
    filas.push(['Stock de intermedios', `${numero(ocup.almacenes.stock)} / ${numero(ocup.almacenes.cap)} · ${ETIQUETA_OCUPACION[sa]}`]);
  }
  return (
    <div className="panel-flot info-flot" role="dialog" aria-label="Información de la corrida" onPointerDown={(e) => e.stopPropagation()}>
      <div className="pf-cab">
        <h4>Información de la corrida</h4>
        <button type="button" className="pf-cerrar" onClick={onCerrar} aria-label="Cerrar información">×</button>
      </div>
      <dl className="info-lista">
        {filas.map(([k, v]) => (
          <div key={k}><dt>{k}</dt><dd>{v}</dd></div>
        ))}
      </dl>
    </div>
  );
}

function formatoNumero(v: number): string {
  return v >= 10 ? numero(Math.round(v)) : v === 0.25 ? '¼' : v === 0.5 ? '½' : numero(v, v % 1 ? 1 : 0);
}
