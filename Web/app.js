const ADMIN_ROLE_ID = 3;
const ADMIN_DATA_REFRESH_MS = 60000;
const MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYW5kcmV3cmNybyIsImEiOiJjbWlzNmluem0waGJkM2dxMjcwejhrdHpyIn0.2L3Op-tGiAmSDlysfhwTsw";
const MAPBOX_SCRIPT_SRC = "./node_modules/mapbox-gl/dist/mapbox-gl.js";

const state = {
  apiBaseUrl: localStorage.getItem("cp.apiBaseUrl") || "http://localhost:5005",
  currentUser: JSON.parse(localStorage.getItem("cp.adminUser") || "null"),
  section: "overview",
  adminData: {
    users: [],
    trips: [],
    reservations: [],
    supportTickets: []
  },
  adminPayments: [],
  isLoadingSupportTickets: false,
  supportChatTicketId: null,
  supportChatPollId: null,
  editContext: null,
  detailContext: null,
  adminDataAutoRefreshId: null,
  isLoadingAdminData: false,
  tripMap: null,
  tripMapReady: false,
  tripSelectionMode: null,
  tripOrigin: null,
  tripDestination: null,
  tripOriginMarker: null,
  tripDestinationMarker: null,
  mapboxLoadPromise: null,
  detailTripMap: null,
  detailTripMapReady: false,
  detailTripOriginMarker: null,
  detailTripDestinationMarker: null,
  tripDrivers: [],
  isLoadingTripDrivers: false,
  safeZones: [],
  publicSafeZones: [],
  safeZoneOverviewMap: null,
  safeZoneOverviewMapReady: false,
  safeZoneOverviewMarkers: [],
  safeZoneEditMap: null,
  safeZoneEditMapReady: false,
  safeZoneDraftPoint: null,
  safeZoneEditMarker: null,
  createTripSafeZoneMarkers: [],
  detailTripSafeZoneMarkers: []
};

const authShell = document.getElementById("authShell");
const dashboard = document.getElementById("dashboard");
const loginMessage = document.getElementById("loginMessage");
const apiStatus = document.getElementById("apiStatus");
const sectionTitle = document.getElementById("sectionTitle");
const sectionDescription = document.getElementById("sectionDescription");
const loggedUserInfo = document.getElementById("loggedUserInfo");
const adminDataMessage = document.getElementById("adminDataMessage");
const adminUsersBody = document.getElementById("adminUsersBody");
const adminTripsBody = document.getElementById("adminTripsBody");
const adminReservationsBody = document.getElementById("adminReservationsBody");
const editModalOverlay = document.getElementById("editModalOverlay");
const editModalTitle = document.getElementById("editModalTitle");
const editModalForm = document.getElementById("editModalForm");
const editModalMessage = document.getElementById("editModalMessage");
const editModalCloseBtn = document.getElementById("editModalCloseBtn");
const detailsModalOverlay = document.getElementById("detailsModalOverlay");
const detailsModalTitle = document.getElementById("detailsModalTitle");
const detailsModalBody = document.getElementById("detailsModalBody");
const detailsModalMessage = document.getElementById("detailsModalMessage");
const detailsModalCloseBtn = document.getElementById("detailsModalCloseBtn");
const createModalOverlay = document.getElementById("createModalOverlay");
const createModalTitle = document.getElementById("createModalTitle");
const createModalForm = document.getElementById("createModalForm");
const createModalMessage = document.getElementById("createModalMessage");
const createModalCloseBtn = document.getElementById("createModalCloseBtn");
const refreshAdminDataBtn = document.getElementById("refreshAdminDataBtn");
const createEntityType = document.getElementById("createEntityType");
const createModalFields = document.getElementById("createModalFields");
const supportDataMessage = document.getElementById("supportDataMessage");
const adminSupportBody = document.getElementById("adminSupportBody");
const supportFilterInput = document.getElementById("supportFilterInput");
const supportStatusFilter = document.getElementById("supportStatusFilter");
const supportCategoryFilter = document.getElementById("supportCategoryFilter");
const supportReloadBtn = document.getElementById("supportReloadBtn");
const adminPaymentsBody = document.getElementById("adminPaymentsBody");
const paymentsFilterInput = document.getElementById("paymentsFilterInput");
const paymentsStatusFilter = document.getElementById("paymentsStatusFilter");
const paymentsReloadBtn = document.getElementById("paymentsReloadBtn");
const adminSafeZonesBody = document.getElementById("adminSafeZonesBody");
const safeZonesDataMessage = document.getElementById("safeZonesDataMessage");
const safeZonesFilterInput = document.getElementById("safeZonesFilterInput");
const safeZonesStatusFilter = document.getElementById("safeZonesStatusFilter");
const safeZonesReloadBtn = document.getElementById("safeZonesReloadBtn");
const safeZoneCreateBtn = document.getElementById("safeZoneCreateBtn");
const safeZoneModalOverlay = document.getElementById("safeZoneModalOverlay");
const safeZoneModalForm = document.getElementById("safeZoneModalForm");
const safeZoneModalTitle = document.getElementById("safeZoneModalTitle");
const safeZoneModalCloseBtn = document.getElementById("safeZoneModalCloseBtn");
const safeZoneModalMessage = document.getElementById("safeZoneModalMessage");
const safeZoneCoordsHint = document.getElementById("safeZoneCoordsHint");

function getCreateTripMapContainer() {
  return createModalForm?.querySelector("#createTripMap") || null;
}

document.getElementById("apiBaseUrl")?.value && (document.getElementById("apiBaseUrl").value = state.apiBaseUrl);

function sanitizeWebError(rawError) {
  if (!rawError) {
    return "Ha ocurrido un error inesperado. Por favor, inténtalo de nuevo.";
  }
  const errorMsg = String(rawError).trim();
  const lower = errorMsg.toLowerCase();
  if (lower.includes("fetch") || lower.includes("connect") || lower.includes("network") ||
      lower.includes("timeout") || lower.includes("socket") || lower.includes("unable to resolve host") ||
      lower.includes("http") || lower.includes("connection")) {
    return "No se pudo establecer conexión con el servidor. Verifica que el backend esté corriendo en http://localhost:5005 o http://localhost:5000";
  }
  return errorMsg;
}

function setMessage(element, text, kind = "") {
  if (!element) {
    return;
  }

  let displayText = text;
  if (kind === "error") {
    displayText = sanitizeWebError(text);
  }

  element.textContent = displayText;
  element.classList.remove("success", "error");

  if (kind) {
    element.classList.add(kind);
  }
}

function toPrettyJson(value) {
  return JSON.stringify(value, null, 2);
}

function formatDateTime(value) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) {
    return "Fecha no disponible";
  }

  return date.toLocaleString();
}

function formatCoordinates(lat, lng) {
  return `Lat: ${Number(lat).toFixed(5)}, Lng: ${Number(lng).toFixed(5)}`;
}

function getTripOriginCoordinates(trip) {
  const origin = trip?.origin ?? trip?.Origin ?? null;

  return {
    lat: origin?.latitude ?? trip?.originLatitude ?? trip?.OriginLatitude ?? null,
    lng: origin?.longitude ?? trip?.originLongitude ?? trip?.OriginLongitude ?? null
  };
}

function getTripDestinationCoordinates(trip) {
  const destination = trip?.destination ?? trip?.Destination ?? null;

  return {
    lat: destination?.latitude ?? trip?.destinationLatitude ?? trip?.DestinationLatitude ?? null,
    lng: destination?.longitude ?? trip?.destinationLongitude ?? trip?.DestinationLongitude ?? null
  };
}

function hasTripCoordinates(point) {
  return point?.lat != null && point?.lng != null;
}

function isDriverUser(user) {
  return Number(user?.roleId) === 2 || String(user?.role || "").toLowerCase() === "driver";
}

function getReservationStatusKey(value) {
  const normalized = String(value ?? "").trim().toLowerCase();

  if (normalized === "1" || normalized === "pending" || normalized === "pendiente") return "pending";
  if (normalized === "2" || normalized === "confirmed" || normalized === "confirmado") return "confirmed";
  if (normalized === "3" || normalized === "boarded" || normalized === "abordado") return "boarded";
  if (normalized === "4" || normalized === "cancelled" || normalized === "cancelado") return "cancelled";

  // legacy mapping if any
  if (normalized === "active") return "confirmed";

  return "unknown";
}

function formatReservationStatus(value) {
  const key = getReservationStatusKey(value);

  if (key === "pending") return "Pendiente";
  if (key === "confirmed") return "Confirmado";
  if (key === "boarded") return "Abordado";
  if (key === "cancelled") return "Cancelado";

  return "Sin estado";
}

function renderReservationCards(reservations, emptyMessage) {
  if (!Array.isArray(reservations) || reservations.length === 0) {
    return `
      <div class="empty-state">
        <strong>Sin registros</strong>
        <p>${escapeHtml(emptyMessage)}</p>
      </div>
    `;
  }

  return reservations.map((reservation) => `
    <article class="reservation-card">
      <div class="reservation-card__header">
        <div>
          <h4>${escapeHtml(reservation.passengerName || "Pasajero sin nombre")}</h4>
          <p>ID viaje: ${escapeHtml(reservation.tripId || "-")}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(getReservationStatusKey(reservation.status))}">
          ${escapeHtml(formatReservationStatus(reservation.status))}
        </span>
      </div>
      <div class="reservation-card__meta">
        <span><strong>Creada:</strong> ${escapeHtml(formatDateTime(reservation.createdAt))}</span>
        ${reservation.id ? `<span><strong>Reserva:</strong> ${escapeHtml(reservation.id)}</span>` : ""}
      </div>
    </article>
  `).join("");
}

function getTripStatusKey(value) {
  const normalized = String(value ?? "").trim().toLowerCase();

  if (normalized === "0" || normalized === "1" || normalized === "awaitingdestination" || normalized === "scheduled" || normalized === "programado" || normalized === "esperando destino") return "awaitingdestination";
  if (normalized === "2" || normalized === "ready" || normalized === "listo") return "ready";
  if (normalized === "3" || normalized === "inprogress" || normalized === "in_progress" || normalized === "en curso" || normalized === "en_curso") return "inprogress";
  if (normalized === "4" || normalized === "finished" || normalized === "finalizado") return "finished";
  if (normalized === "5" || normalized === "cancelled" || normalized === "cancelado") return "cancelled";

  return "unknown";
}

function formatTripStatus(value) {
  const key = getTripStatusKey(value);

  if (key === "awaitingdestination") return "Esperando destino";
  if (key === "ready") return "Listo";
  if (key === "inprogress") return "En curso";
  if (key === "cancelled") return "Cancelado";
  if (key === "finished") return "Finalizado";

  return "Sin estado";
}

function formatTripKind(value) {
  const normalized = String(value ?? "").toLowerCase();
  if (normalized === "regular") return "Regular";
  return value || "Sin tipo";
}

function renderUserDetails(user) {
  if (!user) {
    return `
      <div class="empty-state">
        <strong>Sin resultados</strong>
        <p>Busca un correo institucional para ver los datos del usuario.</p>
      </div>
    `;
  }

  const driverProfile = user.driverProfile || user.DriverProfile;
  const vehicles = Array.isArray(user.vehicles) ? user.vehicles : Array.isArray(user.Vehicles) ? user.Vehicles : [];

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>${escapeHtml(user.fullName || "Usuario")}</h4>
          <p>${escapeHtml(user.email || "Sin email")}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(String(user.role || "unknown").toLowerCase())}">
          ${escapeHtml(String(user.role || "Sin rol"))}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>ID:</strong> ${escapeHtml(user.id || "-")}</div>
        <div><strong>Role ID:</strong> ${escapeHtml(user.roleId ?? "-")}</div>
        <div><strong>Telefono:</strong> ${escapeHtml(user.phoneNumber || "-")}</div>
        <div><strong>Creado:</strong> ${escapeHtml(formatDateTime(user.createdAt))}</div>
        <div><strong>Vehiculos:</strong> ${escapeHtml(vehicles.length)}</div>
      </div>

      ${driverProfile ? `
        <div class="detail-card__section">
          <h5>Perfil de conductor</h5>
          <div class="detail-grid">
            <div><strong>Asientos:</strong> ${escapeHtml(driverProfile.availableSeats ?? "-")}</div>
            <div><strong>Placa:</strong> ${escapeHtml(driverProfile.licensePlate || "-")}</div>
            <div><strong>Marca:</strong> ${escapeHtml(driverProfile.vehicleBrand || "-")}</div>
            <div><strong>Color:</strong> ${escapeHtml(driverProfile.vehicleColor || "-")}</div>
          </div>
        </div>
      ` : ""}

      ${vehicles.length ? `
        <div class="detail-card__section">
          <h5>Vehiculos registrados</h5>
          <div class="detail-stack">
            ${vehicles.map((vehicle) => `
              <article class="detail-card detail-card--nested">
                <div class="detail-grid">
                  <div><strong>ID:</strong> ${escapeHtml(vehicle.id || "-")}</div>
                  <div><strong>Placa:</strong> ${escapeHtml(vehicle.licensePlate || "-")}</div>
                  <div><strong>Marca:</strong> ${escapeHtml(vehicle.brand || "-")}</div>
                  <div><strong>Modelo:</strong> ${escapeHtml(vehicle.model || "-")}</div>
                  <div><strong>Color:</strong> ${escapeHtml(vehicle.color || "-")}</div>
                  <div><strong>Año:</strong> ${escapeHtml(vehicle.vehicleYear ?? "-")}</div>
                  <div><strong>Asientos:</strong> ${escapeHtml(vehicle.totalSeats ?? "-")}</div>
                  <div><strong>Activo:</strong> ${escapeHtml(vehicle.isActive ? "Sí" : "No")}</div>
                  <div><strong>Verificado:</strong> ${escapeHtml(vehicle.isVerified ? "Sí" : "No")}</div>
                </div>
              </article>
            `).join("")}
          </div>
        </div>
      ` : ""}
    </article>
  `;
}

function renderTripDetails(trip) {
  if (!trip) {
    return `
      <div class="empty-state">
        <strong>Sin resultados</strong>
        <p>Consulta un Trip ID para ver la información del viaje.</p>
      </div>
    `;
  }

  const destination = getTripDestinationCoordinates(trip);
  const hasDestination = hasTripCoordinates(destination);
  const origin = getTripOriginCoordinates(trip);
  const tripStatusValue = trip.statusLabel ?? trip.statusId ?? trip.status;

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>${escapeHtml(trip.driverName || "Viaje")}</h4>
          <p>${escapeHtml(formatTripKind(trip.kind))}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(getTripStatusKey(tripStatusValue))}">
          ${escapeHtml(formatTripStatus(tripStatusValue))}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>ID:</strong> ${escapeHtml(trip.id || "-")}</div>
        <div><strong>Conductor:</strong> ${escapeHtml(trip.driverName || "-")}</div>
        <div><strong>Conductor ID:</strong> ${escapeHtml(trip.driverUserId || "-")}</div>
        <div><strong>Vehiculo ID:</strong> ${escapeHtml(trip.vehicleId || "-")}</div>
        <div><strong>Tipo:</strong> ${escapeHtml(formatTripKind(trip.kind))}</div>
        <div><strong>Estado:</strong> ${escapeHtml(formatTripStatus(tripStatusValue))}</div>
        <div><strong>Asientos ofrecidos:</strong> ${escapeHtml(trip.offeredSeats ?? "-")}</div>
        <div><strong>Cupos:</strong> ${escapeHtml(trip.availableSeats ?? "-")}</div>
        <div><strong>Origen:</strong> ${escapeHtml(hasTripCoordinates(origin) ? `${origin.lat}, ${origin.lng}` : "Sin origen")}</div>
        <div><strong>Destino:</strong> ${escapeHtml(hasDestination ? `${destination.lat}, ${destination.lng}` : "Sin destino")}</div>
        <div><strong>Creado:</strong> ${escapeHtml(formatDateTime(trip.createdAt))}</div>
        <div><strong>Actualizado:</strong> ${escapeHtml(trip.updatedAt ? formatDateTime(trip.updatedAt) : "Sin cambios")}</div>
        <div><strong>Cancelado:</strong> ${escapeHtml(trip.cancelledAt ? formatDateTime(trip.cancelledAt) : "No")}</div>
      </div>

      <div class="detail-card__section">
        <h5>Mapa del viaje</h5>
        <div id="detailTripMap" class="trip-map" aria-label="Mapa con origen y destino del viaje"></div>
        <p class="hint-box">Origen y destino se muestran juntos para revisar la ruta antes de editar o eliminar.</p>
      </div>
    </article>
  `;
}

function renderReservationDetails(reservation) {
  if (!reservation) {
    return `
      <div class="empty-state">
        <strong>Sin resultados</strong>
        <p>Consulta una reserva para ver todos sus datos.</p>
      </div>
    `;
  }

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>${escapeHtml(reservation.passengerName || "Reserva")}</h4>
          <p>Viaje: ${escapeHtml(reservation.tripId || "-")}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(getReservationStatusKey(reservation.status))}">
          ${escapeHtml(formatReservationStatus(reservation.status))}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>ID:</strong> ${escapeHtml(reservation.id || "-")}</div>
        <div><strong>Trip ID:</strong> ${escapeHtml(reservation.tripId || "-")}</div>
        <div><strong>Pasajero ID:</strong> ${escapeHtml(reservation.passengerUserId || "-")}</div>
        <div><strong>Pasajero:</strong> ${escapeHtml(reservation.passengerName || "-")}</div>
        <div><strong>Asientos reservados:</strong> ${escapeHtml(reservation.seatsReserved ?? "-")}</div>
        <div><strong>Estado:</strong> ${escapeHtml(formatReservationStatus(reservation.status))}</div>
        <div><strong>Status ID:</strong> ${escapeHtml(reservation.statusId ?? "-")}</div>
        <div><strong>Codigo de abordaje:</strong> ${escapeHtml(reservation.boardingCode || "-")}</div>
        <div><strong>Creada:</strong> ${escapeHtml(formatDateTime(reservation.createdAt))}</div>
      </div>
    </article>
  `;
}

function renderSupportDetails(ticket) {
  if (!ticket) {
    return `
      <div class="empty-state">
        <strong>Sin resultados</strong>
        <p>Selecciona un reporte para ver todos los detalles.</p>
      </div>
    `;
  }

  const tripLine = ticket.tripId ? `<div><strong>Viaje:</strong> ${escapeHtml(ticket.tripId)}</div>` : "";
  const reservationLine = ticket.reservationId ? `<div><strong>Reserva:</strong> ${escapeHtml(ticket.reservationId)}</div>` : "";

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>${escapeHtml(ticket.subject || "Reporte")}</h4>
          <p>${escapeHtml(ticket.userFullName || "Usuario")}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(getSupportStatusKey(ticket.status))}">
          ${escapeHtml(ticket.statusLabel || "Abierto")}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>ID:</strong> ${escapeHtml(ticket.id || "-")}</div>
        <div><strong>Usuario ID:</strong> ${escapeHtml(ticket.userId || "-")}</div>
        <div><strong>Usuario:</strong> ${escapeHtml(ticket.userFullName || "-")}</div>
        <div><strong>Categoria:</strong> ${escapeHtml(ticket.categoryLabel || "-")}</div>
        <div><strong>Estado:</strong> ${escapeHtml(ticket.statusLabel || "-")}</div>
        <div><strong>Asunto:</strong> ${escapeHtml(ticket.subject || "-")}</div>
        <div><strong>Creado:</strong> ${escapeHtml(formatDateTime(ticket.createdAt))}</div>
        <div><strong>Actualizado:</strong> ${escapeHtml(ticket.updatedAt ? formatDateTime(ticket.updatedAt) : "Sin cambios")}</div>
      </div>

      <div class="detail-card__section">
        <h5>Descripcion</h5>
        <p>${escapeHtml(ticket.description || "-")}</p>
      </div>

      <div class="detail-card__section">
        <h5>Referencias</h5>
        <div class="detail-grid">
          ${tripLine ? `<div>${tripLine}</div>` : ""}
          ${reservationLine ? `<div>${reservationLine}</div>` : ""}
        </div>
      </div>
    </article>
  `;
}

