import AttendanceTable from './AttendanceTable'
import StudentSummary from './StudentSummary'

export default function TutorDashboard({
  alumno,
}) {
  const registros = alumno?.registros || []

  const entradas = registros.filter(
    (item) => item.horaEntrada,
  ).length

  const salidas = registros.filter(
    (item) => item.horaSalida,
  ).length

  const ultimo = registros[0]

  return (
    <main className="mx-auto max-w-7xl space-y-5 px-4 py-6 sm:px-6 lg:px-8">
      <section className="app-card border-l-4 border-l-cetis-gold p-5 sm:p-6">
        <h1 className="text-2xl font-black text-cetis-wine">
          Consulta del registro del alumno
        </h1>
        <p className="mt-2 text-sm text-cetis-muted">
          Código validado. Esta vista muestra únicamente la información del alumno relacionado.
        </p>
      </section>

      <div className="grid gap-5 lg:grid-cols-[1.35fr_1fr]">
        <StudentSummary alumno={alumno} />

        <div className="grid grid-cols-2 gap-3 sm:grid-cols-3 lg:grid-cols-1 xl:grid-cols-3">
          <Metric title="Entradas" value={entradas} note="Registradas" />
          <Metric title="Salidas" value={salidas} note="Registradas" accent />
          <Metric
            title="Último registro"
            value={ultimo?.horaSalida || ultimo?.horaEntrada || '—'}
            note={ultimo?.fecha || 'Sin registros'}
            wide
          />
        </div>
      </div>

      <AttendanceTable registros={registros} />

      <section className="app-card p-5 sm:p-6">
        <h3 className="section-title">Información del tutor</h3>

        <div className="mt-4 grid gap-4 text-sm sm:grid-cols-2 lg:grid-cols-4">
          <Info label="Nombre" value={alumno?.tutor?.nombre} />
          <Info label="Parentesco" value={alumno?.tutor?.parentesco} />
          <Info label="Teléfono" value={alumno?.tutor?.telefono} />
          <Info label="Correo" value={alumno?.tutor?.correo} />
        </div>
      </section>
    </main>
  )
}

function Metric({
  title,
  value,
  note,
  accent = false,
  wide = false,
}) {
  return (
    <div className={`app-card border-l-4 ${accent ? 'border-l-[#a62339]' : 'border-l-cetis-gold'} p-4 ${wide ? 'col-span-2 sm:col-span-1' : ''}`}>
      <p className="text-xs font-bold uppercase tracking-wide text-cetis-muted">
        {title}
      </p>
      <p className="mt-2 break-words text-2xl font-black text-cetis-ink">
        {value}
      </p>
      <p className="mt-1 text-xs text-cetis-muted">
        {note}
      </p>
    </div>
  )
}

function Info({ label, value }) {
  return (
    <div className="rounded-2xl bg-cetis-cream p-4">
      <p className="text-xs font-bold uppercase tracking-wide text-cetis-muted">
        {label}
      </p>
      <p className="mt-1 break-words font-bold text-cetis-ink">
        {value || '—'}
      </p>
    </div>
  )
}
