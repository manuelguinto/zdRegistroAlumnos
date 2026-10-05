import { SearchIcon } from './Icons'

export default function AdminStudents({
  alumnos,
  grupos,
  filtros,
  setFiltros,
  onSearch,
  loading,
  onViewStudent,
}) {
  const grupoMap = new Map(
    grupos.map((grupo) => [String(grupo.idGrupo), grupo.nombre]),
  )

  return (
    <section className="app-card overflow-hidden">
      <div className="border-b border-[#eadfce] p-5 sm:p-6">
        <h2 className="section-title">Consulta de alumnos</h2>
        <p className="mt-1 text-sm text-cetis-muted">
          Filtra por grupo, nombre o iniciales.
        </p>

        <form
          className="mt-5 grid gap-3 md:grid-cols-[1fr_1.3fr_0.8fr_auto]"
          onSubmit={(event) => {
            event.preventDefault()
            onSearch()
          }}
        >
          <select
            className="form-input"
            value={filtros.idGrupo}
            onChange={(event) =>
              setFiltros((prev) => ({
                ...prev,
                idGrupo: event.target.value,
              }))
            }
          >
            <option value="">Todos los grupos</option>
            {grupos.map((grupo) => (
              <option key={grupo.idGrupo} value={grupo.idGrupo}>
                {grupo.nombre}
              </option>
            ))}
          </select>

          <input
            className="form-input"
            placeholder="Buscar por nombre"
            value={filtros.nombre}
            onChange={(event) =>
              setFiltros((prev) => ({
                ...prev,
                nombre: event.target.value,
              }))
            }
          />

          <input
            className="form-input uppercase"
            placeholder="Iniciales"
            value={filtros.iniciales}
            onChange={(event) =>
              setFiltros((prev) => ({
                ...prev,
                iniciales: event.target.value.toUpperCase(),
              }))
            }
          />

          <button
            type="submit"
            className="btn-primary gap-2 md:px-5"
            disabled={loading}
          >
            <SearchIcon />
            Buscar
          </button>
        </form>
      </div>

      <div className="hidden overflow-x-auto md:block">
        <table className="w-full min-w-[860px] text-left text-sm">
          <thead className="bg-cetis-cream text-cetis-wine">
            <tr>
              {['Alumno', 'Iniciales', 'Grupo', 'Acciones'].map((item) => (
                <th key={item} className="px-5 py-3 font-extrabold">
                  {item}
                </th>
              ))}
            </tr>
          </thead>
          <tbody className="divide-y divide-[#eee3d5]">
            {alumnos.map((alumno) => {
              return (
                <tr key={alumno.idAlumno} className="bg-white">
                  <td className="px-5 py-4">
                    <p className="font-extrabold text-cetis-ink">
                      {alumno.nombreCompleto}
                    </p>
                    <p className="text-xs text-cetis-muted">
                      {alumno.idAlumno}
                    </p>
                  </td>

                  <td className="px-5 py-4 font-bold text-cetis-wine">
                    {alumno.iniciales || '—'}
                  </td>

                  <td className="px-5 py-4">
                    {grupoMap.get(String(alumno.idGrupo)) || alumno.idGrupo || '—'}
                  </td>

                  <td className="px-5 py-4">
                    <button
                      type="button"
                      className="btn-secondary"
                      onClick={() => onViewStudent(alumno)}
                    >
                      Consultar registro
                    </button>
                  </td>
                </tr>
              )
            })}
          </tbody>
        </table>
      </div>

      <div className="space-y-3 p-4 md:hidden">
        {alumnos.map((alumno) => {
          return (
            <article
              key={alumno.idAlumno}
              className="rounded-2xl border border-[#eadfce] bg-cetis-creamLight p-4"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-black text-cetis-ink">
                    {alumno.nombreCompleto}
                  </p>
                  <p className="mt-1 text-xs text-cetis-muted">
                    {alumno.idAlumno} · {grupoMap.get(String(alumno.idGrupo)) || alumno.idGrupo}
                  </p>
                </div>

                <span className="pill bg-cetis-goldSoft text-cetis-wine">
                  {alumno.iniciales || '—'}
                </span>
              </div>

              <button
                type="button"
                className="btn-secondary mt-4 w-full"
                onClick={() => onViewStudent(alumno)}
              >
                Consultar registro
              </button>
            </article>
          )
        })}

        {!loading && alumnos.length === 0 && (
          <p className="py-8 text-center text-sm text-cetis-muted">
            No se encontraron alumnos.
          </p>
        )}
      </div>
    </section>
  )
}