function escapeHtml(value) {
  return String(value ?? "")
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");
}

function normalizeUrl(url) {
  return url.replace(/\/$/, "");
}

function isLocalApiUrl(url) {
  return /localhost|127\.0\.0\.1|0\.0\.0\.0/i.test(String(url || ""));
}

async function isApiReachable(url) {
  try {
    const response = await fetch(`${normalizeUrl(url)}/api/safe-zones`, {
      method: "GET",
      headers: {
        Accept: "application/json"
      },
      cache: "no-store"
    });

    return response.ok || response.status === 401 || response.status === 403;
  } catch {
    return false;
  }
}

async function resolveApiBaseUrl() {
  const persistedUrl = localStorage.getItem("cp.apiBaseUrl");
  const localCandidates = [
    "http://localhost:5005",
    "http://localhost:5000",
    "http://127.0.0.1:5005",
    "http://127.0.0.1:5000"
  ];

  const candidates = [];
  if (persistedUrl) {
    candidates.push(persistedUrl);
    if (!isLocalApiUrl(persistedUrl)) {
      return persistedUrl;
    }
  }

  for (const candidate of localCandidates) {
    if (!candidates.includes(candidate)) {
      candidates.push(candidate);
    }
  }

  for (const candidate of candidates) {
    if (await isApiReachable(candidate)) {
      state.apiBaseUrl = candidate;
      localStorage.setItem("cp.apiBaseUrl", candidate);
      return candidate;
    }
  }

  if (persistedUrl) {
    state.apiBaseUrl = persistedUrl;
    return persistedUrl;
  }

  return state.apiBaseUrl;
}

async function apiFetch(path, options = {}) {
  const { headers: optionHeaders = {}, ...restOptions } = options;

  const response = await fetch(`${normalizeUrl(state.apiBaseUrl)}${path}`, {
    ...restOptions,
    headers: {
      "Content-Type": "application/json",
      ...optionHeaders
    }
  });

  let data = null;
  const text = await response.text();
  if (text) {
    try {
      data = JSON.parse(text);
    } catch {
      data = text;
    }
  }

  if (!response.ok) {
    const message = typeof data === "string" ? data : data?.message || "Error en la solicitud.";
    throw new Error(message);
  }

  return data;
}

function updateApiStatus(isOnline, extraText = "") {
  apiStatus.textContent = isOnline ? `API conectada${extraText ? `: ${extraText}` : ""}` : "API desconectada";
  apiStatus.classList.toggle("online", isOnline);
}

function openSection(section) {
  state.section = section;
  const sectionMeta = getSectionMeta(section);
  sectionTitle.textContent = sectionMeta.name;
  sectionDescription.textContent = sectionMeta.description;

  document.querySelectorAll(".menu-item").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.section === section);
  });

  document.querySelectorAll("[id^='section-']").forEach((panel) => {
    panel.classList.add("hidden");
  });

  document.getElementById(`section-${section}`).classList.remove("hidden");

  if (["overview", "users", "trips", "reservations", "reports", "admins"].includes(section) && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadAdminData();
  }

  if (section === "support" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadSupportTickets();
  }

  if (section === "payments" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadAdminPayments();
  }

  if (section === "settings" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadThemeSettings();
  }

  if (section === "safe-zones" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadSafeZonesAdmin();
  }
}

function getSectionMeta(section) {
  const map = {
    overview: {
      name: "Resumen",
      description: "Vista general del estado del panel." 
    },
    users: {
      name: "Usuarios",
      description: "Consulta todas las cuentas registradas y agrega nuevos usuarios." 
    },
    trips: {
      name: "Viajes",
      description: "Consulta los viajes registrados y agrega nuevos viajes." 
    },
    reservations: {
      name: "Reservas",
      description: "Consulta las reservas registradas y agrega nuevas reservas."
    },
    reports: {
      name: "Reportes",
      description: "Explora usuarios, viajes y reservas en una sola vista." 
    },
    support: {
      name: "Soporte",
      description: "Revisa quejas de usuarios y actualiza el estado de cada reporte."
    },
    "safe-zones": {
      name: "Zonas seguras",
      description: "Administra paradas tranquilas visibles en el mapa de la app movil."
    },
    admins: {
      name: "Admins",
      description: "Gestiona las cuentas administrativas y define sus roles y permisos."
    },
    payments: {
      name: "Pagos",
      description: "Consulta todos los pagos y reembolsos registrados en el sistema." 
    },
    settings: {
      name: "Ajustes",
      description: "Personaliza la identidad visual y los colores corporativos de tu negocio."
    }
  };

  return map[section] || map.overview;
}

function applyTableFilter(containerId, searchTerm) {
  const tbody = document.getElementById(containerId);
  if (!tbody) {
    return;
  }

  const normalizedSearch = String(searchTerm || "").trim().toLowerCase();
  const rows = Array.from(tbody.querySelectorAll("tr"));

  rows.forEach((row) => {
    const isEmptyRow = row.children.length === 1;
    if (isEmptyRow) {
      row.style.display = "";
      return;
    }

    if (!normalizedSearch) {
      row.style.display = "";
      return;
    }

    const rowText = row.textContent?.toLowerCase() || "";
    row.style.display = rowText.includes(normalizedSearch) ? "" : "none";
  });
}

function applyAllTableFilters() {
  applyTableFilter("adminUsersBody", document.getElementById("usersFilterInput")?.value || "");
  applyTableFilter("adminTripsBody", document.getElementById("tripsFilterInput")?.value || "");
  applyTableFilter("adminReservationsBody", document.getElementById("reservationsFilterInput")?.value || "");
  applyTableFilter("adminSupportBody", supportFilterInput?.value || "");
}

function formatSupportReference(ticketId) {
  if (!ticketId) {
    return "------";
  }

  const compact = String(ticketId).replace(/-/g, "");
  return compact.length >= 8 ? compact.substring(0, 8).toUpperCase() : compact.toUpperCase();
}

function getSupportStatusKey(status) {
  const numeric = Number(status);
  if (numeric === 1) return "open";
  if (numeric === 2) return "inreview";
  if (numeric === 3) return "resolved";
  if (numeric === 4) return "closed";

  const label = String(status || "").trim().toLowerCase();
  if (label.includes("abierto") || label === "open") return "open";
  if (label.includes("revis") || label.includes("review")) return "inreview";
  if (label.includes("resuelt") || label.includes("resolved")) return "resolved";
  if (label.includes("cerrad") || label.includes("closed")) return "closed";
  return "open";
}

function getSupportStatusValue(status) {
  const key = getSupportStatusKey(status);
  if (key === "inreview") return "2";
  if (key === "resolved") return "3";
  if (key === "closed") return "4";
  return "1";
}

function updateDriverFieldsVisibility() {
  const role = document.getElementById("regRole")?.value;
  const isDriver = role === "driver";
  const container = document.getElementById("regDriverFields");
  const seatsInput = document.getElementById("regSeats");
  const plateInput = document.getElementById("regPlate");
  const brandInput = document.getElementById("regBrand");
  const colorInput = document.getElementById("regColor");

  if (container) {
    container.classList.toggle("hidden", !isDriver);
  }

  if (seatsInput) seatsInput.required = isDriver;
  if (plateInput) plateInput.required = isDriver;
  if (brandInput) brandInput.required = isDriver;
  if (colorInput) colorInput.required = isDriver;
}

function getCreateUserOptions() {
  return (state.adminData.users || [])
    .map((user) => `<option value="${escapeHtml(user.id)}">${escapeHtml(user.fullName || "Usuario")} - ${escapeHtml(user.email || "")}</option>`)
    .join("");
}

function getCreateDriverOptions() {
  const drivers = (state.adminData.users || []).filter(isDriverUser);

  if (!drivers.length) {
    return '<option value="" selected disabled>No hay conductores disponibles</option>';
  }

  return [`<option value="" selected disabled>Selecciona un conductor</option>`, ...drivers.map((driver) => `
    <option value="${escapeHtml(driver.id)}">${escapeHtml(driver.fullName || "Conductor")} - ${escapeHtml(driver.email || "")}</option>
  `)].join("");
}

function getCreateModalMarkup(type) {
  if (type === "trip") {
    return `
      <label class="field">
        <span>Conductor registrado</span>
        <select id="createTripDriverSelect">
          ${getCreateDriverOptions()}
        </select>
      </label>
      <label class="field"><span>Nombre del conductor</span><input type="text" id="createTripDriverName" placeholder="Ej: Ana Perez" required /></label>
      <label class="field"><span>Driver User ID</span><input type="text" id="createTripDriverUserIdDisplay" placeholder="Se completa automaticamente" readonly /></label>
      <label class="field"><span>Asientos ofrecidos</span>
        <select id="createOfferedSeats">
          <option value="1">1</option>
          <option value="2">2</option>
          <option value="3">3</option>
          <option value="4" selected>4</option>
          <option value="5">5</option>
        </select>
      </label>
      <div class="trip-map-shell">
        <div class="trip-map-actions">
          <button type="button" class="btn ghost active" data-trip-point="origin">Marcar origen</button>
          <button type="button" class="btn ghost" data-trip-point="destination">Marcar destino</button>
          <button type="button" class="btn ghost" data-trip-action="clear-trip-points">Limpiar mapa</button>
        </div>
        <div id="createTripMap" class="trip-map" aria-label="Mapa para seleccionar origen y destino"></div>
        <p class="hint-box">Haz clic en el mapa para asignar el punto activo. Primero marca el origen y luego el destino.</p>
        <input type="hidden" id="createOriginLatitude" />
        <input type="hidden" id="createOriginLongitude" />
        <input type="hidden" id="createDestinationLatitude" />
        <input type="hidden" id="createDestinationLongitude" />
        <div class="trip-coordinates-grid">
          <div class="trip-coord-box">
            <strong>Origen</strong>
            <p id="createTripOriginSummary">Pendiente de selección</p>
          </div>
          <div class="trip-coord-box">
            <strong>Destino</strong>
            <p id="createTripDestinationSummary">Pendiente de selección</p>
          </div>
        </div>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn ghost" data-create-action="cancel">Cancelar</button>
        <button type="submit" class="btn primary">Crear viaje</button>
      </div>
    `;
  }

  if (type === "reservation") {
    return `
      <label class="field"><span>Trip ID</span><input type="text" id="createReservationTripId" placeholder="ID del viaje" required /></label>
      <label class="field">
        <span>Pasajero registrado</span>
        <select id="createReservationPassengerSelect">
          ${getCreateUserOptions() || '<option value="" selected disabled>No hay usuarios disponibles</option>'}
        </select>
      </label>
      <label class="field"><span>Passenger User ID</span><input type="text" id="createReservationPassengerUserIdDisplay" placeholder="Se completa automaticamente" readonly /></label>
      <label class="field"><span>Asientos reservados</span>
        <select id="createReservationSeatsReserved">
          <option value="1" selected>1</option>
          <option value="2">2</option>
          <option value="3">3</option>
          <option value="4">4</option>
          <option value="5">5</option>
        </select>
      </label>
      <div class="modal-actions">
        <button type="button" class="btn ghost" data-create-action="cancel">Cancelar</button>
        <button type="submit" class="btn primary">Crear reserva</button>
      </div>
    `;
  }

  return `
    <label class="field"><span>Nombre completo</span><input type="text" id="createFullName" autocomplete="name" placeholder="Ej: Ana Perez" required /></label>
    <label class="field"><span>Email</span><input type="email" id="createEmail" autocomplete="email" placeholder="usuario@univalle.edu" required /></label>
    <label class="field"><span>Contrasena</span><input type="password" id="createPassword" autocomplete="new-password" placeholder="Minimo 6 caracteres" minlength="6" required /></label>
    <label class="field"><span>Telefono (opcional)</span><input type="text" id="createPhone" autocomplete="tel" placeholder="Ej: 7654321" /></label>
    <label class="field"><span>Rol</span>
      <select id="createRole">
        <option value="student">Estudiante</option>
        <option value="driver">Chofer</option>
      </select>
    </label>
    <div id="createDriverFields" class="driver-fields hidden">
      <label class="field"><span>Cantidad de personas (asientos)</span>
        <select id="createSeats">
          <option value="1">1</option>
          <option value="2">2</option>
          <option value="3">3</option>
          <option value="4" selected>4</option>
          <option value="5">5</option>
        </select>
      </label>
      <label class="field"><span>Placa del auto</span><input type="text" id="createPlate" minlength="5" placeholder="Ej: 1234-ABC" /></label>
      <label class="field"><span>Marca del auto</span><input type="text" id="createBrand" minlength="2" placeholder="Ej: Toyota" /></label>
      <label class="field"><span>Modelo del auto</span><input type="text" id="createModel" minlength="2" placeholder="Ej: Corolla" /></label>
      <label class="field"><span>Año del auto</span><input type="number" id="createYear" min="1900" max="2100" placeholder="Ej: 2024" /></label>
      <label class="field"><span>Color del auto</span><input type="text" id="createColor" minlength="2" placeholder="Ej: Blanco" /></label>
    </div>
    <div class="modal-actions">
      <button type="button" class="btn ghost" data-create-action="cancel">Cancelar</button>
      <button type="submit" class="btn primary">Crear usuario</button>
    </div>
  `;
}

function updateCreateUserDriverFieldsVisibility() {
  const role = createModalForm?.querySelector("#createRole")?.value;
  const isDriver = role === "driver";
  const container = createModalForm?.querySelector("#createDriverFields");
  const seatsInput = createModalForm?.querySelector("#createSeats");
  const plateInput = createModalForm?.querySelector("#createPlate");
  const brandInput = createModalForm?.querySelector("#createBrand");
  const modelInput = createModalForm?.querySelector("#createModel");
  const yearInput = createModalForm?.querySelector("#createYear");
  const colorInput = createModalForm?.querySelector("#createColor");

  if (container) {
    container.classList.toggle("hidden", !isDriver);
  }

  if (seatsInput) seatsInput.required = isDriver;
  if (plateInput) plateInput.required = isDriver;
  if (brandInput) brandInput.required = isDriver;
  if (modelInput) modelInput.required = isDriver;
  if (yearInput) yearInput.required = isDriver;
  if (colorInput) colorInput.required = isDriver;
}

function formatTripPoint(point) {
  if (!point) {
    return "Pendiente de selección";
  }

  return `${point.lat.toFixed(6)}, ${point.lng.toFixed(6)}`;
}

function resetCreateTripMapState() {
  state.tripSelectionMode = "origin";
  state.tripOrigin = null;
  state.tripDestination = null;
}

function destroyCreateTripMap() {
  if (state.tripOriginMarker) {
    state.tripOriginMarker.remove();
    state.tripOriginMarker = null;
  }

  if (state.tripDestinationMarker) {
    state.tripDestinationMarker.remove();
    state.tripDestinationMarker = null;
  }

  clearSafeZoneMarkers(state.createTripSafeZoneMarkers);

  if (state.tripMap) {
    state.tripMap.remove();
    state.tripMap = null;
  }

  state.tripMapReady = false;
}

function updateCreateTripMapPanels() {
  const originSummary = createModalForm?.querySelector("#createTripOriginSummary");
  const destinationSummary = createModalForm?.querySelector("#createTripDestinationSummary");
  const originLatitude = createModalForm?.querySelector("#createOriginLatitude");
  const originLongitude = createModalForm?.querySelector("#createOriginLongitude");
  const destinationLatitude = createModalForm?.querySelector("#createDestinationLatitude");
  const destinationLongitude = createModalForm?.querySelector("#createDestinationLongitude");
  const originButton = createModalForm?.querySelector("[data-trip-point='origin']");
  const destinationButton = createModalForm?.querySelector("[data-trip-point='destination']");

  if (originSummary) originSummary.textContent = formatTripPoint(state.tripOrigin);
  if (destinationSummary) destinationSummary.textContent = formatTripPoint(state.tripDestination);
  if (originLatitude) originLatitude.value = state.tripOrigin?.lat ?? "";
  if (originLongitude) originLongitude.value = state.tripOrigin?.lng ?? "";
  if (destinationLatitude) destinationLatitude.value = state.tripDestination?.lat ?? "";
  if (destinationLongitude) destinationLongitude.value = state.tripDestination?.lng ?? "";

  if (originButton) originButton.classList.toggle("active", state.tripSelectionMode === "origin");
  if (destinationButton) destinationButton.classList.toggle("active", state.tripSelectionMode === "destination");
}

