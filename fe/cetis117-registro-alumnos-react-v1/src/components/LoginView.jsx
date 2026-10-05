import { useState } from 'react'
import { LockIcon, SearchIcon } from './Icons'

export default function LoginView({
  onSubmit,
  loading,
  error,
}) {
  const [codigo, setCodigo] = useState('')

  function handleSubmit(event) {
    event.preventDefault()
    const valor = codigo.trim()

    if (!valor) return
    onSubmit(valor)
  }

  return (
    <main className="relative mx-auto flex min-h-[calc(100vh-88px)] max-w-7xl items-center px-4 py-5 sm:px-6 lg:px-8">
      <div className="absolute inset-0 -z-10 overflow-hidden">
        <div className="absolute -left-24 top-10 h-72 w-72 rounded-full bg-cetis-wine/10 blur-3xl" />
        <div className="absolute right-0 top-1/3 h-80 w-80 rounded-full bg-cetis-gold/10 blur-3xl" />
      </div>

      <section className="mx-auto w-full max-w-5xl overflow-visible rounded-[2rem] border border-white/80 bg-white/90 shadow-[0_28px_80px_rgba(88,45,28,0.16)] backdrop-blur">
        <div className="relative grid lg:grid-cols-[1.02fr_0.98fr]">
          <div className="relative overflow-hidden rounded-t-[2rem] bg-gradient-to-br from-[#8e1d31] via-[#a82439] to-[#d5a13f] p-7 text-white sm:p-9 lg:rounded-l-[2rem] lg:rounded-tr-none lg:p-10">
            <div className="absolute -right-14 -top-16 h-64 w-64 rounded-full bg-white/10 blur-3xl" />
            <div className="absolute -bottom-20 left-10 h-64 w-64 rounded-full bg-black/10 blur-3xl" />

            <div className="relative z-10">
              <div className="mb-6 flex justify-center">
                <div className="flex h-32 w-32 items-center justify-center rounded-3xl bg-white/95 p-3 shadow-xl sm:h-36 sm:w-36">
                  <img
                    src="/logo-cetis117.jpg"
                    alt="Logo CETIS 117"
                    className="h-full w-full object-contain"
                  />
                </div>
              </div>

              <h1 className="max-w-md text-3xl font-black leading-tight sm:text-4xl">
                Bienvenido al portal de asistencia
              </h1>

              <p className="mt-3 max-w-lg text-sm leading-6 text-white/90 sm:text-base">
                Consulta entradas, salidas y autorizaciones de manera rápida,
                clara y segura.
              </p>

              <div className="mt-7 space-y-3">
                <div className="rounded-2xl border border-white/25 bg-white/90 px-4 py-3 text-cetis-ink shadow-sm">
                  <p className="font-black text-cetis-wine">Padres y tutores</p>
                  <p className="mt-1 text-sm text-cetis-muted">
                    Consulta la asistencia del alumno relacionado.
                  </p>
                </div>

                <div className="rounded-2xl border border-white/25 bg-white/90 px-4 py-3 text-cetis-ink shadow-sm">
                  <p className="font-black text-cetis-wine">Personal autorizado</p>
                  <p className="mt-1 text-sm text-cetis-muted">
                    Consulta alumnos y administra salidas.
                  </p>
                </div>
              </div>
            </div>
          </div>

          <div className="flex items-center justify-center rounded-b-[2rem] bg-white p-7 sm:p-9 lg:rounded-r-[2rem] lg:rounded-bl-none lg:p-10">
            <div className="w-full max-w-sm">
              <div className="mb-7">
                <p className="text-lg font-black tracking-[0.16em] text-[#b7832d]">
                  CETIS 117
                </p>

                <h2 className="mt-1 text-3xl font-black text-cetis-wine">
                  Consulta de asistencia
                </h2>

                <p className="mt-2 text-sm leading-6 text-cetis-muted">
                  Ingresa tu código de seguridad para continuar.
                </p>
              </div>

              <form onSubmit={handleSubmit} className="space-y-4">
                <div>
                  <label className="form-label" htmlFor="codigo">
                    Código de seguridad
                  </label>

                  <div className="relative">
                    <LockIcon className="absolute left-4 top-1/2 h-5 w-5 -translate-y-1/2 text-[#c39439]" />
                    <input
                      id="codigo"
                      value={codigo}
                      onChange={(event) => setCodigo(event.target.value)}
                      className="form-input pl-12 text-base"
                      placeholder="Ingresa tu código"
                      autoComplete="one-time-code"
                    />
                  </div>
                </div>

                {error && (
                  <div className="rounded-xl border border-red-200 bg-red-50 px-4 py-3 text-sm font-semibold text-red-700">
                    {error}
                  </div>
                )}

                <button
                  type="submit"
                  disabled={loading || !codigo.trim()}
                  className="btn-primary w-full gap-2 py-3.5 text-base shadow-lg shadow-cetis-wine/15"
                >
                  <SearchIcon />
                  {loading ? 'Validando...' : 'Consultar'}
                </button>

                <p className="pt-1 text-center text-xs leading-5 text-cetis-muted">
                  El sistema identificará automáticamente tu tipo de acceso.
                </p>
              </form>
            </div>
          </div>

          <div className="pointer-events-none absolute left-1/2 top-4 hidden -translate-x-1/2 rounded-xl border border-[#eadfce] bg-white px-5 py-2 text-xs font-black uppercase tracking-[0.18em] text-cetis-wine shadow-md lg:block">
            Portal escolar
          </div>
        </div>
      </section>
    </main>
  )
}
