const fs = require('fs');
const path = require('path');

const PORT = 5005;
const BASE_URL = `http://localhost:${PORT}`;

// Tests Configuration
const PERF_RUNS = 50;
const STRESS_TIERS = [100, 250, 500, 1000]; // Increasing concurrency tiers to find the breaking point!

async function runPerformanceTest() {
    console.log('\n=========================================');
    console.log(' INICIANDO PRUEBAS DE RENDIMIENTO');
    console.log('=========================================');

    // Test 1: Búsqueda de Viajes
    const tripUrl = `${BASE_URL}/api/trips/match-candidates?referenceLatitude=3.374&referenceLongitude=-76.532`;
    console.log(`P1: Evaluando Búsqueda de Viajes (${PERF_RUNS} peticiones secuenciales)...`);
    const tripTimes = [];
    let tripSuccesses = 0;

    for (let i = 0; i < PERF_RUNS; i++) {
        const start = performance.now();
        try {
            const res = await fetch(tripUrl);
            if (res.ok) tripSuccesses++;
            tripTimes.push(performance.now() - start);
        } catch (err) {
            tripTimes.push(performance.now() - start);
        }
    }

    const tripStats = calculateStats(tripTimes);
    console.log(`Resultados P1: Éxito: ${tripSuccesses}/${PERF_RUNS} | Mín: ${tripStats.min}ms | Máx: ${tripStats.max}ms | Promedio: ${tripStats.avg}ms`);

    // Test 2: Resumen de Calificaciones
    const ratingUrl = `${BASE_URL}/api/users/22222222-2222-2222-2222-222222222222/ratings/summary`;
    console.log(`P2: Evaluando Resumen de Calificaciones (${PERF_RUNS} peticiones secuenciales)...`);
    const ratingTimes = [];
    let ratingSuccesses = 0;

    for (let i = 0; i < PERF_RUNS; i++) {
        const start = performance.now();
        try {
            const res = await fetch(ratingUrl, {
                headers: { 'X-User-Id': '22222222-2222-2222-2222-222222222222' }
            });
            if (res.ok) ratingSuccesses++;
            ratingTimes.push(performance.now() - start);
        } catch (err) {
            ratingTimes.push(performance.now() - start);
        }
    }

    const ratingStats = calculateStats(ratingTimes);
    console.log(`Resultados P2: Éxito: ${ratingSuccesses}/${PERF_RUNS} | Mín: ${ratingStats.min}ms | Máx: ${ratingStats.max}ms | Promedio: ${ratingStats.avg}ms`);

    return {
        tripSearch: { successes: tripSuccesses, total: PERF_RUNS, ...tripStats, slaPassed: tripStats.avg < 200 },
        ratingSummary: { successes: ratingSuccesses, total: PERF_RUNS, ...ratingStats, slaPassed: ratingStats.avg < 200 }
    };
}

async function runStressTest() {
    console.log('\n=========================================');
    console.log(' INICIANDO PRUEBAS DE ESTRÉS (TIERS CONCURRENTES)');
    console.log('=========================================');

    const results = {};

    for (const concurrency of STRESS_TIERS) {
        console.log(`\n--- Evaluando con ${concurrency} usuarios concurrentes ---`);
        
        // S1: Búsqueda de Viajes
        const tripUrl = `${BASE_URL}/api/trips/match-candidates?referenceLatitude=3.374&referenceLongitude=-76.532`;
        const tripPromises = [];
        const startTrip = performance.now();

        for (let i = 0; i < concurrency; i++) {
            tripPromises.push(
                fetch(tripUrl)
                    .then(res => ({ ok: res.ok, status: res.status }))
                    .catch(err => ({ ok: false, error: err.message }))
            );
        }

        const tripResponses = await Promise.all(tripPromises);
        const tripDuration = performance.now() - startTrip;
        const tripSuccesses = tripResponses.filter(r => r.ok).length;
        const tripErrors = tripResponses.filter(r => !r.ok);
        const tripAvgLatency = tripDuration / concurrency;

        console.log(`S1 (Búsqueda de Viajes): Concurrencia: ${concurrency} | Exitosas: ${tripSuccesses}/${concurrency} (${((tripSuccesses/concurrency)*100).toFixed(1)}%) | Latencia Promedio de Ráfaga: ${tripAvgLatency.toFixed(1)}ms`);

        // S2: Zonas Seguras
        const zoneUrl = `${BASE_URL}/api/safe-zones`;
        const zonePromises = [];
        const startZone = performance.now();

        for (let i = 0; i < concurrency; i++) {
            zonePromises.push(
                fetch(zoneUrl)
                    .then(res => ({ ok: res.ok, status: res.status }))
                    .catch(err => ({ ok: false, error: err.message }))
            );
        }

        const zoneResponses = await Promise.all(zonePromises);
        const zoneDuration = performance.now() - startZone;
        const zoneSuccesses = zoneResponses.filter(r => r.ok).length;
        const zoneErrors = zoneResponses.filter(r => !r.ok);
        const zoneAvgLatency = zoneDuration / concurrency;

        console.log(`S2 (Zonas Seguras):     Concurrencia: ${concurrency} | Exitosas: ${zoneSuccesses}/${concurrency} (${((zoneSuccesses/concurrency)*100).toFixed(1)}%) | Latencia Promedio de Ráfaga: ${zoneAvgLatency.toFixed(1)}ms`);

        results[concurrency] = {
            tripSearch: { successes: tripSuccesses, failures: concurrency - tripSuccesses, avgLatency: tripAvgLatency, errorSample: tripErrors.slice(0, 2) },
            safeZones: { successes: zoneSuccesses, failures: concurrency - zoneSuccesses, avgLatency: zoneAvgLatency, errorSample: zoneErrors.slice(0, 2) }
        };
    }

    return results;
}