async function updateCreateTripMapLayers() {
  if (!state.tripMap || !state.tripMapReady || typeof mapboxgl === "undefined") {
    return;
  }

  const originCoordinates = state.tripOrigin ? [state.tripOrigin.lng, state.tripOrigin.lat] : null;
  const destinationCoordinates = state.tripDestination ? [state.tripDestination.lng, state.tripDestination.lat] : null;
  let routeSource = state.tripMap.getSource("create-trip-route");

  if (!routeSource) {
    state.tripMap.addSource("create-trip-route", {
      type: "geojson",
      data: {
        type: "FeatureCollection",
        features: []
      }
    });

    routeSource = state.tripMap.getSource("create-trip-route");
    state.tripMap.addLayer({
      id: "create-trip-route-line",
      type: "line",
      source: "create-trip-route",
      layout: { "line-join": "round", "line-cap": "round" },
      paint: {
        "line-color": "#5f7f6c",
        "line-width": 4
      }
    });
  }

  let coordinates = [];
  if (originCoordinates && destinationCoordinates) {
    try {
      const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${originCoordinates[0]},${originCoordinates[1]};${destinationCoordinates[0]},${destinationCoordinates[1]}?geometries=geojson&overview=full&access_token=${MAPBOX_ACCESS_TOKEN}`;
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("Respuesta no exitosa de la API de Mapbox Directions");
      }
      const data = await response.json();
      if (data.routes && data.routes.length > 0) {
        coordinates = data.routes[0].geometry.coordinates;
      } else {
        coordinates = [originCoordinates, destinationCoordinates];
      }
    } catch (e) {
      console.warn("Fallo al obtener la ruta real de Mapbox, usando línea recta:", e);
      coordinates = [originCoordinates, destinationCoordinates];
    }
  }

  routeSource.setData({
    type: "FeatureCollection",
    features: coordinates.length > 0
      ? [{
          type: "Feature",
          properties: {},
          geometry: {
            type: "LineString",
            coordinates: coordinates
          }
        }]
      : []
  });

  if (originCoordinates && destinationCoordinates) {
    const bounds = new mapboxgl.LngLatBounds();
    if (coordinates.length > 0) {
      coordinates.forEach(coord => bounds.extend(coord));
    } else {
      bounds.extend(originCoordinates);
      bounds.extend(destinationCoordinates);
    }
    state.tripMap.fitBounds(bounds, {
      padding: { top: 50, bottom: 50, left: 50, right: 50 },
      maxZoom: 15
    });
  }

  if (state.tripOrigin) {
    if (!state.tripOriginMarker) {
      state.tripOriginMarker = new mapboxgl.Marker({ color: "#b67a52" })
        .setLngLat(originCoordinates)
        .addTo(state.tripMap);
    } else {
      state.tripOriginMarker.setLngLat(originCoordinates);
    }
  } else if (state.tripOriginMarker) {
    state.tripOriginMarker.remove();
    state.tripOriginMarker = null;
  }

  if (state.tripDestination) {
    if (!state.tripDestinationMarker) {
      state.tripDestinationMarker = new mapboxgl.Marker({ color: "#5f7f6c" })
        .setLngLat(destinationCoordinates)
        .addTo(state.tripMap);
    } else {
      state.tripDestinationMarker.setLngLat(destinationCoordinates);
    }
  } else if (state.tripDestinationMarker) {
    state.tripDestinationMarker.remove();
    state.tripDestinationMarker = null;
  }
}

function setCreateTripPoint(pointType, longitude, latitude) {
  const normalizedPoint = {
    lng: Number(longitude.toFixed(6)),
    lat: Number(latitude.toFixed(6))
  };

  if (pointType === "destination") {
    state.tripDestination = normalizedPoint;
    state.tripSelectionMode = "origin";
  } else {
    state.tripOrigin = normalizedPoint;
    state.tripSelectionMode = "destination";
  }

  updateCreateTripMapPanels();
  updateCreateTripMapLayers();
}

function setCreateTripSelectionMode(mode) {
  state.tripSelectionMode = mode === "destination" ? "destination" : "origin";
  updateCreateTripMapPanels();
}

function initializeCreateTripMap() {
  const container = getCreateTripMapContainer();

  if (!container || typeof mapboxgl === "undefined") {
    setMessage(createModalMessage, "El mapa no pudo inicializarse.", "error");
    return;
  }

  destroyCreateTripMap();
  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;

  const fallbackCenter = state.tripOrigin || state.tripDestination
    ? [state.tripOrigin?.lng ?? state.tripDestination.lng, state.tripOrigin?.lat ?? state.tripDestination.lat]
    : [-74.0721, 4.7110];

  state.tripMap = new mapboxgl.Map({
    container,
    style: "mapbox://styles/mapbox/streets-v12",
    center: fallbackCenter,
    zoom: 12
  });

  state.tripMap.addControl(new mapboxgl.NavigationControl(), "top-right");

  state.tripMap.on("load", () => {
    state.tripMapReady = true;
    updateCreateTripMapPanels();
    updateCreateTripMapLayers();
    void refreshPublicSafeZoneMarkersOnMap(state.tripMap, "createTripSafeZoneMarkers");

    if (navigator.geolocation && !state.tripOrigin && !state.tripDestination) {
      navigator.geolocation.getCurrentPosition((position) => {
        if (!state.tripMap) {
          return;
        }

        state.tripMap.flyTo({
          center: [position.coords.longitude, position.coords.latitude],
          zoom: 13
        });
      });
    }
  });

  state.tripMap.on("click", (event) => {
    setCreateTripPoint(state.tripSelectionMode || "origin", event.lngLat.lng, event.lngLat.lat);
  });
}

function syncCreateTripDriverSelection() {
  const select = createModalForm?.querySelector("#createTripDriverSelect");
  const driverIdDisplay = createModalForm?.querySelector("#createTripDriverUserIdDisplay");
  const driverNameInput = createModalForm?.querySelector("#createTripDriverName");

  if (!select || !driverIdDisplay || !driverNameInput) {
    return;
  }

  const selectedDriver = (state.adminData.users || []).find((user) => String(user.id) === String(select.value));
  driverIdDisplay.value = selectedDriver?.id || "";
  if (selectedDriver && !driverNameInput.value.trim()) {
    driverNameInput.value = selectedDriver.fullName || "";
  }
}

function syncCreateReservationPassengerSelection() {
  const select = createModalForm?.querySelector("#createReservationPassengerSelect");
  const passengerIdDisplay = createModalForm?.querySelector("#createReservationPassengerUserIdDisplay");

  if (!select || !passengerIdDisplay) {
    return;
  }

  passengerIdDisplay.value = String(select.value || "");
}

function renderCreateModalFields(type) {
  if (!createModalFields || !createModalTitle || !createEntityType) {
    return;
  }

  const resolvedType = type || createEntityType.value || "user";
  createEntityType.value = resolvedType;

  const titles = {
    user: "Agregar usuario",
    trip: "Agregar viaje",
    reservation: "Agregar reserva"
  };

  createModalTitle.textContent = titles[resolvedType] || "Agregar dato";
  createModalFields.innerHTML = getCreateModalMarkup(resolvedType);

  if (resolvedType === "user") {
    updateCreateUserDriverFieldsVisibility();
  }

  if (resolvedType === "trip") {
    updateCreateTripMapPanels();
    syncCreateTripDriverSelection();
    setCreateTripSelectionMode("origin");
  }

  if (resolvedType === "reservation") {
    syncCreateReservationPassengerSelection();
  }
}

function openCreateModal(type = "user") {
  if (type === "trip") {
    resetCreateTripMapState();
    destroyCreateTripMap();
  }

  renderCreateModalFields(type);
  createModalOverlay.classList.remove("hidden");
  createModalOverlay.setAttribute("aria-hidden", "false");
  setMessage(createModalMessage, "");

  if (type === "trip") {
    window.requestAnimationFrame(() => {
      initializeCreateTripMap();
    });
  }
}

function closeCreateModal() {
  destroyCreateTripMap();
  createModalOverlay.classList.add("hidden");
  createModalOverlay.setAttribute("aria-hidden", "true");
  setMessage(createModalMessage, "");
}

function closeEditModal() {
  stopSupportChatPolling();
  state.supportChatTicketId = null;
  state.editContext = null;
  editModalForm.innerHTML = "";
  setMessage(editModalMessage, "");
  editModalOverlay.classList.add("hidden");
  editModalOverlay.setAttribute("aria-hidden", "true");
}

function stopSupportChatPolling() {
  if (state.supportChatPollId) {
    window.clearInterval(state.supportChatPollId);
    state.supportChatPollId = null;
  }
}

function isSupportTicketClosed(status) {
  const key = getSupportStatusKey(status);
  return key === "resolved" || key === "closed";
}

function renderSupportChatMessages(messages) {
  const container = document.getElementById("supportChatMessages");
  if (!container) {
    return;
  }

  const adminId = String(state.currentUser?.id || "");
  if (!messages.length) {
    container.innerHTML = '<p class="support-chat-empty">Aun no hay mensajes. Tu primera respuesta habilitara el chat en la app movil.</p>';
    return;
  }

  container.innerHTML = messages.map((msg) => {
    const isAdmin = Number(msg.senderKind) === 2 || String(msg.senderUserId) === adminId;
    return `
      <div class="support-chat-bubble ${isAdmin ? "support-chat-bubble--out" : "support-chat-bubble--in"}">
        <div class="support-chat-bubble__meta">${escapeHtml(msg.senderFullName || "Usuario")} · ${escapeHtml(formatDateTime(msg.createdAt))}</div>
        <div>${escapeHtml(msg.messageText || "")}</div>
      </div>
    `;
  }).join("");

  container.scrollTop = container.scrollHeight;
}

async function loadSupportChatMessages(ticketId, showError = false) {
  const container = document.getElementById("supportChatMessages");
  if (!container || !ticketId) {
    return;
  }

  try {
    const messages = await apiFetch(`/api/admin/support-tickets/${ticketId}/messages`, {
      headers: getAdminHeaders()
    });
    renderSupportChatMessages(Array.isArray(messages) ? messages : []);
  } catch (error) {
    if (showError) {
      container.innerHTML = `<p class="support-chat-empty support-chat-empty--error">${escapeHtml(error.message)}</p>`;
    }
  }
}

async function sendSupportChatMessage(ticketId) {
  const input = document.getElementById("supportChatInput");
  const sendBtn = document.getElementById("supportChatSendBtn");
  if (!input || !sendBtn || !ticketId) {
    return;
  }

  const text = String(input.value || "").trim();
  if (!text) {
    return;
  }

  sendBtn.disabled = true;
  try {
    await apiFetch(`/api/admin/support-tickets/${ticketId}/messages`, {
      method: "POST",
      headers: getAdminHeaders(),
      body: JSON.stringify({ messageText: text })
    });
    input.value = "";
    await loadSupportChatMessages(ticketId);
    await loadSupportTickets();
    setMessage(editModalMessage, "Mensaje enviado. El usuario ya puede chatear si era la primera respuesta.", "success");
  } catch (error) {
    setMessage(editModalMessage, error.message, "error");
  } finally {
    sendBtn.disabled = false;
  }
}

function initializeSupportChatPanel(ticket) {
  stopSupportChatPolling();
  state.supportChatTicketId = ticket?.id || null;

  const sendBtn = document.getElementById("supportChatSendBtn");
  const input = document.getElementById("supportChatInput");
  if (!sendBtn || !input || !state.supportChatTicketId) {
    return;
  }

  const closed = isSupportTicketClosed(ticket.status);
  input.disabled = closed;
  sendBtn.disabled = closed;

  sendBtn.onclick = () => {
    sendSupportChatMessage(state.supportChatTicketId);
  };

  loadSupportChatMessages(state.supportChatTicketId, true);
  state.supportChatPollId = window.setInterval(() => {
    loadSupportChatMessages(state.supportChatTicketId);
  }, 3000);
}

function getDetailTripMapContainer() {
  return detailsModalBody?.querySelector("#detailTripMap") || null;
}

function destroyDetailTripMap() {
  if (state.detailTripOriginMarker) {
    state.detailTripOriginMarker.remove();
    state.detailTripOriginMarker = null;
  }

  if (state.detailTripDestinationMarker) {
    state.detailTripDestinationMarker.remove();
    state.detailTripDestinationMarker = null;
  }

  clearSafeZoneMarkers(state.detailTripSafeZoneMarkers);

  if (state.detailTripMap) {
    state.detailTripMap.remove();
    state.detailTripMap = null;
  }

  state.detailTripMapReady = false;
}

async function updateDetailTripMapLayers(trip) {
  if (!state.detailTripMap || !state.detailTripMapReady || typeof mapboxgl === "undefined") {
    return;
  }

  const origin = getTripOriginCoordinates(trip);
  const destination = getTripDestinationCoordinates(trip);
  const originCoordinates = hasTripCoordinates(origin) ? [origin.lng, origin.lat] : null;
  const destinationCoordinates = hasTripCoordinates(destination) ? [destination.lng, destination.lat] : null;
  let routeSource = state.detailTripMap.getSource("detail-trip-route");

  if (!routeSource) {
    state.detailTripMap.addSource("detail-trip-route", {
      type: "geojson",
      data: {
        type: "FeatureCollection",
        features: []
      }
    });

    routeSource = state.detailTripMap.getSource("detail-trip-route");

    state.detailTripMap.addLayer({
      id: "detail-trip-route-line",
      type: "line",
      source: "detail-trip-route",
      layout: { "line-join": "round", "line-cap": "round" },
      paint: {
        "line-color": "#5f7f6c",
        "line-width": 4
      }
    });
  }

  let coordinates = [];
  if (originCoordinates && destinationCoordinates) {
    try {
      const url = `https://api.mapbox.com/directions/v5/mapbox/driving/${originCoordinates[0]},${originCoordinates[1]};${destinationCoordinates[0]},${destinationCoordinates[1]}?geometries=geojson&overview=full&access_token=${MAPBOX_ACCESS_TOKEN}`;
      const response = await fetch(url);
      if (!response.ok) {
        throw new Error("Respuesta no exitosa de la API de Mapbox Directions");
      }
      const data = await response.json();
      if (data.routes && data.routes.length > 0) {
        coordinates = data.routes[0].geometry.coordinates;
      } else {
        coordinates = [originCoordinates, destinationCoordinates];
      }
    } catch (e) {
      console.warn("Fallo al obtener la ruta real de Mapbox, usando línea recta:", e);
      coordinates = [originCoordinates, destinationCoordinates];
    }
  }

  routeSource.setData({
    type: "FeatureCollection",
    features: coordinates.length > 0
      ? [{
          type: "Feature",
          properties: {},
          geometry: {
            type: "LineString",
            coordinates: coordinates
          }
        }]
      : []
  });

  if (originCoordinates && destinationCoordinates) {
    const bounds = new mapboxgl.LngLatBounds();
    if (coordinates.length > 0) {
      coordinates.forEach(coord => bounds.extend(coord));
    } else {
      bounds.extend(originCoordinates);
      bounds.extend(destinationCoordinates);
    }
    state.detailTripMap.fitBounds(bounds, {
      padding: { top: 50, bottom: 50, left: 50, right: 50 },
      maxZoom: 15
    });
  }

  if (hasTripCoordinates(origin)) {
    if (!state.detailTripOriginMarker) {
      state.detailTripOriginMarker = new mapboxgl.Marker({ color: "#b67a52" })
        .setLngLat(originCoordinates)
        .addTo(state.detailTripMap);
    } else {
      state.detailTripOriginMarker.setLngLat(originCoordinates);
    }
  } else if (state.detailTripOriginMarker) {
    state.detailTripOriginMarker.remove();
    state.detailTripOriginMarker = null;
  }

  if (hasTripCoordinates(destination)) {
    if (!state.detailTripDestinationMarker) {
      state.detailTripDestinationMarker = new mapboxgl.Marker({ color: "#5f7f6c" })
        .setLngLat(destinationCoordinates)
        .addTo(state.detailTripMap);
    } else {
      state.detailTripDestinationMarker.setLngLat(destinationCoordinates);
    }
  } else if (state.detailTripDestinationMarker) {
    state.detailTripDestinationMarker.remove();
    state.detailTripDestinationMarker = null;
  }
}

function initializeDetailTripMap(trip) {
  const container = getDetailTripMapContainer();

  if (!container || typeof mapboxgl === "undefined") {
    setMessage(detailsModalMessage, "El mapa de detalles no pudo inicializarse.", "error");
    return;
  }

  destroyDetailTripMap();
  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;

  const origin = getTripOriginCoordinates(trip);
  const destination = getTripDestinationCoordinates(trip);
  const fallbackCenter = hasTripCoordinates(origin)
    ? [origin.lng, origin.lat]
    : hasTripCoordinates(destination)
      ? [destination.lng, destination.lat]
      : [-74.0721, 4.7110];

  state.detailTripMap = new mapboxgl.Map({
    container,
    style: "mapbox://styles/mapbox/streets-v12",
    center: fallbackCenter,
    zoom: 12
  });

  state.detailTripMap.addControl(new mapboxgl.NavigationControl(), "top-right");

  state.detailTripMap.on("load", () => {
    state.detailTripMapReady = true;
    updateDetailTripMapLayers(trip);
    void refreshPublicSafeZoneMarkersOnMap(state.detailTripMap, "detailTripSafeZoneMarkers");
  });
}

function renderDetailsModalContent(type, entity) {
  if (type === "trip") {
    return renderTripDetails(entity);
  }

  if (type === "reservation") {
    return renderReservationDetails(entity);
  }

  if (type === "support") {
    return renderSupportDetails(entity);
  }

  if (type === "payment") {
    return renderPaymentDetails(entity);
  }

  return renderUserDetails(entity);
}

function openDetailsModal(type, entity) {
  if (!entity) {
    throw new Error("No se encontro el registro seleccionado.");
  }

  destroyDetailTripMap();

  const titles = {
    user: "Ver detalles de usuario",
    trip: "Ver detalles de viaje",
    reservation: "Ver detalles de reserva",
    support: "Ver detalles del reporte",
    payment: "Ver detalles de pago"
  };

  state.detailContext = { type, id: entity.id };
  detailsModalTitle.textContent = titles[type] || "Ver detalles";
  detailsModalBody.innerHTML = renderDetailsModalContent(type, entity);
  setMessage(detailsModalMessage, "");
  detailsModalOverlay.classList.remove("hidden");
  detailsModalOverlay.setAttribute("aria-hidden", "false");

  if (type === "trip") {
    window.requestAnimationFrame(() => {
      initializeDetailTripMap(entity);
    });
  }
}

function closeDetailsModal() {
  state.detailContext = null;
  destroyDetailTripMap();
  detailsModalBody.innerHTML = "";
  setMessage(detailsModalMessage, "");
  detailsModalOverlay.classList.add("hidden");
  detailsModalOverlay.setAttribute("aria-hidden", "true");
}

function openEditModal(type, entity) {
  if (!entity) {
    throw new Error("No se encontro el registro seleccionado para editar.");
  }

  state.editContext = { type, id: entity.id };

  const titles = {
    user: "Editar usuario",
    trip: "Editar viaje",
    reservation: "Editar reserva",
    support: "Detalle del reporte"
  };

  editModalTitle.textContent = titles[type] || "Editar registro";
  editModalForm.innerHTML = getEditFormMarkup(type, entity);
  setMessage(editModalMessage, "");
  editModalOverlay.classList.remove("hidden");
  editModalOverlay.setAttribute("aria-hidden", "false");

  if (type === "support") {
    initializeSupportChatPanel(entity);
  }
}

function getRoleIdValue(user) {
  const numericRoleId = Number(user.roleId);
  if (!Number.isNaN(numericRoleId) && numericRoleId >= 1 && numericRoleId <= 3) {
    return numericRoleId;
  }

  const role = String(user.role || "").trim().toLowerCase();
  if (role === "driver") return 2;
  if (role === "admin") return 3;
  return 1;
}

function getTripStatusValue(status) {
  const key = getTripStatusKey(status);
  if (key === "awaitingdestination") return "1";
  if (key === "ready") return "2";
  if (key === "inprogress") return "3";
  if (key === "finished") return "4";
  if (key === "cancelled") return "5";
  return "1";
}

function getReservationStatusValue(status) {
  const key = getReservationStatusKey(status);
  if (key === "pending") return "1";
  if (key === "confirmed") return "2";
  if (key === "boarded") return "3";
  if (key === "cancelled") return "4";
  return "1";
}

function getNullableNumber(rawValue) {
  const value = String(rawValue ?? "").trim();
  if (!value) {
    return null;
  }

  const parsed = Number(value);
  return Number.isNaN(parsed) ? null : parsed;
}

function getEntityFromState(type, id) {
  const source = {
    user: state.adminData.users,
    trip: state.adminData.trips,
    reservation: state.adminData.reservations,
    support: state.adminData.supportTickets,
    payment: state.adminPayments
  }[type] || [];

  return source.find((item) => String(item.id) === String(id));
}

