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
  isLoadingSupportTickets: false,
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
const kpiSection = document.getElementById("kpiSection");
const kpiApiBase = document.getElementById("kpiApiBase");
const kpiSession = document.getElementById("kpiSession");
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
const adminSafeZonesBody = document.getElementById("adminSafeZonesBody");
const safeZonesDataMessage = document.getElementById("safeZonesDataMessage");
const safeZonesFilterInput = document.getElementById("safeZonesFilterInput");
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

document.getElementById("apiBaseUrl").value = state.apiBaseUrl;

function setMessage(element, text, kind = "") {
  if (!element) {
    return;
  }

  element.textContent = text;
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
  kpiSection.textContent = sectionMeta.name;

  document.querySelectorAll(".menu-item").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.section === section);
  });

  document.querySelectorAll("[id^='section-']").forEach((panel) => {
    panel.classList.add("hidden");
  });

  document.getElementById(`section-${section}`).classList.remove("hidden");

  if (["users", "trips", "reservations", "reports"].includes(section) && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadAdminData();
  }

  if (section === "support" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadSupportTickets();
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
      description: "Monitorea estado de sesion, API activa y accesos rapidos." 
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
    settings: {
      name: "Ajustes",
      description: "Personaliza la identidad visual y los colores corporativos de tu negocio."
    },
    "safe-zones": {
      name: "Zonas seguras",
      description: "Administra paradas tranquilas visibles en el mapa de la app movil."
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
      <label class="field"><span>Asientos ofrecidos</span><input type="number" id="createOfferedSeats" min="1" max="50" value="4" required /></label>
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
      <label class="field"><span>Asientos reservados</span><input type="number" id="createReservationSeatsReserved" min="1" max="50" value="1" required /></label>
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
      <label class="field"><span>Cantidad de personas (asientos)</span><input type="number" id="createSeats" min="1" max="12" placeholder="1 a 12" /></label>
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
        "line-color": "#1f8a86",
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
      state.tripOriginMarker = new mapboxgl.Marker({ color: "#f29d55" })
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
      state.tripDestinationMarker = new mapboxgl.Marker({ color: "#2ea7a0" })
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
  state.editContext = null;
  editModalForm.innerHTML = "";
  setMessage(editModalMessage, "");
  editModalOverlay.classList.add("hidden");
  editModalOverlay.setAttribute("aria-hidden", "true");
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
        "line-color": "#1f8a86",
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
      state.detailTripOriginMarker = new mapboxgl.Marker({ color: "#f29d55" })
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
      state.detailTripDestinationMarker = new mapboxgl.Marker({ color: "#2ea7a0" })
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
    support: "Ver detalles del reporte"
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
    support: state.adminData.supportTickets
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
      <label class="field"><span>Email institucional</span><input type="email" name="email" value="${escapeHtml(entity.email || "")}" required /></label>
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
      <label class="field"><span>Cupos disponibles</span><input type="number" min="0" name="availableSeats" value="${escapeHtml(entity.availableSeats ?? 0)}" required /></label>
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

function startSession(user) {
  state.currentUser = user;
  localStorage.setItem("cp.adminUser", JSON.stringify(user));
  localStorage.setItem("cp.apiBaseUrl", state.apiBaseUrl);

  authShell.classList.add("hidden");
  dashboard.classList.remove("hidden");
  loggedUserInfo.textContent = `Admin: ${user.fullName} (${user.email})`;
  kpiApiBase.textContent = state.apiBaseUrl;
  kpiSession.textContent = "Activa";
  updateApiStatus(true, "login exitoso");
  startAdminAutoRefresh();
  openSection("overview");
}

function logout() {
  state.currentUser = null;
  localStorage.removeItem("cp.adminUser");

  dashboard.classList.add("hidden");
  authShell.classList.remove("hidden");
  stopAdminAutoRefresh();
  updateApiStatus(false);
  kpiSession.textContent = "Inactiva";
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
    applyAllTableFilters();

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
          <button class="btn tiny secondary admin-view-support" data-id="${escapeHtml(ticket.id)}">Ver / Atender</button>
        </div>
      </td>
    </tr>
  `).join("");
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
          <button class="btn tiny secondary admin-edit" data-type="user" data-id="${escapeHtml(user.id)}">Editar</button>
          <button class="btn tiny danger admin-delete" data-type="user" data-id="${escapeHtml(user.id)}">Eliminar</button>
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
          <button class="btn tiny secondary admin-edit" data-type="trip" data-id="${escapeHtml(trip.id)}">Editar</button>
          <button class="btn tiny danger admin-delete" data-type="trip" data-id="${escapeHtml(trip.id)}">Eliminar</button>
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
          <button class="btn tiny secondary admin-edit" data-type="reservation" data-id="${escapeHtml(reservation.id)}">Editar</button>
          <button class="btn tiny danger admin-delete" data-type="reservation" data-id="${escapeHtml(reservation.id)}">Eliminar</button>
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

document.getElementById("overviewQuickActions").addEventListener("click", (event) => {
  const button = event.target.closest(".quick-link");
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

        if (!Number.isInteger(seats) || seats < 1 || seats > 12) {
          throw new Error("Para chofer, la cantidad de personas debe estar entre 1 y 12.");
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
      await apiFetch(`/api/admin/users/${id}`, {
        method: "PUT",
        headers: getAdminHeaders(),
        body: JSON.stringify({
          fullName: String(formData.get("fullName") || "").trim(),
          email: String(formData.get("email") || "").trim().toLowerCase(),
          phoneNumber: String(formData.get("phoneNumber") || "").trim() || null,
          roleId: Number(formData.get("roleId") || 1)
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
  const apiBaseUrlInput = document.getElementById("apiBaseUrl").value.trim();

  if (!apiBaseUrlInput) {
    setMessage(loginMessage, "Debes indicar la URL base del backend.", "error");
    return;
  }

  state.apiBaseUrl = normalizeUrl(apiBaseUrlInput);

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

document.getElementById("usersFilterInput").addEventListener("input", (event) => {
  applyTableFilter("adminUsersBody", event.target.value);
});

document.getElementById("tripsFilterInput").addEventListener("input", (event) => {
  applyTableFilter("adminTripsBody", event.target.value);
});

document.getElementById("reservationsFilterInput").addEventListener("input", (event) => {
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
    primaryLight: "#e08c75",
    secondaryLight: "#6b8f8d",
    textLight: "#1f1d1a",
    primaryDark: "#e27b53",
    secondaryDark: "#85aba9",
    textDark: "#e0e0e0"
  },
  Sunset: {
    primaryLight: "#e08c75",
    secondaryLight: "#6b8f8d",
    textLight: "#1f1d1a",
    primaryDark: "#e27b53",
    secondaryDark: "#85aba9",
    textDark: "#e0e0e0"
  },
  Emerald: {
    primaryLight: "#7abfa6",
    secondaryLight: "#5a8777",
    textLight: "#1c2e28",
    primaryDark: "#67b095",
    secondaryDark: "#78ab98",
    textDark: "#e2e9e6"
  },
  Ocean: {
    primaryLight: "#7bb0db",
    secondaryLight: "#8496ab",
    textLight: "#18232e",
    primaryDark: "#62a1d3",
    secondaryDark: "#9cb1c9",
    textDark: "#e2e8ee"
  },
  Orchid: {
    primaryLight: "#a397db",
    secondaryLight: "#9e7fa6",
    textLight: "#231e33",
    primaryDark: "#9485d4",
    secondaryDark: "#bca4c4",
    textDark: "#edeaf5"
  },
  Midnight: {
    primaryLight: "#cba477",
    secondaryLight: "#8a7b69",
    textLight: "#26211a",
    primaryDark: "#dcb282",
    secondaryDark: "#a89782",
    textDark: "#f4f1ed"
  }
};

function hexToRgb(hex) {
  const result = /^#?([a-f\d]{2})([a-f\d]{2})([a-f\d]{2})$/i.exec(hex);
  return result ? {
    r: parseInt(result[1], 16),
    g: parseInt(result[2], 16),
    b: parseInt(result[3], 16)
  } : { r: 219, g: 91, b: 45 };
}

const THEME_MODES = {
  light: {
    bg: "#f8f4ef",
    bgDeep: "#1c2a2f",
    panelAlt: "#fff6ec",
    panel: "#ffffff"
  },
  dark: {
    bg: "#121212",
    bgDeep: "#0a0a0a",
    panelAlt: "#1e1e1e",
    panel: "#181818"
  }
};

function applyClientTheme(colors) {
  if (!colors) return;
  const root = document.documentElement;
  
  const isDarkMode = window.matchMedia && window.matchMedia('(prefers-color-scheme: dark)').matches;
  const primary = isDarkMode ? (colors.primaryDark || "#e27b53") : (colors.primaryLight || "#db5b2d");
  const secondary = isDarkMode ? (colors.secondaryDark || "#2ea7a0") : (colors.secondaryLight || "#1f8a86");
  const text = isDarkMode ? (colors.textDark || "#e0e0e0") : (colors.textLight || "#1f1d1a");
  
  root.style.setProperty("--accent", primary);
  root.style.setProperty("--accent-2", secondary);
  
  const mode = THEME_MODES[isDarkMode ? 'dark' : 'light'];
  root.style.setProperty("--bg", mode.bg);
  root.style.setProperty("--bg-deep", mode.bgDeep);
  root.style.setProperty("--panel-alt", mode.panelAlt);
  root.style.setProperty("--panel", mode.panel);
  root.style.setProperty("--text", text);
  
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
  const pd = document.getElementById("themePrimaryDark")?.value;
  const sd = document.getElementById("themeSecondaryDark")?.value;
  const td = document.getElementById("themeTextDark")?.value;

  if (pl) document.getElementById("themePrimaryLightHex").textContent = pl;
  if (sl) document.getElementById("themeSecondaryLightHex").textContent = sl;
  if (tl) document.getElementById("themeTextLightHex").textContent = tl;
  if (pd) document.getElementById("themePrimaryDarkHex").textContent = pd;
  if (sd) document.getElementById("themeSecondaryDarkHex").textContent = sd;
  if (td) document.getElementById("themeTextDarkHex").textContent = td;
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
      if (document.getElementById("themePrimaryDark")) document.getElementById("themePrimaryDark").value = colors.primaryDark;
      if (document.getElementById("themeSecondaryDark")) document.getElementById("themeSecondaryDark").value = colors.secondaryDark;
      if (document.getElementById("themeTextDark")) document.getElementById("themeTextDark").value = colors.textDark;
      
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
  primaryLight: document.getElementById("themePrimaryLight")?.value || "#db5b2d",
  secondaryLight: document.getElementById("themeSecondaryLight")?.value || "#1f8a86",
  textLight: document.getElementById("themeTextLight")?.value || "#1f1d1a",
  primaryDark: document.getElementById("themePrimaryDark")?.value || "#e27b53",
  secondaryDark: document.getElementById("themeSecondaryDark")?.value || "#2ea7a0",
  textDark: document.getElementById("themeTextDark")?.value || "#e0e0e0"
});

["themePrimaryLight", "themeSecondaryLight", "themeTextLight", "themePrimaryDark", "themeSecondaryDark", "themeTextDark"].forEach(id => {
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
    console.warn("No se pudo cargar el tema dinámico del backend. Usando Sunset Orange por defecto.", error);
    applyClientTheme(presets.Sunset);
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
    
    const primaryLight = colors.primaryLight || "#e08c75";
    const secondaryLight = colors.secondaryLight || "#6b8f8d";
    const textLight = colors.textLight || "#1f1d1a";
    const primaryDark = colors.primaryDark || "#e27b53";
    const secondaryDark = colors.secondaryDark || "#85aba9";
    const textDark = colors.textDark || "#e0e0e0";

    if (document.getElementById("themePrimaryLight")) document.getElementById("themePrimaryLight").value = primaryLight;
    if (document.getElementById("themeSecondaryLight")) document.getElementById("themeSecondaryLight").value = secondaryLight;
    if (document.getElementById("themeTextLight")) document.getElementById("themeTextLight").value = textLight;
    if (document.getElementById("themePrimaryDark")) document.getElementById("themePrimaryDark").value = primaryDark;
    if (document.getElementById("themeSecondaryDark")) document.getElementById("themeSecondaryDark").value = secondaryDark;
    if (document.getElementById("themeTextDark")) document.getElementById("themeTextDark").value = textDark;
    
    updateHexLabels();

    // Actualizar el preset Custom con los colores cargados de la BD
    presets.Custom = {
      primaryLight,
      secondaryLight,
      textLight,
      primaryDark,
      secondaryDark,
      textDark
    };
    updateCustomPresetVisuals(presets.Custom);
    
    let matchedPreset = null;
    for (const [name, pColors] of Object.entries(presets)) {
      if (name === "Custom") continue;
      if (pColors.primaryLight.toLowerCase() === primaryLight.toLowerCase() &&
          pColors.secondaryLight.toLowerCase() === secondaryLight.toLowerCase() &&
          pColors.textLight.toLowerCase() === textLight.toLowerCase() &&
          pColors.primaryDark.toLowerCase() === primaryDark.toLowerCase() &&
          pColors.secondaryDark.toLowerCase() === secondaryDark.toLowerCase() &&
          pColors.textDark.toLowerCase() === textDark.toLowerCase()) {
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
          pColors.primaryDark.toLowerCase() === colors.primaryDark.toLowerCase() &&
          pColors.secondaryDark.toLowerCase() === colors.secondaryDark.toLowerCase() &&
          pColors.textDark.toLowerCase() === colors.textDark.toLowerCase()) {
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

function renderSafeZonesTable(zones) {
  if (!adminSafeZonesBody) {
    return;
  }

  if (!zones.length) {
    adminSafeZonesBody.innerHTML = `<tr><td colspan="6">No hay zonas seguras registradas.</td></tr>`;
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
        <button type="button" class="btn tiny" data-safe-zone-action="edit" data-id="${zone.id}">Editar</button>
        <button type="button" class="btn tiny danger" data-safe-zone-action="delete" data-id="${zone.id}">Eliminar</button>
      </td>
    </tr>
  `).join("");
}

function applySafeZonesTableFilter() {
  applyTableFilter("adminSafeZonesBody", safeZonesFilterInput?.value || "");
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
    renderSafeZonesTable(state.safeZones);
    applySafeZonesTableFilter();
    setMessage(safeZonesDataMessage, `${state.safeZones.length} zona(s) cargada(s).`, "success");
    window.requestAnimationFrame(() => initializeSafeZonesOverviewMap());
  } catch (error) {
    state.safeZones = [];
    renderSafeZonesTable([]);
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

function renderSafeZonesOnOverviewMap() {
  if (!state.safeZoneOverviewMap || !state.safeZoneOverviewMapReady) {
    return;
  }

  clearSafeZoneMarkers(state.safeZoneOverviewMarkers);

  state.safeZones.forEach((zone) => {
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

  if (state.safeZones.length) {
    const bounds = new mapboxgl.LngLatBounds();
    state.safeZones.forEach((zone) => bounds.extend([zone.longitude, zone.latitude]));
    state.safeZoneOverviewMap.fitBounds(bounds, { padding: 48, maxZoom: 14 });
  }
}

function initializeSafeZonesOverviewMap() {
  const container = document.getElementById("safeZonesOverviewMap");
  if (!container || typeof mapboxgl === "undefined") {
    return;
  }

  if (state.safeZoneOverviewMap) {
    renderSafeZonesOnOverviewMap();
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

function destroySafeZoneEditMap() {
  if (state.safeZoneEditMarker) {
    state.safeZoneEditMarker.remove();
    state.safeZoneEditMarker = null;
  }

  if (state.safeZoneEditMap) {
    state.safeZoneEditMap.remove();
    state.safeZoneEditMap = null;
  }

  state.safeZoneEditMapReady = false;
  state.safeZoneDraftPoint = null;
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
  if (!state.safeZoneEditMarker) {
    state.safeZoneEditMarker = new mapboxgl.Marker({
      element: createSafeZoneMarkerElement(true),
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
  }
}

function initializeSafeZoneEditMap() {
  const container = document.getElementById("safeZoneEditMap");
  if (!container || typeof mapboxgl === "undefined") {
    setMessage(safeZoneModalMessage, "El mapa no pudo inicializarse.", "error");
    return;
  }

  destroySafeZoneEditMap();
  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;

  const fallbackCenter = state.safeZoneDraftPoint
    ? [state.safeZoneDraftPoint.lng, state.safeZoneDraftPoint.lat]
    : [-74.0721, 4.7110];

  state.safeZoneEditMap = new mapboxgl.Map({
    container,
    style: "mapbox://styles/mapbox/streets-v12",
    center: fallbackCenter,
    zoom: 13
  });

  state.safeZoneEditMap.addControl(new mapboxgl.NavigationControl(), "top-right");
  state.safeZoneEditMap.on("load", () => {
    state.safeZoneEditMapReady = true;
    updateSafeZoneEditMapMarker();
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

  destroySafeZoneEditMap();
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

// Startup Execution
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
