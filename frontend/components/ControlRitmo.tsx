'use client';

import type { EstadoVisual } from '@/lib/tipos';

/** Velocidades disponibles, relativas al ritmo base del escenario (x1 = 5 dias en la duracion objetivo / factor del colapso). */
export const VELOCIDADES = [0.25, 0.5, 1, 2, 4, 8];

export type CambioRitmo = { velocidad?: number; pausada?: boolean };

interface Props {
  estado: EstadoVisual | null;
  ocupado: boolean;
  onCambiar: (cambio: CambioRitmo) => void;
  /** Version mas compacta para la barra flotante del mapa. */
  compacto?: boolean;
}

export function textoVelocidad(v: number): string {
  return v === 0.25 ? '¼' : v === 0.5 ? '½' : String(v);
}

/** El ritmo solo se controla en la simulacion del periodo y en el colapso (el dia a dia sigue al reloj real). */
export function ritmoControlable(estado: EstadoVisual | null): boolean {
  return !!estado && estado.estadoCorrida === 'EN_CURSO' && estado.escenario !== 'TIEMPO_REAL';
}

/** Pausar/reanudar y bajar/subir la velocidad de la corrida en curso. */
export default function ControlRitmo({ estado, ocupado, onCambiar, compacto }: Props) {
  if (!ritmoControlable(estado)) return null;
  const e = estado!;
  const v = e.velocidad ?? 1;
  // indice de la velocidad actual (o la mas cercana si vino un valor intermedio)
  let i = VELOCIDADES.indexOf(v);
  if (i < 0) i = VELOCIDADES.reduce((mejor, x, k) => (Math.abs(x - v) < Math.abs(VELOCIDADES[mejor] - v) ? k : mejor), 0);
  const menor = i > 0 ? VELOCIDADES[i - 1] : null;
  const mayor = i < VELOCIDADES.length - 1 ? VELOCIDADES[i + 1] : null;
  return (
    <div className={'control-ritmo' + (compacto ? ' compacto' : '')} role="group" aria-label="Velocidad de la simulación">
      <button type="button" className={'btn-ritmo' + (e.pausada ? ' activo' : '')} disabled={ocupado}
        onClick={() => onCambiar({ pausada: !e.pausada })}
        title={e.pausada ? 'Reanudar la simulación' : 'Pausar la simulación'} aria-label={e.pausada ? 'Reanudar' : 'Pausar'}>
        {e.pausada ? '▶' : '❚❚'}
      </button>
      <button type="button" className="btn-ritmo" disabled={ocupado || menor == null}
        onClick={() => menor != null && onCambiar({ velocidad: menor })}
        title={menor != null ? `Más lento (×${textoVelocidad(menor)})` : 'Velocidad mínima'} aria-label="Más lento">−</button>
      <span className="ritmo-valor" title="Velocidad relativa al ritmo base del escenario">
        {e.pausada ? 'Pausa' : `×${textoVelocidad(VELOCIDADES[i])}`}
      </span>
      <button type="button" className="btn-ritmo" disabled={ocupado || mayor == null}
        onClick={() => mayor != null && onCambiar({ velocidad: mayor })}
        title={mayor != null ? `Más rápido (×${textoVelocidad(mayor)})` : 'Velocidad máxima'} aria-label="Más rápido">+</button>
    </div>
  );
}