function getEditFormMarkup(type, entity) {
  if (type === "support") {
    const tripLine = entity.tripId
      ? `<div><strong>Viaje:</strong> ${escapeHtml(entity.tripId)}</div>`
      : "";
    const reservationLine = entity.reservationId
      ? `<div><strong>Reserva:</strong> ${escapeHtml(entity.reservationId)}</div>`
      : "";

    return `
      <label class="field"><span>Referencia</span><input type="text" value="#${escapeHtml(formatSupportReference(entity.id))}" disabled /></label>
      <label class="field"><span>Usuario</span><input type="text" value="${escapeHtml(entity.userFullName || "")}" disabled /></label>
      <label class="field"><span>Categoria</span><input type="text" value="${escapeHtml(entity.categoryLabel || "")}" disabled /></label>
      <label class="field"><span>Asunto</span><input type="text" value="${escapeHtml(entity.subject || "")}" disabled /></label>
      <div class="support-detail-box">
        <strong>Descripcion del usuario</strong>
        <p>${escapeHtml(entity.description || "")}</p>
      </div>
      <div class="support-detail-meta">
        <div><strong>Creado:</strong> ${escapeHtml(formatDateTime(entity.createdAt))}</div>
        ${entity.updatedAt ? `<div><strong>Actualizado:</strong> ${escapeHtml(formatDateTime(entity.updatedAt))}</div>` : ""}
        ${tripLine}
        ${reservationLine}
      </div>
      <label class="field"><span>Estado del reporte</span>
        <select name="status">
          <option value="1" ${getSupportStatusValue(entity.status) === "1" ? "selected" : ""}>Abierto</option>
          <option value="2" ${getSupportStatusValue(entity.status) === "2" ? "selected" : ""}>En revision</option>
          <option value="3" ${getSupportStatusValue(entity.status) === "3" ? "selected" : ""}>Resuelto</option>
          <option value="4" ${getSupportStatusValue(entity.status) === "4" ? "selected" : ""}>Cerrado</option>
        </select>
      </label>
      <div class="support-chat-panel">
        <h4>Conversacion con el usuario</h4>
        <p class="card-help">Tu primera respuesta habilita el chat en la app movil del estudiante.</p>
        <div id="supportChatMessages" class="support-chat-messages">Cargando mensajes...</div>
        <label class="field">
          <span>Responder al usuario</span>
          <textarea id="supportChatInput" rows="3" maxlength="2000" placeholder="Escribe tu mensaje de soporte..." ${isSupportTicketClosed(entity.status) ? "disabled" : ""}></textarea>
        </label>
        <button type="button" class="btn secondary" id="supportChatSendBtn" ${isSupportTicketClosed(entity.status) ? "disabled" : ""}>Enviar mensaje</button>
      </div>
      <div class="modal-actions">
        <button type="button" class="btn ghost" data-modal-action="cancel">Cerrar</button>
        <button type="submit" class="btn primary">Guardar estado</button>
      </div>
    `;
  }

  if (type === "user") {
    return `
      <label class="field"><span>ID</span><input type="text" value="${escapeHtml(entity.id || "")}" disabled /></label>
      <label class="field"><span>Nombre completo</span><input type="text" name="fullName" value="${escapeHtml(entity.fullName || "")}" required /></label>
      <label class="field"><span>Email institucional</span><input type="email" name="email" value="${escapeHtml(entity.email || "")}" readonly /></label>
      <label class="field"><span>Contrasena (dejar vacio para mantener la actual)</span><input type="password" name="password" placeholder="******" /></label>
      <label class="field"><span>Telefono</span><input type="text" name="phoneNumber" value="${escapeHtml(entity.phoneNumber || "")}" /></label>
      <label class="field"><span>Rol</span>
        <select name="roleId">
          <option value="1" ${getRoleIdValue(entity) === 1 ? "selected" : ""}>Estudiante</option>
          <option value="2" ${getRoleIdValue(entity) === 2 ? "selected" : ""}>Chofer</option>
          <option value="3" ${getRoleIdValue(entity) === 3 ? "selected" : ""}>Administrador</option>
        </select>
      </label>
      <div class="modal-actions">
        <button type="button" class="btn ghost" data-modal-action="cancel">Cancelar</button>
        <button type="submit" class="btn primary">Guardar cambios</button>
      </div>
    `;
  }

  if (type === "trip") {
    const origin = getTripOriginCoordinates(entity);
    const destination = getTripDestinationCoordinates(entity);
    const statusValue = entity.statusId ?? entity.status ?? entity.statusLabel;

    return `
      <label class="field"><span>ID</span><input type="text" value="${escapeHtml(entity.id || "")}" disabled /></label>
      <label class="field"><span>Nombre del conductor</span><input type="text" name="driverName" value="${escapeHtml(entity.driverName || "")}" required /></label>
      <label class="field"><span>Driver User ID (opcional)</span><input type="text" name="driverUserId" value="${escapeHtml(entity.driverUserId || "")}" /></label>
      <label class="field"><span>Latitud origen</span><input type="number" step="any" name="originLatitude" value="${escapeHtml(origin.lat ?? "")}" required /></label>
      <label class="field"><span>Longitud origen</span><input type="number" step="any" name="originLongitude" value="${escapeHtml(origin.lng ?? "")}" required /></label>
      <label class="field"><span>Latitud destino (opcional)</span><input type="number" step="any" name="destinationLatitude" value="${escapeHtml(destination.lat ?? "")}" /></label>
      <label class="field"><span>Longitud destino (opcional)</span><input type="number" step="any" name="destinationLongitude" value="${escapeHtml(destination.lng ?? "")}" /></label>
      <label class="field"><span>Cupos disponibles</span>
        <select name="availableSeats">
          <option value="0" ${Number(entity.availableSeats) === 0 ? "selected" : ""}>0</option>
          <option value="1" ${Number(entity.availableSeats) === 1 ? "selected" : ""}>1</option>
          <option value="2" ${Number(entity.availableSeats) === 2 ? "selected" : ""}>2</option>
          <option value="3" ${Number(entity.availableSeats) === 3 ? "selected" : ""}>3</option>
          <option value="4" ${Number(entity.availableSeats) === 4 ? "selected" : ""}>4</option>
          <option value="5" ${Number(entity.availableSeats) === 5 ? "selected" : ""}>5</option>
        </select>
      </label>
      <label class="field"><span>Estado</span>
        <select name="status">
          <option value="1" ${getTripStatusValue(statusValue) === "1" ? "selected" : ""}>Esperando destino</option>
          <option value="2" ${getTripStatusValue(statusValue) === "2" ? "selected" : ""}>Listo</option>
          <option value="3" ${getTripStatusValue(statusValue) === "3" ? "selected" : ""}>En curso</option>
          <option value="4" ${getTripStatusValue(statusValue) === "4" ? "selected" : ""}>Finalizado</option>
          <option value="5" ${getTripStatusValue(statusValue) === "5" ? "selected" : ""}>Cancelado</option>
        </select>
      </label>
      <div class="modal-actions">
        <button type="button" class="btn ghost" data-modal-action="cancel">Cancelar</button>
        <button type="submit" class="btn primary">Guardar cambios</button>
      </div>
    `;
  }

  return `
    <label class="field"><span>ID</span><input type="text" value="${escapeHtml(entity.id || "")}" disabled /></label>
    <label class="field"><span>Trip ID</span><input type="text" name="tripId" value="${escapeHtml(entity.tripId || "")}" required /></label>
    <label class="field"><span>Nombre del pasajero</span><input type="text" name="passengerName" value="${escapeHtml(entity.passengerName || "")}" required /></label>
    <label class="field"><span>Estado</span>
      <select name="status">
        <option value="1" ${getReservationStatusValue(entity.status) === "1" ? "selected" : ""}>Pendiente</option>
        <option value="2" ${getReservationStatusValue(entity.status) === "2" ? "selected" : ""}>Confirmado</option>
        <option value="3" ${getReservationStatusValue(entity.status) === "3" ? "selected" : ""}>Abordado</option>
        <option value="4" ${getReservationStatusValue(entity.status) === "4" ? "selected" : ""}>Cancelado</option>
      </select>
    </label>
    <div class="modal-actions">
      <button type="button" class="btn ghost" data-modal-action="cancel">Cancelar</button>
      <button type="submit" class="btn primary">Guardar cambios</button>
    </div>
  `;
}

function hasPermission(permissionId) {
  if (!state.currentUser) return false;
  if (state.currentUser.rawRoles && state.currentUser.rawRoles.includes("SuperAdmin")) {
    return true;
  }
  return state.currentUser.permissions && state.currentUser.permissions.includes(permissionId);
}

function updateNavVisibility() {
  const menuNav = document.getElementById("menuNav");
  if (!menuNav) return;

  const mapping = {
    overview: "metrics:view",
    users: "users:read",
    trips: "trips:read",
    reservations: "reservations:read",
    support: "support:read",
    "safe-zones": "trips:read",
    admins: "roles:manage",
    settings: "roles:manage"
  };

  for (const [section, permission] of Object.entries(mapping)) {
    const btn = menuNav.querySelector(`[data-section="${section}"]`);
    if (btn) {
      if (hasPermission(permission)) {
        btn.classList.remove("hidden");
      } else {
        btn.classList.add("hidden");
      }
    }
  }
}

function updateOverviewQuickActionsVisibility() {
  const quickActionsContainer = document.getElementById("overviewQuickActions");
  if (!quickActionsContainer) return;

  const btnUser = quickActionsContainer.querySelector('[data-section="users"]');
  if (btnUser) {
    btnUser.classList.toggle("hidden", !hasPermission("users:write"));
  }

  const btnTrip = quickActionsContainer.querySelector('[data-section="trips"]');
  if (btnTrip) {
    btnTrip.classList.toggle("hidden", !hasPermission("trips:write"));
  }

  const btnRes = quickActionsContainer.querySelector('[data-section="reservations"]');
  if (btnRes) {
    btnRes.classList.toggle("hidden", !hasPermission("reservations:read"));
  }

  const btnSup = quickActionsContainer.querySelector('[data-section="support"]');
  if (btnSup) {
    btnSup.classList.toggle("hidden", !hasPermission("support:read"));
  }

  // Hide the entire Quick Actions card if user has none of the permissions
  const hasAnyAction = hasPermission("users:write") || hasPermission("trips:write") || hasPermission("reservations:read") || hasPermission("support:read");
  const quickActionsCard = document.getElementById("quickActionsCard");
  if (quickActionsCard) {
    quickActionsCard.classList.toggle("hidden", !hasAnyAction);
  }
}

function updateActionButtonsVisibility() {
  document.querySelectorAll('[data-create-type="user"]').forEach(btn => {
    btn.classList.toggle("hidden", !hasPermission("users:write"));
  });
  document.querySelectorAll('[data-create-type="trip"]').forEach(btn => {
    btn.classList.toggle("hidden", !hasPermission("trips:write"));
  });
  document.querySelectorAll('[data-create-type="reservation"]').forEach(btn => {
    btn.classList.toggle("hidden", !hasPermission("reservations:write"));
  });
  
  const safeZoneCreateBtn = document.getElementById("safeZoneCreateBtn");
  if (safeZoneCreateBtn) {
    safeZoneCreateBtn.classList.toggle("hidden", !hasPermission("trips:write"));
  }

  updateOverviewQuickActionsVisibility();
}

function startSession(user) {
  state.currentUser = user;
  localStorage.setItem("cp.adminUser", JSON.stringify(user));
  localStorage.setItem("cp.apiBaseUrl", state.apiBaseUrl);

  authShell.classList.add("hidden");
  dashboard.classList.remove("hidden");
  loggedUserInfo.textContent = `Admin: ${user.fullName} (${user.email})`;
  updateApiStatus(true, "login exitoso");
  startAdminAutoRefresh();

  updateNavVisibility();
  updateActionButtonsVisibility();

  // Find first allowed section based on permissions
  const order = ["overview", "users", "trips", "reservations", "support", "safe-zones", "admins", "settings"];
  const allowed = order.find(sec => {
    const mapping = {
      overview: "metrics:view",
      users: "users:read",
      trips: "trips:read",
      reservations: "reservations:read",
      support: "support:read",
      "safe-zones": "trips:read",
      admins: "roles:manage",
      settings: "roles:manage"
    };
    return hasPermission(mapping[sec]);
  });
  
  openSection(allowed || "overview");
}

function logout() {
  state.currentUser = null;
  localStorage.removeItem("cp.adminUser");

  dashboard.classList.add("hidden");
  authShell.classList.remove("hidden");
  stopAdminAutoRefresh();
  updateApiStatus(false);
}

function startAdminAutoRefresh() {
  if (state.adminDataAutoRefreshId) {
    return;
  }

  state.adminDataAutoRefreshId = window.setInterval(() => {
    const isAdmin = Number(state.currentUser?.roleId) === ADMIN_ROLE_ID;
    if (!isAdmin) {
      return;
    }

    if (["users", "trips", "reservations", "reports"].includes(state.section)) {
      loadAdminData();
    }

    if (state.section === "support") {
      loadSupportTickets();
    }
  }, ADMIN_DATA_REFRESH_MS);
}

function stopAdminAutoRefresh() {
  if (!state.adminDataAutoRefreshId) {
    return;
  }

  window.clearInterval(state.adminDataAutoRefreshId);
  state.adminDataAutoRefreshId = null;
}

function getAdminHeaders() {
  const userId = state.currentUser?.id;
  if (!userId) {
    throw new Error("No hay una sesion de administrador valida.");
  }

  return {
    "X-User-Id": userId
  };
}

async function loadAdminData() {
  if (state.isLoadingAdminData) {
    return;
  }

  state.isLoadingAdminData = true;

  try {
    const [usersResult, tripsResult, reservationsResult] = await Promise.allSettled([
      apiFetch("/api/admin/users", {
        headers: getAdminHeaders()
      }),
      apiFetch("/api/admin/trips", {
        headers: getAdminHeaders()
      }),
      apiFetch("/api/admin/reservations", {
        headers: getAdminHeaders()
      })
    ]);

    const users = usersResult.status === "fulfilled" ? usersResult.value : [];
    const trips = tripsResult.status === "fulfilled" ? tripsResult.value : [];
    const reservations = reservationsResult.status === "fulfilled" ? reservationsResult.value : [];

    state.adminData = {
      users,
      trips,
      reservations
    };

    renderUsersTable(users);
    renderTripsTable(trips);
    renderReservationsTable(reservations);
    renderAdminsTable(users);
    applyAllTableFilters();
    document.getElementById("analyticsConsultBtn")?.click();

    const issueMessages = [];
    if (usersResult.status === "rejected") {
      issueMessages.push(`usuarios: ${usersResult.reason.message}`);
    }
    if (tripsResult.status === "rejected") {
      issueMessages.push(`viajes: ${tripsResult.reason.message}`);
    }
    if (reservationsResult.status === "rejected") {
      issueMessages.push(`reservas: ${reservationsResult.reason.message}`);
    }

    if (issueMessages.length) {
      setMessage(
        adminDataMessage,
        `Usuarios cargados con aviso. Problemas en ${issueMessages.join(" | ")}`,
        "error"
      );
    } else {
      setMessage(
        adminDataMessage,
        `Actualizado ${new Date().toLocaleTimeString()}. Usuarios: ${users.length} | Viajes: ${trips.length} | Reservas: ${reservations.length}`,
        "success"
      );
    }
  } catch (error) {
    state.adminData = {
      users: [],
      trips: [],
      reservations: []
    };

    renderUsersTable([]);
    renderTripsTable([]);
    renderReservationsTable([]);
    setMessage(adminDataMessage, `No se pudieron cargar los datos: ${error.message}`, "error");
  } finally {
    state.isLoadingAdminData = false;
  }
}

async function loadSupportTickets() {
  if (state.isLoadingSupportTickets) {
    return;
  }

  state.isLoadingSupportTickets = true;

  const status = supportStatusFilter?.value || "";
  const category = supportCategoryFilter?.value || "";
  const query = new URLSearchParams();
  if (status) {
    query.set("status", status);
  }
  if (category) {
    query.set("category", category);
  }

  const queryString = query.toString();
  const path = queryString ? `/api/admin/support-tickets?${queryString}` : "/api/admin/support-tickets";

  try {
    const data = await apiFetch(path, {
      headers: getAdminHeaders()
    });

    const items = data.items || [];
    state.adminData.supportTickets = items;
    renderSupportTable(items);
    applyTableFilter("adminSupportBody", supportFilterInput?.value || "");

    const openCount = items.filter((ticket) => getSupportStatusKey(ticket.status) === "open").length;
    const inReviewCount = items.filter((ticket) => getSupportStatusKey(ticket.status) === "inreview").length;

    setMessage(
      supportDataMessage,
      `Actualizado ${new Date().toLocaleTimeString()}. Total: ${items.length} | Abiertos: ${openCount} | En revision: ${inReviewCount}`,
      "success"
    );
  } catch (error) {
    state.adminData.supportTickets = [];
    renderSupportTable([]);
    setMessage(supportDataMessage, `No se pudieron cargar los reportes: ${error.message}`, "error");
  } finally {
    state.isLoadingSupportTickets = false;
  }
}

function renderSupportTable(tickets) {
  if (!adminSupportBody) {
    return;
  }

  if (!tickets.length) {
    adminSupportBody.innerHTML = '<tr><td colspan="7">No hay reportes de soporte.</td></tr>';
    return;
  }

  adminSupportBody.innerHTML = tickets.map((ticket) => `
    <tr>
      <td>#${escapeHtml(formatSupportReference(ticket.id))}</td>
      <td>${escapeHtml(ticket.userFullName || "Usuario")}</td>
      <td>${escapeHtml(ticket.categoryLabel || "-")}</td>
      <td>${escapeHtml(ticket.subject || "-")}</td>
      <td>
        <span class="status-badge status-badge--${escapeHtml(getSupportStatusKey(ticket.status))}">
          ${escapeHtml(ticket.statusLabel || "Abierto")}
        </span>
      </td>
      <td>${escapeHtml(formatDateTime(ticket.createdAt))}</td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-view-details" data-type="support" data-id="${escapeHtml(ticket.id)}">Ver detalles</button>
          ${hasPermission('support:write') ? `<button class="btn tiny secondary admin-view-support" data-id="${escapeHtml(ticket.id)}">Ver / Atender</button>` : ''}
        </div>
      </td>
    </tr>
  `).join("");
}

async function loadAdminPayments() {
  const msgElement = document.getElementById("adminPaymentsMessage");
  if (msgElement) setMessage(msgElement, "");
  try {
    const payments = await apiFetch("/api/payments", {
      headers: getAdminHeaders()
    });
    state.adminPayments = payments || [];
    renderPaymentsTable(state.adminPayments);
  } catch (err) {
    console.error("Error al cargar pagos:", err);
    if (msgElement) {
      setMessage(msgElement, `Error al cargar pagos: ${err.message}`, "error");
    }
  }
}

function renderPaymentsTable(payments) {
  if (!adminPaymentsBody) return;
  if (!payments || !payments.length) {
    adminPaymentsBody.innerHTML = '<tr><td colspan="7">Sin pagos registrados.</td></tr>';
    return;
  }

  adminPaymentsBody.innerHTML = payments.map((payment) => {
    let statusLabel = "Desconocido";
    let statusClass = "";
    switch (payment.status) {
      case 1: statusLabel = "Pendiente"; statusClass = "pending"; break;
      case 2: statusLabel = "Aprobado"; statusClass = "approved"; break;
      case 3: statusLabel = "Rechazado"; statusClass = "rejected"; break;
      case 4: statusLabel = "Cancelado"; statusClass = "cancelled"; break;
      case 5: statusLabel = "Expirado"; statusClass = "expired"; break;
      case 6: statusLabel = "Devuelto"; statusClass = "refunded"; break;
      case 7: statusLabel = "Devuelto parcial"; statusClass = "partially-refunded"; break;
    }

    return `
    <tr data-status="${payment.status}">
      <td>${escapeHtml(payment.id.substring(0, 8))}</td>
      <td>${escapeHtml(payment.passengerName || "-")}</td>
      <td>${escapeHtml(payment.driverName || "-")}</td>
      <td>${escapeHtml(payment.paymentMethodName || "-")}</td>
      <td>${escapeHtml(payment.amount.toFixed(2))} ${escapeHtml(payment.currency)}</td>
      <td><span class="status-pill ${statusClass}">${escapeHtml(statusLabel)}</span></td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-view-details" data-type="payment" data-id="${escapeHtml(payment.id)}">Ver detalles</button>
        </div>
      </td>
    </tr>
    `;
  }).join("");
  applyPaymentsFilter();
}

