package edu.unisabana.dyas.reservas.dominio;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Objects;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Reserva de una sala de estudio de la biblioteca.
 *
 * <p>Es una clase del dominio: solo usa Java estándar. No conoce Spring, JPA ni HTTP.
 * Las reglas de arquitectura de {@code ArquitecturaTest} verifican que siga así.
 */
public final class Reserva {

    /** Hora de apertura de la biblioteca. */
    public static final LocalTime APERTURA = LocalTime.of(7, 0);
    /** Hora de cierre de la biblioteca. */
    public static final LocalTime CIERRE = LocalTime.of(21, 0);
    /** Duración máxima de una reserva. */
    public static final Duration DURACION_MAXIMA = Duration.ofHours(3);

    private static final Pattern CODIGO_SALA = Pattern.compile("[A-Z]-[0-9]{3}");
    private static final Pattern CORREO_INSTITUCIONAL =
            Pattern.compile("[A-Za-z0-9._-]{1,64}@unisabana[.]edu[.]co");

    private final UUID id;
    private final String sala;
    private final String responsable;
    private final LocalDate fecha;
    private final LocalTime inicio;
    private final LocalTime fin;
    private final String registradaPor;
    private EstadoReserva estado;

    private Reserva(UUID id, String sala, String responsable, LocalDate fecha, LocalTime inicio,
                    LocalTime fin, String registradaPor, EstadoReserva estado) {
        this.id = id;
        this.sala = sala;
        this.responsable = responsable;
        this.fecha = fecha;
        this.inicio = inicio;
        this.fin = fin;
        this.registradaPor = registradaPor;
        this.estado = estado;
    }

    /**
     * Crea una reserva nueva validando las reglas de negocio.
     *
     * @param hoy fecha actual; se recibe como parámetro para que el dominio no dependa del reloj del sistema
     */
    public static Reserva nueva(String sala, String responsable, LocalDate fecha, LocalTime inicio,
                                LocalTime fin, String registradaPor, LocalDate hoy) {
        validarSala(sala);
        validarResponsable(responsable);
        validarHorario(fecha, inicio, fin, hoy);
        if (registradaPor == null || registradaPor.isBlank()) {
            throw new ReservaInvalidaException("Toda reserva debe indicar quién la registró");
        }
        return new Reserva(UUID.randomUUID(), sala, responsable, fecha, inicio, fin, registradaPor,
                EstadoReserva.ACTIVA);
    }

    /** Reconstruye una reserva ya guardada (la usa el adaptador de persistencia). No vuelve a validar. */
    public static Reserva reconstruir(UUID id, String sala, String responsable, LocalDate fecha,
                                      LocalTime inicio, LocalTime fin, String registradaPor,
                                      EstadoReserva estado) {
        return new Reserva(Objects.requireNonNull(id), sala, responsable, fecha, inicio, fin, registradaPor,
                Objects.requireNonNull(estado));
    }

    /** Cancela la reserva. Una reserva cancelada no se puede volver a cancelar. */
    public void cancelar() {
        if (estado == EstadoReserva.CANCELADA) {
            throw new ReservaInvalidaException("La reserva " + id + " ya estaba cancelada");
        }
        estado = EstadoReserva.CANCELADA;
    }

    /** Indica si esta reserva ocupa la misma sala, el mismo día y en un horario que se cruza con la otra. */
    public boolean seCruzaCon(Reserva otra) {
        return estaActiva()
                && otra.estaActiva()
                && sala.equals(otra.sala)
                && fecha.equals(otra.fecha)
                && inicio.isBefore(otra.fin)
                && otra.inicio.isBefore(fin);
    }

    public boolean estaActiva() {
        return estado == EstadoReserva.ACTIVA;
    }

    private static void validarSala(String sala) {
        if (sala == null || !CODIGO_SALA.matcher(sala).matches()) {
            throw new ReservaInvalidaException("El código de sala debe tener el formato L-NNN, por ejemplo B-201");
        }
    }

    private static void validarResponsable(String responsable) {
        if (responsable == null || !CORREO_INSTITUCIONAL.matcher(responsable).matches()) {
            throw new ReservaInvalidaException("El responsable debe tener un correo @unisabana.edu.co");
        }
    }

    private static void validarHorario(LocalDate fecha, LocalTime inicio, LocalTime fin, LocalDate hoy) {
        if (fecha == null || inicio == null || fin == null) {
            throw new ReservaInvalidaException("La fecha, la hora de inicio y la hora de fin son obligatorias");
        }
        if (fecha.isBefore(hoy)) {
            throw new ReservaInvalidaException("No se puede reservar en una fecha pasada");
        }
        if (!inicio.isBefore(fin)) {
            throw new ReservaInvalidaException("La hora de inicio debe ser anterior a la hora de fin");
        }
        if (inicio.isBefore(APERTURA) || fin.isAfter(CIERRE)) {
            throw new ReservaInvalidaException("La reserva debe estar entre las " + APERTURA + " y las " + CIERRE);
        }
        if (Duration.between(inicio, fin).compareTo(DURACION_MAXIMA) > 0) {
            throw new ReservaInvalidaException("Una reserva no puede durar más de " + DURACION_MAXIMA.toHours() + " horas");
        }
    }

    public UUID getId() {
        return id;
    }

    public String getSala() {
        return sala;
    }

    public String getResponsable() {
        return responsable;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public LocalTime getInicio() {
        return inicio;
    }

    public LocalTime getFin() {
        return fin;
    }

    public String getRegistradaPor() {
        return registradaPor;
    }

    public EstadoReserva getEstado() {
        return estado;
    }
}
