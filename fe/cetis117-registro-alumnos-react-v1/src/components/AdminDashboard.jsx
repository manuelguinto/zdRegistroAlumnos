import { useEffect, useState } from 'react'
import {
  consultarAlumnos,
  crearAutorizacion,
  obtenerGrupos,
} from '../api'
import AdminStudents from './AdminStudents'
import AuthorizationModule from './AuthorizationModule'
import GroupStatusModule from './GroupStatusModule'
import TutorDashboard from './TutorDashboard'

export default function AdminDashboard({
  codigo,
  usuario,
}) {
  const [grupos, setGrupos] = useState([])
  const [alumnos, setAlumnos] = useState([])
  const [loading, setLoading] = useState(true)
  const [error, setError] = useState('')
  const [alumnoSeleccionado, setAlumnoSeleccionado] = useState(null)

  const [filtros, setFiltros] = useState({
    idGrupo: '',
    nombre: '',
    iniciales: '',
  })

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
    <main className="mx-auto max-w-7xl px-4 py-6 sm:px-6 lg:px-8">
      <section className="app-card mb-5 border-l-4 border-l-cetis-gold p-5 sm:p-6">
        <p className="text-sm font-bold uppercase tracking-[0.18em] text-[#b58b43]">
          Módulo administrativo
        </p>

        <h1 className="mt-1 text-2xl font-black text-cetis-wine sm:text-3xl">
          Gestión de alumnos y autorizaciones
        </h1>

        <p className="mt-2 text-sm text-cetis-muted">
          {usuario.nombreUsuario} · {usuario.rol || 'Usuario de sistema'}
        </p>
      </section>

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
        />
      </div>

      <div className="mt-5">
        <GroupStatusModule
          grupos={grupos}
        />
      </div>
    </main>
  )
}
