import { CommonModule } from '@angular/common';
import {
  Component,
  DestroyRef,
  HostListener,
  OnInit,
  inject
} from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import {
  catchError,
  finalize,
  forkJoin,
  of
} from 'rxjs';

import { Recibo } from '../../core/models/recibo.model';
import { Remesa } from '../../core/models/remesa.model';
import { Vecino } from '../../core/models/vecino.model';
import { ReciboService } from '../../core/services/recibo';
import { RemesaService } from '../../core/services/remesa.service';
import {
  ComunidadUsuario,
  UsuarioContextoService
} from '../../core/services/usuario-contexto.service';
import { VecinoService } from '../../core/services/vecino';
import { ComunidadStateService } from '../../core/state/comunidad-state.service';
import {
  GestionIncidencia,
  IncidenciasService
} from '../../features/incidencias/services/incidencias.service';
import { ComunidadActivaBar } from '../../shared/comunidad-activa-bar/comunidad-activa-bar';

type TipoResultadoBusqueda =
  | 'SECCION'
  | 'COMUNIDAD'
  | 'PROPIETARIO'
  | 'RECIBO'
  | 'REMESA'
  | 'INCIDENCIA';

interface ResultadoBusqueda {
  clave: string;
  tipo: TipoResultadoBusqueda;
  icono: string;
  titulo: string;
  detalle: string;
  ruta: string;
  textoIndexado: string;
  comunidadId?: number;
  comunidadNombre?: string;
}

interface SeccionBuscable {
  titulo: string;
  detalle: string;
  icono: string;
  ruta: string;
  palabrasClave: string;
}

interface UsuarioLocal {
  usuarioId?: number;
  username?: string;
  administradorId?: number | null;
  administradorNombre?: string | null;
}

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [
    CommonModule,
    FormsModule,
    ComunidadActivaBar
  ],
  templateUrl: './navbar.html',
  styleUrl: './navbar.scss'
})
export class Navbar implements OnInit {

  private readonly router =
    inject(Router);

  private readonly destroyRef =
    inject(DestroyRef);

  private readonly comunidadState =
    inject(ComunidadStateService);

  private readonly usuarioContextoService =
    inject(UsuarioContextoService);

  private readonly vecinoService =
    inject(VecinoService);

  private readonly reciboService =
    inject(ReciboService);

  private readonly remesaService =
    inject(RemesaService);

  private readonly incidenciasService =
    inject(IncidenciasService);

  username = 'Usuario';

  administradorNombre = 'Administrador';

  textoBusqueda = '';

  resultados: ResultadoBusqueda[] = [];

  mostrarResultados = false;

  cargandoComunidades = false;

  private cargandoDatosParaComunidadId:
    number | null = null;

  private comunidades: ComunidadUsuario[] = [];

  private vecinos: Vecino[] = [];

  private recibos: Recibo[] = [];

  private remesas: Remesa[] = [];

  private incidencias: GestionIncidencia[] = [];

  private comunidadActivaId: number | null = null;

  private comunidadActivaNombre = '';

  private comunidadDatosCargadosId:
    number | null = null;

  private comunidadesCargadas = false;

