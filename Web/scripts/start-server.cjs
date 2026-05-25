const http = require('http');
const fs = require('fs');
const path = require('path');

const rootDir = path.resolve(__dirname, '..');
const initialPort = Number(process.env.PORT || 5173);

const contentTypes = {
  '.html': 'text/html; charset=utf-8',
  '.js': 'application/javascript; charset=utf-8',
  '.css': 'text/css; charset=utf-8',
  '.json': 'application/json; charset=utf-8',
  '.svg': 'image/svg+xml',
  '.png': 'image/png',
  '.jpg': 'image/jpeg',
  '.jpeg': 'image/jpeg',
  '.ico': 'image/x-icon',
  '.map': 'application/json; charset=utf-8'
};

function sendFile(filePath, response) {
  const extension = path.extname(filePath).toLowerCase();
  const contentType = contentTypes[extension] || 'application/octet-stream';

  fs.readFile(filePath, (error, data) => {
    if (error) {
      response.writeHead(500, { 'Content-Type': 'text/plain; charset=utf-8' });
      response.end('Error reading file.');
      return;
    }

    response.writeHead(200, { 'Content-Type': contentType });
    response.end(data);
  });
}

const server = http.createServer((request, response) => {
  const requestUrl = new URL(request.url, `http://${request.headers.host}`);
  const pathname = decodeURIComponent(requestUrl.pathname);
  const relativePath = pathname === '/' ? '/index.html' : pathname;
  const resolvedPath = path.resolve(rootDir, `.${relativePath}`);

  if (!resolvedPath.startsWith(rootDir)) {
    response.writeHead(403, { 'Content-Type': 'text/plain; charset=utf-8' });
    response.end('Forbidden');
    return;
  }

  fs.stat(resolvedPath, (error, stats) => {
    if (!error && stats.isFile()) {
      sendFile(resolvedPath, response);
      return;
    }

    const fallbackPath = path.join(rootDir, 'index.html');
    sendFile(fallbackPath, response);
  });
});

function listenOnPort(port) {
  server.once('error', (error) => {
    if (error.code === 'EADDRINUSE') {
      listenOnPort(port + 1);
      return;
    }

    throw error;
  });

  server.listen(port, '0.0.0.0', () => {
    console.log(`Static server running at http://localhost:${port}`);
  });
}

listenOnPort(initialPort);