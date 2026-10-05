function statusConfig(estado) {
  if (estado === 'SALIDA_POR_AUTORIZACION') {
    return {
      label: 'Salida por autorización',
      classes: 'bg-emerald-100 text-emerald-700',
      dot: 'bg-emerald-500',
    }
  }

  if (estado === 'SALIDA_POR_HORARIO') {
    return {
      label: 'Salida por horario',
      classes: 'bg-blue-100 text-blue-700',
      dot: 'bg-blue-500',
    }
  }

  return {
    label: 'Pendiente',
    classes: 'bg-amber-100 text-amber-800',
    dot: 'bg-amber-500',
  }
}

export default function GroupStatusModule({
  grupos,
}) {
  return (
    <section className="app-card overflow-hidden">
      <div className="border-b border-[#eadfce] p-5 sm:p-6">
        <h2 className="section-title">
          Estado de salida por grupo
        </h2>

        <p className="mt-1 text-sm text-cetis-muted">
          Consulta rápida para saber si el grupo ya tiene habilitada su salida.
        </p>
      </div>

      <div className="grid gap-3 p-5 sm:grid-cols-2 lg:grid-cols-3">
        {grupos.map((grupo) => {
          const status =
            statusConfig(grupo.estadoSalida)

          return (
            <article
              key={grupo.idGrupo}
              className="rounded-2xl border border-[#eadfce] bg-cetis-creamLight p-4"
            >
              <div className="flex items-start justify-between gap-3">
                <div>
                  <p className="font-black text-cetis-ink">
                    {grupo.nombre}
                  </p>

                  <p className="mt-1 text-sm text-cetis-muted">
                    {grupo.salidaHabilitada
                      ? 'El grupo ya puede registrar su salida'
                      : 'El grupo aún no tiene salida habilitada'}
                  </p>
                </div>

                <span
                  className={`pill shrink-0 ${status.classes}`}
                >
                  <span
                    className={`h-2 w-2 rounded-full ${status.dot}`}
                  />
                  {status.label}
                </span>
              </div>
            </article>
          )
        })}
      </div>
    </section>
  )
}