  private readonly secciones: SeccionBuscable[] = [
    {
      titulo: 'Panel de Control',
      detalle: 'Dashboard y resumen general',
      icono: '📊',
      ruta: '/dashboard',
      palabrasClave: 'inicio dashboard panel control resumen'
    },
    {
      titulo: 'Comunidades',
      detalle: 'Gestión de comunidades',
      icono: '🏢',
      ruta: '/comunidades',
      palabrasClave: 'comunidad comunidades fincas edificios'
    },
    {
      titulo: 'Vecinos',
      detalle: 'Propietarios, viviendas y coeficientes',
      icono: '👥',
      ruta: '/vecinos',
      palabrasClave: 'vecinos propietarios coeficientes viviendas'
    },
    {
      titulo: 'Conceptos',
      detalle: 'Conceptos de cobro',
      icono: '📋',
      ruta: '/conceptos',
      palabrasClave: 'conceptos cuotas cobros'
    },
    {
      titulo: 'Recibos',
      detalle: 'Recibos emitidos',
      icono: '🧾',
      ruta: '/recibos',
      palabrasClave: 'recibos cobros pendientes pdf email'
    },
    {
      titulo: 'Remesas SEPA',
      detalle: 'Generación y validación de remesas',
      icono: '🏦',
      ruta: '/remesas',
      palabrasClave: 'remesas sepa xml c19 banco'
    },
    {
      titulo: 'Incidencias',
      detalle: 'Gestión de incidencias',
      icono: '⚠️',
      ruta: '/incidencias',
      palabrasClave: 'incidencias averias problemas'
    },
    {
      titulo: 'Tesorería',
      detalle: 'Movimientos bancarios',
      icono: '💶',
      ruta: '/tesoreria/movimientos',
      palabrasClave: 'tesoreria movimientos banco conciliacion'
    },
    {
      titulo: 'Importar Norma 43',
      detalle: 'Importación de extractos bancarios',
      icono: '📥',
      ruta: '/tesoreria/norma43',
      palabrasClave: 'norma 43 importar extracto bancario'
    },
    {
      titulo: 'Diario Contable',
      detalle: 'Asientos del diario',
      icono: '📘',
      ruta: '/tesoreria/diario',
      palabrasClave: 'diario contable asientos contabilidad'
    },
    {
      titulo: 'Libro Mayor',
      detalle: 'Consulta del libro mayor',
      icono: '📗',
      ruta: '/tesoreria/mayor',
      palabrasClave: 'libro mayor cuentas contabilidad'
    },
    {
      titulo: 'Balance Sumas y Saldos',
      detalle: 'Balance contable',
      icono: '📊',
      ruta: '/tesoreria/balance',
      palabrasClave: 'balance sumas saldos contabilidad'
    },
    {
      titulo: 'Presupuestos',
      detalle: 'Presupuestos de comunidades',
      icono: '💰',
      ruta: '/presupuestos',
      palabrasClave: 'presupuestos gastos ingresos cuotas'
    },
    {
      titulo: 'Cuentas presentadoras',
      detalle: 'Configuración bancaria SEPA',
      icono: '⚙️',
      ruta: '/configuracion/cuentas-presentador',
      palabrasClave: 'cuentas presentadoras bancos sepa configuracion'
    }
  ];

  get cargandoBusqueda(): boolean {
    return this.cargandoComunidades
      || this.cargandoDatosParaComunidadId !== null;
  }