function applyPaymentsFilter() {
  const textVal = (paymentsFilterInput?.value || "").toLowerCase();
  const statusVal = paymentsStatusFilter?.value || "";
  const tbody = adminPaymentsBody;
  if (!tbody) return;
  const rows = tbody.querySelectorAll("tr");
  rows.forEach((row) => {
    if (row.cells.length === 1) return;
    const matchesText = (row.textContent || "").toLowerCase().includes(textVal);
    const matchesStatus = statusVal === "" || row.dataset.status === statusVal;
    row.style.display = (matchesText && matchesStatus) ? "" : "none";
  });
}

function renderPaymentDetails(payment) {
  if (!payment) {
    return `
      <div class="empty-state">
        <strong>Sin resultados</strong>
        <p>Consulta un pago para ver todos sus datos.</p>
      </div>
    `;
  }

  let statusLabel = "Desconocido";
  let statusClass = "unknown";
  switch (payment.status) {
    case 1: statusLabel = "Pendiente"; statusClass = "pending"; break;
    case 2: statusLabel = "Aprobado"; statusClass = "approved"; break;
    case 3: statusLabel = "Rechazado"; statusClass = "rejected"; break;
    case 4: statusLabel = "Cancelado"; statusClass = "cancelled"; break;
    case 5: statusLabel = "Expirado"; statusClass = "expired"; break;
    case 6: statusLabel = "Devuelto"; statusClass = "refunded"; break;
    case 7: statusLabel = "Devuelto parcial"; statusClass = "partially-refunded"; break;
  }

  let refundsHtml = "";
  if (payment.refunds && payment.refunds.length > 0) {
    refundsHtml = `
      <div class="detail-section" style="margin-top: 16px;">
        <h5 style="margin-bottom: 8px; font-weight: bold; border-bottom: 1px solid var(--border-color); padding-bottom: 4px;">Historial de Reembolsos</h5>
        <div class="table-wrap">
          <table class="admin-table" style="font-size: 13px;">
            <thead>
              <tr>
                <th>Monto</th>
                <th>Estado</th>
                <th>Motivo</th>
                <th>Rechazo Motivo</th>
                <th>Solicitado</th>
              </tr>
            </thead>
            <tbody>
              ${payment.refunds.map(refund => {
                let rStatus = "Desconocido";
                if (refund.status === 1) rStatus = "Solicitado";
                else if (refund.status === 2) rStatus = "Aprobado";
                else if (refund.status === 3) rStatus = "Rechazado";
                return `
                  <tr>
                    <td>${escapeHtml(refund.amount.toFixed(2))} ${escapeHtml(payment.currency)}</td>
                    <td>${escapeHtml(rStatus)}</td>
                    <td>${escapeHtml(refund.reason || "-")}</td>
                    <td>${escapeHtml(refund.rejectionReason || "-")}</td>
                    <td>${escapeHtml(formatDateTime(refund.requestedAt))}</td>
                  </tr>
                `;
              }).join("")}
            </tbody>
          </table>
        </div>
      </div>
    `;
  } else {
    refundsHtml = `
      <div class="detail-section" style="margin-top: 16px;">
        <h5 style="margin-bottom: 8px; font-weight: bold; border-bottom: 1px solid var(--border-color); padding-bottom: 4px;">Historial de Reembolsos</h5>
        <p style="color: var(--text-secondary); font-size: 13px;">No hay reembolsos registrados para este pago.</p>
      </div>
    `;
  }

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>Pago #${escapeHtml(payment.id.substring(0, 8))}</h4>
          <p>Reserva: ${escapeHtml(payment.reservationId || "-")}</p>
        </div>
        <span class="status-badge status-badge--${statusClass}">
          ${escapeHtml(statusLabel)}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>Monto:</strong> ${escapeHtml(payment.amount.toFixed(2))} ${escapeHtml(payment.currency)}</div>
        <div><strong>Monto Devuelto:</strong> ${escapeHtml(payment.refundedAmount.toFixed(2))} ${escapeHtml(payment.currency)}</div>
        <div><strong>Pasajero:</strong> ${escapeHtml(payment.passengerName || "-")}</div>
        <div><strong>Conductor:</strong> ${escapeHtml(payment.driverName || "-")}</div>
        <div><strong>Método de Pago:</strong> ${escapeHtml(payment.paymentMethodName || "-")}</div>
        <div><strong>Referencia Externa:</strong> ${escapeHtml(payment.externalReference || "-")}</div>
        <div><strong>Descripción:</strong> ${escapeHtml(payment.description || "-")}</div>
        <div><strong>Nro. Comprobante:</strong> ${escapeHtml(payment.receiptNumber || "-")}</div>
        <div><strong>Fecha Creación:</strong> ${escapeHtml(formatDateTime(payment.createdAt))}</div>
        <div><strong>Fecha Confirmación:</strong> ${escapeHtml(formatDateTime(payment.confirmedAt))}</div>
      </div>

      ${refundsHtml}
    </article>
  `;
}

function renderUsersTable(users) {
  if (!users.length) {
    adminUsersBody.innerHTML = '<tr><td colspan="5">Sin usuarios.</td></tr>';
    return;
  }

  adminUsersBody.innerHTML = users.map((user) => `
    <tr>
      <td>${escapeHtml(user.fullName)}</td>
      <td>${escapeHtml(user.email)}</td>
      <td>${escapeHtml(user.role)} (${escapeHtml(user.roleId)})</td>
      <td>${escapeHtml(user.phoneNumber || "-")}</td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-view-details" data-type="user" data-id="${escapeHtml(user.id)}">Ver detalles</button>
          ${hasPermission('users:write') ? `<button class="btn tiny secondary admin-edit" data-type="user" data-id="${escapeHtml(user.id)}">Editar</button>` : ''}
          ${hasPermission('users:delete') ? `<button class="btn tiny danger admin-delete" data-type="user" data-id="${escapeHtml(user.id)}">Eliminar</button>` : ''}
        </div>
      </td>
    </tr>
  `).join("");
}

function renderTripsTable(trips) {
  if (!trips.length) {
    adminTripsBody.innerHTML = '<tr><td colspan="7">Sin viajes.</td></tr>';
    return;
  }

  adminTripsBody.innerHTML = trips.map((trip) => `
    ${(() => {
      const origin = getTripOriginCoordinates(trip);
      const destination = getTripDestinationCoordinates(trip);
      const tripStatusValue = trip.statusLabel ?? trip.statusId ?? trip.status;

      return `
    <tr>
      <td>${escapeHtml(trip.id || "-")}</td>
      <td>${escapeHtml(trip.driverName || "-")}</td>
      <td>${escapeHtml(hasTripCoordinates(origin) ? `${origin.lat}, ${origin.lng}` : "-")}</td>
      <td>${escapeHtml(hasTripCoordinates(destination) ? `${destination.lat}, ${destination.lng}` : "-")}</td>
      <td>${escapeHtml(formatTripStatus(tripStatusValue))}</td>
      <td>${escapeHtml(trip.availableSeats)}</td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-view-details" data-type="trip" data-id="${escapeHtml(trip.id)}">Ver detalles</button>
          ${hasPermission('trips:write') ? `<button class="btn tiny secondary admin-edit" data-type="trip" data-id="${escapeHtml(trip.id)}">Editar</button>` : ''}
          ${hasPermission('trips:delete') ? `<button class="btn tiny danger admin-delete" data-type="trip" data-id="${escapeHtml(trip.id)}">Eliminar</button>` : ''}
        </div>
      </td>
    </tr>
  `;
    })()}
  `).join("");
}

function renderReservationsTable(reservations) {
  if (!reservations.length) {
    adminReservationsBody.innerHTML = '<tr><td colspan="5">Sin reservas.</td></tr>';
    return;
  }

  adminReservationsBody.innerHTML = reservations.map((reservation) => `
    <tr>
      <td>${escapeHtml(reservation.passengerName)}</td>
      <td>${escapeHtml(reservation.tripId)}</td>
      <td>${escapeHtml(formatReservationStatus(reservation.status))}</td>
      <td>${escapeHtml(new Date(reservation.createdAt).toLocaleString())}</td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-view-details" data-type="reservation" data-id="${escapeHtml(reservation.id)}">Ver detalles</button>
          ${hasPermission('reservations:write') ? `<button class="btn tiny secondary admin-edit" data-type="reservation" data-id="${escapeHtml(reservation.id)}">Editar</button>` : ''}
          ${hasPermission('reservations:delete') ? `<button class="btn tiny danger admin-delete" data-type="reservation" data-id="${escapeHtml(reservation.id)}">Eliminar</button>` : ''}
        </div>
      </td>
    </tr>
  `).join("");
}

async function editAdminEntity(type, id) {
  const entity = getEntityFromState(type, id);
  openEditModal(type, entity);
}

async function viewAdminEntity(type, id) {
  const entity = getEntityFromState(type, id);
  openDetailsModal(type, entity);
}

async function deleteAdminEntity(type, id) {
  const confirmed = confirm("Esta accion eliminara el registro. Deseas continuar?");
  if (!confirmed) {
    return;
  }

  const routes = {
    user: `/api/admin/users/${id}`,
    trip: `/api/admin/trips/${id}`,
    reservation: `/api/admin/reservations/${id}`
  };

  await apiFetch(routes[type], {
    method: "DELETE",
    headers: getAdminHeaders()
  });

  await loadAdminData();
}

document.getElementById("menuNav").addEventListener("click", (event) => {
  const button = event.target.closest(".menu-item");
  if (!button) {
    return;
  }

  openSection(button.dataset.section);
});



document.addEventListener("click", (event) => {
  const createButton = event.target.closest("[data-create-type]");
  if (!createButton) {
    return;
  }

  openCreateModal(createButton.dataset.createType);
});

document.getElementById("logoutBtn").addEventListener("click", () => {
  logout();
});

editModalCloseBtn?.addEventListener("click", () => {
  closeEditModal();
});

detailsModalCloseBtn?.addEventListener("click", () => {
  closeDetailsModal();
});

createModalCloseBtn?.addEventListener("click", () => {
  closeCreateModal();
});

createModalOverlay?.addEventListener("click", (event) => {
  if (event.target === createModalOverlay) {
    closeCreateModal();
  }
});

detailsModalOverlay?.addEventListener("click", (event) => {
  if (event.target === detailsModalOverlay) {
    closeDetailsModal();
  }
});

createModalForm?.addEventListener("click", (event) => {
  const tripPointButton = event.target.closest("[data-trip-point]");
  if (tripPointButton) {
    setCreateTripSelectionMode(tripPointButton.dataset.tripPoint);
    return;
  }

  const clearTripPointsButton = event.target.closest("[data-trip-action='clear-trip-points']");
  if (clearTripPointsButton) {
    resetCreateTripMapState();
    updateCreateTripMapPanels();
    updateCreateTripMapLayers();
    return;
  }

  const cancelButton = event.target.closest("[data-create-action='cancel']");
  if (cancelButton) {
    closeCreateModal();
  }
});

createModalForm?.addEventListener("change", (event) => {
  if (event.target === createEntityType) {
    renderCreateModalFields(createEntityType.value);
    return;
  }

  if (event.target?.id === "createRole") {
    updateCreateUserDriverFieldsVisibility();
  }

  if (event.target?.id === "createTripDriverSelect") {
    syncCreateTripDriverSelection();
  }

  if (event.target?.id === "createReservationPassengerSelect") {
    syncCreateReservationPassengerSelection();
  }
});

createModalForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const type = createEntityType?.value || "user";

  try {
    if (type === "user") {
      const fullName = createModalForm.querySelector("#createFullName")?.value.trim() || "";
      const email = createModalForm.querySelector("#createEmail")?.value.trim().toLowerCase() || "";
      const password = createModalForm.querySelector("#createPassword")?.value || "";
      const phoneNumber = createModalForm.querySelector("#createPhone")?.value.trim() || null;
      const role = createModalForm.querySelector("#createRole")?.value || "student";

      if (!fullName || !email || !password) {
        throw new Error("Completa los datos obligatorios del usuario.");
      }

      const payload = { fullName, email, password, phoneNumber, role };

      if (role === "driver") {
        const seats = Number(createModalForm.querySelector("#createSeats")?.value);
        const plate = createModalForm.querySelector("#createPlate")?.value.trim().toUpperCase() || "";
        const brand = createModalForm.querySelector("#createBrand")?.value.trim() || "";
        const model = createModalForm.querySelector("#createModel")?.value.trim() || "";
        const year = Number(createModalForm.querySelector("#createYear")?.value || 0);
        const color = createModalForm.querySelector("#createColor")?.value.trim() || "";

        if (!Number.isInteger(seats) || seats < 1 || seats > 5) {
          throw new Error("Para chofer, la cantidad de personas debe estar entre 1 y 5.");
        }

        if (!model) {
          throw new Error("Completa el modelo del auto.");
        }

        if (!Number.isInteger(year) || year < 1900 || year > 2100) {
          throw new Error("Completa un año valido para el auto.");
        }

        payload.driverProfile = {
          availableSeats: seats,
          licensePlate: plate,
          vehicleBrand: brand,
          vehicleModel: model,
          vehicleYear: year,
          vehicleColor: color
        };
      }

      await apiFetch("/api/users/register", {
        method: "POST",
        body: JSON.stringify(payload)
      });

      closeCreateModal();
      window.location.reload();
      return;
    }

    if (type === "trip") {
      const driverName = createModalForm.querySelector("#createTripDriverName")?.value.trim() || "";
      const driverUserId = createModalForm.querySelector("#createTripDriverUserIdDisplay")?.value.trim() || null;
      const offeredSeats = Number(createModalForm.querySelector("#createOfferedSeats")?.value || 4);
      const origin = state.tripOrigin;
      const destination = state.tripDestination;

      if (!driverName) {
        throw new Error("Completa el nombre del conductor.");
      }

      if (!origin || !destination) {
        throw new Error("Selecciona el origen y el destino en el mapa.");
      }

      const createdTrip = await apiFetch("/api/trips/origin", {
        method: "POST",
        body: JSON.stringify({
          latitude: origin.lat,
          longitude: origin.lng,
          driverName,
          driverUserId,
          offeredSeats
        })
      });

      await apiFetch(`/api/trips/${createdTrip.id}/destination`, {
        method: "POST",
        body: JSON.stringify({
          latitude: destination.lat,
          longitude: destination.lng
        })
      });

      closeCreateModal();
      window.location.reload();
      return;
    }

    const tripId = createModalForm.querySelector("#createReservationTripId")?.value.trim() || "";
    const passengerUserId = createModalForm.querySelector("#createReservationPassengerUserIdDisplay")?.value.trim() || "";
    const seatsReserved = Number(createModalForm.querySelector("#createReservationSeatsReserved")?.value || 1);

    if (!tripId || !passengerUserId) {
      throw new Error("Completa el Trip ID y el pasajero.");
    }

    await apiFetch(`/api/Trips/${tripId}/Reservations`, {
      method: "POST",
      body: JSON.stringify({
        passengerUserId,
        seatsReserved
      })
    });

    closeCreateModal();
    window.location.reload();
  } catch (error) {
    setMessage(createModalMessage, error.message, "error");
  }
});

editModalOverlay?.addEventListener("click", (event) => {
  if (event.target === editModalOverlay) {
    closeEditModal();
  }
});

editModalForm?.addEventListener("click", (event) => {
  const cancelButton = event.target.closest("[data-modal-action='cancel']");
  if (!cancelButton) {
    return;
  }

  closeEditModal();
});

editModalForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  if (!state.editContext) {
    return;
  }

  const { type, id } = state.editContext;
  const formData = new FormData(editModalForm);

  try {
    if (type === "user") {
      const password = String(formData.get("password") || "").trim();
      await apiFetch(`/api/admin/users/${id}`, {
        method: "PUT",
        headers: getAdminHeaders(),
        body: JSON.stringify({
          fullName: String(formData.get("fullName") || "").trim(),
          email: String(formData.get("email") || "").trim().toLowerCase(),
          phoneNumber: String(formData.get("phoneNumber") || "").trim() || null,
          roleId: Number(formData.get("roleId") || 1),
          password: password || null
        })
      });
    }

    if (type === "trip") {
      const destinationLatitude = getNullableNumber(formData.get("destinationLatitude"));
      const destinationLongitude = getNullableNumber(formData.get("destinationLongitude"));

      await apiFetch(`/api/admin/trips/${id}`, {
        method: "PUT",
        headers: getAdminHeaders(),
        body: JSON.stringify({
          driverName: String(formData.get("driverName") || "").trim(),
          driverUserId: String(formData.get("driverUserId") || "").trim() || null,
          originLatitude: Number(formData.get("originLatitude")),
          originLongitude: Number(formData.get("originLongitude")),
          destinationLatitude,
          destinationLongitude,
          availableSeats: Number(formData.get("availableSeats")),
          status: String(formData.get("status") || "0").trim()
        })
      });
    }

    if (type === "reservation") {
      await apiFetch(`/api/admin/reservations/${id}`, {
        method: "PUT",
        headers: getAdminHeaders(),
        body: JSON.stringify({
          tripId: String(formData.get("tripId") || "").trim(),
          passengerName: String(formData.get("passengerName") || "").trim(),
          status: String(formData.get("status") || "0").trim()
        })
      });
    }

    if (type === "support") {
      await apiFetch(`/api/admin/support-tickets/${id}/status`, {
        method: "PATCH",
        headers: getAdminHeaders(),
        body: JSON.stringify({
          status: Number(formData.get("status") || 1)
        })
      });
    }

    closeEditModal();

    if (type === "support") {
      await loadSupportTickets();
      setMessage(supportDataMessage, "Estado del reporte actualizado correctamente.", "success");
      return;
    }

    await loadAdminData();
    setMessage(adminDataMessage, "Registro actualizado correctamente.", "success");
  } catch (error) {
    setMessage(editModalMessage, error.message, "error");
  }
});

document.addEventListener("click", async (event) => {
  const viewBtn = event.target.closest(".admin-view-details");
  if (viewBtn) {
    try {
      await viewAdminEntity(viewBtn.dataset.type, viewBtn.dataset.id);
    } catch (error) {
      setMessage(adminDataMessage, `No se pudieron ver los detalles: ${error.message}`, "error");
    }
    return;
  }

  const editBtn = event.target.closest(".admin-edit");
  if (editBtn) {
    try {
      await editAdminEntity(editBtn.dataset.type, editBtn.dataset.id);
      setMessage(adminDataMessage, "Edita los datos y guarda los cambios en el formulario.");
    } catch (error) {
      setMessage(adminDataMessage, `No se pudo actualizar: ${error.message}`, "error");
    }
    return;
  }

  const deleteBtn = event.target.closest(".admin-delete");
  if (deleteBtn) {
    try {
      await deleteAdminEntity(deleteBtn.dataset.type, deleteBtn.dataset.id);
      setMessage(adminDataMessage, "Registro eliminado correctamente.", "success");
    } catch (error) {
      setMessage(adminDataMessage, `No se pudo eliminar: ${error.message}`, "error");
    }
  }
});

document.getElementById("loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const email = document.getElementById("loginEmail").value.trim().toLowerCase();
  const password = document.getElementById("loginPassword").value;

  try {
    const user = await apiFetch("/api/users/login", {
      method: "POST",
      body: JSON.stringify({ email, password })
    });

    const isAdmin = Number(user.roleId) === ADMIN_ROLE_ID;
    if (!isAdmin) {
      setMessage(loginMessage, "Usuario autenticado, pero no tiene permisos de administrador.", "error");
      return;
    }

    setMessage(loginMessage, "Sesion iniciada correctamente.", "success");
    startSession(user);
  } catch (error) {
    updateApiStatus(false);
    setMessage(loginMessage, error.message, "error");
  }
});

document.getElementById("usersFilterInput")?.addEventListener("input", (event) => {
  applyTableFilter("adminUsersBody", event.target.value);
});

document.getElementById("tripsFilterInput")?.addEventListener("input", (event) => {
  applyTableFilter("adminTripsBody", event.target.value);
});

document.getElementById("reservationsFilterInput")?.addEventListener("input", (event) => {
  applyTableFilter("adminReservationsBody", event.target.value);
});

supportFilterInput?.addEventListener("input", (event) => {
  applyTableFilter("adminSupportBody", event.target.value);
});

supportStatusFilter?.addEventListener("change", () => {
  loadSupportTickets();
});

supportCategoryFilter?.addEventListener("change", () => {
  loadSupportTickets();
});

supportReloadBtn?.addEventListener("click", () => {
  loadSupportTickets();
});

document.getElementById("section-support")?.addEventListener("click", async (event) => {
  const viewBtn = event.target.closest(".admin-view-support");
  if (!viewBtn) {
    return;
  }

  try {
    const ticket = getEntityFromState("support", viewBtn.dataset.id);
    if (!ticket) {
      throw new Error("No se encontro el reporte seleccionado.");
    }

    openEditModal("support", ticket);
    setMessage(supportDataMessage, "Revisa la descripcion y actualiza el estado cuando corresponda.");
  } catch (error) {
    setMessage(supportDataMessage, error.message, "error");
  }
});

// --- Ajustes / Apariencia (Temas Pastel Dinámicos) ---
const presets = {
  Custom: {
    primaryLight: "#82254B",
    secondaryLight: "#6E1E3F",
    textLight: "#111827",
    bgLight: "#FFFFFF",
    cardLight: "#F5F5F5",
    borderLight: "#9CA8B0",
    primaryDark: "#82254B",
    secondaryDark: "#6E1E3F",
    textDark: "#ffffff",
    bgDark: "#121011",
    cardDark: "#251a1e",
    borderDark: "#6E1E3F"
  },
  Univalle: {
    primaryLight: "#82254B",
    secondaryLight: "#6E1E3F",
    textLight: "#111827",
    bgLight: "#FFFFFF",
    cardLight: "#F5F5F5",
    borderLight: "#9CA8B0",
    primaryDark: "#82254B",
    secondaryDark: "#6E1E3F",
    textDark: "#ffffff",
    bgDark: "#121011",
    cardDark: "#251a1e",
    borderDark: "#6E1E3F"
  }
};

function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 95, g: 127, b: 108 };
}

const THEME_MODES = {
  light: {
    bg: "#f7f5ef",
    bgDeep: "#24302b",
    panelAlt: "#eff3ed",
    panel: "#ffffff"
  },
  dark: {
    bg: "#0f1412",
    bgDeep: "#121815",
    panelAlt: "#1c2621",
    panel: "#151c19"
  }
};

function applyClientTheme(colors) {
  if (!colors) return;
  const root = document.documentElement;
  
  const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  const primary = isDarkMode ? (colors.primaryDark || "#8fac98") : (colors.primaryLight || "#5f7f6c");
  const secondary = isDarkMode ? (colors.secondaryDark || "#d0a27d") : (colors.secondaryLight || "#b67a52");
  const text = isDarkMode ? (colors.textDark || "#edf2ee") : (colors.textLight || "#24302b");
  
  root.style.setProperty("--accent", primary);
  root.style.setProperty("--accent-2", secondary);
  
  let bg, bgDeep, panelAlt, panel, border;
  
  if (primary.toLowerCase() === "#82254b" || secondary.toLowerCase() === "#6e1e3f") {
    // Univalle specific premium color theme integration!
    if (isDarkMode) {
      bg = "#121011"; // Muy oscuro granate
      bgDeep = "#1c1417";
      panel = "#251a1e"; // Fondos de tarjetas oscuro
      panelAlt = "#322328"; // Fondo secundario oscuro
      border = "#6E1E3F"; // Borde granate oscuro
    } else {
      bg = "#FFFFFF";     // Blanco (Fondos principales)
      bgDeep = "#6E1E3F"; // Granate oscuro (Cabeceras y destaques)
      panel = "#F5F5F5";  // Gris muy claro (Fondos de tarjetas)
      panelAlt = "#e9ebed"; // Gris claro / claro-medio para fondos secundarios/tablas
      border = "#9CA8B0";   // Gris claro (Bordes de inputs/paneles)
    }
  } else {
    // Default dynamic theme values
    const mode = THEME_MODES[isDarkMode ? 'dark' : 'light'];
    bg = isDarkMode ? (colors.bgDark || mode.bg) : (colors.bgLight || mode.bg);
    bgDeep = mode.bgDeep;
    panelAlt = mode.panelAlt;
    panel = isDarkMode ? (colors.cardDark || mode.panel) : (colors.cardLight || mode.panel);
    border = isDarkMode ? (colors.borderDark || "rgba(255, 255, 255, 0.16)") : (colors.borderLight || "rgba(31, 29, 26, 0.16)");
  }
  
  root.style.setProperty("--bg", bg);
  root.style.setProperty("--bg-deep", bgDeep);
  root.style.setProperty("--panel-alt", panelAlt);
  root.style.setProperty("--panel", panel);
  root.style.setProperty("--border-color", border);
  root.style.setProperty("--text", text);
  
  const primaryRgb = hexToRgb(primary);
  root.style.setProperty("--primary-ring", `rgba(${primaryRgb.r}, ${primaryRgb.g}, ${primaryRgb.b}, 0.06)`);
  
  // Calcular y establecer el color de texto secundario (muted) al 60% de opacidad
  const textRgb = hexToRgb(text);
  root.style.setProperty("--muted", `rgba(${textRgb.r}, ${textRgb.g}, ${textRgb.b}, 0.6)`);
  
  const secondaryRgb = hexToRgb(secondary);
  root.style.setProperty("--ring-color", `rgba(${secondaryRgb.r}, ${secondaryRgb.g}, ${secondaryRgb.b}, 0.22)`);
  
  // Calcular contraste para el texto del sidebar
  const luminance = (secondaryRgb.r * 0.299 + secondaryRgb.g * 0.587 + secondaryRgb.b * 0.114) / 255;
  root.style.setProperty("--sidebar-text", luminance > 0.6 ? "#1a1a1a" : "#ffffff");
}

function updateHexLabels() {
  const pl = document.getElementById("themePrimaryLight")?.value;
  const sl = document.getElementById("themeSecondaryLight")?.value;
  const tl = document.getElementById("themeTextLight")?.value;
  const bgl = document.getElementById("themeBgLight")?.value;
  const cl = document.getElementById("themeCardLight")?.value;
  const bdl = document.getElementById("themeBorderLight")?.value;
  const pd = document.getElementById("themePrimaryDark")?.value;
  const sd = document.getElementById("themeSecondaryDark")?.value;
  const td = document.getElementById("themeTextDark")?.value;
  const bgd = document.getElementById("themeBgDark")?.value;
  const cd = document.getElementById("themeCardDark")?.value;
  const bdd = document.getElementById("themeBorderDark")?.value;

  if (pl) document.getElementById("themePrimaryLightHex").textContent = pl;
  if (sl) document.getElementById("themeSecondaryLightHex").textContent = sl;
  if (tl) document.getElementById("themeTextLightHex").textContent = tl;
  if (bgl) document.getElementById("themeBgLightHex").textContent = bgl;
  if (cl) document.getElementById("themeCardLightHex").textContent = cl;
  if (bdl) document.getElementById("themeBorderLightHex").textContent = bdl;
  if (pd) document.getElementById("themePrimaryDarkHex").textContent = pd;
  if (sd) document.getElementById("themeSecondaryDarkHex").textContent = sd;
  if (td) document.getElementById("themeTextDarkHex").textContent = td;
  if (bgd) document.getElementById("themeBgDarkHex").textContent = bgd;
  if (cd) document.getElementById("themeCardDarkHex").textContent = cd;
  if (bdd) document.getElementById("themeBorderDarkHex").textContent = bdd;
}

function updatePresetActiveState(presetName) {
  document.querySelectorAll(".preset-card").forEach(card => {
    card.classList.toggle("active", card.dataset.preset === presetName);
  });
}

// Sincronizar previsualización de burbujas en la tarjeta Personalizado (Custom)
function updateCustomPresetVisuals(colors) {
  const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  const prim = isDarkMode ? colors.primaryDark : colors.primaryLight;
  const sec = isDarkMode ? colors.secondaryDark : colors.secondaryLight;
  const text = isDarkMode ? colors.textDark : colors.textLight;

  const dotPrimary = document.getElementById("presetDotCustomPrimary");
  const dotSecondary = document.getElementById("presetDotCustomSecondary");
  const dotText = document.getElementById("presetDotCustomText");

  if (dotPrimary) dotPrimary.style.backgroundColor = prim;
  if (dotSecondary) dotSecondary.style.backgroundColor = sec;
  if (dotText) dotText.style.backgroundColor = text;
}

document.querySelectorAll(".preset-card").forEach(card => {
  card.addEventListener("click", () => {
    const presetName = card.dataset.preset;
    const colors = presets[presetName];
    if (colors) {
      if (document.getElementById("themePrimaryLight")) document.getElementById("themePrimaryLight").value = colors.primaryLight;
      if (document.getElementById("themeSecondaryLight")) document.getElementById("themeSecondaryLight").value = colors.secondaryLight;
      if (document.getElementById("themeTextLight")) document.getElementById("themeTextLight").value = colors.textLight;
      if (document.getElementById("themeBgLight")) document.getElementById("themeBgLight").value = colors.bgLight;
      if (document.getElementById("themeCardLight")) document.getElementById("themeCardLight").value = colors.cardLight;
      if (document.getElementById("themeBorderLight")) document.getElementById("themeBorderLight").value = colors.borderLight;
      if (document.getElementById("themePrimaryDark")) document.getElementById("themePrimaryDark").value = colors.primaryDark;
      if (document.getElementById("themeSecondaryDark")) document.getElementById("themeSecondaryDark").value = colors.secondaryDark;
      if (document.getElementById("themeTextDark")) document.getElementById("themeTextDark").value = colors.textDark;
      if (document.getElementById("themeBgDark")) document.getElementById("themeBgDark").value = colors.bgDark;
      if (document.getElementById("themeCardDark")) document.getElementById("themeCardDark").value = colors.cardDark;
      if (document.getElementById("themeBorderDark")) document.getElementById("themeBorderDark").value = colors.borderDark;
      
      updateHexLabels();
      updatePresetActiveState(presetName);
      applyClientTheme(colors);
      if (presetName === "Custom") {
        updateCustomPresetVisuals(colors);
      }
    }
  });
});

const getCurrentThemeColors = () => ({
  primaryLight: document.getElementById("themePrimaryLight")?.value || "#5f7f6c",
  secondaryLight: document.getElementById("themeSecondaryLight")?.value || "#b67a52",
  textLight: document.getElementById("themeTextLight")?.value || "#24302b",
  bgLight: document.getElementById("themeBgLight")?.value || "#f7f5ef",
  cardLight: document.getElementById("themeCardLight")?.value || "#ffffff",
  borderLight: document.getElementById("themeBorderLight")?.value || "rgba(31, 29, 26, 0.16)",
  primaryDark: document.getElementById("themePrimaryDark")?.value || "#8fac98",
  secondaryDark: document.getElementById("themeSecondaryDark")?.value || "#d0a27d",
  textDark: document.getElementById("themeTextDark")?.value || "#edf2ee",
  bgDark: document.getElementById("themeBgDark")?.value || "#0f1412",
  cardDark: document.getElementById("themeCardDark")?.value || "#151c19",
  borderDark: document.getElementById("themeBorderDark")?.value || "rgba(255, 255, 255, 0.16)"
});

["themePrimaryLight", "themeSecondaryLight", "themeTextLight", "themeBgLight", "themeCardLight", "themeBorderLight",
 "themePrimaryDark", "themeSecondaryDark", "themeTextDark", "themeBgDark", "themeCardDark", "themeBorderDark"].forEach(id => {
  document.getElementById(id)?.addEventListener("input", () => {
    const val = document.getElementById(id).value;
    const hexEl = document.getElementById(id + "Hex");
    if (hexEl) hexEl.textContent = val;
    
    const currentColors = getCurrentThemeColors();
    
    // Guardar dinámicamente en presets.Custom
    presets.Custom = { ...currentColors };
    updateCustomPresetVisuals(currentColors);
    
    applyClientTheme(currentColors);
    updatePresetActiveState("Custom");
  });
});

window.matchMedia('(prefers-color-scheme: dark)').addEventListener('change', () => {
  const currentColors = getCurrentThemeColors();
  updateCustomPresetVisuals(currentColors);
  applyClientTheme(currentColors);
});

async function fetchThemeOnStartup() {
  try {
    const response = await fetch(`${normalizeUrl(state.apiBaseUrl)}/api/settings/theme`, {
      method: "GET",
      headers: { "Content-Type": "application/json" }
    });
    if (response.ok) {
      const colors = await response.json();
      applyClientTheme(colors);
    }
  } catch (error) {
    console.warn("No se pudo cargar el tema dinámico del backend. Usando tema Univalle por defecto.", error);
    applyClientTheme(presets.Univalle);
  }
}

async function loadThemeSettings() {
  const messageEl = document.getElementById("settingsMessage");
  setMessage(messageEl, "Cargando colores de tema...");
  try {
    const colors = await apiFetch("/api/settings/theme", {
      method: "GET",
      headers: state.currentUser ? { "X-User-Id": state.currentUser.id } : {}
    });
    
    const primaryLight = colors.primaryLight || "#5f7f6c";
    const secondaryLight = colors.secondaryLight || "#b67a52";
    const textLight = colors.textLight || "#24302b";
    const bgLight = colors.bgLight || "#f7f5ef";
    const cardLight = colors.cardLight || "#ffffff";
    const borderLight = colors.borderLight || "rgba(31, 29, 26, 0.16)";
    const primaryDark = colors.primaryDark || "#8fac98";
    const secondaryDark = colors.secondaryDark || "#d0a27d";
    const textDark = colors.textDark || "#edf2ee";
    const bgDark = colors.bgDark || "#0f1412";
    const cardDark = colors.cardDark || "#151c19";
    const borderDark = colors.borderDark || "rgba(255, 255, 255, 0.16)";

    if (document.getElementById("themePrimaryLight")) document.getElementById("themePrimaryLight").value = primaryLight;
    if (document.getElementById("themeSecondaryLight")) document.getElementById("themeSecondaryLight").value = secondaryLight;
    if (document.getElementById("themeTextLight")) document.getElementById("themeTextLight").value = textLight;
    if (document.getElementById("themeBgLight")) document.getElementById("themeBgLight").value = bgLight;
    if (document.getElementById("themeCardLight")) document.getElementById("themeCardLight").value = cardLight;
    if (document.getElementById("themeBorderLight")) document.getElementById("themeBorderLight").value = borderLight;
    if (document.getElementById("themePrimaryDark")) document.getElementById("themePrimaryDark").value = primaryDark;
    if (document.getElementById("themeSecondaryDark")) document.getElementById("themeSecondaryDark").value = secondaryDark;
    if (document.getElementById("themeTextDark")) document.getElementById("themeTextDark").value = textDark;
    if (document.getElementById("themeBgDark")) document.getElementById("themeBgDark").value = bgDark;
    if (document.getElementById("themeCardDark")) document.getElementById("themeCardDark").value = cardDark;
    if (document.getElementById("themeBorderDark")) document.getElementById("themeBorderDark").value = borderDark;
    
    updateHexLabels();

    // Actualizar el preset Custom con los colores cargados de la BD
    presets.Custom = {
      primaryLight,
      secondaryLight,
      textLight,
      bgLight,
      cardLight,
      borderLight,
      primaryDark,
      secondaryDark,
      textDark,
      bgDark,
      cardDark,
      borderDark
    };
    updateCustomPresetVisuals(presets.Custom);
    
    let matchedPreset = null;
    for (const [name, pColors] of Object.entries(presets)) {
      if (name === "Custom") continue;
      if (pColors.primaryLight.toLowerCase() === primaryLight.toLowerCase() &&
          pColors.secondaryLight.toLowerCase() === secondaryLight.toLowerCase() &&
          pColors.textLight.toLowerCase() === textLight.toLowerCase() &&
          pColors.bgLight.toLowerCase() === bgLight.toLowerCase() &&
          pColors.cardLight.toLowerCase() === cardLight.toLowerCase() &&
          pColors.borderLight.toLowerCase() === borderLight.toLowerCase() &&
          pColors.primaryDark.toLowerCase() === primaryDark.toLowerCase() &&
          pColors.secondaryDark.toLowerCase() === secondaryDark.toLowerCase() &&
          pColors.textDark.toLowerCase() === textDark.toLowerCase() &&
          pColors.bgDark.toLowerCase() === bgDark.toLowerCase() &&
          pColors.cardDark.toLowerCase() === cardDark.toLowerCase() &&
          pColors.borderDark.toLowerCase() === borderDark.toLowerCase()) {
        matchedPreset = name;
        break;
      }
    }
    
    if (!matchedPreset) {
      matchedPreset = "Custom";
    }
    
    updatePresetActiveState(matchedPreset);
    applyClientTheme(presets[matchedPreset]);
    setMessage(messageEl, "Configuración cargada correctamente.", "success");
  } catch (error) {
    setMessage(messageEl, `Error al cargar la configuración: ${error.message}`, "error");
  }
}

document.getElementById("themeSettingsForm")?.addEventListener("submit", async (event) => {
  event.preventDefault();
  const messageEl = document.getElementById("settingsMessage");
  setMessage(messageEl, "Guardando colores...");
  
  const colors = getCurrentThemeColors();
  
  try {
    await apiFetch("/api/settings/theme", {
      method: "PUT",
      headers: state.currentUser ? { "X-User-Id": state.currentUser.id } : {},
      body: JSON.stringify(colors)
    });
    
    presets.Custom = { ...colors };
    updateCustomPresetVisuals(colors);
    applyClientTheme(colors);
    
    // Determinar si corresponde a algún preset predefinido o es Personalizado
    let matchedPreset = null;
    for (const [name, pColors] of Object.entries(presets)) {
      if (name === "Custom") continue;
      if (pColors.primaryLight.toLowerCase() === colors.primaryLight.toLowerCase() &&
          pColors.secondaryLight.toLowerCase() === colors.secondaryLight.toLowerCase() &&
          pColors.textLight.toLowerCase() === colors.textLight.toLowerCase() &&
          pColors.bgLight.toLowerCase() === colors.bgLight.toLowerCase() &&
          pColors.cardLight.toLowerCase() === colors.cardLight.toLowerCase() &&
          pColors.borderLight.toLowerCase() === colors.borderLight.toLowerCase() &&
          pColors.primaryDark.toLowerCase() === colors.primaryDark.toLowerCase() &&
          pColors.secondaryDark.toLowerCase() === colors.secondaryDark.toLowerCase() &&
          pColors.textDark.toLowerCase() === colors.textDark.toLowerCase() &&
          pColors.bgDark.toLowerCase() === colors.bgDark.toLowerCase() &&
          pColors.cardDark.toLowerCase() === colors.cardDark.toLowerCase() &&
          pColors.borderDark.toLowerCase() === colors.borderDark.toLowerCase()) {
        matchedPreset = name;
        break;
      }
    }
    
    if (!matchedPreset) {
      matchedPreset = "Custom";
    }
    
    updatePresetActiveState(matchedPreset);
    setMessage(messageEl, "¡Colores guardados exitosamente! Se han propagado al sistema.", "success");
  } catch (error) {
    setMessage(messageEl, `Error al guardar los colores: ${error.message}`, "error");
  }
});

function createSafeZoneMarkerElement(isActive = true) {
  const element = document.createElement("div");
  element.className = isActive ? "safe-zone-pin" : "safe-zone-pin safe-zone-pin--inactive";
  element.innerHTML = '<span class="safe-zone-pin__icon">🛡️</span>';
  return element;
}

function getSafeZoneEditIsActive() {
  return document.getElementById("safeZoneIsActive")?.checked !== false;
}

function syncSafeZoneEditMarkerStyle() {
  if (!state.safeZoneEditMarker) {
    return;
  }

  const element = state.safeZoneEditMarker.getElement();
  if (!element) {
    return;
  }

  const isActive = getSafeZoneEditIsActive();
  element.className = isActive ? "safe-zone-pin" : "safe-zone-pin safe-zone-pin--inactive";
}

function clearSafeZoneMarkers(markers) {
  markers.forEach((marker) => marker.remove());
  markers.length = 0;
}

async function ensurePublicSafeZonesLoaded() {
  if (state.publicSafeZones.length) {
    return state.publicSafeZones;
  }

  const zones = await apiFetch("/api/safe-zones");
  state.publicSafeZones = Array.isArray(zones) ? zones : [];
  return state.publicSafeZones;
}

async function refreshPublicSafeZoneMarkersOnMap(map, markersKey) {
  if (!map || typeof mapboxgl === "undefined") {
    return;
  }

  const markers = state[markersKey];
  if (!markers) {
    return;
  }

  clearSafeZoneMarkers(markers);

  try {
    const zones = await ensurePublicSafeZonesLoaded();
    zones.filter((zone) => zone.isActive !== false).forEach((zone) => {
      const marker = new mapboxgl.Marker({
        element: createSafeZoneMarkerElement(true),
        anchor: "bottom"
      })
        .setLngLat([zone.longitude, zone.latitude])
        .setPopup(new mapboxgl.Popup({ offset: 18 }).setHTML(
          `<strong>${escapeHtml(zone.name)}</strong><br/><small>${escapeHtml(zone.purposeLabel || "")}</small>`
        ))
        .addTo(map);
      markers.push(marker);
    });
  } catch (error) {
    console.warn("No se pudieron cargar zonas seguras en el mapa:", error.message);
  }
}

function getSafeZonePurposeLabel(purpose) {
  const value = Number(purpose);
  if (value === 1) {
    return "Solo recogida";
  }
  if (value === 2) {
    return "Solo destino";
  }
  return "Recogida y destino";
}

function filterSafeZonesList(zones) {
  const status = safeZonesStatusFilter?.value || "";
  const search = (safeZonesFilterInput?.value || "").trim().toLowerCase();

  return zones.filter((zone) => {
    if (status === "active" && !zone.isActive) {
      return false;
    }
    if (status === "inactive" && zone.isActive) {
      return false;
    }
    if (search) {
      const haystack = `${zone.name} ${zone.campusArea || ""} ${zone.addressLabel || ""}`.toLowerCase();
      if (!haystack.includes(search)) {
        return false;
      }
    }
    return true;
  });
}

function updateSafeZonesStatusMessage() {
  if (!safeZonesDataMessage) {
    return;
  }

  const total = state.safeZones.length;
  const visible = filterSafeZonesList(state.safeZones);

  if (!total) {
    setMessage(safeZonesDataMessage, "No hay zonas seguras registradas.", "");
    return;
  }

  if (visible.length === total) {
    setMessage(safeZonesDataMessage, `${total} zona(s) cargada(s).`, "success");
    return;
  }

  setMessage(safeZonesDataMessage, `Mostrando ${visible.length} de ${total} zona(s).`, "success");
}

function refreshSafeZonesDisplay() {
  const visibleZones = filterSafeZonesList(state.safeZones);
  renderSafeZonesTable(visibleZones, state.safeZones.length);

  if (state.safeZoneOverviewMapReady) {
    renderSafeZonesOnOverviewMap(visibleZones);
  }

  updateSafeZonesStatusMessage();
}

function renderSafeZonesTable(zones, totalCount = zones.length) {
  if (!adminSafeZonesBody) {
    return;
  }

  if (!zones.length) {
    const emptyMessage = totalCount === 0
      ? "No hay zonas seguras registradas."
      : "No hay zonas que coincidan con el filtro.";
    adminSafeZonesBody.innerHTML = `<tr><td colspan="6">${emptyMessage}</td></tr>`;
    return;
  }

  adminSafeZonesBody.innerHTML = zones.map((zone) => `
    <tr data-search="${escapeHtml(`${zone.name} ${zone.campusArea || ""} ${zone.addressLabel || ""}`.toLowerCase())}">
      <td>${escapeHtml(zone.name)}</td>
      <td>${escapeHtml(zone.campusArea || "-")}</td>
      <td>${escapeHtml(zone.purposeLabel || getSafeZonePurposeLabel(zone.purpose))}</td>
      <td>${zone.isActive ? "Activa" : "Inactiva"}</td>
      <td>${zone.latitude?.toFixed?.(5) ?? zone.latitude}, ${zone.longitude?.toFixed?.(5) ?? zone.longitude}</td>
      <td class="table-actions">
        ${hasPermission('trips:write') ? `
          <button type="button" class="btn tiny" data-safe-zone-action="edit" data-id="${zone.id}">Editar</button>
          <button type="button" class="btn tiny danger" data-safe-zone-action="delete" data-id="${zone.id}">Eliminar</button>
        ` : ''}
      </td>
    </tr>
  `).join("");
}

function applySafeZonesTableFilter() {
  refreshSafeZonesDisplay();
}

async function loadSafeZonesAdmin() {
  if (!safeZonesDataMessage) {
    return;
  }

  setMessage(safeZonesDataMessage, "Cargando zonas seguras...", "");

  try {
    const zones = await apiFetch("/api/admin/safe-zones", {
      headers: getAdminHeaders()
    });
    state.safeZones = Array.isArray(zones) ? zones : [];
    state.publicSafeZones = state.safeZones.filter((zone) => zone.isActive);
    refreshSafeZonesDisplay();
    window.requestAnimationFrame(() => initializeSafeZonesOverviewMap());
  } catch (error) {
    state.safeZones = [];
    renderSafeZonesTable([], 0);
    setMessage(safeZonesDataMessage, error.message, "error");
  }
}

function destroySafeZonesOverviewMap() {
  clearSafeZoneMarkers(state.safeZoneOverviewMarkers);
  if (state.safeZoneOverviewMap) {
    state.safeZoneOverviewMap.remove();
    state.safeZoneOverviewMap = null;
  }
  state.safeZoneOverviewMapReady = false;
}

function renderSafeZonesOnOverviewMap(zones = null) {
  if (!state.safeZoneOverviewMap || !state.safeZoneOverviewMapReady) {
    return;
  }

  const visibleZones = zones ?? filterSafeZonesList(state.safeZones);

  clearSafeZoneMarkers(state.safeZoneOverviewMarkers);

  visibleZones.forEach((zone) => {
    const marker = new mapboxgl.Marker({
      element: createSafeZoneMarkerElement(zone.isActive),
      anchor: "bottom"
    })
      .setLngLat([zone.longitude, zone.latitude])
      .setPopup(new mapboxgl.Popup({ offset: 18 }).setHTML(
        `<strong>${escapeHtml(zone.name)}</strong><br/><small>${escapeHtml(zone.purposeLabel || getSafeZonePurposeLabel(zone.purpose))}</small>`
      ))
      .addTo(state.safeZoneOverviewMap);
    state.safeZoneOverviewMarkers.push(marker);
  });

  if (visibleZones.length) {
    const bounds = new mapboxgl.LngLatBounds();
    visibleZones.forEach((zone) => bounds.extend([zone.longitude, zone.latitude]));
    state.safeZoneOverviewMap.fitBounds(bounds, { padding: 48, maxZoom: 14 });
  }
}

function initializeSafeZonesOverviewMap() {
  const container = document.getElementById("safeZonesOverviewMap");
  if (!container || typeof mapboxgl === "undefined") {
    return;
  }

  if (state.safeZoneOverviewMap) {
    renderSafeZonesOnOverviewMap(filterSafeZonesList(state.safeZones));
    return;
  }

  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;
  const fallbackCenter = state.safeZones[0]
    ? [state.safeZones[0].longitude, state.safeZones[0].latitude]
    : [-74.0721, 4.7110];

  state.safeZoneOverviewMap = new mapboxgl.Map({
    container,
    style: "mapbox://styles/mapbox/streets-v12",
    center: fallbackCenter,
    zoom: 12
  });

  state.safeZoneOverviewMap.addControl(new mapboxgl.NavigationControl(), "top-right");
  state.safeZoneOverviewMap.on("load", () => {
    state.safeZoneOverviewMapReady = true;
    renderSafeZonesOnOverviewMap();
  });
}

function destroySafeZoneEditMap(clearDraftPoint = true) {
  if (state.safeZoneEditMarker) {
    state.safeZoneEditMarker.remove();
    state.safeZoneEditMarker = null;
  }

  if (state.safeZoneEditMap) {
    state.safeZoneEditMap.remove();
    state.safeZoneEditMap = null;
  }

  state.safeZoneEditMapReady = false;
  if (clearDraftPoint) {
    state.safeZoneDraftPoint = null;
  }
}

function updateSafeZoneCoordsHint() {
  if (!safeZoneCoordsHint) {
    return;
  }

  if (!state.safeZoneDraftPoint) {
    safeZoneCoordsHint.textContent = "Coordenadas: sin definir";
    return;
  }

  safeZoneCoordsHint.textContent = `Coordenadas: ${state.safeZoneDraftPoint.lat.toFixed(6)}, ${state.safeZoneDraftPoint.lng.toFixed(6)}`;
}

function updateSafeZoneEditMapMarker() {
  if (!state.safeZoneEditMap || !state.safeZoneDraftPoint) {
    return;
  }

  const coordinates = [state.safeZoneDraftPoint.lng, state.safeZoneDraftPoint.lat];
  const isActive = getSafeZoneEditIsActive();

  if (!state.safeZoneEditMarker) {
    state.safeZoneEditMarker = new mapboxgl.Marker({
      element: createSafeZoneMarkerElement(isActive),
      anchor: "bottom",
      draggable: true
    })
      .setLngLat(coordinates)
      .addTo(state.safeZoneEditMap);

    state.safeZoneEditMarker.on("dragend", () => {
      const lngLat = state.safeZoneEditMarker.getLngLat();
      state.safeZoneDraftPoint = {
        lng: Number(lngLat.lng.toFixed(6)),
        lat: Number(lngLat.lat.toFixed(6))
      };
      updateSafeZoneCoordsHint();
    });
  } else {
    state.safeZoneEditMarker.setLngLat(coordinates);
    syncSafeZoneEditMarkerStyle();
  }
}

function initializeSafeZoneEditMap() {
  const container = document.getElementById("safeZoneEditMap");
  if (!container || typeof mapboxgl === "undefined") {
    setMessage(safeZoneModalMessage, "El mapa no pudo inicializarse.", "error");
    return;
  }

  destroySafeZoneEditMap(false);
  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;

  const fallbackCenter = state.safeZoneDraftPoint
    ? [state.safeZoneDraftPoint.lng, state.safeZoneDraftPoint.lat]
    : [-74.0721, 4.7110];

  state.safeZoneEditMap = new mapboxgl.Map({
    container,
    style: "mapbox://styles/mapbox/streets-v12",
    center: fallbackCenter,
    zoom: state.safeZoneDraftPoint ? 15 : 13
  });

  state.safeZoneEditMap.addControl(new mapboxgl.NavigationControl(), "top-right");
  state.safeZoneEditMap.on("load", () => {
    state.safeZoneEditMapReady = true;
    state.safeZoneEditMap.resize();
    updateSafeZoneEditMapMarker();
    if (state.safeZoneDraftPoint) {
      state.safeZoneEditMap.flyTo({
        center: [state.safeZoneDraftPoint.lng, state.safeZoneDraftPoint.lat],
        zoom: 15
      });
    }
  });

  state.safeZoneEditMap.on("click", (event) => {
    state.safeZoneDraftPoint = {
      lng: Number(event.lngLat.lng.toFixed(6)),
      lat: Number(event.lngLat.lat.toFixed(6))
    };
    updateSafeZoneCoordsHint();
    updateSafeZoneEditMapMarker();
  });
}

function openSafeZoneModal(zone = null) {
  if (!safeZoneModalOverlay || !safeZoneModalForm) {
    return;
  }

  destroySafeZoneEditMap(false);
  setMessage(safeZoneModalMessage, "");

  const isEdit = Boolean(zone);
  safeZoneModalTitle.textContent = isEdit ? "Editar zona segura" : "Nueva zona segura";
  document.getElementById("safeZoneEditId").value = isEdit ? zone.id : "";
  document.getElementById("safeZoneName").value = zone?.name || "";
  document.getElementById("safeZoneCampusArea").value = zone?.campusArea || "";
  document.getElementById("safeZoneDescription").value = zone?.description || "";
  document.getElementById("safeZonePurpose").value = String(zone?.purpose ?? 0);
  document.getElementById("safeZoneDisplayOrder").value = String(zone?.displayOrder ?? 0);
  document.getElementById("safeZoneIsActive").checked = zone?.isActive !== false;

  state.safeZoneDraftPoint = zone
    ? { lat: zone.latitude, lng: zone.longitude }
    : null;
  updateSafeZoneCoordsHint();

  safeZoneModalOverlay.classList.remove("hidden");
  safeZoneModalOverlay.setAttribute("aria-hidden", "false");

  window.requestAnimationFrame(() => initializeSafeZoneEditMap());
}

function closeSafeZoneModal() {
  destroySafeZoneEditMap();
  if (safeZoneModalOverlay) {
    safeZoneModalOverlay.classList.add("hidden");
    safeZoneModalOverlay.setAttribute("aria-hidden", "true");
  }
  setMessage(safeZoneModalMessage, "");
}

async function deleteSafeZone(id) {
  const confirmed = confirm("Eliminar esta zona segura? Los usuarios dejaran de verla en el mapa.");
  if (!confirmed) {
    return;
  }

  await apiFetch(`/api/admin/safe-zones/${id}`, {
    method: "DELETE",
    headers: getAdminHeaders()
  });

  state.publicSafeZones = [];
  await loadSafeZonesAdmin();
}

safeZonesReloadBtn?.addEventListener("click", () => loadSafeZonesAdmin());
safeZoneCreateBtn?.addEventListener("click", () => openSafeZoneModal());
safeZonesFilterInput?.addEventListener("input", applySafeZonesTableFilter);
safeZonesStatusFilter?.addEventListener("change", applySafeZonesTableFilter);
document.getElementById("safeZoneIsActive")?.addEventListener("change", syncSafeZoneEditMarkerStyle);
safeZoneModalCloseBtn?.addEventListener("click", closeSafeZoneModal);

safeZoneModalOverlay?.addEventListener("click", (event) => {
  if (event.target === safeZoneModalOverlay) {
    closeSafeZoneModal();
  }
});

safeZoneModalForm?.addEventListener("click", (event) => {
  const action = event.target.closest("[data-safe-zone-action]")?.dataset.safeZoneAction;
  if (action === "cancel") {
    closeSafeZoneModal();
  }
  if (action === "clear-point") {
    state.safeZoneDraftPoint = null;
    if (state.safeZoneEditMarker) {
      state.safeZoneEditMarker.remove();
      state.safeZoneEditMarker = null;
    }
    updateSafeZoneCoordsHint();
  }
});

safeZoneModalForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const id = document.getElementById("safeZoneEditId")?.value.trim() || "";
  const name = document.getElementById("safeZoneName")?.value.trim() || "";
  const campusArea = document.getElementById("safeZoneCampusArea")?.value.trim() || null;
  const description = document.getElementById("safeZoneDescription")?.value.trim() || null;
  const purpose = Number(document.getElementById("safeZonePurpose")?.value || 0);
  const displayOrder = Number(document.getElementById("safeZoneDisplayOrder")?.value || 0);
  const isActive = document.getElementById("safeZoneIsActive")?.checked ?? true;

  if (!name) {
    setMessage(safeZoneModalMessage, "El nombre es obligatorio.", "error");
    return;
  }

  if (!state.safeZoneDraftPoint) {
    setMessage(safeZoneModalMessage, "Marca la ubicacion en el mapa.", "error");
    return;
  }

  const payload = {
    name,
    description,
    campusArea,
    purpose,
    displayOrder,
    isActive,
    latitude: state.safeZoneDraftPoint.lat,
    longitude: state.safeZoneDraftPoint.lng
  };

  try {
    if (id) {
      await apiFetch(`/api/admin/safe-zones/${id}`, {
        method: "PUT",
        headers: getAdminHeaders(),
        body: JSON.stringify(payload)
      });
    } else {
      await apiFetch("/api/admin/safe-zones", {
        method: "POST",
        headers: getAdminHeaders(),
        body: JSON.stringify(payload)
      });
    }

    state.publicSafeZones = [];
    closeSafeZoneModal();
    await loadSafeZonesAdmin();
  } catch (error) {
    setMessage(safeZoneModalMessage, error.message, "error");
  }
});

document.addEventListener("click", (event) => {
  const safeZoneButton = event.target.closest("[data-safe-zone-action]");
  if (!safeZoneButton || !safeZoneButton.dataset.id) {
    return;
  }

  const zone = state.safeZones.find((item) => String(item.id) === String(safeZoneButton.dataset.id));
  if (!zone) {
    return;
  }

  if (safeZoneButton.dataset.safeZoneAction === "edit") {
    openSafeZoneModal(zone);
  }

  if (safeZoneButton.dataset.safeZoneAction === "delete") {
    deleteSafeZone(zone.id).catch((error) => setMessage(safeZonesDataMessage, error.message, "error"));
  }
});

// --- Admins Management & Modal logic ---
const adminCreateBtn = document.getElementById("adminCreateBtn");
const adminModalOverlay = document.getElementById("adminModalOverlay");
const adminModalCloseBtn = document.getElementById("adminModalCloseBtn");
const adminModalCancelBtn = document.getElementById("adminModalCancelBtn");
const adminModalForm = document.getElementById("adminModalForm");
const adminModalMessage = document.getElementById("adminModalMessage");
const adminRolePreset = document.getElementById("adminRolePreset");
const adminCustomRoleNameContainer = document.getElementById("adminCustomRoleNameContainer");
const adminCustomRoleName = document.getElementById("adminCustomRoleName");
const permissionCheckboxes = document.querySelectorAll('#adminModalForm input[name="permissions"]');

function formatRoleNameCleanly(roleName) {
  if (!roleName) return "Admin";
  if (roleName === "SuperAdmin") return "Super Administrador";
  
  const match = roleName.match(/^Admin\s*-\s*(.+?)\s*\([^\)]+\)$/);
  if (match) {
    return match[1];
  }
  
  if (roleName.startsWith("Admin - ")) {
    return roleName.substring(8);
  }
  
  return roleName;
}

function formatPermissionLabel(perm) {
  const mapping = {
    "metrics:view": "Ver métricas",
    "users:read": "Ver usuarios",
    "users:write": "Modificar usuarios",
    "users:delete": "Eliminar usuarios",
    "trips:read": "Ver viajes",
    "trips:write": "Modificar viajes",
    "trips:delete": "Eliminar viajes",
    "reservations:read": "Ver reservas",
    "reservations:write": "Modificar reservas",
    "reservations:delete": "Eliminar reservas",
    "support:read": "Ver reportes",
    "support:write": "Responder reportes",
    "roles:manage": "Gestionar roles"
  };
  return mapping[perm] || perm;
}

function renderAdminsTable(users) {
  const adminAdminsBody = document.getElementById("adminAdminsBody");
  if (!adminAdminsBody) {
    return;
  }

  const admins = users.filter(u => u.role === "admin");

  if (!admins.length) {
    adminAdminsBody.innerHTML = '<tr><td colspan="5">Sin administradores.</td></tr>';
    return;
  }

  adminAdminsBody.innerHTML = admins.map((user) => {
    const cleanRoles = (user.rawRoles && user.rawRoles.length > 0) 
      ? user.rawRoles.map(formatRoleNameCleanly).join(", ") 
      : "Admin";
    const permsText = (user.permissions && user.permissions.length > 0) 
      ? user.permissions.map(formatPermissionLabel).join(", ") 
      : "Ninguno";

    return `
      <tr>
        <td>${escapeHtml(user.fullName)}</td>
        <td>${escapeHtml(user.email)}</td>
        <td>${escapeHtml(cleanRoles)}</td>
        <td><small style="color: var(--muted);">${escapeHtml(permsText)}</small></td>
        <td>${escapeHtml(formatDateTime(user.createdAt))}</td>
      </tr>
    `;
  }).join("");
}

function handlePresetChange() {
  const preset = adminRolePreset.value;
  const adminPresets = {
    Soporte: ["support:read", "support:write", "users:read", "metrics:view"],
    Analista: ["metrics:view", "users:read", "trips:read", "reservations:read"],
    Secretariado: ["users:read", "users:write", "trips:read", "reservations:read"],
    Personalizado: []
  };

  if (preset === "Personalizado") {
    adminCustomRoleNameContainer.classList.remove("hidden");
    adminCustomRoleName.required = true;
    permissionCheckboxes.forEach(cb => {
      cb.disabled = false;
    });
  } else {
    adminCustomRoleNameContainer.classList.add("hidden");
    adminCustomRoleName.required = false;
    adminCustomRoleName.value = "";
    const allowedPerms = adminPresets[preset] || [];
    permissionCheckboxes.forEach(cb => {
      cb.checked = allowedPerms.includes(cb.value);
      cb.disabled = true;
    });
  }
}

adminRolePreset?.addEventListener("change", handlePresetChange);

adminCreateBtn?.addEventListener("click", () => {
  if (adminModalForm) {
    adminModalForm.reset();
  }
  if (adminModalMessage) {
    setMessage(adminModalMessage, "");
  }
  
  if (adminRolePreset) {
    adminRolePreset.value = "Soporte";
    handlePresetChange();
  }

  adminModalOverlay?.classList.remove("hidden");
  adminModalOverlay?.setAttribute("aria-hidden", "false");
});

function closeAdminModal() {
  adminModalOverlay?.classList.add("hidden");
  adminModalOverlay?.setAttribute("aria-hidden", "true");
  if (adminModalMessage) {
    setMessage(adminModalMessage, "");
  }
}

adminModalCloseBtn?.addEventListener("click", closeAdminModal);
adminModalCancelBtn?.addEventListener("click", closeAdminModal);

adminModalForm?.addEventListener("submit", async (event) => {
  event.preventDefault();

  const fullName = document.getElementById("adminFullName").value.trim();
  const email = document.getElementById("adminEmail").value.trim().toLowerCase();
  const password = document.getElementById("adminPassword").value;
  const preset = adminRolePreset.value;
  
  let roleName = "";
  if (preset === "Personalizado") {
    roleName = adminCustomRoleName.value.trim();
  } else {
    roleName = preset;
  }

  const permissions = Array.from(permissionCheckboxes).filter(cb => cb.checked).map(cb => cb.value);

  if (!fullName || !email || !password) {
    setMessage(adminModalMessage, "Todos los campos son obligatorios.", "error");
    return;
  }

  if (!email.endsWith("@univalle.edu")) {
    setMessage(adminModalMessage, "Solo se permiten correos institucionales @univalle.edu", "error");
    return;
  }

  setMessage(adminModalMessage, "Creando cuenta de administrador...", "");

  try {
    const payload = {
      fullName,
      email,
      password,
      roleName,
      permissions
    };

    await apiFetch("/api/admin/users/create-web-admin", {
      method: "POST",
      headers: getAdminHeaders(),
      body: JSON.stringify(payload)
    });

    setMessage(adminModalMessage, "Administrador creado correctamente.", "success");
    setTimeout(() => {
      closeAdminModal();
      loadAdminData();
    }, 1500);
  } catch (error) {
    setMessage(adminModalMessage, error.message, "error");
  }
});

document.getElementById("adminsFilterInput")?.addEventListener("input", (event) => {
  applyTableFilter("adminAdminsBody", event.target.value);
});

paymentsFilterInput?.addEventListener("input", applyPaymentsFilter);
paymentsStatusFilter?.addEventListener("change", applyPaymentsFilter);
paymentsReloadBtn?.addEventListener("click", loadAdminPayments);

// --- Lógica de Exportación y Análisis Multiformato ---
function exportToPDF(data, headers, filename) {
  if (!data || !data.length) {
    alert("No hay datos para exportar.");
    return;
  }
  
  const printWindow = window.open("", "_blank");
  if (!printWindow) {
    alert("Por favor habilita las ventanas emergentes para exportar a PDF.");
    return;
  }

  const titleText = filename.replace(/_/g, ' ').toUpperCase();
  
  const html = `
    <!DOCTYPE html>
    <html lang="es">
    <head>
      <meta charset="UTF-8">
      <title>Reporte - ${titleText}</title>
      <style>
        body {
          font-family: 'Inter', -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
          color: #111827;
          margin: 40px;
          background-color: #fff;
        }
        .header-container {
          display: flex;
          justify-content: space-between;
          align-items: flex-end;
          border-bottom: 3px solid #82254B;
          padding-bottom: 12px;
          margin-bottom: 24px;
        }
        h2 {
          color: #82254B;
          margin: 0;
          font-size: 24px;
          font-weight: 700;
        }
        .meta-info {
          font-size: 12px;
          color: #6B7280;
          text-align: right;
        }
        table {
          width: 100%;
          border-collapse: collapse;
          margin-top: 15px;
        }
        th, td {
          border: 1px solid #E5E7EB;
          padding: 10px 12px;
          text-align: left;
          font-size: 11px;
          line-height: 1.4;
        }
        th {
          background-color: #F9FAFB;
          color: #374151;
          font-weight: 600;
          text-transform: uppercase;
          font-size: 10px;
          letter-spacing: 0.05em;
        }
        tr:nth-child(even) {
          background-color: #F9FAFB;
        }
        .footer {
          margin-top: 40px;
          border-top: 1px solid #E5E7EB;
          padding-top: 12px;
          font-size: 10px;
          color: #9CA3AF;
          display: flex;
          justify-content: space-between;
        }
        @media print {
          body { margin: 20px; }
          .no-print { display: none; }
        }
      </style>
    </head>
    <body>
      <div class="header-container">
        <div>
          <h2>Reporte de ${titleText}</h2>
          <div style="font-size: 12px; color: #4B5563; margin-top: 4px;">Sistema de Carpooling Univalle</div>
        </div>
        <div class="meta-info">
          <div>Fecha: ${new Date().toLocaleDateString('es-ES')}</div>
          <div>Hora: ${new Date().toLocaleTimeString('es-ES')}</div>
        </div>
      </div>
      <table>
        <thead>
          <tr>
            ${headers.map(h => `<th>${escapeHtml(h)}</th>`).join('')}
          </tr>
        </thead>
        <tbody>
          ${data.map(row => `
            <tr>
              ${row.map(val => `<td>${escapeHtml(String(val ?? ""))}</td>`).join('')}
            </tr>
          `).join('')}
        </tbody>
      </table>
      <div class="footer">
        <span>© ${new Date().getFullYear()} Universidad del Valle - Equipo 12</span>
        <span>Confidencial - Uso Administrativo</span>
      </div>
      <script>
        window.onload = function() {
          setTimeout(() => {
            window.print();
          }, 300);
        };
      </script>
    </body>
    </html>
  `;
  
  printWindow.document.write(html);
  printWindow.document.close();
}

function performExport(data, headers, filename, format) {
  if (!data || !data.length) {
    alert("No hay datos para exportar.");
    return;
  }
  
  if (format === "pdf") {
    exportToPDF(data, headers, filename);
  } else if (format === "excel") {
    // Para Excel en español, el separador punto y coma (;) es el óptimo
    const csvContent = [
      headers.join(";"),
      ...data.map(row => row.map(val => {
        const strVal = String(val ?? "");
        return `"${strVal.replace(/"/g, '""')}"`;
      }).join(";"))
    ].join("\n");
    const blob = new Blob(["\ufeff" + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", filename + "_excel.csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  } else {
    // CSV Estándar (separado por comas)
    const csvContent = [
      headers.join(","),
      ...data.map(row => row.map(val => {
        const strVal = String(val ?? "");
        return `"${strVal.replace(/"/g, '""')}"`;
      }).join(","))
    ].join("\n");
    const blob = new Blob(["\ufeff" + csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", filename + ".csv");
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
    URL.revokeObjectURL(url);
  }
}

function getExportUsersData() {
  const users = state.adminData.users || [];
  const exportType = document.getElementById("exportUserTypeSelect")?.value || "all";
  
  let filteredUsers = users;
  let filename = "usuarios_todos";
  
  if (exportType === "students") {
    filteredUsers = users.filter(u => Number(u.roleId) === 1 || String(u.role || "").toLowerCase().includes("student") || String(u.role || "").toLowerCase().includes("passenger"));
    filename = "usuarios_estudiantes";
  } else if (exportType === "drivers") {
    filteredUsers = users.filter(isDriverUser);
    filename = "conductores";
  } else if (exportType === "admins") {
    filteredUsers = users.filter(u => Number(u.roleId) === 3 || String(u.role || "").toLowerCase().includes("admin"));
    filename = "administradores";
  }
  
  let headers = ["ID", "Nombre", "Email", "Rol", "Telefono", "Fecha Creacion"];
  let data = filteredUsers.map(u => [u.id, u.fullName, u.email, u.role, u.phoneNumber, u.createdAt]);
  
  if (exportType === "drivers") {
    headers = ["ID", "Nombre", "Email", "Rol", "Telefono", "Vehiculos", "Placa", "Fecha Creacion"];
    data = filteredUsers.map(d => {
      const profile = d.driverProfile || d.DriverProfile;
      const plate = profile ? profile.licensePlate : (d.vehicles && d.vehicles[0] ? d.vehicles[0].licensePlate : "");
      return [d.id, d.fullName, d.email, d.role, d.phoneNumber, d.vehicles?.length || 0, plate, d.createdAt];
    });
  }
  return { data, headers, filename };
}

function getExportTripsData() {
  const trips = state.adminData.trips || [];
  const headers = ["ID Viaje", "Conductor", "Origen", "Destino", "Estado", "Cupos", "Tipo", "Fecha Creacion"];
  const data = trips.map(t => {
    const statusVal = t.statusLabel ?? t.statusId ?? t.status;
    const origin = getTripOriginCoordinates(t);
    const destination = getTripDestinationCoordinates(t);
    const originText = hasTripCoordinates(origin) ? `${origin.lat}, ${origin.lng}` : "-";
    const destText = hasTripCoordinates(destination) ? `${destination.lat}, ${destination.lng}` : "-";
    return [
      t.id,
      t.driverName || "-",
      originText,
      destText,
      formatTripStatus(statusVal),
      t.availableSeats,
      formatTripKind(t.kind),
      t.createdAt || "-"
    ];
  });
  return { data, headers, filename: "viajes" };
}

function getExportReservationsData() {
  const reservations = state.adminData.reservations || [];
  const headers = ["ID Reserva", "ID Viaje", "Pasajero", "Asientos", "Estado", "Fecha Creacion"];
  const data = reservations.map(r => [r.id, r.tripId, r.passengerName, r.seatsReserved, formatReservationStatus(r.status), r.createdAt]);
  return { data, headers, filename: "reservas" };
}

// Vinculación de eventos de dropdown de Usuarios
document.getElementById("exportUsersExcel")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportUsersData();
  performExport(data, headers, filename, "excel");
});
document.getElementById("exportUsersCsv")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportUsersData();
  performExport(data, headers, filename, "csv");
});
document.getElementById("exportUsersPdf")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportUsersData();
  performExport(data, headers, filename, "pdf");
});

