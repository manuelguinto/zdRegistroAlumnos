import { useEffect, useMemo, useState } from 'react'
import {
  consultarAlumnos,
  crearAutorizacion,
  obtenerGrupos,
} from '../api'
import AdminStudents from './AdminStudents'
import AuthorizationModule from './AuthorizationModule'
import GroupStatusModule from './GroupStatusModule'
import TutorDashboard from './TutorDashboard'
import GeneralExitModal from './GeneralExitModal'

export default function AdminDashboard({
  codigo,
  usuario,
}) {
  const [grupos, setGrupos] = useState([])
  const [alumnos, setAlumnos] = useState([])
  const [loading, setLoading] = useState(true)
  const [generalLoading, setGeneralLoading] = useState(false)
  const [error, setError] = useState('')
  const [generalMessage, setGeneralMessage] = useState('')
  const [generalModalOpen, setGeneralModalOpen] = useState(false)
  const [alumnoSeleccionado, setAlumnoSeleccionado] = useState(null)

  const [filtros, setFiltros] = useState({
    idGrupo: '',
    nombre: '',
    iniciales: '',
  })

  const generalActiva = useMemo(
    () => grupos.some(
      (grupo) => grupo.estadoSalida === 'SALIDA_GENERAL',
    ),
    [grupos],
  )

  useEffect(() => {
    cargarInicial()
  }, [])

  async function cargarInicial() {
    setLoading(true)
    setError('')

    try {
      const [gruposData, alumnosData] = await Promise.all([
        obtenerGrupos(codigo),
        consultarAlumnos(codigo),
      ])

      setGrupos(gruposData || [])
      setAlumnos(alumnosData || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function refrescarDespuesDeAutorizacion() {
    const [gruposData, alumnosData] = await Promise.all([
      obtenerGrupos(codigo),
      consultarAlumnos(codigo, filtros),
    ])

    setGrupos(gruposData || [])
    setAlumnos(alumnosData || [])
  }

  async function buscar() {
    setLoading(true)
    setError('')

    try {
      const data = await consultarAlumnos(
        codigo,
        filtros,
      )

      setAlumnos(data || [])
    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  async function buscarAlumnoPorIniciales(iniciales) {
    const data = await consultarAlumnos(codigo, {
      iniciales,
    })

    return data?.[0] || null
  }

  async function crearSalidaGeneral(payload) {
    setGeneralLoading(true)
    setGeneralMessage('')
    setError('')

    try {
      const response = await crearAutorizacion(payload)

      setGeneralMessage(
        response?.mensaje ||
          'Salida general autorizada correctamente',
      )

      setGeneralModalOpen(false)

      await refrescarDespuesDeAutorizacion()

    } catch (err) {
      setError(err.message)
    } finally {
      setGeneralLoading(false)
    }
  }

  if (alumnoSeleccionado) {
    return (
      <div>
        <div className="mx-auto max-w-7xl px-4 pt-6 sm:px-6 lg:px-8">
          <button
            type="button"
            className="btn-secondary"
            onClick={() => setAlumnoSeleccionado(null)}
          >
            ← Volver a administración
          </button>
        </div>

        <TutorDashboard alumno={alumnoSeleccionado} />
      </div>
    )
  }

  return (
    <>
      <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
        <section className="app-card mb-5 border-l-4 border-l-cetis-gold p-5 sm:p-6">
          <div className="flex flex-col gap-5 md:flex-row md:items-center md:justify-between">
            <div>
              <p className="text-sm font-bold uppercase tracking-[0.18em] text-[#b58b43]">
                Módulo administrativo
              </p>

              <h1 className="mt-1 text-2xl font-black text-cetis-wine sm:text-3xl">
                Gestión de alumnos y autorizaciones
              </h1>

              <p className="mt-2 text-sm text-cetis-muted">
                {usuario.nombreUsuario} · {usuario.rol || 'Usuario de sistema'}
              </p>
            </div>

            <button
              type="button"
              onClick={() => setGeneralModalOpen(true)}
              disabled={generalActiva}
              className={`min-h-24 min-w-[220px] rounded-2xl border px-6 py-4 text-left shadow-sm transition ${
                generalActiva
                  ? 'cursor-not-allowed border-emerald-200 bg-emerald-50 text-emerald-800'
                  : 'border-cetis-wine bg-cetis-wine text-white hover:bg-cetis-wineDark'
              }`}
            >
              <span className="block text-xs font-black uppercase tracking-[0.16em] opacity-80">
                Toda la escuela
              </span>

              <span className="mt-1 block text-xl font-black">
                {generalActiva
                  ? 'Salida general activa'
                  : 'Salida general'}
              </span>

              <span className="mt-1 block text-xs opacity-80">
                {generalActiva
                  ? 'Ya fue autorizada hoy'
                  : 'Autorizar salida para todos'}
              </span>
            </button>
          </div>
        </section>

        {generalMessage && (
          <div className="mb-5 rounded-2xl border border-emerald-200 bg-emerald-50 px-5 py-4 text-sm font-semibold text-emerald-700">
            {generalMessage}
          </div>
        )}

        {error && (
          <div className="mb-5 rounded-2xl border border-red-200 bg-red-50 px-5 py-4 text-sm font-semibold text-red-700">
            {error}
          </div>
        )}

        <div className="grid gap-5 xl:grid-cols-[1.7fr_0.8fr]">
          <AdminStudents
            alumnos={alumnos}
            grupos={grupos}
            filtros={filtros}
            setFiltros={setFiltros}
            onSearch={buscar}
            loading={loading}
            onViewStudent={setAlumnoSeleccionado}
          />

          <AuthorizationModule
            codigo={codigo}
            usuario={usuario}
            grupos={grupos}
            searchStudent={buscarAlumnoPorIniciales}
            createAuthorization={crearAutorizacion}
            generalActiva={generalActiva}
          />
        </div>

        <div className="mt-5">
          <GroupStatusModule
            grupos={grupos}
          />
        </div>
      </main>

      <GeneralExitModal
        open={generalModalOpen}
        onClose={() => setGeneralModalOpen(false)}
        onConfirm={crearSalidaGeneral}
        loading={generalLoading}
        usuario={usuario}
      />
    </>
  )
}