function runUsabilityAudit() {
    console.log('\n=========================================');
    console.log(' INICIANDO AUDITORÍA DE USABILIDAD Y ACCESIBILIDAD');
    console.log('=========================================');

    const htmlPath = path.join(__dirname, '../../../../Web/index.html');
    const cssPath = path.join(__dirname, '../../../../Web/styles.css');

    const auditResults = { html: [], css: [], summary: { passed: 0, failed: 0 } };

    // HTML Audit
    if (fs.existsSync(htmlPath)) {
        const html = fs.readFileSync(htmlPath, 'utf8');
        console.log('Analizando Web/index.html...');

        // 1. Viewport Meta tag
        const hasViewport = html.includes('name="viewport"') || html.includes("name='viewport'");
        addAuditResult(auditResults.html, 'Viewport Meta Tag', hasViewport, 'El viewport meta tag es obligatorio para responsividad móvil.');

        // 2. Alt tags on images
        const imgTags = html.match(/<img[^>]*>/g) || [];
        let missingAlt = 0;
        imgTags.forEach(tag => {
            if (!tag.includes('alt=')) missingAlt++;
        });
        addAuditResult(auditResults.html, 'Atributos ALT en imágenes', missingAlt === 0, `${imgTags.length} imágenes encontradas, ${missingAlt} no tienen el atributo ALT para lectores de pantalla.`, `Falta ALT en ${missingAlt} de ${imgTags.length} imágenes.`);

        // 3. Label tags on form inputs
        const inputTags = html.match(/<input[^>]*>/g) || [];
        let inputsChecked = 0;
        let inputsLackingLabelOrAria = 0;
        inputTags.forEach(tag => {
            if (tag.includes('type="hidden"') || tag.includes("type='hidden'")) return;
            inputsChecked++;
            const hasId = tag.match(/id=["']([^"']+)["']/);
            if (hasId) {
                const id = hasId[1];
                const hasLabel = html.includes(`for="${id}"`) || html.includes(`for='${id}'`);
                const hasAriaLabel = tag.includes('aria-label') || tag.includes('aria-labelledby');
                if (!hasLabel && !hasAriaLabel) inputsLackingLabelOrAria++;
            } else {
                if (!tag.includes('aria-label')) inputsLackingLabelOrAria++;
            }
        });
        addAuditResult(auditResults.html, 'Etiquetas LABEL o descriptores ARIA en Inputs', inputsLackingLabelOrAria === 0, `Se revisaron ${inputsChecked} campos de entrada. ${inputsLackingLabelOrAria} no tienen label correspondiente ni descriptor ARIA.`, `Falta label/aria en ${inputsLackingLabelOrAria} de ${inputsChecked} inputs.`);

        // 4. Heading Structure H1
        const hasH1 = html.includes('<h1') && html.includes('</h1>');
        addAuditResult(auditResults.html, 'Estructura Semántica - Encabezado H1', hasH1, 'Debe haber un encabezado h1 principal para navegación semántica y SEO.');

        // 5. Button descriptions
        const buttons = html.match(/<button[^>]*>([\s\S]*?)<\/button>/g) || [];
        let descriptiveButtons = 0;
        buttons.forEach(btn => {
            const hasText = btn.replace(/<[^>]*>/g, '').trim().length > 0;
            const hasAria = btn.includes('aria-label') || btn.includes('aria-labelledby');
            if (hasText || hasAria) descriptiveButtons++;
        });
        addAuditResult(auditResults.html, 'Descriptores en Botones', descriptiveButtons === buttons.length, `De ${buttons.length} botones, ${buttons.length - descriptiveButtons} están vacíos y carecen de etiqueta descriptiva aria-label.`);
    } else {
        console.log(`Error: No se encontró el archivo HTML en la ruta: ${htmlPath}`);
    }

    // CSS Audit
    if (fs.existsSync(cssPath)) {
        const css = fs.readFileSync(cssPath, 'utf8');
        console.log('Analizando Web/styles.css...');

        // 1. Media Queries
        const hasMediaQueries = css.includes('@media');
        addAuditResult(auditResults.css, 'Media Queries (@media)', hasMediaQueries, 'El archivo CSS debe usar media queries para definir puntos de quiebre responsivos.');

        // 2. Responsive Units
        const hasResponsiveUnits = css.includes('rem') || css.includes('em') || css.includes('%') || css.includes('vw') || css.includes('vh');
        addAuditResult(auditResults.css, 'Unidades Adaptativas (rem/em/%/vw/vh)', hasResponsiveUnits, 'Se deben priorizar unidades flexibles sobre pixeles fijos.');

        // 3. Touch target sizes
        const hasTouchTargetPadding = css.includes('padding') && (css.includes('margin') || css.includes('gap'));
        addAuditResult(auditResults.css, 'Objetivos de Selección Táctil (touch targets)', hasTouchTargetPadding, 'Los botones e interactivos deben poseer espaciado suficiente para evitar clics erróneos en pantallas táctiles.');

        // 4. Focus states
        const hasFocusStyles = css.includes(':focus');
        addAuditResult(auditResults.css, 'Indicadores de Enfoque Teclado (:focus)', hasFocusStyles, 'Los elementos interactivos deben definir estilos de foco para navegación por teclado (accesibilidad).');
    } else {
        console.log(`Error: No se encontró el archivo CSS en la ruta: ${cssPath}`);
    }

    // Compile Usability Summary
    auditResults.summary.passed = auditResults.html.filter(r => r.passed).length + auditResults.css.filter(r => r.passed).length;
    auditResults.summary.failed = auditResults.html.filter(r => !r.passed).length + auditResults.css.filter(r => !r.passed).length;

    console.log(`Auditoría de Usabilidad Terminada. Pasadas: ${auditResults.summary.passed} | Fallidas: ${auditResults.summary.failed}`);
    return auditResults;
}

