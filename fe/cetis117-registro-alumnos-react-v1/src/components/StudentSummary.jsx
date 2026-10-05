function StatusPill({ active }) {
  return (
    <span
      className={`pill ${
        active
          ? 'bg-emerald-100 text-emerald-700'
          : 'bg-slate-100 text-slate-600'
      }`}
    >
      <span className={`h-2 w-2 rounded-full ${active ? 'bg-emerald-500' : 'bg-slate-400'}`} />
      {active ? 'Activo' : 'Inactivo'}
    </span>
  )
}

export default function StudentSummary({
  alumno,
  groupName,
}) {
  return (
    <section className="app-card p-5 sm:p-6">
      <p className="section-title">Información del alumno</p>

      <div className="mt-5 flex flex-col gap-5 sm:flex-row sm:items-center">
        <div className="flex h-24 w-24 shrink-0 items-center justify-center rounded-3xl bg-gradient-to-br from-cetis-goldSoft to-[#f8e1d9] text-4xl">
          🎓
        </div>

        <div className="min-w-0">
          <h2 className="break-words text-2xl font-black text-cetis-ink">
            {alumno.nombreCompleto || 'Alumno'}
          </h2>

          <div className="mt-3 flex flex-wrap gap-x-5 gap-y-2 text-sm text-cetis-muted">
            <span>
              <b className="text-cetis-ink">Grupo:</b>{' '}
              {groupName || alumno.idGrupo || '—'}
            </span>
            <span>
              <b className="text-cetis-ink">Matrícula:</b>{' '}
              {alumno.idAlumno || '—'}
            </span>
            <span>
              <b className="text-cetis-ink">Iniciales:</b>{' '}
              {alumno.iniciales || '—'}
            </span>
            <StatusPill active={alumno.activo !== false} />
          </div>
        </div>
      </div>
    </section>
  )
}
