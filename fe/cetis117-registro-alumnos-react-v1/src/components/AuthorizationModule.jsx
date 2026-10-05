import { useState } from 'react'
import { CheckIcon, SearchIcon } from './Icons'

export default function AuthorizationModule({
  codigo,
  usuario,
  grupos,
  searchStudent,
  createAuthorization,
  generalActiva = false,
}) {
  function getNombreGrupo(idGrupo) {
    const grupo = grupos.find(
      (item) => String(item.idGrupo) === String(idGrupo),
    )

    return grupo?.nombre || idGrupo || '—'
  }

  const [tipo, setTipo] = useState('ALUMNO')
  const [iniciales, setIniciales] = useState('')
  const [alumno, setAlumno] = useState(null)
  const [idGrupo, setIdGrupo] = useState('')
  const [motivo, setMotivo] = useState('')
  const [loading, setLoading] = useState(false)
  const [message, setMessage] = useState(null)

  async function buscarAlumno() {
    setMessage(null)
    setAlumno(null)

    const value = iniciales.trim().toUpperCase()

    if (!value) {
      setMessage({
        type: 'error',
        text: 'Ingresa las iniciales del alumno.',
      })
      return
    }

    setLoading(true)

    try {
      const found = await searchStudent(value)

      if (!found) {
        setMessage({
          type: 'error',
          text: 'No se encontró un alumno con esas iniciales.',
        })
        return
      }

      setAlumno(found)
    } catch (error) {
      setMessage({
        type: 'error',
        text: error.message,
      })
    } finally {
      setLoading(false)
    }
  }

  async function autorizar() {
    setMessage(null)

    if (!motivo.trim()) {
      setMessage({
        type: 'error',
        text: 'Captura el motivo de la autorización.',
      })
      return
    }

    if (tipo === 'ALUMNO' && !alumno) {
      setMessage({
        type: 'error',
        text: 'Primero busca y selecciona al alumno.',
      })
      return
    }

    if (tipo === 'GRUPO' && !idGrupo) {
      setMessage({
        type: 'error',
        text: 'Selecciona el grupo.',
      })
      return
    }

    setLoading(true)

    try {
      const payload =
        tipo === 'ALUMNO'
          ? {
              tipo: 'ALUMNO',
              iniciales: alumno.iniciales,
              idUsuarioAutoriza: usuario.idUsuario,
              motivo: motivo.trim(),
            }
          : {
              tipo: 'GRUPO',
              idGrupo,
              idUsuarioAutoriza: usuario.idUsuario,
              motivo: motivo.trim(),
            }

      const response = await createAuthorization(payload)

      setMessage({
        type:
          response.resultado?.includes('EXISTENTE')
            ? 'warning'
            : 'success',
        text:
          response.mensaje ||
          'Autorización registrada correctamente.',
      })

      if (!response.resultado?.includes('EXISTENTE')) {
        setMotivo('')
      }

    } catch (error) {
      setMessage({
        type: 'error',
        text: error.message,
      })
    } finally {
      setLoading(false)
    }
  }

  function cambiarTipo(value) {
    setTipo(value)
    setMessage(null)
    setAlumno(null)
    setIniciales('')
    setIdGrupo('')
    setMotivo('')
  }

  return (
    <section className="app-card overflow-hidden">
      <div className="border-b border-[#eadfce] p-5 sm:p-6">
        <h2 className="section-title">Autorizar salida</h2>
        <p className="mt-1 text-sm text-cetis-muted">
          Autoriza de forma individual o para todo un grupo.
        </p>
      </div>

      <div className="space-y-5 p-5 sm:p-6">
        {generalActiva && (
          <div className="rounded-2xl border border-fuchsia-200 bg-fuchsia-50 p-4 text-sm leading-6 text-fuchsia-800">
            <b>Salida general activa.</b> Ya no es necesario registrar
            autorizaciones por alumno o por grupo para el día de hoy.
          </div>
        )}
        <div>
          <span className="form-label">Tipo de salida</span>
          <div className="grid grid-cols-2 gap-2">
            {['ALUMNO', 'GRUPO'].map((value) => (
              <button
                key={value}
                type="button"
                onClick={() => cambiarTipo(value)}
                disabled={generalActiva}
                className={`rounded-xl border px-4 py-3 text-sm font-extrabold transition ${
                  tipo === value
                    ? 'border-cetis-wine bg-cetis-wine text-white'
                    : 'border-[#dfd3c4] bg-white text-cetis-wine hover:bg-cetis-cream'
                }`}
              >
                {value === 'ALUMNO' ? 'Alumno' : 'Grupo'}
              </button>
            ))}
          </div>
        </div>

        {tipo === 'ALUMNO' ? (
          <div>
            <label className="form-label" htmlFor="iniciales">
              Iniciales del alumno
            </label>
            <div className="flex gap-2">
              <input
                id="iniciales"
                className="form-input uppercase"
                placeholder="Ej. MSGG"
                value={iniciales}
                onChange={(event) => {
                  setIniciales(event.target.value.toUpperCase())
                  setAlumno(null)
                  setMessage(null)
                }}
                disabled={generalActiva}
              />
              <button
                type="button"
                className="btn-secondary shrink-0 gap-2"
                onClick={buscarAlumno}
                disabled={loading || generalActiva}
              >
                <SearchIcon />
                <span className="hidden sm:inline">Buscar</span>
              </button>
            </div>

            {alumno && (
              <div className="mt-3 rounded-2xl border border-emerald-200 bg-emerald-50 p-4">
                <p className="font-black text-emerald-800">
                  {alumno.nombreCompleto}
                </p>
                <p className="mt-1 text-sm text-emerald-700">
                  {alumno.iniciales} · {getNombreGrupo(alumno.idGrupo)}
                </p>
              </div>
            )}
          </div>
        ) : (
          <div>
            <label className="form-label" htmlFor="grupo">
              Grupo
            </label>
            <select
              id="grupo"
              className="form-input"
              value={idGrupo}
              onChange={(event) => {
                setIdGrupo(event.target.value)
                setMessage(null)
              }}
              disabled={generalActiva}
            >
              <option value="">Selecciona un grupo</option>
              {grupos.map((grupo) => (
                <option key={grupo.idGrupo} value={grupo.idGrupo}>
                  {grupo.nombre}
                </option>
              ))}
            </select>
          </div>
        )}

        <div>
          <label className="form-label" htmlFor="motivo">
            Motivo
          </label>
          <textarea
            id="motivo"
            className="form-input min-h-24 resize-y"
            placeholder="Ej. Solicitud del tutor"
            value={motivo}
            onChange={(event) => setMotivo(event.target.value)}
            disabled={generalActiva}
          />
        </div>

        {message && (
          <div
            className={`rounded-xl border px-4 py-3 text-sm font-semibold ${
              message.type === 'success'
                ? 'border-emerald-200 bg-emerald-50 text-emerald-700'
                : message.type === 'warning'
                  ? 'border-amber-200 bg-amber-50 text-amber-800'
                  : 'border-red-200 bg-red-50 text-red-700'
            }`}
          >
            {message.text}
          </div>
        )}

        <button
          type="button"
          onClick={autorizar}
          disabled={loading || generalActiva}
          className="btn-primary w-full gap-2"
        >
          <CheckIcon />
          {loading ? 'Procesando...' : 'Autorizar salida'}
        </button>

        <p className="text-xs leading-5 text-cetis-muted">
          Autorizado por: <b>{usuario.nombreUsuario || usuario.idUsuario}</b>
        </p>
      </div>
    </section>
  )
}