function addAuditResult(list, name, passed, successMsg, failMsg = '') {
    const result = { name, passed, detail: passed ? successMsg : (failMsg || successMsg) };
    list.push(result);
    console.log(` - [${passed ? 'PASÓ' : 'FALLÓ'}] ${name}: ${result.detail}`);
}

function calculateStats(times) {
    const min = Math.min(...times);
    const max = Math.max(...times);
    const avg = times.reduce((a, b) => a + b, 0) / times.length;
    const stdDev = Math.sqrt(times.map(t => Math.pow(t - avg, 2)).reduce((a, b) => a + b, 0) / times.length);
    return {
        min: Math.round(min),
        max: Math.round(max),
        avg: Math.round(avg),
        stdDev: Math.round(stdDev)
    };
}

async function main() {
    console.log('Iniciando Suite de Pruebas No Funcionales para CarPooling...');
    
    // Check if server is running
    try {
        await fetch(BASE_URL);
    } catch (err) {
        console.error(`\n[ERROR] El servidor backend de CarPooling no está corriendo en ${BASE_URL}.`);
        console.error('Por favor inicia el backend primero usando `dotnet run` e intenta nuevamente.');
        process.exit(1);
    }

    const perf = await runPerformanceTest();
    const stress = await runStressTest();
    const usability = runUsabilityAudit();

    const results = {
        timestamp: new Date().toISOString(),
        performance: perf,
        stress: stress,
        usability: usability
    };

    const outputPath = path.join(__dirname, '../non_functional_results.json');
    fs.writeFileSync(outputPath, JSON.stringify(results, null, 2));
    console.log(`\nResultados exportados con éxito a: ${outputPath}`);
}

main().catch(err => {
    console.error('Error durante la ejecución del script:', err);
    process.exit(1);
});
