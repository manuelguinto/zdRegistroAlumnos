function statusClasses(estado) {
  if (estado === 'Completado') {
    return 'bg-emerald-100 text-emerald-700'
  }

  if (estado === 'EntradaRegistrada') {
    return 'bg-amber-100 text-amber-800'
  }

  return 'bg-slate-100 text-slate-700'
}

export default function AttendanceTable({
  registros = [],
}) {
  return (
    <section className="app-card overflow-hidden">
      <div className="border-b border-[#eadfce] px-5 py-5 sm:px-6">
        <h3 className="section-title">Bitácora de asistencia</h3>
        <p className="mt-1 text-sm text-cetis-muted">
          Entradas, salidas y autorizaciones registradas.
        </p>
      </div>

      {registros.length === 0 ? (
        <div className="px-5 py-10 text-center text-sm text-cetis-muted">
          No hay registros de asistencia disponibles.
        </div>
      ) : (
        <>
          <div className="hidden overflow-x-auto md:block">
            <table className="w-full min-w-[840px] text-left text-sm">
              <thead className="bg-cetis-cream text-cetis-wine">
                <tr>
                  {['Fecha', 'Entrada', 'Salida', 'Estado', 'Autorizó', 'Motivo'].map((item) => (
                    <th key={item} className="px-5 py-3 font-extrabold">
                      {item}
                    </th>
                  ))}
                </tr>
              </thead>
              <tbody className="divide-y divide-[#eee3d5]">
                {registros.map((registro, index) => (
                  <tr key={`${registro.fecha}-${index}`} className="bg-white">
                    <td className="px-5 py-4 font-semibold">{registro.fecha || '—'}</td>
                    <td className="px-5 py-4">
                      <div className="inline-flex min-w-24 flex-col rounded-2xl border border-[#ead6b1] bg-[#fff8e8] px-4 py-2 shadow-sm">
                        <span className="text-[10px] font-black uppercase tracking-wide text-[#aa7722]">
                          Entrada
                        </span>
                        <span className="mt-0.5 text-lg font-black text-cetis-ink">
                          {registro.horaEntrada || '—'}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <div className="inline-flex min-w-24 flex-col rounded-2xl border border-[#e8c5cc] bg-[#fff1f4] px-4 py-2 shadow-sm">
                        <span className="text-[10px] font-black uppercase tracking-wide text-cetis-wine">
                          Salida
                        </span>
                        <span className="mt-0.5 text-lg font-black text-cetis-wine">
                          {registro.horaSalida || '—'}
                        </span>
                      </div>
                    </td>
                    <td className="px-5 py-4">
                      <span className={`pill ${statusClasses(registro.estado)}`}>
                        {registro.estado || '—'}
                      </span>
                    </td>
                    <td className="px-5 py-4">{registro.nombreAutoriza || '—'}</td>
                    <td className="px-5 py-4">{registro.motivo || '—'}</td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>

          <div className="space-y-3 p-4 md:hidden">
            {registros.map((registro, index) => (
              <article
                key={`${registro.fecha}-${index}`}
                className="rounded-2xl border border-[#eadfce] bg-cetis-creamLight p-4"
              >
                <div className="flex items-start justify-between gap-3">
                  <div>
                    <p className="font-black text-cetis-ink">{registro.fecha || '—'}</p>
                    <div className="mt-2 grid grid-cols-2 gap-2">
                      <div className="rounded-xl border border-[#ead6b1] bg-[#fff8e8] px-3 py-2">
                        <span className="block text-[10px] font-black uppercase tracking-wide text-[#aa7722]">
                          Entrada
                        </span>
                        <span className="mt-0.5 block text-base font-black text-cetis-ink">
                          {registro.horaEntrada || '—'}
                        </span>
                      </div>

                      <div className="rounded-xl border border-[#e8c5cc] bg-[#fff1f4] px-3 py-2">
                        <span className="block text-[10px] font-black uppercase tracking-wide text-cetis-wine">
                          Salida
                        </span>
                        <span className="mt-0.5 block text-base font-black text-cetis-wine">
                          {registro.horaSalida || '—'}
                        </span>
                      </div>
                    </div>
                  </div>
                  <span className={`pill ${statusClasses(registro.estado)}`}>
                    {registro.estado || '—'}
                  </span>
                </div>

                {(registro.nombreAutoriza || registro.motivo) && (
                  <div className="mt-3 rounded-xl bg-white p-3 text-sm">
                    <p>
                      <b>Autorizó:</b> {registro.nombreAutoriza || '—'}
                    </p>
                    <p className="mt-1">
                      <b>Motivo:</b> {registro.motivo || '—'}
                    </p>
                  </div>
                )}
              </article>
            ))}
          </div>
        </>
      )}
    </section>
  )
}
