import { useState } from 'react'
import {
  consultarAlumnos,
  validarAcceso,
} from './api'
import Header from './components/Header'
import LoginView from './components/LoginView'
import TutorDashboard from './components/TutorDashboard'
import AdminDashboard from './components/AdminDashboard'

export default function App() {
  const [session, setSession] = useState(null)
  const [alumnoTutor, setAlumnoTutor] = useState(null)
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState('')

  async function ingresar(codigo) {
    setLoading(true)
    setError('')

    try {
      const acceso = await validarAcceso(codigo)

      if (!acceso?.valido) {
        setError(
          acceso?.mensaje ||
            'Código de seguridad inválido',
        )
        return
      }

      if (acceso.tipoAcceso === 'TUTOR') {
        const alumnos = await consultarAlumnos(codigo)
        const alumno = alumnos?.[0]

        if (!alumno) {
          setError(
            'El código es válido, pero no tiene un alumno asociado.',
          )
          return
        }

        setAlumnoTutor(alumno)
        setSession({
          ...acceso,
          codigo,
        })
        return
      }

      if (acceso.tipoAcceso === 'SISTEMA') {
        setSession({
          ...acceso,
          codigo,
        })
        return
      }

      setError('Tipo de acceso no reconocido.')

    } catch (err) {
      setError(err.message)
    } finally {
      setLoading(false)
    }
  }

  function salir() {
    setSession(null)
    setAlumnoTutor(null)
    setError('')
  }

  return (
    <div className="min-h-screen">
      <Header
        subtitle={
          session?.tipoAcceso === 'SISTEMA'
            ? 'Administración de asistencia'
            : 'Consulta de asistencia por código de seguridad'
        }
        userLabel={
          session?.tipoAcceso === 'SISTEMA'
            ? session.nombreUsuario
            : session?.tipoAcceso === 'TUTOR'
              ? 'Portal de padres'
              : null
        }
        onLogout={session ? salir : null}
      />

      {!session && (
        <LoginView
          onSubmit={ingresar}
          loading={loading}
          error={error}
        />
      )}

      {session?.tipoAcceso === 'TUTOR' && alumnoTutor && (
        <TutorDashboard alumno={alumnoTutor} />
      )}

      {session?.tipoAcceso === 'SISTEMA' && (
        <AdminDashboard
          codigo={session.codigo}
          usuario={session}
        />
      )}
    </div>
  )
}
