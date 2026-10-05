import { useEffect, useState } from 'react'

function horaActual() {
  const now = new Date()
  const hh = String(now.getHours()).padStart(2, '0')
  const mm = String(now.getMinutes()).padStart(2, '0')
  return `${hh}:${mm}`
}

export default function GeneralExitModal({
  open,
  onClose,
  onConfirm,
  loading,
  usuario,
}) {
  const [hora, setHora] = useState(horaActual())
  const [motivo, setMotivo] = useState('')
  const [error, setError] = useState('')

  useEffect(() => {
    if (open) {
      setHora(horaActual())
      setMotivo('')
      setError('')
    }
  }, [open])

  if (!open) return null

  async function confirmar() {
    setError('')

    if (!hora) {
      setError('Captura la hora de salida general.')
      return
    }

    if (!motivo.trim()) {
      setError('Captura el motivo de la salida general.')
      return
    }

    await onConfirm({
      tipo: 'GENERAL',
      horaSalidaGeneral: hora,
      idUsuarioAutoriza: usuario.idUsuario,
      motivo: motivo.trim(),
    })
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/35 px-4 backdrop-blur-sm">
      <div className="w-full max-w-md overflow-hidden rounded-3xl border border-[#eadfce] bg-white shadow-2xl">
        <div className="border-b border-[#eadfce] bg-gradient-to-r from-cetis-wine to-[#b32a40] p-5 text-white">
          <p className="text-xs font-black uppercase tracking-[0.18em] text-white/75">
            Toda la escuela
          </p>
          <h2 className="mt-1 text-2xl font-black">
            Autorizar salida general
          </h2>
        </div>

        <div className="space-y-4 p-5">
          <div className="rounded-2xl border border-amber-200 bg-amber-50 p-4 text-sm leading-6 text-amber-900">
            Los alumnos que hayan registrado entrada hoy aparecerán con esta
            hora de salida, sin modificar masivamente sus registros.
          </div>

          <div>
            <label className="form-label" htmlFor="hora-general">
              Hora de salida general
            </label>
            <input
              id="hora-general"
              type="time"
              className="form-input"
              value={hora}
              onChange={(event) => setHora(event.target.value)}
            />
          </div>

          <div>
            <label className="form-label" htmlFor="motivo-general">
              Motivo
            </label>
            <textarea
              id="motivo-general"
              className="form-input min-h-24 resize-y"
              placeholder="Ej. Reunión docente"
              value={motivo}
              onChange={(event) => setMotivo(event.target.value)}
            />
          </div>

          <div className="rounded-2xl bg-cetis-cream p-4 text-sm text-cetis-muted">
            Autorizado por:{' '}
            <b className="text-cetis-ink">
              {usuario.nombreUsuario || usuario.idUsuario}
            </b>
          </div>

          {error && (
            <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
              {error}
            </div>
          )}

          <div className="grid grid-cols-2 gap-3 pt-1">
            <button
              type="button"
              onClick={onClose}
              className="btn-secondary"
              disabled={loading}
            >
              Cancelar
            </button>

            <button
              type="button"
              onClick={confirmar}
              className="btn-primary"
              disabled={loading}
            >
              {loading ? 'Procesando...' : 'Confirmar salida'}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
