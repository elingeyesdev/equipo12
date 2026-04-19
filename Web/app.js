const allowedAdminEmails = ["admin@univalle.edu", "coordinador@univalle.edu"];

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

    const isAdmin = allowedAdminEmails.includes(email);
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
    resultElement.textContent = toPrettyJson(user);
  } catch (error) {
    resultElement.textContent = `Error: ${error.message}`;
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
    resultElement.textContent = toPrettyJson(trip);
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

    resultElement.textContent = toPrettyJson(response);
  } catch (error) {
    resultElement.textContent = `Error: ${error.message}`;
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

    resultElement.textContent = toPrettyJson(response);
  } catch (error) {
    resultElement.textContent = `Error: ${error.message}`;
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

    activeReservations.textContent = toPrettyJson(active);
    boardedReservations.textContent = toPrettyJson(boarded);
    setMessage(
      summary,
      `Reporte cargado. Activas: ${active.length} | Abordadas: ${boarded.length}`,
      "success"
    );
  } catch (error) {
    activeReservations.textContent = "[]";
    boardedReservations.textContent = "[]";
    setMessage(summary, `No se pudo cargar el reporte: ${error.message}`, "error");
  }
});

if (state.currentUser) {
  startSession(state.currentUser);
} else {
  updateApiStatus(false);
}