  ngOnInit(): void {

    const usuario =
      this.leerUsuarioLocal();

    if (usuario) {
      this.username =
        usuario.username
        || 'Usuario';

      this.administradorNombre =
        usuario.administradorNombre
        || `Administrador ID ${usuario.administradorId ?? '-'}`;
    }

    this.comunidadState
      .comunidad$
      .pipe(
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe(comunidad => {

        const nuevoId =
          comunidad?.id
          ?? null;

        if (
          this.comunidadActivaId
          !== nuevoId
        ) {
          this.vecinos = [];
          this.recibos = [];
          this.remesas = [];
          this.incidencias = [];

          this.comunidadDatosCargadosId =
            null;
        }

        this.comunidadActivaId =
          nuevoId;

        this.comunidadActivaNombre =
          comunidad?.nombre
          || '';

        if (
          this.mostrarResultados
          && this.textoBusqueda.trim().length >= 2
        ) {
          this.actualizarResultados();
          this.cargarDatosComunidadActiva();
        }
      });

    this.comunidadState.init();

    this.cargarComunidades();
  }

  abrirBuscador(): void {

    this.mostrarResultados = true;

    this.cargarComunidades();

    if (
      this.textoBusqueda.trim().length >= 2
    ) {
      this.actualizarResultados();
      this.cargarDatosComunidadActiva();
    }
  }

  alCambiarTexto(): void {

    this.mostrarResultados = true;

    const texto =
      this.textoBusqueda.trim();

    if (texto.length < 2) {
      this.resultados = [];
      return;
    }

    this.actualizarResultados();
    this.cargarComunidades();
    this.cargarDatosComunidadActiva();
  }

  abrirPrimerResultado(): void {

    const primero =
      this.resultados[0];

    if (!primero) {
      return;
    }

    this.seleccionarResultado(
      primero
    );
  }

  seleccionarResultado(
    resultado: ResultadoBusqueda
  ): void {

    if (
      resultado.comunidadId !== undefined
      && resultado.comunidadNombre
    ) {
      this.comunidadState.setComunidad({
        id: resultado.comunidadId,
        nombre: resultado.comunidadNombre
      });
    }

    this.cerrarBuscador();

    void this.router.navigateByUrl(
      resultado.ruta
    );
  }

  limpiarBusqueda(): void {

    this.textoBusqueda = '';
    this.resultados = [];
    this.mostrarResultados = false;
  }

  cerrarBuscador(): void {

    this.mostrarResultados = false;
  }

  trackByResultado(
    _index: number,
    resultado: ResultadoBusqueda
  ): string {

    return resultado.clave;
  }

  etiquetaTipo(
    tipo: TipoResultadoBusqueda
  ): string {

    switch (tipo) {

      case 'SECCION':
        return 'Sección';

      case 'COMUNIDAD':
        return 'Comunidad';

      case 'PROPIETARIO':
        return 'Propietario';

      case 'RECIBO':
        return 'Recibo';

      case 'REMESA':
        return 'Remesa';

      case 'INCIDENCIA':
        return 'Incidencia';
    }
  }

  @HostListener(
    'document:click',
    ['$event']
  )
  cerrarAlPulsarFuera(
    event: MouseEvent
  ): void {

    const elemento =
      event.target as HTMLElement | null;

    if (
      !elemento?.closest(
        '.search-box'
      )
    ) {
      this.mostrarResultados = false;
    }
  }

  private cargarComunidades(): void {

    if (
      this.comunidadesCargadas
      || this.cargandoComunidades
    ) {
      return;
    }

    const usuario =
      this.leerUsuarioLocal();

    const usuarioId =
      Number(usuario?.usuarioId);

    if (
      !Number.isInteger(usuarioId)
      || usuarioId <= 0
    ) {
      return;
    }

    this.cargandoComunidades = true;

    this.usuarioContextoService
      .obtenerComunidades(usuarioId)
      .pipe(
        finalize(() => {
          this.cargandoComunidades = false;
        }),
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe({

        next: comunidades => {

          this.comunidades =
            comunidades ?? [];

          this.comunidadesCargadas =
            true;

          this.actualizarResultados();
        },

        error: error => {

          console.error(
            'No se pudieron cargar las comunidades para el buscador:',
            error
          );
        }
      });
  }

  private cargarDatosComunidadActiva(): void {

    const comunidadId =
      this.comunidadActivaId;

    if (
      !comunidadId
      || this.comunidadDatosCargadosId === comunidadId
      || this.cargandoDatosParaComunidadId === comunidadId
    ) {
      return;
    }

    this.cargandoDatosParaComunidadId =
      comunidadId;

    forkJoin({

      vecinos:
        this.vecinoService
          .getVecinosPorComunidad(
            comunidadId,
            0,
            2000,
            'todos'
          )
          .pipe(
            catchError(error => {

              console.error(
                'No se pudieron cargar los propietarios para el buscador:',
                error
              );

              return of({
                content: [] as Vecino[],
                totalElements: 0,
                totalPages: 0,
                number: 0,
                size: 2000
              });
            })
          ),

      recibos:
        this.reciboService
          .getRecibos(
            comunidadId
          )
          .pipe(
            catchError(error => {

              console.error(
                'No se pudieron cargar los recibos para el buscador:',
                error
              );

              return of(
                [] as Recibo[]
              );
            })
          ),

      remesas:
        this.remesaService
          .getRemesas(
            comunidadId
          )
          .pipe(
            catchError(error => {

              console.error(
                'No se pudieron cargar las remesas para el buscador:',
                error
              );

              return of(
                [] as Remesa[]
              );
            })
          ),

      incidencias:
        this.incidenciasService
          .listarPorComunidad(
            comunidadId
          )
          .pipe(
            catchError(error => {

              console.error(
                'No se pudieron cargar las incidencias para el buscador:',
                error
              );

              return of(
                [] as GestionIncidencia[]
              );
            })
          )
    })
      .pipe(
        finalize(() => {

          if (
            this.cargandoDatosParaComunidadId
            === comunidadId
          ) {
            this.cargandoDatosParaComunidadId =
              null;
          }
        }),
        takeUntilDestroyed(
          this.destroyRef
        )
      )
      .subscribe(datos => {

        if (
          this.comunidadActivaId
          !== comunidadId
        ) {
          return;
        }

        this.vecinos =
          datos.vecinos.content;

        this.recibos =
          datos.recibos;

        this.remesas =
          datos.remesas;

        this.incidencias =
          datos.incidencias;

        this.comunidadDatosCargadosId =
          comunidadId;

        this.actualizarResultados();
      });
  }

  private actualizarResultados(): void {

    const texto =
      this.normalizar(
        this.textoBusqueda
      );

    if (texto.length < 2) {
      this.resultados = [];
      return;
    }

    const resultados: ResultadoBusqueda[] = [
      ...this.resultadosSecciones(),
      ...this.resultadosComunidades(),
      ...this.resultadosPropietarios(),
      ...this.resultadosRecibos(),
      ...this.resultadosRemesas(),
      ...this.resultadosIncidencias()
    ];

    this.resultados =
      resultados
        .filter(resultado =>
          resultado.textoIndexado
            .includes(texto)
        )
        .sort((a, b) =>
            this.puntuacionResultado(
              a,
              texto
            )
            - this.puntuacionResultado(
              b,
              texto
            )
        )
        .slice(
          0,
          20
        );
  }

  private resultadosSecciones():
    ResultadoBusqueda[] {

    return this.secciones.map(
      (seccion, index) => ({

        clave:
          `seccion-${index}`,

        tipo:
          'SECCION',

        icono:
        seccion.icono,

        titulo:
        seccion.titulo,

        detalle:
        seccion.detalle,

        ruta:
        seccion.ruta,

        textoIndexado:
          this.normalizar(
            [
              seccion.titulo,
              seccion.detalle,
              seccion.palabrasClave
            ].join(' ')
          )
      })
    );
  }

  private resultadosComunidades():
    ResultadoBusqueda[] {

    return this.comunidades.map(
      comunidad => ({

        clave:
          `comunidad-${comunidad.id}`,

        tipo:
          'COMUNIDAD',

        icono:
          '🏢',

        titulo:
        comunidad.nombre,

        detalle:
          `Comunidad ID ${comunidad.id}`,

        ruta:
          `/comunidades/editar/${comunidad.id}`,

        textoIndexado:
          this.normalizar(
            `${comunidad.nombre} ${comunidad.id}`
          ),

        comunidadId:
        comunidad.id,

        comunidadNombre:
        comunidad.nombre
      })
    );
  }

  private resultadosPropietarios():
    ResultadoBusqueda[] {

    return this.vecinos.map(
      vecino => ({

        clave:
          `propietario-${vecino.id}`,

        tipo:
          'PROPIETARIO',

        icono:
          '👤',

        titulo:
        vecino.nombre,

        detalle:
          [
            vecino.vivienda
              ? `Vivienda ${vecino.vivienda}`
              : '',
            vecino.nif
              ? `NIF ${vecino.nif}`
              : ''
          ]
            .filter(Boolean)
            .join(' · ')
          || 'Propietario',

        ruta:
          `/vecinos/editar/${vecino.id}`,

        textoIndexado:
          this.normalizar(
            [
              vecino.id,
              vecino.nombre,
              vecino.vivienda,
              vecino.nif,
              vecino.email,
              vecino.telefono1,
              vecino.telefono2,
              vecino.telefono3
            ]
              .filter(valor =>
                valor !== null
                && valor !== undefined
              )
              .join(' ')
          ),

        comunidadId:
        vecino.comunidadId,

        comunidadNombre:
        this.comunidadActivaNombre
      })
    );
  }

  private resultadosRecibos():
    ResultadoBusqueda[] {

    return this.recibos.map(
      recibo => ({

        clave:
          `recibo-${recibo.id}`,

        tipo:
          'RECIBO',

        icono:
          '🧾',

        titulo:
          `Recibo ${recibo.id} · ${recibo.nombreVecino}`,

        detalle:
          [
            recibo.vivienda
              ? `Vivienda ${recibo.vivienda}`
              : '',
            `${Number(recibo.importe).toFixed(2)} €`,
            recibo.estado
          ]
            .filter(Boolean)
            .join(' · '),

        ruta:
          `/recibos/comunidad/${recibo.comunidadId}`,

        textoIndexado:
          this.normalizar(
            [
              recibo.id,
              recibo.nombreVecino,
              recibo.vivienda,
              recibo.concepto,
              recibo.estado,
              recibo.tipoRemesa,
              recibo.etiquetaExtra,
              recibo.importe
            ]
              .filter(valor =>
                valor !== null
                && valor !== undefined
              )
              .join(' ')
          ),

        comunidadId:
        recibo.comunidadId,

        comunidadNombre:
        this.comunidadActivaNombre
      })
    );
  }

  private resultadosRemesas():
    ResultadoBusqueda[] {

    return this.remesas.map(
      remesa => ({

        clave:
          `remesa-${remesa.id}`,

        tipo:
          'REMESA',

        icono:
          '🏦',

        titulo:
          `Remesa ${remesa.id}`,

        detalle:
          [
            remesa.fechaCobro
              ? `Cobro ${remesa.fechaCobro}`
              : '',
            `${Number(remesa.totalImporte).toFixed(2)} €`,
            remesa.estado
          ]
            .filter(Boolean)
            .join(' · '),

        ruta:
          `/remesas/${remesa.id}`,

        textoIndexado:
          this.normalizar(
            [
              remesa.id,
              remesa.identificadorFichero,
              remesa.nombreArchivo,
              remesa.estado,
              remesa.tipoRemesa,
              remesa.esquemaSepa,
              remesa.fechaCobro,
              remesa.totalImporte
            ]
              .filter(valor =>
                valor !== null
                && valor !== undefined
              )
              .join(' ')
          ),

        comunidadId:
        remesa.comunidadId,

        comunidadNombre:
        this.comunidadActivaNombre
      })
    );
  }

  private resultadosIncidencias():
    ResultadoBusqueda[] {

    return this.incidencias.map(
      incidencia => {

        const comunidadId =
          incidencia.comunidad?.id
          ?? this.comunidadActivaId
          ?? undefined;

        const comunidadNombre =
          incidencia.comunidad?.nombre
          || this.comunidadActivaNombre;

        return {

          clave:
            `incidencia-${incidencia.id}`,

          tipo:
            'INCIDENCIA',

          icono:
            '⚠️',

          titulo:
            `Incidencia ${incidencia.id} · ${incidencia.titulo}`,

          detalle:
            [
              incidencia.prioridad,
              incidencia.estado
            ]
              .filter(Boolean)
              .join(' · '),

          ruta:
            `/incidencias/${incidencia.id}`,

          textoIndexado:
            this.normalizar(
              [
                incidencia.id,
                incidencia.titulo,
                incidencia.descripcion,
                incidencia.observacionesInternas,
                incidencia.prioridad,
                incidencia.estado
              ]
                .filter(valor =>
                  valor !== null
                  && valor !== undefined
                )
                .join(' ')
            ),

          comunidadId,

          comunidadNombre
        };
      }
    );
  }

  private puntuacionResultado(
    resultado: ResultadoBusqueda,
    texto: string
  ): number {

    const titulo =
      this.normalizar(
        resultado.titulo
      );

    if (titulo === texto) {
      return 0;
    }

    if (
      titulo.startsWith(
        texto
      )
    ) {
      return 1;
    }

    if (
      titulo.includes(
        texto
      )
    ) {
      return 2;
    }

    return 3;
  }

  private normalizar(
    valor: unknown
  ): string {

    return String(
      valor
      ?? ''
    )
      .normalize('NFD')
      .replace(
        /[\u0300-\u036f]/g,
        ''
      )
      .toLowerCase()
      .trim();
  }

  private leerUsuarioLocal():
    UsuarioLocal | null {

    const contenido =
      localStorage.getItem(
        'usuario'
      );

    if (!contenido) {
      return null;
    }

    try {
      return JSON.parse(
        contenido
      ) as UsuarioLocal;

    } catch (error) {
      console.error(
        'No se pudo leer el usuario local:',
        error
      );

      return null;
    }
  }
}
