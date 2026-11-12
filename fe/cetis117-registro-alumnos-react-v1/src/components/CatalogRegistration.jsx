import { useState } from 'react'
import { guardarCatalogo, importarCatalogo } from '../api'

const grupoInicial = { nombre: '', horaSalida: '14:00' }
const alumnoInicial = {
  nombre: '', apellidoPaterno: '', apellidoMaterno: '', idGrupo: '',
  tutorNombre: '', tutorParentesco: '', tutorTelefono: '', tutorTelefonoAlternativo: '', tutorCorreo: '',
}

function Campo({ label, name, value, onChange, type = 'text', required = true, ...props }) {
  return <label className="block text-sm font-semibold text-cetis-text">
    {label}{required ? ' *' : ' (opcional)'}
    <input className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5 font-normal focus:border-cetis-wine focus:outline-none focus:ring-2 focus:ring-cetis-wine/20"
      name={name} type={type} value={value} onChange={onChange} required={required} {...props} />
  </label>
}

export default function CatalogRegistration({ codigo, grupos, onSaved }) {
  const [vista, setVista] = useState(null)
  const [grupo, setGrupo] = useState(grupoInicial)
  const [alumno, setAlumno] = useState(alumnoInicial)
  const [tipo, setTipo] = useState('grupos')
  const [archivo, setArchivo] = useState(null)
  const [ocupado, setOcupado] = useState(false)
  const [error, setError] = useState('')
  const [detalles, setDetalles] = useState([])
  const [resultado, setResultado] = useState(null)
  const [aviso, setAviso] = useState('')

  function cambiarVista(valor) {
    setVista(valor); setError(''); setDetalles([]); setResultado(null); setAviso('')
  }
  const cambiarGrupo = e => setGrupo(actual => ({ ...actual, [e.target.name]: e.target.value }))
  const cambiarAlumno = e => setAlumno(actual => ({ ...actual, [e.target.name]: e.target.value }))

  async function guardar(event) {
    event.preventDefault()
    setOcupado(true); setError(''); setDetalles([]); setResultado(null); setAviso('')
    try {
      let response
      if (vista === 'masiva') {
        if (!archivo) throw new Error('Selecciona un archivo CSV.')
        if (archivo.size > 10 * 1024 * 1024) throw new Error('El archivo no debe superar 10 MB.')
        response = await importarCatalogo(tipo, archivo, codigo)
      } else {
        const datos = vista === 'grupos' ? grupo : {
          nombre: alumno.nombre.trim(),
          apellidoPaterno: alumno.apellidoPaterno.trim(), apellidoMaterno: alumno.apellidoMaterno.trim(),
          idGrupo: alumno.idGrupo,
          tutor: { nombre: alumno.tutorNombre.trim(), parentesco: alumno.tutorParentesco.trim(),
            telefono: alumno.tutorTelefono.trim(), telefonoAlternativo: alumno.tutorTelefonoAlternativo.trim(),
            correo: alumno.tutorCorreo.trim() },
        }
        response = await guardarCatalogo(vista, datos, codigo)
      }
      setResultado(response)
      if (vista === 'grupos') setGrupo(grupoInicial)
      if (vista === 'alumnos') setAlumno(alumnoInicial)
      try { await onSaved() }
      catch { setAviso('Los datos se guardaron. Actualiza la consulta para ver los cambios.') }
    } catch (err) {
      setError(err.message); setDetalles(err.detalles || [])
    } finally { setOcupado(false) }
  }

  return <section className="app-card mb-5 p-5 sm:p-6" aria-labelledby="altas-titulo">
    <div className="flex flex-wrap items-center justify-between gap-4">
      <div><h2 id="altas-titulo" className="text-xl font-black text-cetis-wine">Altas de grupos y alumnos</h2>
        <p className="mt-1 text-sm text-cetis-muted">Registra un grupo, un alumno o carga un archivo CSV.</p></div>
      <div className="flex flex-wrap gap-2">
        {[['grupos', 'Alta de grupo'], ['alumnos', 'Alta de alumno'], ['masiva', 'Alta masiva']].map(([valor, titulo]) =>
          <button key={valor} type="button" disabled={ocupado} aria-pressed={vista === valor}
            className={vista === valor ? 'btn-primary' : 'btn-secondary'} onClick={() => cambiarVista(valor)}>{titulo}</button>)}
        {vista && <button type="button" className="btn-secondary" disabled={ocupado} onClick={() => cambiarVista(null)}>Cerrar</button>}
      </div>
    </div>
    {vista && <form onSubmit={guardar} className="mt-6 border-t border-slate-200 pt-5">
      <fieldset disabled={ocupado}>
        {vista === 'grupos' && <>
          <p className="mb-4 text-sm text-cetis-muted">El ID se asigna automáticamente al guardar: último ID registrado + 1.</p>
          <div className="grid gap-4 sm:grid-cols-3">
            <Campo label="Nombre del grupo" name="nombre" placeholder="Ej. 1° A" value={grupo.nombre} onChange={cambiarGrupo} />
            <Campo label="Hora de salida" name="horaSalida" type="time" value={grupo.horaSalida} onChange={cambiarGrupo} />
          </div>
        </>}
        {vista === 'alumnos' && <>
          <p className="mb-4 text-sm text-cetis-muted">El ID se asigna automáticamente: último ID registrado + 1. La huella usará ese mismo ID; también se generan las iniciales y el código del tutor.</p>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Campo label="Nombre(s)" name="nombre" value={alumno.nombre} onChange={cambiarAlumno} />
            <Campo label="Apellido paterno" name="apellidoPaterno" value={alumno.apellidoPaterno} onChange={cambiarAlumno} />
            <Campo label="Apellido materno" name="apellidoMaterno" value={alumno.apellidoMaterno} onChange={cambiarAlumno} />
            <label className="block text-sm font-semibold">Grupo *<select name="idGrupo" required value={alumno.idGrupo} onChange={cambiarAlumno}
              className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
              <option value="">Selecciona un grupo</option>
              {grupos.map(g => <option key={g.idGrupo} value={g.idGrupo}>{g.nombre}</option>)}
            </select></label>
          </div>
          {!grupos.length && <p className="mt-3 text-sm text-amber-800">Primero registra un grupo para poder asignarlo al alumno.</p>}
          <h3 className="mb-4 mt-6 font-bold text-cetis-wine">Datos del tutor</h3>
          <div className="grid gap-4 sm:grid-cols-2 lg:grid-cols-3">
            <Campo label="Nombre del tutor" name="tutorNombre" value={alumno.tutorNombre} onChange={cambiarAlumno} />
            <Campo label="Parentesco" name="tutorParentesco" placeholder="Ej. Madre, padre o tutor" value={alumno.tutorParentesco} onChange={cambiarAlumno} />
            <Campo label="Teléfono" name="tutorTelefono" type="tel" value={alumno.tutorTelefono} onChange={cambiarAlumno} />
            <Campo label="Teléfono alternativo" name="tutorTelefonoAlternativo" type="tel" required={false} value={alumno.tutorTelefonoAlternativo} onChange={cambiarAlumno} />
            <Campo label="Correo" name="tutorCorreo" type="email" value={alumno.tutorCorreo} onChange={cambiarAlumno} />
          </div>
        </>}
        {vista === 'masiva' && <div className="grid gap-4 sm:grid-cols-2">
          <label className="block text-sm font-semibold">Tipo de carga *<select value={tipo} onChange={e => { setTipo(e.target.value); setResultado(null); setError(''); setDetalles([]) }}
            className="mt-2 w-full rounded-xl border border-slate-300 bg-white px-3 py-2.5">
            <option value="grupos">Registrar grupos</option><option value="alumnos">Registrar alumnos</option>
          </select></label>
          <label className="block text-sm font-semibold">Archivo CSV *<input type="file" required accept=".csv,text/csv" onChange={e => setArchivo(e.target.files?.[0] || null)} className="mt-2 block w-full rounded-xl border border-slate-300 p-2" /></label>
          <p className="text-sm text-cetis-muted sm:col-span-2">Carga primero los grupos. Usa CSV UTF-8 separado por comas, hasta 10 MB. Si hay errores de validación, no se guarda ningún registro. Los IDs existentes se actualizan.</p>
        </div>}
        <button type="submit" className="btn-primary mt-5" disabled={ocupado || (vista === 'alumnos' && !grupos.length)}>
          {ocupado ? 'Guardando…' : vista === 'masiva' ? 'Validar e importar CSV' : vista === 'grupos' ? 'Guardar grupo' : 'Guardar alumno'}
        </button>
      </fieldset>
    </form>}
    {error && <div role="alert" className="mt-4 rounded-xl border border-red-200 bg-red-50 p-4 text-sm text-red-800">
      <p className="font-bold">{error}</p>
      {detalles.length > 0 && <div className="mt-3 max-h-72 overflow-auto"><table className="w-full text-left"><thead><tr><th className="p-2">Fila</th><th className="p-2">Campo</th><th className="p-2">Error</th></tr></thead>
        <tbody>{detalles.map((e, i) => <tr key={i} className="border-t border-red-200"><td className="p-2">{vista === 'masiva' ? e.fila : '—'}</td><td className="p-2">{e.campo || 'Registro'}</td><td className="p-2">{e.error}</td></tr>)}</tbody></table></div>}
    </div>}
    {resultado && <div role="status" className="mt-4 rounded-xl border border-emerald-200 bg-emerald-50 p-4 text-sm text-emerald-900">
      <p className="font-bold">Datos guardados correctamente</p>
      {resultado.idGrupo && <p>ID del grupo asignado: <strong>{resultado.idGrupo}</strong></p>}
      {resultado.idAlumno && <p>ID del alumno y de su huella: <strong>{resultado.idAlumno}</strong></p>}
      <p>Insertados: {resultado.insertados} · Actualizados: {resultado.actualizados} · Rechazados: {resultado.rechazados}</p>
      {resultado.codigoTutorHash && <p className="mt-2">Código de acceso del tutor: <strong className="font-mono text-lg">{resultado.codigoTutorHash}</strong>. Compártelo únicamente con el tutor.</p>}
      {aviso && <p className="mt-2">{aviso}</p>}
    </div>}
  </section>
}
