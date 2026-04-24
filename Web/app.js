const ADMIN_ROLE_ID = 3;
const ADMIN_DATA_REFRESH_MS = 30000;
const MAPBOX_ACCESS_TOKEN = "pk.eyJ1IjoiYW5kcmV3cmNybyIsImEiOiJjbWlzNmluem0waGJkM2dxMjcwejhrdHpyIn0.2L3Op-tGiAmSDlysfhwTsw";
const MAPBOX_SCRIPT_SRC = "./node_modules/mapbox-gl/dist/mapbox-gl.js";

const state = {
  apiBaseUrl: localStorage.getItem("cp.apiBaseUrl") || "http://localhost:5005",
  currentUser: JSON.parse(localStorage.getItem("cp.adminUser") || "null"),
  section: "overview",
  adminData: {
    users: [],
    trips: [],
    reservations: []
  },
  editContext: null,
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
  tripDrivers: [],
  isLoadingTripDrivers: false
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
const reportViewSwitch = document.getElementById("reportViewSwitch");
const tripMapContainer = document.getElementById("tripMap");
const tripDriverSelect = document.getElementById("tripDriverSelect");
const tripDriverUserIdDisplay = document.getElementById("tripDriverUserIdDisplay");
const selectOriginBtn = document.getElementById("selectOriginBtn");
const selectDestinationBtn = document.getElementById("selectDestinationBtn");
const tripSelectionInstruction = document.getElementById("tripSelectionInstruction");
const tripOriginSummary = document.getElementById("tripOriginSummary");
const tripDestinationSummary = document.getElementById("tripDestinationSummary");
const editModalOverlay = document.getElementById("editModalOverlay");
const editModalTitle = document.getElementById("editModalTitle");
const editModalForm = document.getElementById("editModalForm");
const editModalMessage = document.getElementById("editModalMessage");
const editModalCloseBtn = document.getElementById("editModalCloseBtn");

document.getElementById("apiBaseUrl").value = state.apiBaseUrl;

function setMessage(element, text, kind = "") {
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

function isDriverUser(user) {
  return Number(user?.roleId) === 2 || String(user?.role || "").toLowerCase() === "driver";
}

function renderTripDriverOptions() {
  if (!tripDriverSelect) {
    return;
  }

  const currentValue = tripDriverSelect.value;

  if (!state.tripDrivers.length) {
    tripDriverSelect.innerHTML = '<option value="" selected disabled>No hay conductores registrados</option>';
    updateTripDriverSelection();
    return;
  }

  const defaultOption = '<option value="" disabled>Selecciona un conductor</option>';
  const options = state.tripDrivers.map((driver) => (
    `<option value="${escapeHtml(driver.id)}">${escapeHtml(driver.fullName)} - ${escapeHtml(driver.email)}</option>`
  )).join("");

  tripDriverSelect.innerHTML = `${defaultOption}${options}`;

  const hasCurrentValue = state.tripDrivers.some((driver) => String(driver.id) === String(currentValue));
  if (hasCurrentValue) {
    tripDriverSelect.value = currentValue;
  } else {
    tripDriverSelect.selectedIndex = 0;
  }

  updateTripDriverSelection();
}

function updateTripDriverSelection() {
  const selectedDriverId = tripDriverSelect?.value || "";
  if (tripDriverUserIdDisplay) {
    tripDriverUserIdDisplay.value = selectedDriverId;
  }
}

async function loadTripDrivers(force = false) {
  if (!tripDriverSelect || state.isLoadingTripDrivers) {
    return;
  }

  if (!force && state.tripDrivers.length > 0) {
    renderTripDriverOptions();
    return;
  }

  state.isLoadingTripDrivers = true;
  tripDriverSelect.innerHTML = '<option value="" selected disabled>Cargando conductores...</option>';

  try {
    const users = await apiFetch("/api/admin/users", {
      headers: getAdminHeaders()
    });

    state.tripDrivers = (users || []).filter(isDriverUser);
    renderTripDriverOptions();
  } catch (error) {
    state.tripDrivers = [];
    renderTripDriverOptions();
    setMessage(document.getElementById("tripCreateMessage"), `No se pudieron cargar conductores: ${error.message}`, "error");
  } finally {
    state.isLoadingTripDrivers = false;
  }
}

function setTripSelectionInstruction(text) {
  if (tripSelectionInstruction) {
    tripSelectionInstruction.textContent = text;
  }
}

function setTripSelectionMode(mode) {
  state.tripSelectionMode = mode;

  selectOriginBtn?.classList.toggle("active", mode === "origin");
  selectDestinationBtn?.classList.toggle("active", mode === "destination");

  if (mode === "origin") {
    setTripSelectionInstruction("Toca el mapa para establecer el origen del viaje.");
    return;
  }

  if (mode === "destination") {
    setTripSelectionInstruction("Toca el mapa para establecer el destino del viaje.");
    return;
  }

  setTripSelectionInstruction("Presiona un boton y toca el mapa para establecer origen o destino.");
}

function updateTripCoordinateSummary() {
  if (tripOriginSummary) {
    tripOriginSummary.textContent = state.tripOrigin
      ? formatCoordinates(state.tripOrigin.lat, state.tripOrigin.lng)
      : "Sin seleccionar";
  }

  if (tripDestinationSummary) {
    tripDestinationSummary.textContent = state.tripDestination
      ? formatCoordinates(state.tripDestination.lat, state.tripDestination.lng)
      : "Sin seleccionar";
  }
}

function setTripPoint(mode, lat, lng) {
  const point = { lat, lng };

  if (mode === "origin") {
    state.tripOrigin = point;

    if (state.tripOriginMarker) {
      state.tripOriginMarker.setLngLat([lng, lat]);
    } else {
      state.tripOriginMarker = new mapboxgl.Marker({ color: "#db5b2d" }).setLngLat([lng, lat]).addTo(state.tripMap);
    }

    setMessage(document.getElementById("tripCreateMessage"), "Origen seleccionado en el mapa.");
  }

  if (mode === "destination") {
    state.tripDestination = point;

    if (state.tripDestinationMarker) {
      state.tripDestinationMarker.setLngLat([lng, lat]);
    } else {
      state.tripDestinationMarker = new mapboxgl.Marker({ color: "#1f8a86" }).setLngLat([lng, lat]).addTo(state.tripMap);
    }

    setMessage(document.getElementById("tripCreateMessage"), "Destino seleccionado en el mapa.");
  }

  updateTripCoordinateSummary();
  setTripSelectionMode(null);
}

function renderTripCoordinatesOnMap(trip) {
  if (!trip || !state.tripMapReady || !state.tripMap) {
    return;
  }

  const hasOrigin = trip.originLatitude != null && trip.originLongitude != null;
  const hasDestination = trip.destinationLatitude != null && trip.destinationLongitude != null;

  if (!hasOrigin) {
    return;
  }

  state.tripOrigin = {
    lat: Number(trip.originLatitude),
    lng: Number(trip.originLongitude)
  };

  if (state.tripOriginMarker) {
    state.tripOriginMarker.setLngLat([state.tripOrigin.lng, state.tripOrigin.lat]);
  } else {
    state.tripOriginMarker = new mapboxgl.Marker({ color: "#db5b2d" })
      .setLngLat([state.tripOrigin.lng, state.tripOrigin.lat])
      .addTo(state.tripMap);
  }

  if (hasDestination) {
    state.tripDestination = {
      lat: Number(trip.destinationLatitude),
      lng: Number(trip.destinationLongitude)
    };

    if (state.tripDestinationMarker) {
      state.tripDestinationMarker.setLngLat([state.tripDestination.lng, state.tripDestination.lat]);
    } else {
      state.tripDestinationMarker = new mapboxgl.Marker({ color: "#1f8a86" })
        .setLngLat([state.tripDestination.lng, state.tripDestination.lat])
        .addTo(state.tripMap);
    }
  } else {
    state.tripDestination = null;
    if (state.tripDestinationMarker) {
      state.tripDestinationMarker.remove();
      state.tripDestinationMarker = null;
    }
  }

  updateTripCoordinateSummary();

  if (hasDestination) {
    const bounds = new mapboxgl.LngLatBounds(
      [state.tripOrigin.lng, state.tripOrigin.lat],
      [state.tripDestination.lng, state.tripDestination.lat]
    );
    state.tripMap.fitBounds(bounds, { padding: 60, duration: 700 });
  } else {
    state.tripMap.flyTo({ center: [state.tripOrigin.lng, state.tripOrigin.lat], zoom: 14, duration: 700 });
  }
}

function ensureMapboxLoaded() {
  if (typeof mapboxgl !== "undefined") {
    return Promise.resolve(true);
  }

  if (state.mapboxLoadPromise) {
    return state.mapboxLoadPromise;
  }

  state.mapboxLoadPromise = new Promise((resolve) => {
    const resolveSafely = (loaded) => {
      state.mapboxLoadPromise = null;
      resolve(loaded);
    };

    const existingScript = document.querySelector(`script[src="${MAPBOX_SCRIPT_SRC}"]`);
    if (existingScript) {
      if (typeof mapboxgl !== "undefined") {
        resolveSafely(true);
        return;
      }

      const onLoad = () => resolveSafely(typeof mapboxgl !== "undefined");
      const onError = () => resolveSafely(false);
      existingScript.addEventListener("load", onLoad, { once: true });
      existingScript.addEventListener("error", onError, { once: true });
      window.setTimeout(() => resolveSafely(typeof mapboxgl !== "undefined"), 5000);
      return;
    }

    const script = document.createElement("script");
    script.src = MAPBOX_SCRIPT_SRC;
    script.async = true;
    script.addEventListener("load", () => resolveSafely(typeof mapboxgl !== "undefined"), { once: true });
    script.addEventListener("error", () => resolveSafely(false), { once: true });
    document.head.appendChild(script);
  });

  return state.mapboxLoadPromise;
}

async function initTripMap() {
  if (!tripMapContainer || state.tripMap) {
    return;
  }

  const mapboxLoaded = await ensureMapboxLoaded();
  if (!mapboxLoaded || typeof mapboxgl === "undefined") {
    setMessage(document.getElementById("tripCreateMessage"), "Mapbox no pudo cargarse. Revisa internet o bloqueadores del navegador.", "error");
    return;
  }

  mapboxgl.accessToken = MAPBOX_ACCESS_TOKEN;

  state.tripMap = new mapboxgl.Map({
    container: "tripMap",
    style: "mapbox://styles/mapbox/streets-v12",
    center: [-76.532, 3.376],
    zoom: 12
  });

  state.tripMap.addControl(new mapboxgl.NavigationControl({ showCompass: true }), "top-right");

  state.tripMap.on("load", () => {
    state.tripMapReady = true;
    state.tripMap.resize();
    updateTripCoordinateSummary();
  });

  state.tripMap.on("click", (event) => {
    if (!state.tripSelectionMode) {
      setMessage(document.getElementById("tripCreateMessage"), "Presiona seleccionar origen o destino antes de tocar el mapa.");
      return;
    }

    setTripPoint(state.tripSelectionMode, event.lngLat.lat, event.lngLat.lng);
  });
}

function getReservationStatusKey(value) {
  const normalized = String(value ?? "").trim().toLowerCase();

  if (normalized === "0" || normalized === "active") return "active";
  if (normalized === "1" || normalized === "cancelled") return "cancelled";
  if (normalized === "2" || normalized === "boarded") return "boarded";

  return "unknown";
}

function formatReservationStatus(value) {
  const key = getReservationStatusKey(value);

  if (key === "active") return "Activo";
  if (key === "cancelled") return "Cancelado";
  if (key === "boarded") return "Abordado";

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

  if (normalized === "0" || normalized === "awaitingdestination") return "awaitingdestination";
  if (normalized === "1" || normalized === "ready") return "ready";
  if (normalized === "2" || normalized === "cancelled") return "cancelled";
  if (normalized === "3" || normalized === "inprogress") return "inprogress";
  if (normalized === "4" || normalized === "finished") return "finished";

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

  const driverProfile = user.driverProfile;

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

  const hasDestination = trip.destinationLatitude != null && trip.destinationLongitude != null;

  return `
    <article class="detail-card">
      <div class="detail-card__header">
        <div>
          <h4>${escapeHtml(trip.driverName || "Viaje")}</h4>
          <p>${escapeHtml(formatTripKind(trip.kind))}</p>
        </div>
        <span class="status-badge status-badge--${escapeHtml(getTripStatusKey(trip.status))}">
          ${escapeHtml(formatTripStatus(trip.status))}
        </span>
      </div>

      <div class="detail-grid">
        <div><strong>ID:</strong> ${escapeHtml(trip.id || "-")}</div>
        <div><strong>Cupos:</strong> ${escapeHtml(trip.availableSeats ?? "-")}</div>
        <div><strong>Origen:</strong> ${escapeHtml(`${trip.originLatitude}, ${trip.originLongitude}`)}</div>
        <div><strong>Destino:</strong> ${escapeHtml(hasDestination ? `${trip.destinationLatitude}, ${trip.destinationLongitude}` : "Sin destino")}</div>
        <div><strong>Creado:</strong> ${escapeHtml(formatDateTime(trip.createdAt))}</div>
        <div><strong>Actualizado:</strong> ${escapeHtml(trip.updatedAt ? formatDateTime(trip.updatedAt) : "Sin cambios")}</div>
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

  if (section === "trips") {
    loadTripDrivers();
    initTripMap().catch(() => {
      setMessage(document.getElementById("tripCreateMessage"), "No se pudo inicializar el mapa de viajes.", "error");
    });
    if (state.tripMapReady) {
      window.setTimeout(() => state.tripMap.resize(), 0);
    }
  }

  if (section === "reports" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    updateReportView();
    loadAdminData();
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
      description: "Crea y consulta cuentas de estudiantes y choferes." 
    },
    trips: {
      name: "Viajes",
      description: "Crea viajes, consulta su estado y administra destinos." 
    },
    reports: {
      name: "Reportes",
      description: "Explora usuarios, viajes y reservas con filtros instantaneos." 
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
}

function updateReportView() {
  const activeButton = reportViewSwitch?.querySelector(".report-view-btn.active");
  const selectedView = activeButton?.dataset.view || "overview";
  const reportCards = document.querySelectorAll("#section-reports [data-report-view]");

  reportCards.forEach((card) => {
    card.classList.toggle("hidden", card.dataset.reportView !== selectedView);
  });
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

function closeEditModal() {
  state.editContext = null;
  editModalForm.innerHTML = "";
  setMessage(editModalMessage, "");
  editModalOverlay.classList.add("hidden");
  editModalOverlay.setAttribute("aria-hidden", "true");
}

function openEditModal(type, entity) {
  if (!entity) {
    throw new Error("No se encontro el registro seleccionado para editar.");
  }

  state.editContext = { type, id: entity.id };

  const titles = {
    user: "Editar usuario",
    trip: "Editar viaje",
    reservation: "Editar reserva"
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
  if (key === "ready") return "1";
  if (key === "cancelled") return "2";
  if (key === "inprogress") return "3";
  if (key === "finished") return "4";
  return "0";
}

function getReservationStatusValue(status) {
  const key = getReservationStatusKey(status);
  if (key === "cancelled") return "1";
  if (key === "boarded") return "2";
  return "0";
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
    reservation: state.adminData.reservations
  }[type] || [];

  return source.find((item) => String(item.id) === String(id));
}

function getEditFormMarkup(type, entity) {
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
    return `
      <label class="field"><span>ID</span><input type="text" value="${escapeHtml(entity.id || "")}" disabled /></label>
      <label class="field"><span>Nombre del conductor</span><input type="text" name="driverName" value="${escapeHtml(entity.driverName || "")}" required /></label>
      <label class="field"><span>Driver User ID (opcional)</span><input type="text" name="driverUserId" value="${escapeHtml(entity.driverUserId || "")}" /></label>
      <label class="field"><span>Latitud origen</span><input type="number" step="any" name="originLatitude" value="${escapeHtml(entity.originLatitude ?? "")}" required /></label>
      <label class="field"><span>Longitud origen</span><input type="number" step="any" name="originLongitude" value="${escapeHtml(entity.originLongitude ?? "")}" required /></label>
      <label class="field"><span>Latitud destino (opcional)</span><input type="number" step="any" name="destinationLatitude" value="${escapeHtml(entity.destinationLatitude ?? "")}" /></label>
      <label class="field"><span>Longitud destino (opcional)</span><input type="number" step="any" name="destinationLongitude" value="${escapeHtml(entity.destinationLongitude ?? "")}" /></label>
      <label class="field"><span>Cupos disponibles</span><input type="number" min="0" name="availableSeats" value="${escapeHtml(entity.availableSeats ?? 0)}" required /></label>
      <label class="field"><span>Estado</span>
        <select name="status">
          <option value="0" ${getTripStatusValue(entity.status) === "0" ? "selected" : ""}>Esperando destino</option>
          <option value="1" ${getTripStatusValue(entity.status) === "1" ? "selected" : ""}>Listo</option>
          <option value="2" ${getTripStatusValue(entity.status) === "2" ? "selected" : ""}>Cancelado</option>
          <option value="3" ${getTripStatusValue(entity.status) === "3" ? "selected" : ""}>En curso</option>
          <option value="4" ${getTripStatusValue(entity.status) === "4" ? "selected" : ""}>Finalizado</option>
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
        <option value="0" ${getReservationStatusValue(entity.status) === "0" ? "selected" : ""}>Activo</option>
        <option value="1" ${getReservationStatusValue(entity.status) === "1" ? "selected" : ""}>Cancelado</option>
        <option value="2" ${getReservationStatusValue(entity.status) === "2" ? "selected" : ""}>Abordado</option>
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
    if (!isAdmin || state.section !== "reports") {
      return;
    }

    loadAdminData();
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
    const data = await apiFetch("/api/admin/all-data", {
      headers: getAdminHeaders()
    });

    state.adminData = {
      users: data.users || [],
      trips: data.trips || [],
      reservations: data.reservations || []
    };

    renderUsersTable(data.users || []);
    renderTripsTable(data.trips || []);
    renderReservationsTable(data.reservations || []);
    applyAllTableFilters();
    setMessage(
      adminDataMessage,
      `Actualizado ${new Date().toLocaleTimeString()}. Usuarios: ${(data.users || []).length} | Viajes: ${(data.trips || []).length} | Reservas: ${(data.reservations || []).length}`,
      "success"
    );
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
    <tr>
      <td>${escapeHtml(trip.id || "-")}</td>
      <td>${escapeHtml(trip.driverName || "-")}</td>
      <td>${escapeHtml(`${trip.originLatitude}, ${trip.originLongitude}`)}</td>
      <td>${escapeHtml(trip.destinationLatitude != null && trip.destinationLongitude != null ? `${trip.destinationLatitude}, ${trip.destinationLongitude}` : "-")}</td>
      <td>${escapeHtml(formatTripStatus(trip.status))}</td>
      <td>${escapeHtml(trip.availableSeats)}</td>
      <td>
        <div class="action-row">
          <button class="btn tiny secondary admin-edit" data-type="trip" data-id="${escapeHtml(trip.id)}">Editar</button>
          <button class="btn tiny danger admin-delete" data-type="trip" data-id="${escapeHtml(trip.id)}">Eliminar</button>
        </div>
      </td>
    </tr>
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

document.getElementById("logoutBtn").addEventListener("click", () => {
  logout();
});

reportViewSwitch?.addEventListener("click", (event) => {
  const button = event.target.closest(".report-view-btn");
  if (!button) {
    return;
  }

  reportViewSwitch.querySelectorAll(".report-view-btn").forEach((btn) => {
    btn.classList.toggle("active", btn === button);
  });

  updateReportView();
});

editModalCloseBtn?.addEventListener("click", () => {
  closeEditModal();
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

    closeEditModal();
    await loadAdminData();
    setMessage(adminDataMessage, "Registro actualizado correctamente.", "success");
  } catch (error) {
    setMessage(editModalMessage, error.message, "error");
  }
});

document.getElementById("section-reports").addEventListener("click", async (event) => {
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

document.getElementById("registerUserForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const role = document.getElementById("regRole").value;
  const registerMessage = document.getElementById("registerMessage");
  const payload = {
    fullName: document.getElementById("regFullName").value.trim(),
    email: document.getElementById("regEmail").value.trim(),
    password: document.getElementById("regPassword").value,
    phoneNumber: document.getElementById("regPhone").value.trim() || null,
    role
  };

  if (role === "driver") {
    const seatsRaw = document.getElementById("regSeats").value.trim();
    const plate = document.getElementById("regPlate").value.trim().toUpperCase();
    const brand = document.getElementById("regBrand").value.trim();
    const color = document.getElementById("regColor").value.trim();

    const seats = Number(seatsRaw);
    if (!Number.isInteger(seats) || seats < 1 || seats > 12) {
      setMessage(registerMessage, "Para chofer, la cantidad de personas debe estar entre 1 y 12.", "error");
      return;
    }

    if (plate.length < 5) {
      setMessage(registerMessage, "Para chofer, la placa debe tener al menos 5 caracteres.", "error");
      return;
    }

    if (brand.length < 2) {
      setMessage(registerMessage, "Para chofer, la marca debe tener al menos 2 caracteres.", "error");
      return;
    }

    if (color.length < 2) {
      setMessage(registerMessage, "Para chofer, el color debe tener al menos 2 caracteres.", "error");
      return;
    }

    payload.driverProfile = {
      availableSeats: seats,
      licensePlate: plate,
      vehicleBrand: brand,
      vehicleColor: color
    };
  }

  try {
    const created = await apiFetch("/api/users/register", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    setMessage(registerMessage, `Usuario creado: ${created.email}`, "success");
  } catch (error) {
    setMessage(registerMessage, error.message, "error");
  }
});

document.getElementById("searchUserForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const email = encodeURIComponent(document.getElementById("searchEmail").value.trim());
  const resultElement = document.getElementById("userResult");

  try {
    const user = await apiFetch(`/api/users/email/${email}`);
    resultElement.innerHTML = renderUserDetails(user);
  } catch (error) {
    resultElement.innerHTML = `
      <div class="empty-state empty-state--error">
        <strong>Error</strong>
        <p>${escapeHtml(error.message)}</p>
      </div>
    `;
  }
});

document.getElementById("createTripForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const message = document.getElementById("tripCreateMessage");
  const selectedDriverId = String(tripDriverSelect?.value || "").trim();
  const selectedDriver = state.tripDrivers.find((driver) => String(driver.id) === selectedDriverId);

  if (!selectedDriver) {
    setMessage(message, "Selecciona un conductor registrado para crear el viaje.", "error");
    return;
  }

  if (!state.tripOrigin) {
    setMessage(message, "Selecciona el origen en el mapa antes de crear el viaje.", "error");
    return;
  }

  if (!state.tripDestination) {
    setMessage(message, "Selecciona el destino en el mapa antes de crear el viaje.", "error");
    return;
  }

  const payload = {
    latitude: state.tripOrigin.lat,
    longitude: state.tripOrigin.lng,
    driverName: selectedDriver.fullName
  };
  payload.driverUserId = selectedDriver.id;

  const resultElement = document.getElementById("tripResult");

  try {
    const createdTrip = await apiFetch("/api/trips/origin", {
      method: "POST",
      body: JSON.stringify(payload)
    });

    const trip = await apiFetch(`/api/trips/${createdTrip.id}/destination`, {
      method: "POST",
      body: JSON.stringify({
        latitude: state.tripDestination.lat,
        longitude: state.tripDestination.lng
      })
    });

    setMessage(message, `Viaje creado. ID: ${trip.id}`, "success");
    resultElement.innerHTML = renderTripDetails(trip);
  } catch (error) {
    setMessage(message, error.message, "error");
  }
});

document.getElementById("tripActionForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const tripId = document.getElementById("tripId").value.trim();
  const action = document.getElementById("tripAction").value;
  const resultElement = document.getElementById("tripResult");

  const routeMap = {
    get: { path: `/api/trips/${tripId}`, method: "GET" },
    start: { path: `/api/trips/${tripId}/start`, method: "POST" },
    finish: { path: `/api/trips/${tripId}/finish`, method: "POST" },
    cancel: { path: `/api/trips/${tripId}/cancel`, method: "POST" }
  };

  const route = routeMap[action];

  try {
    const response = await apiFetch(route.path, {
      method: route.method,
      body: route.method === "POST" ? "{}" : undefined
    });

    resultElement.innerHTML = renderTripDetails(response);

    if (action === "get") {
      initTripMap().then(() => {
        renderTripCoordinatesOnMap(response);
      });
    }
  } catch (error) {
    resultElement.innerHTML = `
      <div class="empty-state empty-state--error">
        <strong>Error</strong>
        <p>${escapeHtml(error.message)}</p>
      </div>
    `;
  }
});

document.getElementById("tripDestinationForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const tripId = document.getElementById("destTripId").value.trim();
  const payload = {
    latitude: Number(document.getElementById("destLat").value),
    longitude: Number(document.getElementById("destLng").value)
  };
  const resultElement = document.getElementById("tripResult");

  try {
    const response = await apiFetch(`/api/trips/${tripId}/destination`, {
      method: "POST",
      body: JSON.stringify(payload)
    });

    resultElement.innerHTML = renderTripDetails(response);
  } catch (error) {
    resultElement.innerHTML = `
      <div class="empty-state empty-state--error">
        <strong>Error</strong>
        <p>${escapeHtml(error.message)}</p>
      </div>
    `;
  }
});

