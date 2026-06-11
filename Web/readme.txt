Panel Administrativo Web - CarPooling

Este modulo implementa:
- Layout principal (sidebar + topbar + dashboard)
- Navegacion por secciones: Resumen, Usuarios, Viajes, Reservas, Soporte, Zonas seguras y Ajustes
- Login administrativo
- Conexion a endpoints basicos del backend

Archivos principales:
- index.html
- styles.css
- app.js

Como usar:
1) Ejecuta el backend ASP.NET en modo Development (por defecto: http://localhost:5005).
	- Si lo levantas en http://localhost:5000, el panel lo detecta automaticamente.
2) En la carpeta Web instala dependencias y levanta el servidor local:
	- npm install
	- npm start
3) Abre el panel en http://127.0.0.1:5173
4) Inicia sesion con un usuario valido del backend y uno de los correos admin permitidos en app.js:
	- admin@univalle.edu
	- coordinador@univalle.edu
5) Usa las secciones del panel para:
	- Registrar y buscar usuarios
	- Crear y gestionar viajes
	- Consultar reportes de reservas por viaje
	- Crear, editar y eliminar zonas seguras en el mapa (visibles en la app movil)

Notas:
- El panel usa localStorage para recordar sesion y URL base de API.
- Si cambias el puerto del backend, actualiza la URL base al iniciar sesion.
- El panel intenta primero el backend guardado y, si es local, prueba automaticamente 5005 y 5000.
- Se habilito CORS en Program.cs para facilitar pruebas del frontend.