// Vinculación de eventos de dropdown de Viajes
document.getElementById("exportTripsExcel")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportTripsData();
  performExport(data, headers, filename, "excel");
});
document.getElementById("exportTripsCsv")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportTripsData();
  performExport(data, headers, filename, "csv");
});
document.getElementById("exportTripsPdf")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportTripsData();
  performExport(data, headers, filename, "pdf");
});

// Vinculación de eventos de dropdown de Reservas
document.getElementById("exportReservationsExcel")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportReservationsData();
  performExport(data, headers, filename, "excel");
});
document.getElementById("exportReservationsCsv")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportReservationsData();
  performExport(data, headers, filename, "csv");
});
document.getElementById("exportReservationsPdf")?.addEventListener("click", () => {
  const { data, headers, filename } = getExportReservationsData();
  performExport(data, headers, filename, "pdf");
});

// Control global para menús desplegables (Dropdowns)
document.addEventListener("click", (e) => {
  const toggle = e.target.closest(".dropdown-toggle");
  if (toggle) {
    const dropdown = toggle.closest(".dropdown");
    if (dropdown) {
      const isOpen = dropdown.classList.contains("open");
      document.querySelectorAll(".dropdown.open").forEach(d => d.classList.remove("open"));
      if (!isOpen) {
        dropdown.classList.add("open");
      }
      e.stopPropagation();
      return;
    }
  }
  if (!e.target.closest(".dropdown-menu")) {
    document.querySelectorAll(".dropdown.open").forEach(d => d.classList.remove("open"));
  }
});