document.getElementById("reportForm").addEventListener("submit", async (event) => {
  event.preventDefault();

  const tripId = document.getElementById("reportTripId").value.trim();

  const summary = document.getElementById("reportSummary");
  const activeReservations = document.getElementById("activeReservations");
  const boardedReservations = document.getElementById("boardedReservations");

  try {
    const [active, boarded] = await Promise.all([
      apiFetch(`/api/trips/${tripId}/reservations`),
      apiFetch(`/api/trips/${tripId}/reservations/boarded`)
    ]);

    activeReservations.innerHTML = renderReservationCards(active, "No hay reservas activas para este viaje.");
    boardedReservations.innerHTML = renderReservationCards(boarded, "No hay pasajeros abordados para este viaje.");
    setMessage(
      summary,
      `Reporte cargado. Activas: ${active.length} | Abordadas: ${boarded.length}`,
      "success"
    );
  } catch (error) {
    activeReservations.innerHTML = renderReservationCards([], "No se pudieron cargar las reservas activas.");
    boardedReservations.innerHTML = renderReservationCards([], "No se pudieron cargar los pasajeros abordados.");
    setMessage(summary, `No se pudo cargar el reporte: ${error.message}`, "error");
  }
});

document.getElementById("regRole").addEventListener("change", () => {
  updateDriverFieldsVisibility();
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

tripDriverSelect?.addEventListener("change", () => {
  updateTripDriverSelection();
});

selectOriginBtn?.addEventListener("click", () => {
  setTripSelectionMode("origin");
});

selectDestinationBtn?.addEventListener("click", () => {
  setTripSelectionMode("destination");
});

updateDriverFieldsVisibility();
updateTripCoordinateSummary();
updateTripDriverSelection();
setTripSelectionMode(null);
updateReportView();

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
