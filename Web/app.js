const ADMIN_ROLE_ID = 3;

const state = {
  apiBaseUrl: localStorage.getItem("cp.apiBaseUrl") || "http://localhost:5005",
  currentUser: JSON.parse(localStorage.getItem("cp.adminUser") || "null"),
  section: "overview"
};

const authShell = document.getElementById("authShell");
const dashboard = document.getElementById("dashboard");
const loginMessage = document.getElementById("loginMessage");
const apiStatus = document.getElementById("apiStatus");
const sectionTitle = document.getElementById("sectionTitle");
const kpiSection = document.getElementById("kpiSection");
const kpiApiBase = document.getElementById("kpiApiBase");
const kpiSession = document.getElementById("kpiSession");
const loggedUserInfo = document.getElementById("loggedUserInfo");
const adminDataMessage = document.getElementById("adminDataMessage");
const adminUsersBody = document.getElementById("adminUsersBody");
const adminTripsBody = document.getElementById("adminTripsBody");
const adminReservationsBody = document.getElementById("adminReservationsBody");

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

function formatReservationStatus(value) {
  const normalized = String(value ?? "").toLowerCase();

  if (normalized === "active") return "Activo";
  if (normalized === "boarded") return "Abordado";
  if (normalized === "cancelled") return "Cancelado";

  return value || "Sin estado";
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
        <span class="status-badge status-badge--${escapeHtml(String(reservation.status || "unknown").toLowerCase())}">
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

function formatTripStatus(value) {
  const normalized = String(value ?? "").toLowerCase();

  if (normalized === "awaitingdestination") return "Esperando destino";
  if (normalized === "ready") return "Listo";
  if (normalized === "inprogress") return "En curso";
  if (normalized === "cancelled") return "Cancelado";
  if (normalized === "finished") return "Finalizado";

  return value || "Sin estado";
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
        <span class="status-badge status-badge--${escapeHtml(String(trip.status || "unknown").toLowerCase())}">
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
  const response = await fetch(`${normalizeUrl(state.apiBaseUrl)}${path}`, {
    headers: {
      "Content-Type": "application/json",
      ...(options.headers || {})
    },
    ...options
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
  sectionTitle.textContent = getSectionName(section);
  kpiSection.textContent = getSectionName(section);

  document.querySelectorAll(".menu-item").forEach((btn) => {
    btn.classList.toggle("active", btn.dataset.section === section);
  });

  document.querySelectorAll("[id^='section-']").forEach((panel) => {
    panel.classList.add("hidden");
  });

  document.getElementById(`section-${section}`).classList.remove("hidden");

  if (section === "reports" && state.currentUser && Number(state.currentUser.roleId) === ADMIN_ROLE_ID) {
    loadAdminData();
  }
}

function getSectionName(section) {
  const map = {
    overview: "Resumen",
    users: "Usuarios",
    trips: "Viajes",
    reports: "Reportes"
  };

  return map[section] || "Resumen";
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
  openSection("overview");
}

function logout() {
  state.currentUser = null;
  localStorage.removeItem("cp.adminUser");

  dashboard.classList.add("hidden");
  authShell.classList.remove("hidden");
  updateApiStatus(false);
  kpiSession.textContent = "Inactiva";
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
  try {
    const data = await apiFetch("/api/admin/all-data", {
      headers: getAdminHeaders()
    });

    renderUsersTable(data.users || []);
    renderTripsTable(data.trips || []);
    renderReservationsTable(data.reservations || []);
    setMessage(
      adminDataMessage,
      `Datos cargados. Usuarios: ${(data.users || []).length} | Viajes: ${(data.trips || []).length} | Reservas: ${(data.reservations || []).length}`,
      "success"
    );
  } catch (error) {
    renderUsersTable([]);
    renderTripsTable([]);
    renderReservationsTable([]);
    setMessage(adminDataMessage, `No se pudieron cargar los datos: ${error.message}`, "error");
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
    adminTripsBody.innerHTML = '<tr><td colspan="6">Sin viajes.</td></tr>';
    return;
  }

  adminTripsBody.innerHTML = trips.map((trip) => `
    <tr>
      <td>${escapeHtml(trip.driverName || "-")}</td>
      <td>${escapeHtml(`${trip.originLatitude}, ${trip.originLongitude}`)}</td>
      <td>${escapeHtml(trip.destinationLatitude != null && trip.destinationLongitude != null ? `${trip.destinationLatitude}, ${trip.destinationLongitude}` : "-")}</td>
      <td>${escapeHtml(trip.status)}</td>
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
      <td>${escapeHtml(reservation.status)}</td>
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
  if (type === "user") {
    const fullName = prompt("Nombre completo:");
    if (fullName === null) return;
    const email = prompt("Email institucional (@univalle.edu):");
    if (email === null) return;
    const phoneNumber = prompt("Telefono (opcional, vacio para null):", "");
    if (phoneNumber === null) return;
    const roleId = prompt("RoleId (1=student, 2=driver, 3=admin):", "1");
    if (roleId === null) return;

    await apiFetch(`/api/admin/users/${id}`, {
      method: "PUT",
      headers: getAdminHeaders(),
      body: JSON.stringify({
        fullName: fullName.trim(),
        email: email.trim().toLowerCase(),
        phoneNumber: phoneNumber.trim() || null,
        roleId: Number(roleId)
      })
    });
  }

  if (type === "trip") {
    const driverName = prompt("Nombre del conductor:");
    if (driverName === null) return;
    const originLatitude = prompt("Latitud origen:");
    if (originLatitude === null) return;
    const originLongitude = prompt("Longitud origen:");
    if (originLongitude === null) return;
    const destinationLatitude = prompt("Latitud destino (vacio para null):", "");
    if (destinationLatitude === null) return;
    const destinationLongitude = prompt("Longitud destino (vacio para null):", "");
    if (destinationLongitude === null) return;
    const availableSeats = prompt("Cupos disponibles:", "0");
    if (availableSeats === null) return;
    const status = prompt("Estado (0..4 | active/ready/cancelled/inprogress/finished):", "0");
    if (status === null) return;
    const driverUserId = prompt("DriverUserId (GUID opcional):", "");
    if (driverUserId === null) return;

    await apiFetch(`/api/admin/trips/${id}`, {
      method: "PUT",
      headers: getAdminHeaders(),
      body: JSON.stringify({
        driverName: driverName.trim(),
        originLatitude: Number(originLatitude),
        originLongitude: Number(originLongitude),
        destinationLatitude: destinationLatitude.trim() ? Number(destinationLatitude) : null,
        destinationLongitude: destinationLongitude.trim() ? Number(destinationLongitude) : null,
        availableSeats: Number(availableSeats),
        status: status.trim(),
        driverUserId: driverUserId.trim() || null
      })
    });
  }

  if (type === "reservation") {
    const tripId = prompt("TripId (GUID):");
    if (tripId === null) return;
    const passengerName = prompt("Nombre del pasajero:");
    if (passengerName === null) return;
    const status = prompt("Estado (0=active,1=cancelled,2=boarded):", "0");
    if (status === null) return;

    await apiFetch(`/api/admin/reservations/${id}`, {
      method: "PUT",
      headers: getAdminHeaders(),
      body: JSON.stringify({
        tripId: tripId.trim(),
        passengerName: passengerName.trim(),
        status: status.trim()
      })
    });
  }

  await loadAdminData();
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

document.getElementById("logoutBtn").addEventListener("click", () => {
  logout();
});

document.getElementById("loadAdminDataBtn").addEventListener("click", async () => {
  await loadAdminData();
});

document.getElementById("section-reports").addEventListener("click", async (event) => {
  const editBtn = event.target.closest(".admin-edit");
  if (editBtn) {
    try {
      await editAdminEntity(editBtn.dataset.type, editBtn.dataset.id);
      setMessage(adminDataMessage, "Registro actualizado correctamente.", "success");
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
  const payload = {
    fullName: document.getElementById("regFullName").value.trim(),
    email: document.getElementById("regEmail").value.trim(),
    password: document.getElementById("regPassword").value,
    phoneNumber: document.getElementById("regPhone").value.trim() || null,
    role
  };

  if (role === "driver") {
    payload.driverProfile = {
      availableSeats: 3,
      licensePlate: "TEMP-000",
      vehicleBrand: "Por definir",
      vehicleColor: "Por definir"
    };
  }

  const registerMessage = document.getElementById("registerMessage");

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

  const payload = {
    latitude: Number(document.getElementById("tripOriginLat").value),
    longitude: Number(document.getElementById("tripOriginLng").value),
    driverName: document.getElementById("tripDriverName").value.trim()
  };

  const driverUserId = document.getElementById("tripDriverUserId").value.trim();
  if (driverUserId) {
    payload.driverUserId = driverUserId;
  }

  const message = document.getElementById("tripCreateMessage");
  const resultElement = document.getElementById("tripResult");

  try {
    const trip = await apiFetch("/api/trips/origin", {
      method: "POST",
      body: JSON.stringify(payload)
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