let tripsChartInstance = null;
let usersChartInstance = null;
let reservationsChartInstance = null;

function renderAdminCharts(filteredUsers, filteredTrips, filteredReservations) {
  if (typeof Chart === 'undefined') return;

  const style = getComputedStyle(document.body);
  const primaryColor = style.getPropertyValue('--accent').trim() || '#82254B';
  const secondaryColor = style.getPropertyValue('--accent-2').trim() || '#6E1E3F';
  const textSecondaryColor = style.getPropertyValue('--text').trim() || '#111827';
  
  Chart.defaults.color = textSecondaryColor;
  Chart.defaults.font.family = "'Inter', sans-serif";

  // 1. Gráfica de Viajes por Día (Barras)
  const tripsByDay = {};
  filteredTrips.forEach(t => {
    if (!t.createdAt) return;
    const dateStr = new Date(t.createdAt).toLocaleDateString();
    tripsByDay[dateStr] = (tripsByDay[dateStr] || 0) + 1;
  });
  
  const tripDates = Object.keys(tripsByDay).sort((a,b) => new Date(a) - new Date(b));
  const tripCounts = tripDates.map(d => tripsByDay[d]);

  const ctxTrips = document.getElementById('tripsChart')?.getContext('2d');
  if (ctxTrips) {
    if (tripsChartInstance) tripsChartInstance.destroy();
    tripsChartInstance = new Chart(ctxTrips, {
      type: 'bar',
      data: {
        labels: tripDates.length ? tripDates : ['Sin datos'],
        datasets: [{
          label: 'Viajes Realizados',
          data: tripCounts.length ? tripCounts : [0],
          backgroundColor: primaryColor,
          borderRadius: 4
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: { y: { beginAtZero: true, ticks: { stepSize: 1 } } }
      }
    });
  }

  // 2. Gráfica Usuarios vs Conductores (Pastel)
  const totalDrivers = filteredUsers.filter(isDriverUser).length;
  const totalPassengers = filteredUsers.length - totalDrivers;

  const ctxUsers = document.getElementById('usersChart')?.getContext('2d');
  if (ctxUsers) {
    if (usersChartInstance) usersChartInstance.destroy();
    usersChartInstance = new Chart(ctxUsers, {
      type: 'pie',
      data: {
        labels: ['Pasajeros Promedio', 'Conductores'],
        datasets: [{
          data: [totalPassengers, totalDrivers],
          backgroundColor: [primaryColor, '#9CA8B0'], // Granate Univalle y Gris Univalle
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { position: 'bottom' } }
      }
    });
  }

  // 3. Gráfica Reservas (Dona)
  const reservationsStatus = {
    'Pendientes': 0,
    'Confirmadas': 0,
    'Abordadas': 0,
    'Canceladas': 0
  };
  
  filteredReservations.forEach(r => {
    const st = Number(r.status);
    if (st === 1) reservationsStatus['Pendientes']++;
    else if (st === 2) reservationsStatus['Confirmadas']++;
    else if (st === 3) reservationsStatus['Abordadas']++;
    else reservationsStatus['Canceladas']++;
  });

  const ctxReservations = document.getElementById('reservationsChart')?.getContext('2d');
  if (ctxReservations) {
    if (reservationsChartInstance) reservationsChartInstance.destroy();
    reservationsChartInstance = new Chart(ctxReservations, {
      type: 'doughnut',
      data: {
        labels: Object.keys(reservationsStatus),
        datasets: [{
          data: Object.values(reservationsStatus),
          backgroundColor: ['#f59e0b', primaryColor, secondaryColor, '#ef4444'],
          borderWidth: 0
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { position: 'bottom' } }
      }
    });
  }
}

document.getElementById("analyticsConsultBtn")?.addEventListener("click", () => {
  const startStr = document.getElementById("analyticsStartDate")?.value;
  const endStr = document.getElementById("analyticsEndDate")?.value;
  
  let start = null;
  let end = null;

  if (startStr) {
    if (startStr.includes("-") && startStr.split("-")[0].length === 4) {
      const [y, m, d] = startStr.split("-");
      start = new Date(y, m - 1, d, 0, 0, 0, 0);
    } else {
      start = new Date(startStr);
      if (!isNaN(start.getTime())) start.setHours(0, 0, 0, 0);
      else start = null;
    }
  }
  
  if (endStr) {
    if (endStr.includes("-") && endStr.split("-")[0].length === 4) {
      const [y, m, d] = endStr.split("-");
      end = new Date(y, m - 1, d, 23, 59, 59, 999);
    } else {
      end = new Date(endStr);
      if (!isNaN(end.getTime())) end.setHours(23, 59, 59, 999);
      else end = null;
    }
  }

  const filterByDate = (item) => {
    if (!item.createdAt) return false;
    const d = new Date(item.createdAt);
    let isValid = true;
    if (start) isValid = isValid && d >= start;
    if (end) isValid = isValid && d <= end;
    return isValid;
  };

  const filteredUsers = (state.adminData.users || []).filter(filterByDate);
  const filteredTrips = (state.adminData.trips || []).filter(filterByDate);
  const filteredReservations = (state.adminData.reservations || []).filter(filterByDate);

  document.getElementById("analyticsUsersCount").textContent = filteredUsers.length;
  document.getElementById("analyticsTripsCount").textContent = filteredTrips.length;
  document.getElementById("analyticsReservationsCount").textContent = filteredReservations.length;
  
  renderAdminCharts(filteredUsers, filteredTrips, filteredReservations);
});

// Startup Execution
resolveApiBaseUrl().finally(() => {
  fetchThemeOnStartup().finally(() => {
    if (state.currentUser) {
      if (Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
        startSession(state.currentUser);
      } else {
        logout();
        setMessage(loginMessage, "La sesion guardada no corresponde a un administrador.", "error");
      }
    } else {
      updateApiStatus(false);
    }
  });
});
